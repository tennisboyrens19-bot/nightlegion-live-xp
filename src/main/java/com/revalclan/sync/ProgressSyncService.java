package com.revalclan.sync;

import com.revalclan.PlayerDataCollector;
import com.revalclan.RevalClanConfig;
import com.revalclan.util.SyncStateManager;
import com.revalclan.util.WebhookService;
import com.revalclan.util.Worlds;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;

/** Reads already available state; never opens bank, log or any game UI. */
@Singleton
public class ProgressSyncService {
    @Inject private Client client;
    @Inject private PlayerDataCollector collector;
    @Inject private RevalClanConfig config;
    @Inject private WebhookService webhook;
    @Inject private SyncStateManager syncState;
    @Inject private MilestoneEvidence evidence;
    private final SyncSchedule schedule = new SyncSchedule();
    private long account = -1;
    private final java.util.concurrent.atomic.AtomicReference<Map<String, Object>> pending = new java.util.concurrent.atomic.AtomicReference<>();
    private long observations;

    private long now() { return System.nanoTime() / 1_000_000; }
    public void reset() {
        schedule.reset(now());
        pending.set(null);
        account = -1;
        evidence.reset();
    }
    public void request() { schedule.request(); }
    /** Called only on RuneLite's client thread after clan validation. */
    public void onTick(boolean inClan) {
        List<String> flags = Worlds.flagNames(client);
        if (!inClan || client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null
            || config.personalLinkToken() == null || config.personalLinkToken().trim().isEmpty()
            || flags.stream().anyMatch(f -> f.equals("SEASONAL") || f.equals("DEADMAN")
                || f.equals("TOURNAMENT") || f.equals("BETA_WORLD"))) return;
        if (account != client.getAccountHash()) {
            reset(); account = client.getAccountHash();
        }
        if (observations != webhook.getObservationVersion()) {
            observations = webhook.getObservationVersion();
            schedule.request();
        }
        long ticket = schedule.begin(now());
        if (ticket < 0) return;
        final long sentAccount = account;
        try {
            Map<String, Object> data = pending.get();
            if (data == null) {
                data = collector.collectSyncData();
                data.put("eventType", "PROGRESS");
                data.put("eventTimestamp", System.currentTimeMillis());
                data.put("observationId", UUID.randomUUID().toString());
                data.put("worldFlags", flags);
                data.put("accountHash", sentAccount);
                pending.set(data);
            }
            final Map<String, Object> sent = data;
            webhook.sendDataAsync(sent, response -> {
                if (!schedule.isCurrent(ticket)) return;
                if (schedule.finish(ticket, true, now())) {
                    pending.compareAndSet(sent, null);
                    syncState.ackHandler(sentAccount).accept(response);
                }
            }, error -> schedule.finish(ticket, false, now()));
        } catch (RuntimeException error) {
            schedule.finish(ticket, false, now());
        }
    }
}
