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

	/**
	 * Async send; hands the parsed JSON response to the consumer on success
	 * (null consumer = fire and forget).
	 * Consumer runs on the HTTP thread — do not touch the client from it.
	 */
	public void sendDataAsync(Map<String, Object> data, Consumer<JsonObject> onResponse) {
		JsonObject payload = gson.toJsonTree(data).getAsJsonObject();
		transport.request("community_reval_event", payload, response -> {
			if (onResponse == null) return;
			try {
				onResponse.accept(response);
			} catch (RuntimeException error) {
				log.warn("NightLegion event response handler failed: {}", error.getMessage());
			}
		}, error -> log.warn("NightLegion event submission failed: {}", error.getMessage()));
	}
}

