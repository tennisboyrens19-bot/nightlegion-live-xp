package com.revalclan.nightlegion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.revalclan.RevalClanConfig;
import com.revalclan.util.WebhookService;
import com.revalclan.util.EventFilterManager;
import com.revalclan.api.RevalApiService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import net.runelite.api.Client;
import net.runelite.api.Player;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NightLegionAuthenticationTest {
    private MockWebServer server;
    private OkHttpClient base;
    private OkHttpClient authenticated;
    private NightLegionAuthentication auth;
    private final AtomicReference<String> token = new AtomicReference<>("fixture-token-not-real");

    @Before public void setup() throws Exception {
        server = new MockWebServer(); server.start();
        base = new OkHttpClient();
        RevalClanConfig config = new RevalClanConfig() {
            @Override public String personalLinkToken() { return token.get(); }
        };
        auth = new NightLegionAuthentication(config);
        Client client = mock(Client.class); Player player = mock(Player.class);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getName()).thenReturn("Test Player");
        when(client.getAccountHash()).thenReturn(9223372036854775700L);
        auth.capture(client);
        authenticated = auth.decorate(base, server.url("/"));
    }
    @After public void teardown() throws Exception {
        server.shutdown(); base.dispatcher().executorService().shutdownNow(); base.connectionPool().evictAll();
    }
    @Test public void nativeRequestPathQueryBodyAreNotTranslated() throws Exception {
        String json="{\"accountHash\":\"9223372036854775700\",\"optionId\":\"a\"}";
        server.enqueue(new MockResponse().setBody("{\"status\":\"success\"}"));
        Request request=new Request.Builder().url(server.url("/plugin/competitions/votes/fixture/cast?x=1"))
            .post(RequestBody.create(MediaType.parse("application/json"),json)).build();
        try(Response response=authenticated.newCall(request).execute()) { assertEquals(200,response.code()); }
        RecordedRequest received=server.takeRequest(1,TimeUnit.SECONDS);
        assertEquals("/plugin/competitions/votes/fixture/cast?x=1",received.getPath());
        assertEquals("POST",received.getMethod()); assertEquals(json,received.getBody().readUtf8());
        assertEquals(token.get(),received.getHeader("X-NightLegion-Token"));
        assertEquals("9223372036854775700",received.getHeader("X-NightLegion-Account-Hash"));
        assertEquals("Test Player",received.getHeader("X-NightLegion-RSN"));
    }
    @Test public void noTokenSendsNoRequest() throws Exception {
        token.set(" ");
        try { authenticated.newCall(new Request.Builder().url(server.url("/plugin/account")).build()).execute(); fail(); }
        catch(IOException expected) { assertTrue(expected.getMessage().contains("/runelite_link")); }
        assertEquals(0,server.getRequestCount());
    }
    @Test public void tokenRotationUsesSavedTokenWithoutReconstructingClient() throws Exception {
        for(String value:new String[]{"first-fixture","second-fixture"}) {
            token.set(value);server.enqueue(new MockResponse());
            try(Response response=authenticated.newCall(new Request.Builder().url(server.url("/event-filters")).build()).execute()) { assertEquals(200,response.code()); }
            assertEquals(value,server.takeRequest().getHeader("X-NightLegion-Token"));
        }
    }
    @Test public void existingHttpTimeoutsAndRetryPolicyArePreserved() {
        assertEquals(base.connectTimeoutMillis(),authenticated.connectTimeoutMillis());
        assertEquals(base.readTimeoutMillis(),authenticated.readTimeoutMillis());
        assertEquals(base.writeTimeoutMillis(),authenticated.writeTimeoutMillis());
        assertEquals(base.retryOnConnectionFailure(),authenticated.retryOnConnectionFailure());
        assertEquals(base.followRedirects(),authenticated.followRedirects());
    }
    @Test public void redirectsToAnotherOriginNeverCarryTokenOrAccount() throws Exception {
        try(MockWebServer other=new MockWebServer()) {
            other.start();server.enqueue(new MockResponse().setResponseCode(302).addHeader("Location",other.url("/next")));
            other.enqueue(new MockResponse());
            try(Response response=authenticated.newCall(new Request.Builder().url(server.url("/plugin/points")).build()).execute()) { assertEquals(200,response.code()); }
            assertEquals(token.get(),server.takeRequest().getHeader("X-NightLegion-Token"));
            RecordedRequest forwarded=other.takeRequest();
            assertNull(forwarded.getHeader("X-NightLegion-Token"));
            assertNull(forwarded.getHeader("X-NightLegion-Account-Hash"));assertNull(forwarded.getHeader("X-NightLegion-RSN"));
        }
    }
    @Test public void hashIdentityCaptureDoesNotReadClientFromHttpThread() throws Exception {
        Client client=mock(Client.class);Player player=mock(Player.class);
        when(client.getLocalPlayer()).thenReturn(player);when(player.getName()).thenReturn("Alt Player");
        when(client.getAccountHash()).thenReturn(123L);auth.capture(client);clearInvocations(client,player);
        server.enqueue(new MockResponse());
        try(Response response=authenticated.newCall(new Request.Builder().url(server.url("/plugin/account?accountHash=123")).build()).execute()) { assertEquals(200,response.code()); }
        assertEquals("123",server.takeRequest().getHeader("X-NightLegion-Account-Hash"));verifyNoInteractions(client,player);
    }
    private OkHttpClient redirectTestOnly() {
        // Preserve production URL building, intercept only to route a test to the local server.
        return authenticated.newBuilder().addInterceptor(chain -> {
            HttpUrl url=chain.request().url();
            return chain.proceed(chain.request().newBuilder().url(server.url(url.encodedPath()+ (url.encodedQuery()==null?"":"?"+url.encodedQuery()))).build());
        }).build();
    }
    private static void field(Object object,String name,Object value) throws Exception {
        Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);
    }
    @Test public void originalWebhookStillUsesGzipAndDirectAcknowledgement() throws Exception {
        WebhookService webhook=new WebhookService();field(webhook,"httpClient",redirectTestOnly());field(webhook,"gson",new Gson());
        server.enqueue(new MockResponse().setBody("{\"sync\":{\"fingerprint\":\"abc\",\"stale\":false}}"));
        CountDownLatch ack=new CountDownLatch(1);AtomicReference<JsonObject> output=new AtomicReference<>();
        webhook.sendDataAsync(Map.of("eventType","LOGIN","accountHash",123L),response -> {output.set(response);ack.countDown();});
        assertTrue(ack.await(3,TimeUnit.SECONDS));
        RecordedRequest request=server.takeRequest();assertEquals("/reval-webhook",request.getPath());
        assertEquals("gzip",request.getHeader("Content-Encoding"));
        String body=new String(new GZIPInputStream(new ByteArrayInputStream(request.getBody().readByteArray())).readAllBytes(),StandardCharsets.UTF_8);
        assertEquals("LOGIN",new Gson().fromJson(body,JsonObject.class).get("eventType").getAsString());
        assertEquals("abc",output.get().getAsJsonObject("sync").get("fingerprint").getAsString());
    }
    @Test public void originalWebhookRetainsSeparateScreenshotMultipart() throws Exception {
        WebhookService webhook=new WebhookService();field(webhook,"httpClient",redirectTestOnly());field(webhook,"gson",new Gson());
        server.enqueue(new MockResponse().setBody("{}"));CountDownLatch ack=new CountDownLatch(1);
        webhook.sendDataAsync(Map.of("eventType","DEATH"),new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff,(byte)0xd9},r -> ack.countDown());
        assertTrue(ack.await(3,TimeUnit.SECONDS));RecordedRequest request=server.takeRequest();
        assertTrue(request.getHeader("Content-Type").startsWith("multipart/form-data"));
        String body=request.getBody().readUtf8();assertTrue(body.contains("name=\"payload\""));assertTrue(body.contains("name=\"screenshot\""));
        assertFalse(body.contains("companion/request"));
    }
    @Test public void originalAccountApiParsesNativeResponseWithNoQueueEnvelope() throws Exception {
        RevalApiService api=new RevalApiService(base,new Gson(),auth);field(api,"httpClient",redirectTestOnly());
        server.enqueue(new MockResponse().setBody("{\"status\":\"success\",\"data\":{\"osrsAccount\":{\"osrsNickname\":\"Test Player\"}}}"));
        CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
        api.fetchAccount(123L,r->{result.set(r.getData().getOsrsAccount().getOsrsNickname());done.countDown();},e->{result.set(e.toString());done.countDown();});
        assertTrue(done.await(3,TimeUnit.SECONDS));assertEquals("Test Player",result.get());
        assertEquals("/plugin/account?accountHash=123",server.takeRequest().getPath());
    }
}
