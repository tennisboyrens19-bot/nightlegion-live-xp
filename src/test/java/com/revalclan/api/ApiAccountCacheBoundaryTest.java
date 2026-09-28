package com.revalclan.api;

import com.google.gson.Gson;
import com.revalclan.api.account.AccountResponse;
import com.revalclan.api.points.PointsResponse;
import okhttp3.*;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Deterministic ordering at the real HTTP callback/parser/cache boundary. */
public class ApiAccountCacheBoundaryTest {
    private final List<Request> requests = new ArrayList<>();
    private final List<Callback> callbacks = new ArrayList<>();

    private RevalApiService service() {
        OkHttpClient http = mock(OkHttpClient.class);
        when(http.newCall(any(Request.class))).thenAnswer(invocation -> {
            requests.add(invocation.getArgument(0));
            Call call = mock(Call.class);
            doAnswer(enqueue -> { callbacks.add(enqueue.getArgument(0)); return null; }).when(call).enqueue(any());
            return call;
        });
        return new RevalApiService(http, new Gson());
    }

    @Test public void logoutDropsOldAccountAndPointsCallbacksAndCannotRepopulateCache() throws Exception {
        RevalApiService api = service();
        AtomicInteger stale = new AtomicInteger();
        api.fetchAccount(11, value -> stale.incrementAndGet(), error -> stale.incrementAndGet());
        api.fetchPoints(value -> stale.incrementAndGet(), error -> stale.incrementAndGet());
        api.clearCache();
        AtomicReference<AccountResponse> account = new AtomicReference<>();
        AtomicReference<PointsResponse> points = new AtomicReference<>();
        api.fetchAccount(22, account::set, error -> fail(error.toString()));
        api.fetchPoints(points::set, error -> fail(error.toString()));
        respond(2, "{\"status\":\"success\",\"data\":{\"osrsAccount\":{\"osrsNickname\":\"Current\"}}}");
        respond(3, "{\"status\":\"success\",\"data\":{\"ranks\":[]}}");
        AccountResponse acceptedAccount = account.get();
        PointsResponse acceptedPoints = points.get();
        respond(0, "{\"status\":\"success\",\"data\":{\"osrsAccount\":{\"osrsNickname\":\"Previous\"}}}");
        respond(1, "{\"status\":\"success\",\"data\":{}}");
        assertEquals(0, stale.get());
        api.fetchAccount(22, account::set, error -> fail(error.toString()));
        api.fetchPoints(points::set, error -> fail(error.toString()));
        assertSame(acceptedAccount, account.get());
        assertSame(acceptedPoints, points.get());
        assertEquals(4, requests.size());
    }

    @Test public void refreshSupersedesPendingAccountAndRejectsLateErrors() throws Exception {
        RevalApiService api = service();
        AtomicInteger stale = new AtomicInteger();
        api.fetchAccount(11, value -> stale.incrementAndGet(), error -> stale.incrementAndGet());
        AtomicReference<AccountResponse> refreshed = new AtomicReference<>();
        api.refreshAccount(11, refreshed::set, error -> fail(error.toString()));
        callbacks.get(0).onFailure(null, new IOException("Previous request timed out"));
        respond(1, "{\"status\":\"success\",\"data\":{\"osrsAccount\":{\"activityPoints\":1.5}}}");
        assertEquals(0, stale.get());
        assertEquals(1.5, refreshed.get().getData().getOsrsAccount().getActivityPoints(), 0);
        api.fetchPoints(value -> stale.incrementAndGet(), error -> stale.incrementAndGet());
        api.clearCache();
        callbacks.get(2).onFailure(null, new IOException("Previous points request timed out"));
        assertEquals(0, stale.get());
    }

    private void respond(int index, String json) throws Exception {
        callbacks.get(index).onResponse(null, new Response.Builder().request(requests.get(index))
            .protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(ResponseBody.create(MediaType.parse("application/json"), json)).build());
    }
}
