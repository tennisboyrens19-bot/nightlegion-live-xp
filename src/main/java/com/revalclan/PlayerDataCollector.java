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
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;

/**
 * Collects ALL player data by coordinating various managers
 */
@Slf4j
@Singleton
public class PlayerDataCollector {
	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;
	private long bankAccountHash = -1;
	private final Set<Integer> bankOwnedItems = new HashSet<>();

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
		captureBankItems();
		if (bankAccountHash == client.getAccountHash() && !bankOwnedItems.isEmpty()) {
			data.put("ownedItemIds", new ArrayList<>(bankOwnedItems));
		}
		attachFingerprint(data);
		return data;
	}

	public void captureBankItems() {
		if (!Collections.disjoint(Worlds.flagNames(client),
			Arrays.asList("SEASONAL", "DEADMAN", "TOURNAMENT", "BETA_WORLD"))) return;
		if (bankAccountHash != client.getAccountHash()) {
			bankOwnedItems.clear();
			bankAccountHash = client.getAccountHash();
		}
		// Only an open, real bank is ownership evidence; cached containers can
		// outlive the interface. Never turn a bank placeholder into an owned item.
		Widget bank = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
		ItemContainer container = client.getItemContainer(95);
		if (bank != null && !bank.isHidden() && container != null) {
			for (Item item : container.getItems()) {
				if (item.getId() <= 0 || item.getQuantity() <= 0) continue;
				ItemComposition composition = itemManager.getItemComposition(item.getId());
				if (composition.getPlaceholderTemplateId() != -1) continue;
				bankOwnedItems.add(itemManager.canonicalize(item.getId()));
			}
		}
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
