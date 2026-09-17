package com.revalclan.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.revalclan.api.NightLegionTransport;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Singleton
public class WebhookService {
	@Inject
	private Gson gson;

	@Inject
	private NightLegionTransport transport;
    private final java.util.concurrent.atomic.AtomicLong progressVersion = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong observationVersion = new java.util.concurrent.atomic.AtomicLong();
    public long getProgressVersion() { return progressVersion.get(); }
    public long getObservationVersion() { return observationVersion.get(); }

	/**
	 * Async send; hands the parsed JSON response to the consumer on success
	 * (null consumer = fire and forget).
	 * Consumer runs on the HTTP thread — do not touch the client from it.
	 */
    public void sendDataAsync(Map<String, Object> data, Consumer<JsonObject> onResponse) {
        sendDataAsync(data, onResponse,
            error -> log.warn("NightLegion event submission failed: {}", error.getMessage()));
    }

    public void sendDataAsync(Map<String, Object> data, Consumer<JsonObject> onResponse,
                              Consumer<Exception> onFailure) {
        JsonObject payload = gson.toJsonTree(data).getAsJsonObject();
        transport.request("community_reval_event", payload, response -> {
            if (response == null || (response.has("ok") && !response.get("ok").getAsBoolean())) {
                onFailure.accept(new IOException("NightLegion did not save the update."));
                return;
            }
            progressVersion.incrementAndGet();
            String type = String.valueOf(data.get("eventType"));
            if (!"PROGRESS".equals(type) && !"SYNC".equals(type)
                && !"LOGIN".equals(type) && !"LOGOUT".equals(type)) observationVersion.incrementAndGet();
            if (onResponse != null) onResponse.accept(response);
        }, onFailure);
    }
}
