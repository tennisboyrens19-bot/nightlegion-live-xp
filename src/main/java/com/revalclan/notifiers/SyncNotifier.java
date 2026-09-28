package com.revalclan.notifiers;

import com.revalclan.PlayerDataCollector;
import com.revalclan.util.SyncStateManager;
import com.google.gson.JsonObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Full account sync — manual button press or server-requested fingerprint
 * repair. ALWAYS sends the full state.
 */
@Singleton
public class SyncNotifier extends BaseNotifier {
	public enum SyncResult { SUCCESS, INCOMPLETE, FAILED }
	@Inject
	private PlayerDataCollector dataCollector;

	@Inject
	private SyncStateManager syncStateManager;

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	protected String getEventType() {
		return "SYNC";
	}

	/**
	 * Trigger a full account sync.
	 * Collects all player data (collection log, quests, diaries, combat achievements, etc.)
	 * and sends it to the webhook.
	 */
	public void triggerSync() {
		triggerSync(null);
	}

	/** Completion runs on the HTTP thread; callers must marshal UI work to the client thread. */
	public void triggerSync(Consumer<SyncResult> onComplete) {
		Map<String, Object> data = dataCollector.collectSyncData();
		Consumer<JsonObject> ack = syncStateManager.ackHandler(client.getAccountHash());
		Object fingerprint = data.get("syncFingerprint");
		sendNotification(data, response -> {
			SyncResult result = resultFor(response, fingerprint);
			if (result != SyncResult.FAILED && matchesFingerprint(response, fingerprint)) ack.accept(response);
			if (onComplete != null) onComplete.accept(result);
		}, error -> {
			if (onComplete != null) onComplete.accept(SyncResult.FAILED);
		});
	}

	private static SyncResult resultFor(JsonObject response, Object fingerprint) {
		try {
			boolean accepted = isTrue(response, "accepted");
			boolean legacyOk = isTrue(response, "ok");
			if ((!accepted && !legacyOk) || (response.has("accepted") && !accepted)
				|| (response.has("ok") && !legacyOk)
				|| !response.has("status") || !"success".equals(response.get("status").getAsString())) {
				return SyncResult.FAILED;
			}
			JsonObject sync = response.has("sync") && response.get("sync").isJsonObject()
				? response.getAsJsonObject("sync") : null;
			if (!matchesFingerprint(response, fingerprint) || sync == null || !sync.has("stale") || sync.get("stale").getAsBoolean()
				|| (response.has("scoringWarnings") && (!response.get("scoringWarnings").isJsonArray()
					|| response.getAsJsonArray("scoringWarnings").size() > 0))) return SyncResult.INCOMPLETE;
			return SyncResult.SUCCESS;
		} catch (RuntimeException e) {
			return SyncResult.INCOMPLETE;
		}
	}

	private static boolean isTrue(JsonObject response, String key) {
		return response.has(key) && response.get(key).isJsonPrimitive()
			&& response.getAsJsonPrimitive(key).isBoolean() && response.get(key).getAsBoolean();
	}

	private static boolean matchesFingerprint(JsonObject response, Object fingerprint) {
		try {
			return fingerprint != null && response.has("sync") && response.get("sync").isJsonObject()
				&& response.getAsJsonObject("sync").has("fingerprint")
				&& fingerprint.equals(response.getAsJsonObject("sync").get("fingerprint").getAsString());
		} catch (RuntimeException e) {
			return false;
		}
	}
}
