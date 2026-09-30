package com.revalclan.notifiers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.revalclan.session.SessionTracker;
import com.revalclan.util.*;
import net.runelite.api.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real filter parsing, loot events, raid ledger, correlation window and BaseNotifier envelope. */
public class LootNotifierReplayTest {
    private final Gson gson = new Gson();
    private final List<JsonObject> payloads = new ArrayList<>();
    private final Map<String, String> stored = new HashMap<>();
    private final AtomicReference<String> account = new AtomicReference<>("first");
    private ConfigManager config;
    private Client client;
    private EventFilterManager filters;
    private ItemManager itemManager;

    @Before public void setup() throws Exception {
        config = mock(ConfigManager.class);
        when(config.getRSProfileConfiguration(anyString(), anyString())).thenAnswer(call ->
            stored.get(account.get() + ":" + call.getArgument(1)));
        doAnswer(call -> { stored.put(account.get() + ":" + call.getArgument(1), String.valueOf((Object) call.getArgument(2))); return null; })
            .when(config).setRSProfileConfiguration(anyString(), anyString(), any());
        doAnswer(call -> { stored.remove(account.get() + ":" + call.getArgument(1)); return null; })
            .when(config).unsetRSProfileConfiguration(anyString(), anyString());
        client = mock(Client.class);
        when(client.getAccountHash()).thenAnswer(call -> account.get().equals("first") ? 11L : 22L);
        when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
        filters = new EventFilterManager();
        JsonObject fixture;
        try (InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
            "/fixtures/nightlegion-point-rule-loot-filters.json")), StandardCharsets.UTF_8)) {
            fixture = gson.fromJson(reader, JsonObject.class);
        }
        Method parse = EventFilterManager.class.getDeclaredMethod("parseFilters", JsonObject.class);
        parse.setAccessible(true);
        inject(filters, "filters", parse.invoke(filters, fixture));
        itemManager = mock(ItemManager.class);
        when(itemManager.getItemComposition(anyInt())).thenAnswer(call -> {
            ItemComposition composition = mock(ItemComposition.class);
            int id = call.getArgument(0);
            when(composition.getName()).thenReturn(id == 26822 ? "Abyssal lantern" : id == 27279 ? "Thread of Elidinis" : "Item " + id);
            when(composition.getPrice()).thenReturn(1);
            return composition;
        });
    }

    @Test public void backendWhitelistedLowValuePointItemsReachPayloadButCosmeticAndBlacklistDoNot() throws Exception {
        LootNotifier notifier = notifier();
        assertEquals(1_000_000, filters.getFilters().getLootMinValue());
        doAnswer(call -> {
            if (call.getMethod().getReturnType() == long.class) return 2_000_000L;
            return 2_000_000;
        }).when(itemManager).getItemPrice(995);
        event(notifier, "Guardians of the Rift", new ItemStack(26822, 1), new ItemStack(26824, 1), new ItemStack(995, 1));
        flush(notifier);
        assertEquals(1, payloads.size());
        assertEquals(1, payloads.get(0).getAsJsonArray("items").size());
        assertEquals(26822, firstItem(0).get("id").getAsInt());
        assertEquals(0, firstItem(0).get("gePrice").getAsInt());
        assertEquals("LOOT", payloads.get(0).get("eventType").getAsString());
        assertEquals(11L, payloads.get(0).get("accountHash").getAsLong());
        event(notifier, "Tombs of Amascut", new ItemStack(27279, 1));
        flush(notifier);
        assertEquals(2, payloads.size());
        assertEquals(27279, firstItem(1).get("id").getAsInt());
    }

    @Test public void reopeningRaidChestIsSuppressedAcrossRestartButNextIdenticalRewardIsNew() throws Exception {
        LootNotifier notifier = notifier();
        event(notifier, "Tombs of Amascut", new ItemStack(27279, 2));
        event(notifier, "Tombs of Amascut", new ItemStack(27279, 2));
        flush(notifier);
        assertEquals(1, payloads.size());
        LootNotifier restarted = notifier();
        event(restarted, "Tombs of Amascut", new ItemStack(27279, 1));
        flush(restarted);
        assertEquals(1, payloads.size());
        restarted.onGameMessage("Your completed Tombs of Amascut: Expert Mode count is: 12.");
        event(restarted, "Tombs of Amascut", new ItemStack(27279, 2));
        flush(restarted);
        assertEquals(2, payloads.size());
        assertEquals(firstItem(0), firstItem(1));
        account.set("second");
        event(restarted, "Tombs of Amascut", new ItemStack(27279, 2));
        flush(restarted);
        assertEquals(3, payloads.size());
        assertEquals(22L, payloads.get(2).get("accountHash").getAsLong());
    }

    @Test public void separateIdenticalNonRaidDropsRemainSeparateAndLogoutDropsPendingOldLoot() throws Exception {
        LootNotifier notifier = notifier();
        event(notifier, "Guardians of the Rift", new ItemStack(26822, 1));
        flush(notifier);
        event(notifier, "Guardians of the Rift", new ItemStack(26822, 1));
        flush(notifier);
        assertEquals(2, payloads.size());
        assertEquals(firstItem(0), firstItem(1));
        event(notifier, "Guardians of the Rift", new ItemStack(26822, 1));
        notifier.reset();
        account.set("second");
        flush(notifier);
        assertEquals(2, payloads.size());
    }

    @Test public void distinctIdenticalDropsFlushedTogetherHaveDifferentPayloadIdentities() throws Exception {
        LootNotifier notifier = notifier();
        // Distinct kills can complete in the same tick. Their identical item/source
        // payloads must remain distinct even when the correlation window flushes both.
        for (int i = 0; i < 50; i++) {
            event(notifier, "Guardians of the Rift", new ItemStack(26822, 1));
        }
        flush(notifier);
        assertEquals(50, payloads.size());
        Set<String> identities = new HashSet<>();
        for (JsonObject payload : payloads) identities.add(payload.toString());
        assertEquals("Same-millisecond flush must not collapse separate events in backend dedupe", 50, identities.size());
    }

    private LootNotifier notifier() throws Exception {
        LootNotifier notifier = new LootNotifier();
        notifier.client = client; notifier.filterManager = filters; notifier.itemManager = itemManager;
        notifier.clanMembership = mock(ClanMembership.class);
        when(notifier.clanMembership.isMember()).thenReturn(true);
        notifier.webhookService = new WebhookService() {
            @Override public void sendDataAsync(Map<String, Object> data, Consumer<JsonObject> response) {
                payloads.add(gson.toJsonTree(data).getAsJsonObject());
            }
        };
        inject(notifier, "sessionTracker", mock(SessionTracker.class));
        inject(notifier, "raidPartyTracker", mock(RaidPartyTracker.class));
        RaidRewardLedger ledger = new RaidRewardLedger(); inject(ledger, "configManager", config);
        inject(notifier, "raidRewardLedger", ledger);
        return notifier;
    }
    private JsonObject firstItem(int index) { return payloads.get(index).getAsJsonArray("items").get(0).getAsJsonObject(); }
    private static void event(LootNotifier notifier, String source, ItemStack... items) {
        notifier.onLootReceived(new LootReceived(source, 0, LootRecordType.EVENT, Arrays.asList(items), 1, null));
    }
    private static void flush(LootNotifier notifier) { notifier.onGameTick(); notifier.onGameTick(); }
    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
