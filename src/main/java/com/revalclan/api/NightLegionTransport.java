package com.revalclan.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.revalclan.RevalClanConfig;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Authenticated NightLegion replacement for Reval's production HTTP services.
 * The copied client keeps its public endpoint/model contracts, while every
 * request is queued to NightLegionBot with the current RuneLite RSN.
 */
@Singleton
public final class NightLegionTransport
{
	private static final String BASE = "https://nightlegion-livexp.onrender.com";
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final int MAX_POLLS = 60;
	private final OkHttpClient client;
	private final Gson gson;
	private final RevalClanConfig config;
	private final Client runeLiteClient;
	private final ScheduledExecutorService poller;
	private volatile String authoritativeRsn = "";

	@Inject
	public NightLegionTransport(OkHttpClient client, Gson gson, RevalClanConfig config,
		Client runeLiteClient)
	{
		this.client = client;
		this.gson = gson;
		this.config = config;
		this.runeLiteClient = runeLiteClient;
		ThreadFactory factory = runnable ->
		{
			Thread thread = new Thread(runnable, "nightlegion-reval-transport");
			thread.setDaemon(true);
			return thread;
		};
		this.poller = Executors.newSingleThreadScheduledExecutor(factory);
	}

	public void request(String action, JsonObject data, Consumer<JsonObject> success,
		Consumer<Exception> failure)
	{
		String token = config.personalLinkToken() == null ? "" : config.personalLinkToken().trim();
		if (token.isEmpty())
		{
			failure.accept(new IllegalStateException(
				"Paste your NightLegion Personal Link Token in the plugin settings first."));
			return;
		}
		JsonObject envelope = new JsonObject();
		envelope.addProperty("action", action);
		envelope.addProperty("rsn", currentRsn());
		envelope.add("data", data == null ? new JsonObject() : data);
		Request request = new Request.Builder()
			.url(BASE + "/companion/request")
			.header("X-NightLegion-Token", token)
			.post(RequestBody.create(JSON, gson.toJson(envelope)))
			.build();
		client.newCall(request).enqueue(new Callback()
		{
			@Override public void onFailure(Call call, IOException error) { failure.accept(error); }
			@Override public void onResponse(Call call, Response response)
			{
				try (Response ignored = response)
				{
					String text = response.body() == null ? "" : response.body().string();
					if (response.code() != 202)
					{
						failure.accept(new IOException(errorText(text, response.code())));
						return;
					}
					JsonObject accepted = gson.fromJson(text, JsonObject.class);
					String id = accepted != null && accepted.has("request_id")
						? accepted.get("request_id").getAsString() : "";
					if (id.isEmpty()) failure.accept(new IOException("NightLegion returned no request id."));
					else poll(id, token, 0, success, failure);
				}
				catch (Exception error) { failure.accept(error); }
			}
		});
	}

	/** Capture identity on RuneLite's client thread; network/Swing threads only read this value. */
	public void captureCurrentRsn()
	{
		if (runeLiteClient.getLocalPlayer() != null && runeLiteClient.getLocalPlayer().getName() != null)
		{
			String value = runeLiteClient.getLocalPlayer().getName().trim();
			if (!value.isEmpty()) authoritativeRsn = value;
		}
	}

	public void clearCurrentRsn()
	{
		authoritativeRsn = "";
	}

	private void poll(String id, String token, int attempt, Consumer<JsonObject> success,
		Consumer<Exception> failure)
	{
		if (attempt > MAX_POLLS)
		{
			failure.accept(new IOException("NightLegion is taking too long to respond."));
			return;
		}
		poller.schedule(() ->
		{
			Request request = new Request.Builder()
				.url(BASE + "/companion/result/" + id)
				.header("X-NightLegion-Token", token)
				.get().build();
			client.newCall(request).enqueue(new Callback()
			{
				@Override public void onFailure(Call call, IOException error)
				{
					poll(id, token, attempt + 1, success, failure);
				}
				@Override public void onResponse(Call call, Response response)
				{
					try (Response ignored = response)
					{
						if (response.code() == 202)
						{
							poll(id, token, attempt + 1, success, failure);
							return;
						}
						String text = response.body() == null ? "" : response.body().string();
						if (!response.isSuccessful())
						{
							failure.accept(new IOException(errorText(text, response.code())));
							return;
						}
						JsonObject result = gson.fromJson(text, JsonObject.class);
						if (result == null) failure.accept(new IOException("NightLegion returned invalid data."));
						else if (result.has("ok") && !result.get("ok").getAsBoolean())
							failure.accept(new IOException(result.has("error")
								? result.get("error").getAsString() : "NightLegion request failed."));
						else success.accept(result);
					}
					catch (Exception error) { failure.accept(error); }
				}
			});
		}, attempt == 0 ? 0 : 500, TimeUnit.MILLISECONDS);
	}

	private String currentRsn()
	{
		return authoritativeRsn;
	}

	private String errorText(String text, int status)
	{
		try
		{
			JsonObject value = gson.fromJson(text, JsonObject.class);
			if (value != null && value.has("error")) return value.get("error").getAsString();
		}
		catch (RuntimeException ignored) { }
		return "NightLegion HTTP " + status;
	}
}
