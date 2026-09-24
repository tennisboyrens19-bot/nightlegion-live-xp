package com.revalclan.collectionlog;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.revalclan.PlayerDataCollector;
import com.revalclan.notifiers.SyncNotifier;
import com.revalclan.util.ClanMembership;
import com.revalclan.util.SyncStateManager;
import com.revalclan.util.WebhookService;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises the actual button -> notifier -> gzipped HTTP -> acknowledgement path. */
public class SyncAcknowledgementTest {
    private static final String SUCCESS = "{\"ok\":true,\"status\":\"success\",\"sync\":{\"fingerprint\":\"state-a\",\"stale\":false},\"scoringWarnings\":[]}";
    private MockWebServer server;
    private OkHttpClient http;
    private Client client;
    private ClientThread clientThread;
    private PlayerDataCollector collector;
    private ConfigManager config;
    private SyncStateManager state;
    private CollectionLogSyncButton button;
    private final List<String> messages = new CopyOnWriteArrayList<>();
    private final AtomicReference<CountDownLatch> completed = new AtomicReference<>();

    @Before public void setup() throws Exception {
        server = new MockWebServer();
        server.start();
        http = new OkHttpClient.Builder().readTimeout(500, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false).addInterceptor(chain -> chain.proceed(
                chain.request().newBuilder().url(server.url(chain.request().url().encodedPath())).build())).build();
        client = mock(Client.class);
        when(client.getAccountHash()).thenReturn(11L);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
        when(client.getTickCount()).thenReturn(10);
        doAnswer(call -> {
            String message = call.getArgument(2);
            messages.add(message);
            if (message.contains("successfully") || message.contains("Failed to sync") || message.contains("incomplete")) {
                completed.get().countDown();
            }
            return null;
        }).when(client).addChatMessage(any(), anyString(), anyString(), anyString());
        clientThread = mock(ClientThread.class);
        doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; })
            .when(clientThread).invokeLater(any(Runnable.class));
        collector = mock(PlayerDataCollector.class);
        when(collector.collectSyncData()).thenAnswer(call -> new HashMap<>(Map.of("syncFingerprint", "state-a")));
        config = mock(ConfigManager.class);
        state = new SyncStateManager();
        field(state, "configManager", config);
        WebhookService webhook = new WebhookService();
        field(webhook, "gson", new Gson());
        field(webhook, "httpClient", http);
        ClanMembership membership = mock(ClanMembership.class);
        when(membership.isMember()).thenReturn(true);
        SyncNotifier notifier = new SyncNotifier();
        field(notifier, "client", client);
        field(notifier, "dataCollector", collector);
        field(notifier, "syncStateManager", state);
        field(notifier, "webhookService", webhook);
        field(notifier, "clanMembership", membership);
        button = new CollectionLogSyncButton();
        field(button, "client", client);
        field(button, "clientThread", clientThread);
        field(button, "syncNotifier", notifier);
    }

    @After public void teardown() throws Exception {
        http.dispatcher().cancelAll();
        server.shutdown();
        http.dispatcher().executorService().shutdownNow();
        http.connectionPool().evictAll();
    }

    @Test public void successWaitsForTheServerAcknowledgement() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        server.setDispatcher(new Dispatcher() {
            @Override public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                assertTrue(release.await(3, TimeUnit.SECONDS));
                return new MockResponse().setBody(SUCCESS);
            }
        });
        begin();
        assertNotNull(server.takeRequest(1, TimeUnit.SECONDS));
        assertEquals(List.of("NightLegion: Sending account data..."), messages);
        release.countDown();
        awaitResult("successfully");
        verify(config).setConfiguration("nightlegion", "syncFingerprint_11", "state-a");
        verify(clientThread).invokeLater(any(Runnable.class));
    }

    @Test public void httpAndMalformedResponsesNeverReportSuccess() throws Exception {
        for (MockResponse response : new MockResponse[] {
            new MockResponse().setResponseCode(401), new MockResponse().setResponseCode(403),
            new MockResponse().setResponseCode(500), new MockResponse().setBody("not json"),
            new MockResponse().setBody(""), new MockResponse().setBody("{\"ok\":false,\"status\":\"error\"}")
        }) {
            server.enqueue(response);
            begin();
            awaitResult("Failed to sync");
        }
        verify(config, never()).setConfiguration(anyString(), anyString(), anyString());
    }

    @Test public void incompleteAndMismatchedAcknowledgementsNeverReportSuccess() throws Exception {
        for (String response : new String[] {
            "{\"ok\":true,\"status\":\"success\"}",
            SUCCESS.replace("\"stale\":false", "\"stale\":true"),
            SUCCESS.replace("state-a", "another-state"),
            SUCCESS.replace("\"scoringWarnings\":[]", "\"scoringWarnings\":[\"missing data\"]")
        }) {
            server.enqueue(new MockResponse().setBody(response));
            begin();
            awaitResult("incomplete");
        }
		verify(config, never()).setConfiguration("nightlegion", "syncFingerprint_11", "another-state");
    }

    @Test public void timedOutRequestFailsAndAnExplicitRetryCanSucceed() throws Exception {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        begin();
        awaitResult("Failed to sync");
        server.enqueue(new MockResponse().setBody(SUCCESS));
        begin();
        awaitResult("successfully");
        assertEquals(2, server.getRequestCount());
    }

    @Test public void scheduledSyncIsCancelledOnAccountSwitchOrLogout() throws Exception {
        invoke(button, "scheduleSync");
        when(client.getAccountHash()).thenReturn(22L);
        when(client.getTickCount()).thenReturn(20);
        button.onGameTick(new GameTick());
        verify(collector, never()).collectSyncData();

        invoke(button, "scheduleSync");
        GameStateChanged logout = new GameStateChanged();
        logout.setGameState(GameState.LOGIN_SCREEN);
        button.onGameStateChanged(logout);
        when(client.getTickCount()).thenReturn(30);
        button.onGameTick(new GameTick());
        verify(collector, never()).collectSyncData();
        assertEquals(0, server.getRequestCount());
    }

    @Test public void lateResponseCannotAnnounceSuccessToAnotherLogin() throws Exception {
        AtomicReference<Runnable> callback = new AtomicReference<>();
        CountDownLatch received = new CountDownLatch(1);
        doAnswer(call -> { callback.set(call.getArgument(0)); received.countDown(); return null; })
            .when(clientThread).invokeLater(any(Runnable.class));
        server.enqueue(new MockResponse().setBody(SUCCESS));
        begin();
        assertTrue(received.await(3, TimeUnit.SECONDS));
        GameStateChanged logout = new GameStateChanged();
        logout.setGameState(GameState.LOGIN_SCREEN);
        button.onGameStateChanged(logout);
        // Even re-entering the same account must invalidate the previous login's UI callback.
        callback.get().run();
        assertEquals(List.of("NightLegion: Sending account data..."), messages);
    }

    @Test public void staleRepairIsConsumedOnlyByItsOriginalAccount() {
        JsonObject stale = new Gson().fromJson(SUCCESS.replace("\"stale\":false", "\"stale\":true"), JsonObject.class);
        state.ackHandler(11L).accept(stale);
        assertFalse(state.consumeFullSyncRequest(22L));
        assertTrue(state.consumeFullSyncRequest(11L));
        assertFalse(state.consumeFullSyncRequest(11L));
        verify(config).unsetConfiguration("nightlegion", "syncFingerprint_11");
        verify(config, never()).unsetConfiguration("nightlegion", "syncFingerprint_22");
    }

    @Test public void lateStaleResponseCannotOverwriteAnotherAccountsPendingRepair() {
        JsonObject stale = new Gson().fromJson(SUCCESS.replace("\"stale\":false", "\"stale\":true"), JsonObject.class);
        state.ackHandler(22L).accept(stale);
        state.ackHandler(11L).accept(stale);
        assertTrue(state.consumeFullSyncRequest(22L));
        assertFalse(state.consumeFullSyncRequest(22L));
        assertTrue(state.consumeFullSyncRequest(11L));
        assertFalse(state.consumeFullSyncRequest(11L));
    }

    private void begin() throws Exception {
        messages.clear();
        completed.set(new CountDownLatch(1));
        invoke(button, "performSync");
    }

    private void awaitResult(String expected) throws Exception {
        assertTrue("Missing completion: " + messages, completed.get().await(3, TimeUnit.SECONDS));
        assertEquals(2, messages.size());
        assertTrue(messages.toString(), messages.get(1).contains(expected));
    }

    private static void invoke(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static void field(Object target, String name, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
}
