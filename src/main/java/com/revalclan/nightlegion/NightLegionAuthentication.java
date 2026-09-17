package com.revalclan.nightlegion;

import com.revalclan.RevalClanConfig;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/** Authentication only. Does not schedule, collect, translate, queue or poll data. */
@Singleton
public final class NightLegionAuthentication {
    public static final String ORIGIN = "https://nightlegion-livexp.onrender.com";
    private final RevalClanConfig config;
    private volatile Identity identity = new Identity("", "");

    private static final class Identity {
        final String accountHash;
        final String rsn;
        Identity(String accountHash, String rsn) { this.accountHash = accountHash; this.rsn = rsn; }
    }

    @Inject public NightLegionAuthentication(RevalClanConfig config) { this.config = config; }

    /** Called on the client thread; HTTP threads never access RuneLite state. */
    public void capture(Client client) {
        if (client.getLocalPlayer() != null && client.getLocalPlayer().getName() != null) {
            identity = new Identity(Long.toString(client.getAccountHash()), client.getLocalPlayer().getName());
        }
    }

    public OkHttpClient decorate(OkHttpClient client) { return decorate(client, HttpUrl.parse(ORIGIN)); }

    // Test seam, not a user-configurable destination.
    OkHttpClient decorate(OkHttpClient client, HttpUrl origin) {
        return client.newBuilder()
            .addInterceptor(chain -> {
                if (sameOrigin(chain.request().url(), origin) && token().isEmpty()) {
                    throw new IOException("Paste your NightLegion Personal Link Token from /runelite_link into the plugin settings.");
                }
                return chain.proceed(chain.request());
            })
            .addNetworkInterceptor(chain -> {
                Request.Builder request = chain.request().newBuilder()
                    .removeHeader("X-NightLegion-Token")
                    .removeHeader("X-NightLegion-Account-Hash")
                    .removeHeader("X-NightLegion-RSN");
                // Re-check on redirects: credentials must never follow a different origin.
                if (sameOrigin(chain.request().url(), origin)) {
                    String token = token();
                    if (token.isEmpty()) throw new IOException("NightLegion token was removed.");
                    Identity current = identity;
                    request.header("X-NightLegion-Token", token);
                    if (!current.accountHash.isEmpty()) request.header("X-NightLegion-Account-Hash", current.accountHash);
                    if (!current.rsn.isEmpty()) request.header("X-NightLegion-RSN", current.rsn);
                }
                return chain.proceed(request.build());
            }).build();
    }

    private String token() {
        String value = config.personalLinkToken();
        return value == null ? "" : value.trim();
    }

    static boolean sameOrigin(HttpUrl url, HttpUrl origin) {
        return origin != null && url.scheme().equals(origin.scheme())
            && url.host().equals(origin.host()) && url.port() == origin.port();
    }
}
