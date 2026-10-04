package com.revalclan.notifiers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.revalclan.RevalClanConfig;
import com.revalclan.session.SessionTracker;
import com.revalclan.util.ClanMembership;
import com.revalclan.util.EventFilterManager;
import com.revalclan.util.RaidPartyTracker;
import com.revalclan.util.RaidRewardLedger;
import com.revalclan.util.ScreenshotService;
import com.revalclan.util.WebhookService;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.SkullIcon;
import net.runelite.api.WorldType;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises real notifier events and JSON serialization against both ItemManager return types. */
public class NotifierGePriceCompatibilityTest {
    @Test public void clueStackExceedingIntRetainsItsUnitPriceAndTotal() throws Exception {
        verifyClue(1_500_000_000L);
    }

    @Test public void deathSortAndLostStacksRetainLongValues() throws Exception {
        verifyDeath(1_500_000_000L);
    }

    @Test public void lootStackAndSessionRetainLongValuesWithoutChangingUnitPriceFilter() throws Exception {
        verifyLoot(1_500_000_000L);
    }

    @Test public void unitPricesAboveIntSurviveAllThreeNotifiersOnLongApi() throws Exception {
        assumeTrue("RuneLite versions returning int cannot supply a unit price above Integer.MAX_VALUE",
            ItemManager.class.getMethod("getItemPrice", int.class).getReturnType() == long.class);
        verifyClue(3_000_000_123L);
        verifyDeath(3_000_000_123L);
        verifyLoot(3_000_000_123L);
    }

    private void verifyClue(long price) throws Exception {
        Fixture fixture = new Fixture();
        fixture.price(10001, price);
        ClueNotifier notifier = fixture.configure(new ClueNotifier());
        Widget reward = mock(Widget.class);
        when(reward.getItemId()).thenReturn(10001);
        when(reward.getItemQuantity()).thenReturn(3);
        Widget container = mock(Widget.class);
        when(container.getChildren()).thenReturn(new Widget[]{reward});
        when(fixture.client.getWidget(InterfaceID.TrailRewardscreen.ITEMS)).thenReturn(container);
        WidgetLoaded loaded = mock(WidgetLoaded.class);
        when(loaded.getGroupId()).thenReturn(InterfaceID.TRAIL_REWARDSCREEN);

        notifier.onChatMessage("You have completed 42 hard Treasure Trails.");
        notifier.onWidgetLoaded(loaded);
        JsonObject payload = fixture.onlyPayload("CLUE");
        assertEquals("hard", payload.get("tier").getAsString());
        assertEquals(42, payload.get("count").getAsInt());
        assertNumber(payload, "totalValue", price * 3);
        assertNumber(payload.getAsJsonArray("items").get(0).getAsJsonObject(), "price", price);
        verify(fixture.session).addClue("hard");
        // A second open without another completed-clue chat message remains suppressed.
        notifier.onWidgetLoaded(loaded);
        assertEquals(1, fixture.payloads.size());
    }

