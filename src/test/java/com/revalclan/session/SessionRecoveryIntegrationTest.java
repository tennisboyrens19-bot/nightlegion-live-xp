package com.revalclan.session;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.revalclan.util.ClanMembership;
import com.revalclan.util.WebhookService;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Filepath;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real temporary session files and gzip HTTP, without any RuneLite credentials or login. */
public class SessionRecoveryIntegrationTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final Gson gson = new Gson();
    private MockWebServer server;
    private OkHttpClient http;
    private Client client;
    private ClientThread thread;
    private SessionStore store;
    private WebhookService webhook;
    private ClanMembership membership;
    private final AtomicReference<CountDownLatch> idle = new AtomicReference<>();

    @Before public void setup() throws Exception {
        server = new MockWebServer();
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        server.start(loopback, 0);
        http = new OkHttpClient.Builder().readTimeout(300, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false).addInterceptor(chain -> chain.proceed(
                chain.request().newBuilder().url(server.url("/reval-webhook")
                    .newBuilder().host(loopback.getHostAddress()).build()).build())).build();
        http.dispatcher().setIdleCallback(() -> { if (idle.get() != null) idle.get().countDown(); });
        webhook = new WebhookService();
        inject(webhook, "httpClient", http); inject(webhook, "gson", gson);
        client = mock(Client.class);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Replay Alpha");
        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getAccountHash()).thenReturn(11L);
        when(client.getWorld()).thenReturn(302);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
        when(client.getOverallExperience()).thenReturn(1000L);
        thread = mock(ClientThread.class);
        doAnswer(call -> { call.<Runnable>getArgument(0).run(); return null; })
            .when(thread).invokeLater(any(Runnable.class));
        membership = mock(ClanMembership.class);
        when(membership.isMember()).thenReturn(true);
        store = new SessionStore(gson);
        Path root = temporary.getRoot().toPath();
        Constructor<Filepath> constructor = Filepath.class.getDeclaredConstructor(Path.class, Path.class);
        constructor.setAccessible(true);
        store.initialize(constructor.newInstance(root, root));
    }

    @After public void teardown() throws Exception {
        http.dispatcher().cancelAll(); server.shutdown();
        http.dispatcher().executorService().shutdownNow(); http.connectionPool().evictAll();
    }

    @Test public void timeoutAndHttpFailuresKeepDiskCopyAndPermitReplayWithoutRestart() throws Exception {
        SessionTracker tracker = tracker();
        int attempt = 0;
        for (MockResponse failure : new MockResponse[]{
            new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE),
            new MockResponse().setResponseCode(401), new MockResponse().setResponseCode(403),
            new MockResponse().setResponseCode(500), new MockResponse().setBody("not json"),
            new MockResponse().setBody("")
        }) {
            String id = "retry-" + ++attempt;
            store.write(persisted(id, 11L, true));
            enqueue(failure); tracker.recoverPersistedSessions(0);
            assertEquals(id, request().getAsJsonObject("sessionSummary").get("sessionId").getAsString());
            awaitIdle(); assertEquals(1, store.readAll().size());
            enqueue(new MockResponse().setBody("{\"sessionStored\":\"stored\"}"));
            tracker.recoverPersistedSessions(11L);
            assertEquals(id, request().getAsJsonObject("sessionSummary").get("sessionId").getAsString());
            awaitIdle(); assertTrue(store.readAll().isEmpty());
        }
    }

    @Test public void incompleteAcknowledgementCanRetryAndDuplicateRemovesOnlyThatSession() throws Exception {
        SessionTracker tracker = tracker();
        store.write(persisted("ack-retry", 11L, true));
        enqueue(new MockResponse().setBody("{\"ok\":true}"));
        tracker.recoverPersistedSessions(0); request(); awaitIdle();
        assertEquals(1, store.readAll().size());
        store.write(persisted("other-account", 22L, false));
        enqueue(new MockResponse().setBody("{\"sessionStored\":\"duplicate\"}"));
        tracker.recoverPersistedSessions(11L);
        assertEquals("ack-retry", request().getAsJsonObject("sessionSummary").get("sessionId").getAsString());
        awaitIdle(); assertEquals(1, store.readAll().size());
        assertEquals("other-account", store.readAll().get(0).sessionId());
    }

    @Test public void restartRecoversExactDiskSessionWithOriginalAccountEnvelope() throws Exception {
        SessionTracker first = tracker(); first.startSession();
        first.addKill("Zulrah"); first.addLoot("Zulrah", 12922, "Tanzanite fang", 1, 100_000);
        for (int i = 0; i < 50; i++) first.onGameTick();
        SessionStore.PersistedSession saved = store.readAll().get(0);
        when(client.getAccountHash()).thenReturn(22L);
        when(client.getLocalPlayer().getName()).thenReturn("Replay Beta");
        SessionTracker restarted = tracker();
        enqueue(new MockResponse().setBody("{\"sessionStored\":\"stored\"}"));
        restarted.recoverPersistedSessions(0);
        JsonObject request = request(); awaitIdle();
        assertEquals(11L, request.get("accountHash").getAsLong());
        assertEquals("Replay Alpha", request.get("username").getAsString());
        JsonObject summary = request.getAsJsonObject("sessionSummary");
        assertEquals(saved.sessionId(), summary.get("sessionId").getAsString());
        assertEquals("recovered", summary.get("endReason").getAsString());
        assertEquals(1, summary.getAsJsonObject("kills").get("Zulrah").getAsInt());
        assertEquals(100_000, summary.get("totalLootValue").getAsInt());
        assertTrue(store.readAll().isEmpty());
    }

    @Test public void membershipProofCannotReleaseAnotherAccountsUnprovenSession() throws Exception {
        SessionTracker tracker = tracker();
        store.write(persisted("mine", 11L, false)); store.write(persisted("other", 22L, false));
        tracker.recoverPersistedSessions(0); assertEquals(0, server.getRequestCount());
        enqueue(new MockResponse().setBody("{\"sessionStored\":\"stored\"}"));
        tracker.recoverPersistedSessions(11L);
        assertEquals("mine", request().getAsJsonObject("sessionSummary").get("sessionId").getAsString());
        awaitIdle(); assertEquals(1, server.getRequestCount());
        assertEquals("other", store.readAll().get(0).sessionId());
    }

    @Test public void logoutAndNewAccountDoNotShareSessionAccumulations() throws Exception {
        SessionTracker tracker = tracker(); tracker.startSession();
        tracker.addKill("Zulrah"); tracker.addClue("elite");
        tracker.addLoot("Zulrah", 12922, "Tanzanite fang", 1, 100_000);
        Map<String, Object> first = tracker.finalizeSession(); assertNull(tracker.finalizeSession());
        when(client.getAccountHash()).thenReturn(22L);
        when(client.getLocalPlayer().getName()).thenReturn("Replay Beta");
        tracker.startSession(); Map<String, Object> second = tracker.finalizeSession();
        assertNotEquals(first.get("sessionId"), second.get("sessionId"));
        assertTrue(((Map<?, ?>) second.get("kills")).isEmpty());
        assertTrue(((Map<?, ?>) second.get("clues")).isEmpty());
        assertEquals(0L, second.get("totalLootValue")); assertEquals(2, store.readAll().size());
    }

    @Test public void delayedOldSessionAcknowledgementCannotDeleteNewAccountsSession() throws Exception {
        AtomicReference<Runnable> callback = new AtomicReference<>();
        doAnswer(call -> { callback.set(call.getArgument(0)); return null; })
            .when(thread).invokeLater(any(Runnable.class));
        SessionTracker tracker = tracker();
        store.write(persisted("old-session", 11L, true));
        enqueue(new MockResponse().setBody("{\"sessionStored\":\"stored\"}"));
        tracker.recoverPersistedSessions(0); request(); awaitIdle();
        assertNotNull(callback.get());
        when(client.getAccountHash()).thenReturn(22L);
        when(client.getLocalPlayer().getName()).thenReturn("Replay Beta");
        tracker.startSession();
        for (int i = 0; i < 50; i++) tracker.onGameTick();
        callback.get().run();
        assertEquals(1, store.readAll().size());
        assertEquals(22L, store.readAll().get(0).accountHash);
        assertNotEquals("old-session", store.readAll().get(0).sessionId());
    }

    private SessionTracker tracker() throws Exception {
        SessionTracker tracker = new SessionTracker();
        inject(tracker, "client", client); inject(tracker, "clientThread", thread);
        inject(tracker, "gson", gson); inject(tracker, "webhookService", webhook);
        inject(tracker, "store", store); inject(tracker, "membership", membership);
        return tracker;
    }
    private SessionStore.PersistedSession persisted(String id, long hash, boolean member) {
        SessionStore.PersistedSession saved = new SessionStore.PersistedSession();
        saved.accountHash = hash; saved.username = "Replay account " + hash;
        saved.world = 302; saved.worldFlags = List.of(); saved.member = member;
        saved.summary = gson.toJsonTree(Map.of("sessionId", id, "startedAt", System.currentTimeMillis(),
            "endReason", "recovered")).getAsJsonObject();
        return saved;
    }
    private void enqueue(MockResponse response) {
        idle.set(new CountDownLatch(1)); server.enqueue(response);
    }
    private JsonObject request() throws Exception {
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull("Expected session HTTP request", request);
        assertEquals("gzip", request.getHeader("Content-Encoding"));
        return gson.fromJson(new InputStreamReader(new GZIPInputStream(request.getBody().inputStream()),
            StandardCharsets.UTF_8), JsonObject.class);
    }
    private void awaitIdle() throws Exception {
        assertTrue("HTTP callback did not finish", idle.get().await(3, TimeUnit.SECONDS));
    }
    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
}
