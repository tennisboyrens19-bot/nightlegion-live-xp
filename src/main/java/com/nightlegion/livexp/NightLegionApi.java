package com.nightlegion.livexp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class NightLegionApi
{
    // Use the same production relay as LiveXP and rank telemetry. The old
    // nightlegion-companion-test URL had its own queue, while the Discord bot
    // polls nightlegion-livexp. Requests sent to the test queue therefore sat
    // pending until the RuneLite client timed out.
    private static final String BASE = "https://nightlegion-livexp.onrender.com";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ScheduledExecutorService executor;
    private final NightLegionLiveXpConfig config;
    private final Gson gson;

    NightLegionApi(OkHttpClient client, ScheduledExecutorService executor, NightLegionLiveXpConfig config, Gson gson)
    {
        this.client = client;
        this.executor = executor;
        this.config = config;
        this.gson = gson;
    }

    void action(String action, String rsn, JsonObject data, Consumer<JsonObject> ok, Consumer<String> fail)
    {
        String token = token();
        if (token.isEmpty())
        {
            fail.accept("Paste your Personal Link Token in NightLegion settings first.");
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("action", action);
        body.addProperty("rsn", rsn == null ? "" : rsn);
        body.add("data", data == null ? new JsonObject() : data);

        Request request = new Request.Builder()
            .url(BASE + "/companion/request")
            .header("X-NightLegion-Token", token)
            .post(RequestBody.create(JSON, gson.toJson(body)))
            .build();

        client.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                fail.accept("Could not reach NightLegion.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                try (Response ignored = response)
                {
                    if (response.body() == null)
                    {
                        fail.accept("NightLegion returned an empty response.");
                        return;
                    }
                    String text = response.body().string();
                    if (response.code() != 202)
                    {
                        fail.accept(errorText(text, response.code()));
                        return;
                    }
                    JsonObject json = gson.fromJson(text, JsonObject.class);
                    String requestId = json != null && json.has("request_id") ? json.get("request_id").getAsString() : "";
                    if (requestId.isEmpty())
                    {
                        fail.accept("NightLegion did not return a request id.");
                        return;
                    }
                    poll(requestId, 0, ok, fail);
                }
            }
        });
    }

    private void poll(String requestId, int attempt, Consumer<JsonObject> ok, Consumer<String> fail)
    {
        // Allow up to ~30 seconds. Normal replies arrive in a few seconds, but
        // this avoids false timeouts during a deploy/restart or short network
        // hiccup.
        if (attempt > 60)
        {
            fail.accept("NightLegion is taking too long to respond. Try Refresh.");
            return;
        }

        executor.schedule(() ->
        {
            Request request = new Request.Builder()
                .url(BASE + "/companion/result/" + requestId)
                .header("X-NightLegion-Token", token())
                .get()
                .build();

            client.newCall(request).enqueue(new Callback()
            {
                @Override
                public void onFailure(Call call, IOException e)
                {
                    poll(requestId, attempt + 1, ok, fail);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException
                {
                    try (Response ignored = response)
                    {
                        if (response.code() == 202)
                        {
                            poll(requestId, attempt + 1, ok, fail);
                            return;
                        }
                        if (response.body() == null)
                        {
                            fail.accept("NightLegion returned an empty response.");
                            return;
                        }
                        String text = response.body().string();
                        if (response.code() != 200)
                        {
                            fail.accept(errorText(text, response.code()));
                            return;
                        }
                        JsonObject json = gson.fromJson(text, JsonObject.class);
                        if (json == null)
                        {
                            fail.accept("NightLegion returned invalid data.");
                            return;
                        }
                        if (json.has("ok") && !json.get("ok").getAsBoolean())
                        {
                            fail.accept(json.has("error") ? json.get("error").getAsString() : "Request failed.");
                            return;
                        }
                        ok.accept(json);
                    }
                }
            });
        }, attempt == 0 ? 0 : 500, TimeUnit.MILLISECONDS);
    }

    private String token()
    {
        return config.token() == null ? "" : config.token().trim();
    }

    private String errorText(String text, int code)
    {
        try
        {
            JsonObject json = gson.fromJson(text, JsonObject.class);
            if (json != null && json.has("error"))
            {
                return json.get("error").getAsString();
            }
        }
        catch (Exception ignored)
        {
        }
        return "NightLegion HTTP " + code;
    }
}
