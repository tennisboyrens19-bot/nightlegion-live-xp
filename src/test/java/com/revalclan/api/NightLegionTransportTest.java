package com.revalclan.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.revalclan.RevalClanConfig;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.api.Client;
import net.runelite.api.Player;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NightLegionTransportTest {
    private MockWebServer server;
    private OkHttpClient http;
    private NightLegionTransport transport;
    private final AtomicReference<String> token = new AtomicReference<>("fixture-token-not-a-real-secret");
    private final AtomicReference<JsonObject> result = new AtomicReference<>();
    private final AtomicReference<Exception> failure = new AtomicReference<>();
    private final CountDownLatch done = new CountDownLatch(1);

    @Before public void setup() throws Exception {
        server = new MockWebServer();
        server.start();
        http = new OkHttpClient();
        Client client = mock(Client.class);
        Player player = mock(Player.class);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Test player");
        RevalClanConfig config = new RevalClanConfig() {
            @Override public String personalLinkToken() { return token.get(); }
        };
        String base = server.url("/").toString();
        transport = new NightLegionTransport(http, new Gson(), config, client, base.substring(0, base.length()-1));
        transport.captureCurrentRsn();
    }

    @After public void cleanup() throws Exception {
        transport.invalidateRequests();
        java.lang.reflect.Field field = NightLegionTransport.class.getDeclaredField("poller");
        field.setAccessible(true);
        ((java.util.concurrent.ScheduledExecutorService)field.get(transport)).shutdownNow();
        server.shutdown();
        http.dispatcher().executorService().shutdownNow();
        http.connectionPool().evictAll();
    }

    private void request() {
        transport.request("community_reval_api", new JsonObject(), value -> {
            result.set(value); done.countDown();
        }, error -> { failure.set(error); done.countDown(); });
    }

    @Test public void emptyTokenFailsLocallyWithoutHttp() throws Exception {
        token.set(" ");
        request();
        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertNotNull(failure.get());
        assertTrue(failure.get().getMessage().contains("Personal Link Token"));
        assertEquals(0, server.getRequestCount());
    }

    @Test public void acceptedRequestPollsAndUsesCurrentCharacterAndSavedToken() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202).setBody("{\"request_id\":\"fixture-id\"}"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true,\"data\":{}}"));
        token.set("new-fixture-token");
        request();
        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertNull(failure.get());
        assertTrue(result.get().get("ok").getAsBoolean());
        RecordedRequest post = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("new-fixture-token", post.getHeader("X-NightLegion-Token"));
        JsonObject envelope = new Gson().fromJson(post.getBody().readUtf8(), JsonObject.class);
        assertEquals("Test player", envelope.get("rsn").getAsString());
        assertEquals("/companion/result/fixture-id", server.takeRequest(1, TimeUnit.SECONDS).getPath());
    }

    @Test public void unauthorizedResponseHasActionableErrorWithoutToken() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"untrusted response\"}"));
        request();
        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertTrue(failure.get().getMessage().contains("not authorized"));
        assertFalse(failure.get().getMessage().contains(token.get()));
    }

    @Test public void serverErrorIsNotPresentedAsMissingToken() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        request();
        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertTrue(failure.get().getMessage().contains("server is unavailable"));
    }

    @Test public void tokenChangeRejectsInFlightOldResponseAndStopsPolling() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202).setBody("{\"request_id\":\"old-id\"}").setBodyDelay(250, TimeUnit.MILLISECONDS));
        request();
        assertNotNull(server.takeRequest(1, TimeUnit.SECONDS));
        transport.invalidateRequests();
        token.set("replacement-fixture-token");
        assertFalse(done.await(800, TimeUnit.MILLISECONDS));
        assertEquals(1, server.getRequestCount());
    }

    @Test public void resultFailureIsPropagated() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202).setBody("{\"request_id\":\"fixture-id\"}"));
        server.enqueue(new MockResponse().setBody("{\"ok\":false,\"error\":\"fixture rejection\"}"));
        request();
        assertTrue(done.await(3, TimeUnit.SECONDS));
        assertEquals("fixture rejection", failure.get().getMessage());
    }
}