    private void verifyDeath(long price) throws Exception {
        Fixture fixture = new Fixture();
        fixture.price(10001, price);
        fixture.price(10002, price + 1);
        fixture.price(10003, price + 2);
        fixture.price(10004, price + 3);
        fixture.price(10005, 100);
        ItemContainer inventory = mock(ItemContainer.class);
        // Deliberately unsorted, with both a >int stack total and a low-priced item.
        when(inventory.getItems()).thenReturn(new Item[]{
            new Item(10005, 1), new Item(10002, 1), new Item(10001, 3),
            new Item(10004, 1), new Item(10003, 1)
        });
        when(fixture.client.getItemContainer(93)).thenReturn(inventory);
        Player player = mock(Player.class);
        when(player.getSkullIcon()).thenReturn(SkullIcon.NONE);
        when(player.getName()).thenReturn("Tester");
        when(player.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
        when(fixture.client.getLocalPlayer()).thenReturn(player);
        Player killer = mock(Player.class);
        when(killer.getName()).thenReturn("Killer");
        when(killer.getInteracting()).thenReturn(player);
        InteractingChanged attacking = new InteractingChanged(killer, player);
        ActorDeath death = new ActorDeath(player);
        DeathNotifier notifier = fixture.configure(new DeathNotifier());

        notifier.onInteractingChanged(attacking);
        notifier.onActorDeath(death);
        JsonObject payload = fixture.onlyPayload("DEATH");
        JsonArray kept = payload.getAsJsonArray("keptItems");
        assertEquals(3, kept.size());
        for (int i = 0; i < 3; i++) {
            JsonObject item = kept.get(i).getAsJsonObject();
            assertEquals(10004 - i, item.get("id").getAsInt());
            assertNumber(item, "gePrice", price + 3 - i);
        }
        JsonArray lost = payload.getAsJsonArray("lostItems");
        assertEquals(2, lost.size());
        assertEquals(10001, lost.get(0).getAsJsonObject().get("id").getAsInt());
        assertEquals(3, lost.get(0).getAsJsonObject().get("quantity").getAsInt());
        assertNumber(lost.get(0).getAsJsonObject(), "gePrice", price);
        assertEquals(10005, lost.get(1).getAsJsonObject().get("id").getAsInt());
        assertNumber(payload, "totalLostValue", price * 3 + 100);
        verify(fixture.session).addDeath("Killer", price * 3 + 100);
        verify(fixture.screenshot).captureScreenshot();
    }

    private void verifyLoot(long price) throws Exception {
        Fixture fixture = new Fixture();
        fixture.price(10001, price);
        fixture.price(10002, 700_000);
        LootNotifier notifier = fixture.configure(new LootNotifier());
        inject(notifier, "raidPartyTracker", mock(RaidPartyTracker.class));
        RaidRewardLedger ledger = mock(RaidRewardLedger.class);
        when(ledger.record(anyString(), anyCollection())).thenReturn(true);
        inject(notifier, "raidRewardLedger", ledger);
        notifier.onLootReceived(new LootReceived("Guardians of the Rift", 0, LootRecordType.EVENT,
            Arrays.asList(new ItemStack(10001, 3), new ItemStack(10002, 2)), 1, null));
        assertTrue(fixture.payloads.isEmpty());
        notifier.onGameTick();
        assertTrue(fixture.payloads.isEmpty());
        notifier.onGameTick();

        JsonObject payload = fixture.onlyPayload("LOOT");
        JsonArray items = payload.getAsJsonArray("items");
        assertEquals("Two 700k units must remain below the unchanged 1M unit-price threshold", 1, items.size());
        assertEquals(10001, items.get(0).getAsJsonObject().get("id").getAsInt());
        assertNumber(items.get(0).getAsJsonObject(), "gePrice", price);
        assertNumber(payload, "totalGEValue", price * 3);
        assertNumber(payload, "totalHAValue", 3);
        verify(fixture.session).addLoot("Guardians of the Rift", 10001, "Item 10001", 3, price);
        verify(fixture.session).addLoot("Guardians of the Rift", 10002, "Item 10002", 2, 700_000L);
    }

    private static void assertNumber(JsonObject object, String field, long expected) {
        assertTrue(field + " must remain a JSON number", object.getAsJsonPrimitive(field).isNumber());
        assertEquals(expected, object.get(field).getAsLong());
        assertEquals("The serialized integer must retain all digits", Long.toString(expected), object.get(field).toString());
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class Fixture {
        final Client client = mock(Client.class);
        final ItemManager itemManager = mock(ItemManager.class);
        final SessionTracker session = mock(SessionTracker.class);
        final ScreenshotService screenshot = mock(ScreenshotService.class);
        final WebhookService webhook = mock(WebhookService.class);
        final List<JsonObject> payloads = new ArrayList<>();
        final Gson gson = new Gson();

        Fixture() {
            when(client.getWorldType()).thenReturn(EnumSet.noneOf(WorldType.class));
            when(client.getAccountHash()).thenReturn(11L);
            when(screenshot.captureScreenshot()).thenReturn(CompletableFuture.completedFuture(null));
            doAnswer(call -> {
                Map<String, Object> payload = call.getArgument(0);
                payloads.add(gson.fromJson(gson.toJson(payload), JsonObject.class));
                return null;
            }).when(webhook).sendDataAsync(anyMap(), any());
            doAnswer(call -> {
                Map<String, Object> payload = call.getArgument(0);
                payloads.add(gson.fromJson(gson.toJson(payload), JsonObject.class));
                return null;
            }).when(webhook).sendDataAsync(anyMap(), any(), any());
        }

        void price(int itemId, long price) {
            // Compile the same test on RuneLite's old int and new long signatures.
            // Reject unsupported old-API fixtures rather than silently narrowing them.
            doAnswer(call -> {
                if (call.getMethod().getReturnType() == long.class) return Long.valueOf(price);
                return Integer.valueOf(Math.toIntExact(price));
            }).when(itemManager).getItemPrice(itemId);
            ItemComposition composition = mock(ItemComposition.class);
            when(composition.getName()).thenReturn("Item " + itemId);
            when(composition.getPrice()).thenReturn(1);
            when(itemManager.getItemComposition(itemId)).thenReturn(composition);
        }

        <T extends BaseNotifier> T configure(T notifier) throws Exception {
            notifier.client = client;
            notifier.itemManager = itemManager;
            notifier.filterManager = new EventFilterManager();
            notifier.webhookService = webhook;
            notifier.screenshotService = screenshot;
            notifier.config = mock(RevalClanConfig.class);
            when(notifier.config.notifyDeath()).thenReturn(true);
            notifier.clanMembership = mock(ClanMembership.class);
            when(notifier.clanMembership.isMember()).thenReturn(true);
            inject(notifier, "sessionTracker", session);
            return notifier;
        }

        JsonObject onlyPayload(String eventType) {
            assertEquals(1, payloads.size());
            JsonObject payload = payloads.get(0);
            assertEquals(eventType, payload.get("eventType").getAsString());
            return payload;
        }
    }
}
