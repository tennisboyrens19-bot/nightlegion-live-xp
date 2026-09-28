"""Exact acknowledgement/account-boundary fixes; every old hunk must match once."""

FIXES = {
    'src/main/java/com/revalclan/RevalClanPlugin.java': 'Consume fingerprint-repair requests only for the active account.',
    'src/main/java/com/revalclan/collectionlog/CollectionLogSyncButton.java': 'Report acknowledged sync results and discard pending work/callbacks across account boundaries.',
    'src/main/java/com/revalclan/notifiers/BaseNotifier.java': 'Propagate clan-gate rejection to the manual sync completion callback.',
    'src/main/java/com/revalclan/notifiers/SyncNotifier.java': 'Classify server acknowledgement as successful, incomplete or failed before reporting completion.',
    'src/main/java/com/revalclan/util/SyncStateManager.java': 'Bind a queued fingerprint-repair request to its originating account.',
    'src/main/java/com/revalclan/util/WebhookService.java': 'Propagate transport, HTTP and malformed-acknowledgement failures to the optional sync callback.',
}


def adapt_sync(path, source, one):
    if path == 'src/main/java/com/revalclan/RevalClanPlugin.java':
        source = one(source, r'''		// here (not invokeLater from the ack) because a stale ack can arrive on a
		// LOGOUT response: GameTick only fires while logged in, so the repair
		// naturally waits for the next login.
		if (syncStateManager.consumeFullSyncRequest()) {
			syncNotifier.triggerSync();
		}
	}
''', r'''		// here (not invokeLater from the ack) because a stale ack can arrive on a
		// LOGOUT response: GameTick only fires while logged in, so the repair
		// naturally waits for the next login.
		if (syncStateManager.consumeFullSyncRequest(client.getAccountHash())) {
			syncNotifier.triggerSync();
		}
	}
''')
    if path == 'src/main/java/com/revalclan/collectionlog/CollectionLogSyncButton.java':
        source = one(source, r'''import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.VarbitID;
''', r'''import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.VarbitID;
''')
        source = one(source, r'''import net.runelite.api.widgets.WidgetType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
''', r'''import net.runelite.api.widgets.WidgetType;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
''')
        source = one(source, r'''	private Client client;

	@Inject
	private EventBus eventBus;

	@Inject
''', r'''	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private EventBus eventBus;

	@Inject
''')
        source = one(source, r'''	private int baseMenuHeight = -1;
	private int lastAttemptedSync = -1;
	private int pendingSyncTick = -1;

	public void startUp() {
		eventBus.register(this);
	}

	public void shutDown() {
		eventBus.unregister(this);
	}

	@Subscribe
''', r'''	private int baseMenuHeight = -1;
	private int lastAttemptedSync = -1;
	private int pendingSyncTick = -1;
	private long pendingSyncAccountHash;
	private int syncGeneration;

	public void startUp() {
		resetPendingSync();
		eventBus.register(this);
	}

	public void shutDown() {
		resetPendingSync();
		eventBus.unregister(this);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGIN_SCREEN) resetPendingSync();
	}

	private void resetPendingSync() {
		pendingSyncTick = -1;
		lastAttemptedSync = -1;
		baseMenuHeight = -1;
		syncGeneration++;
	}

	@Subscribe
''')
        source = one(source, r'''
	private void scheduleSync() {
		pendingSyncTick = client.getTickCount() + SYNC_DELAY_TICKS;
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (pendingSyncTick != -1 && client.getTickCount() >= pendingSyncTick) {
			pendingSyncTick = -1;
			performSync();
''', r'''
	private void scheduleSync() {
		pendingSyncTick = client.getTickCount() + SYNC_DELAY_TICKS;
		pendingSyncAccountHash = client.getAccountHash();
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		if (pendingSyncTick != -1 && (client.getGameState() != GameState.LOGGED_IN
			|| pendingSyncAccountHash != client.getAccountHash())) {
			resetPendingSync();
			return;
		}
		if (pendingSyncTick != -1 && client.getTickCount() >= pendingSyncTick) {
			pendingSyncTick = -1;
			performSync();
''')
        source = one(source, r'''	}

	private void performSync() {
		try {
			syncNotifier.triggerSync();
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
				"NightLegion: Synced account data successfully!", "");
		} catch (Exception e) {
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "NightLegion: Failed to sync. Please try again.", "");
		}
''', r'''	}

	private void performSync() {
		final int generation = ++syncGeneration;
		final long accountHash = client.getAccountHash();
		try {
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
				"NightLegion: Sending account data...", "");
			syncNotifier.triggerSync(result -> clientThread.invokeLater((Runnable) () -> {
				if (generation != syncGeneration || client.getGameState() != GameState.LOGGED_IN
					|| accountHash != client.getAccountHash()) return;
				String message;
				switch (result) {
					case SUCCESS: message = "NightLegion: Synced account data successfully!"; break;
					case INCOMPLETE: message = "NightLegion: Sync received, but some data or scoring is incomplete. Please refresh your profile and try again."; break;
					default: message = "NightLegion: Failed to sync. Please try again.";
				}
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, "");
			}));
		} catch (Exception e) {
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "NightLegion: Failed to sync. Please try again.", "");
		}
''')
    if path == 'src/main/java/com/revalclan/notifiers/BaseNotifier.java':
        source = one(source, r'''	 */
	protected void sendNotification(Map<String, Object> data, Consumer<JsonObject> onResponse) {
		sendNotification(getEventType(), data, onResponse);
	}

	/** The one send primitive behind the two overloads above. */
''', r'''	 */
	protected void sendNotification(Map<String, Object> data, Consumer<JsonObject> onResponse) {
		sendNotification(getEventType(), data, onResponse);
	}

	protected void sendNotification(Map<String, Object> data, Consumer<JsonObject> onResponse,
		Consumer<Exception> onFailure) {
		if (!passesClanCheck()) {
			onFailure.accept(new IllegalStateException("Clan membership not confirmed"));
			return;
		}
		addEventMetadata(getEventType(), data, true);
		webhookService.sendDataAsync(data, null, onResponse, onFailure);
	}

	/** The one send primitive behind the two overloads above. */
''')
    if path == 'src/main/java/com/revalclan/notifiers/SyncNotifier.java':
        source = one(source, r'''
import com.revalclan.PlayerDataCollector;
import com.revalclan.util.SyncStateManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Full account sync — manual button press or server-requested fingerprint
''', r'''
import com.revalclan.PlayerDataCollector;
import com.revalclan.util.SyncStateManager;
import com.google.gson.JsonObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Full account sync — manual button press or server-requested fingerprint
''')
        source = one(source, r''' */
@Singleton
public class SyncNotifier extends BaseNotifier {
	@Inject
	private PlayerDataCollector dataCollector;

''', r''' */
@Singleton
public class SyncNotifier extends BaseNotifier {
	public enum SyncResult { SUCCESS, INCOMPLETE, FAILED }
	@Inject
	private PlayerDataCollector dataCollector;

''')
        source = one(source, r'''	 * and sends it to the webhook.
	 */
	public void triggerSync() {
		Map<String, Object> data = dataCollector.collectSyncData();
		sendNotification(data, syncStateManager.ackHandler(client.getAccountHash()));
	}
}
''', r'''	 * and sends it to the webhook.
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
			if (!response.has("ok") || !response.get("ok").getAsBoolean()
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
''')
    if path == 'src/main/java/com/revalclan/util/SyncStateManager.java':
        source = one(source, r'''import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
''', r'''import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
''')
        source = one(source, r'''	@Inject private ConfigManager configManager;

	/** Set from the webhook response thread; consumed on the game-tick thread */
	private final AtomicBoolean fullSyncRequested = new AtomicBoolean(false);

	/** Canonical state fingerprint; null when the data is too incomplete to hash safely. */
	@SuppressWarnings("unchecked")
''', r'''	@Inject private ConfigManager configManager;

	/** Set from the webhook response thread; consumed on the game-tick thread */
	private final Set<Long> fullSyncRequested = ConcurrentHashMap.newKeySet();

	/** Canonical state fingerprint; null when the data is too incomplete to hash safely. */
	@SuppressWarnings("unchecked")
''')
        source = one(source, r'''			boolean stale = sync.has("stale") && !sync.get("stale").isJsonNull() && sync.get("stale").getAsBoolean();
			if (stale) {
				clearAckedFingerprint(accountHash);
				fullSyncRequested.set(true);
				log.info("Sync fingerprint stale — full sync requested");
				return;
			}
''', r'''			boolean stale = sync.has("stale") && !sync.get("stale").isJsonNull() && sync.get("stale").getAsBoolean();
			if (stale) {
				clearAckedFingerprint(accountHash);
				fullSyncRequested.add(accountHash);
				log.info("Sync fingerprint stale — full sync requested");
				return;
			}
''')
        source = one(source, r'''
	/** Consumed from the game-tick loop */
	public boolean consumeFullSyncRequest() {
		return fullSyncRequested.getAndSet(false);
	}

	private static String sha256Hex(String input) throws Exception {
''', r'''
	/** Consumed from the game-tick loop */
	public boolean consumeFullSyncRequest() {
		for (Long accountHash : fullSyncRequested) {
			if (fullSyncRequested.remove(accountHash)) return true;
		}
		return false;
	}

	/** A late acknowledgement for one account must never request a sync for another. */
	public boolean consumeFullSyncRequest(long accountHash) {
		return fullSyncRequested.remove(accountHash);
	}

	private static String sha256Hex(String input) throws Exception {
''')
    if path == 'src/main/java/com/revalclan/util/WebhookService.java':
        source = one(source, r'''	 * @param onResponse Optional consumer for the parsed JSON response body
	 */
	public void sendDataAsync(Map<String, Object> data, byte[] screenshotJpeg, Consumer<JsonObject> onResponse) {
		try {
			byte[] payload = gzip(gson.toJson(data).getBytes(StandardCharsets.UTF_8));

''', r'''	 * @param onResponse Optional consumer for the parsed JSON response body
	 */
	public void sendDataAsync(Map<String, Object> data, byte[] screenshotJpeg, Consumer<JsonObject> onResponse) {
		sendDataAsync(data, screenshotJpeg, onResponse, null);
	}

	public void sendDataAsync(Map<String, Object> data, byte[] screenshotJpeg,
		Consumer<JsonObject> onResponse, Consumer<Exception> onFailure) {
		try {
			byte[] payload = gzip(gson.toJson(data).getBytes(StandardCharsets.UTF_8));

''')
        source = one(source, r'''				@Override
				public void onFailure(Call call, IOException e) {
					log.error("Failed to send data to webhook: {}", e.getMessage());
				}

				@Override
''', r'''				@Override
				public void onFailure(Call call, IOException e) {
					log.error("Failed to send data to webhook: {}", e.getMessage());
					reportFailure(onFailure, e);
				}

				@Override
''')
        source = one(source, r'''					try {
						if (!response.isSuccessful()) {
							log.warn("Webhook returned non-successful status: {}", response.code());
							return;
						}
						if (onResponse == null || response.body() == null) return;
						JsonObject parsed = parseJsonOrNull(response);
						if (parsed == null) return;
						try {
							onResponse.accept(parsed);
						} catch (Exception e) {
''', r'''					try {
						if (!response.isSuccessful()) {
							log.warn("Webhook returned non-successful status: {}", response.code());
							reportFailure(onFailure, new IOException("Webhook HTTP " + response.code()));
							return;
						}
						if (onResponse == null) return;
						if (response.body() == null) {
							reportFailure(onFailure, new IOException("Empty webhook acknowledgement"));
							return;
						}
						JsonObject parsed = parseJsonOrNull(response);
						if (parsed == null) {
							reportFailure(onFailure, new IOException("Invalid webhook acknowledgement"));
							return;
						}
						try {
							onResponse.accept(parsed);
						} catch (Exception e) {
''')
        source = one(source, r'''			});
		} catch (IOException e) {
			log.error("Failed to prepare webhook data: {}", e.getMessage());
		} catch (Exception e) {
			log.error("Unexpected error preparing webhook", e);
		}
	}

''', r'''			});
		} catch (IOException e) {
			log.error("Failed to prepare webhook data: {}", e.getMessage());
			reportFailure(onFailure, e);
		} catch (Exception e) {
			log.error("Unexpected error preparing webhook", e);
			reportFailure(onFailure, e);
		}
	}

	private static void reportFailure(Consumer<Exception> onFailure, Exception error) {
		if (onFailure == null) return;
		try {
			onFailure.accept(error);
		} catch (Exception e) {
			log.warn("Webhook failure handler failed: {}", e.getMessage());
		}
	}

''')
    return source
