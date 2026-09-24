package com.revalclan.collectionlog;

import com.google.gson.Gson;
import com.revalclan.api.account.AccountResponse;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.ItemComposition;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CollectionLogOwnershipTest {
    private CollectionLogManager manager(boolean guest) throws Exception {
        return manager(guest, java.util.EnumSet.noneOf(net.runelite.api.WorldType.class));
    }

    private CollectionLogManager manager(boolean guest, java.util.EnumSet<net.runelite.api.WorldType> worlds) throws Exception {
        EnumComposition replacements = (EnumComposition) Proxy.newProxyInstance(
            EnumComposition.class.getClassLoader(), new Class<?>[]{EnumComposition.class},
            (p, m, args) -> m.getName().equals("getIntValue") && (int) args[0] == 100 ? 200 : -1);
        Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class},
            (p, m, args) -> {
                switch (m.getName()) {
                    case "getWorldType": return worlds;
                    case "getEnum": return replacements;
                    case "getVarbitValue": return guest ? 1 : 0;
                    case "getVarpValue": return 1;
                    case "getItemDefinition":
                        return Proxy.newProxyInstance(ItemComposition.class.getClassLoader(),
                            new Class<?>[]{ItemComposition.class}, (ip, im, ia) ->
                                im.getName().equals("getName") ? "Skotos" : null);
                    default: return null;
                }
            });
        CollectionLogManager manager = new CollectionLogManager();
        Field field = CollectionLogManager.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(manager, client);
        manager.getCategoryStructIdMap().put("all_pets", 1);
        manager.getCategoryItemMap().put(1, new HashSet<>(Arrays.asList(200)));
        manager.getCategoryTabSlugs().put(475, new HashSet<>(Arrays.asList("all_pets")));
        return manager;
    }

    @Test
    public void rawAndReplacementIdsProduceOneObtainedItemAndLatestQuantity() throws Exception {
        CollectionLogManager manager = manager(false);
        manager.onCollectionLogItemObtained(100, 1, "Skotos");
        manager.onCollectionLogItemObtained(200, 2, "Skotos");
        assertEquals(1, manager.getObtainedItems().size());
        assertEquals(2, manager.getObtainedItems().get(200).getCount());
        Map categories = (Map) manager.sync().get("categories");
        Map page = (Map) ((Map) categories.get("Other")).get("all_pets");
        List items = (List) page.get("items");
        assertEquals(1, items.size());
        assertEquals(200, ((Map) items.get(0)).get("id"));
        assertEquals(true, ((Map) items.get(0)).get("obtained"));
        manager.clearObtainedItems();
        assertTrue(manager.getObtainedItems().isEmpty());
    }

    @Test
    public void zeroCountsAndGuestLogsNeverBecomeOwned() throws Exception {
        CollectionLogManager own = manager(false);
        own.onCollectionLogItemObtained(100, 0, "Skotos");
        own.onCollectionLogItemObtained(100, -1, "Skotos");
        assertTrue(own.getObtainedItems().isEmpty());
        CollectionLogManager guest = manager(true);
        guest.onCollectionLogItemObtained(100, 1, "Skotos");
        assertTrue(guest.getObtainedItems().isEmpty());
    }

    @Test
    public void petIdsRequireExactUnambiguousCacheName() throws Exception {
        CollectionLogManager manager = manager(false);
        assertEquals(Integer.valueOf(200), manager.getPetItemId("Skotos"));
        assertNull(manager.getPetItemId("Skoto"));
        manager.getCategoryItemMap().get(1).add(201);
        assertNull(manager.getPetItemId("Skotos"));
    }

    @Test
    public void temporaryWorldsCannotContributeOwnershipEvidence() throws Exception {
        for (String name : Arrays.asList("SEASONAL", "DEADMAN", "TOURNAMENT_WORLD", "BETA_WORLD",
            "NOSAVE_MODE", "QUEST_SPEEDRUNNING", "PVP_ARENA", "LAST_MAN_STANDING")) {
            CollectionLogManager manager = manager(false,
                java.util.EnumSet.of(net.runelite.api.WorldType.valueOf(name)));
            manager.onCollectionLogItemObtained(100, 1, "Skotos");
            assertTrue(name, manager.getObtainedItems().isEmpty());
        }
    }

    @Test
    public void ordinaryPvpAndFreshStartProgressRemainsValid() throws Exception {
        for (net.runelite.api.WorldType world : Arrays.asList(net.runelite.api.WorldType.PVP,
            net.runelite.api.WorldType.HIGH_RISK, net.runelite.api.WorldType.FRESH_START_WORLD)) {
            CollectionLogManager manager = manager(false, java.util.EnumSet.of(world));
            manager.onCollectionLogItemObtained(100, 1, "Skotos");
            assertEquals(world.name(), 1, manager.getObtainedItems().size());
        }
    }

    @Test
    public void partialPageDoesNotErasePreviouslyConfirmedOwnership() throws Exception {
        CollectionLogManager manager = manager(false);
        manager.onCollectionLogItemObtained(100, 1, "Skotos");
        manager.onCollectionLogItemObtained(200, 0, "Skotos");
        manager.onCollectionLogItemObtained(-1, 1, "Unknown");
        assertEquals(1, manager.getObtainedItems().size());
        assertEquals(1, manager.getObtainedItems().get(200).getCount());
        manager.clearObtainedItems();
        assertTrue(manager.getObtainedItems().isEmpty());
    }

    @Test
    public void accountJsonRetainsSourceTypeAndSpriteId() {
        AccountResponse.PointsLogEntry entry = new Gson().fromJson(
            "{\"sourceType\":\"pet\",\"sourceId\":null,\"pointSourceId\":\"new_pet\",\"itemId\":21273,\"pointsChange\":100}",
            AccountResponse.PointsLogEntry.class);
        assertEquals("pet", entry.getSourceType());
        assertNull(entry.getSourceId());
        assertEquals(Integer.valueOf(21273), entry.getItemId());
    }
}
