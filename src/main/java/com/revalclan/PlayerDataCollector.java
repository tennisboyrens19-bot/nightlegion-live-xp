package com.revalclan;

import com.revalclan.collectionlog.CollectionLogManager;
import com.revalclan.combatachievements.CombatAchievementManager;
import com.revalclan.diaries.AchievementDiaryManager;
import com.revalclan.pbs.ClogPersonalBestCapture;
import com.revalclan.pbs.PersonalBestManager;
import com.revalclan.player.PlayerManager;
import com.revalclan.quests.QuestManager;
import com.revalclan.util.SyncStateManager;
import com.revalclan.util.Worlds;
import com.revalclan.util.ClanRanks;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarClientID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Collects ALL player data by coordinating various managers
 */
@Slf4j
@Singleton
public class PlayerDataCollector {
	@Inject
	private Client client;

	@Inject
	private PlayerManager playerManager;

	@Inject
	private QuestManager questManager;

	@Inject
	private AchievementDiaryManager achievementDiaryManager;

	@Inject
	private CombatAchievementManager combatAchievementManager;

	@Inject
	private CollectionLogManager collectionLogManager;

	@Inject
	private PersonalBestManager personalBestManager;

	@Inject
	private ClogPersonalBestCapture clogPersonalBestCapture;

	@Inject
	private SyncStateManager syncStateManager;

	/**
	 * Full payload for SYNC — the only path that carries the collection log.
	 */
	public Map<String, Object> collectSyncData() {
		Map<String, Object> data = collectFullState();
		data.put("collectionLog", collectionLogManager.sync());
		attachFingerprint(data);
		return data;
	}

	/**
	 * LOGIN/LOGOUT payload: slim (player + fingerprint) when unchanged since
	 * the last acked fingerprint, full otherwise. Never carries the collection
	 * log — no real scan exists at boundaries.
	 */
	public Map<String, Object> collectBoundaryData() {
		Map<String, Object> data = collectFullState();

		String fingerprint = attachFingerprint(data);
		if (fingerprint != null) {
			String acked = syncStateManager.getAckedFingerprint(client.getAccountHash());
			if (fingerprint.equals(acked)) {
				Map<String, Object> slim = new HashMap<>();
				slim.put("player", data.get("player"));
				slim.put("syncFingerprint", fingerprint);
				if (data.containsKey("currentClanRank")) slim.put("currentClanRank", data.get("currentClanRank"));
				if (data.containsKey("clanRoster")) slim.put("clanRoster", data.get("clanRoster"));
				data = slim;
			}
		}

		// Jagex's own "Time played" (minutes): a server snapshot the client receives at
		// every login/hop, independent of the summary tab's display toggle. The backend
		// reads it on LOGIN as calibration for the sessions it stores.
		int playtime = client.getVarcIntValue(VarClientID.ACCOUNT_SUMMARY_PLAYTIME);
		if (playtime > 0) data.put("playtimeMinutes", playtime);

		return data;
	}

	private Map<String, Object> collectFullState() {
		Map<String, Object> data = new HashMap<>();
		data.put("player", playerManager.sync());
		data.put("quests", questManager.sync());
		data.put("achievementDiaries", achievementDiaryManager.sync());
		data.put("combatAchievements", combatAchievementManager.sync());
		data.put("personalBests", personalBestManager.sync());
		data.put("clogPersonalBests", clogPersonalBestCapture.sync());
		ClanChannel clan = client.getClanChannel();
		String localName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
		if (clan != null && localName != null) {
			ClanChannelMember localMember = clan.findMember(localName);
			if (localMember != null) data.put("currentClanRank", localMember.getRank().toString());
			if (ClanRanks.isDeputyOwnerPlus(client)) {
				List<Map<String, Object>> roster = new ArrayList<>();
				for (ClanChannelMember member : clan.getMembers()) {
					if (member == null || member.getName() == null || roster.size() >= 500) continue;
					Map<String, Object> row = new HashMap<>();
					row.put("rsn", member.getName());
					row.put("rank", member.getRank().toString());
					roster.add(row);
				}
				data.put("clanRoster", roster);
			}
		}
		return data;
	}

	/**
	 * Attach the state fingerprint (null when skipped). Seasonal worlds are
	 * skipped — leagues state is a different character.
	 */
	private String attachFingerprint(Map<String, Object> data) {
		if (Worlds.isSeasonal(client)) return null;
		String fingerprint = syncStateManager.computeFingerprint(data);
		if (fingerprint != null) {
			data.put("syncFingerprint", fingerprint);
		}
		return fingerprint;
	}
}
