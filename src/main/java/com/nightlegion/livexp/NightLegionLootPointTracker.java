package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.events.ServerNpcLoot;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NightLegion transport adaptation of Reval's pinned LootNotifier.
 * It retains event coverage, the two-tick correlation buffer, self-drop and
 * equipment-swap rejection, and unit-price qualification without using Reval.
 */
public final class NightLegionLootPointTracker
{
    private static final Logger log = LoggerFactory.getLogger(NightLegionLootPointTracker.class);
    private static final long MIN_VALUE = 1_000_000L;
    private static final int BUFFER_TICKS = 2;
    private static final int CLOG_TICKS = 10;
    private static final int SELF_DROP_TICKS = 10;
    private static final int UNEQUIP_TICKS = 2;
    private static final Pattern CLOG = Pattern.compile("New item added to your collection log: (?<item>.+)", Pattern.CASE_INSENSITIVE);
    private static final Set<Integer> MAD_ANGEL_IDS = Set.of(
        16305, 16306, 16307, 16308, 16309, 16310, 16311, 16312, 16313, 16314, 16315);
    private static final Set<Integer> SAILING_NPC_IDS = Set.of(
        NpcID.SAILING_BULL_SHARK_DEAD, NpcID.SAILING_HAMMERHEAD_SHARK_DEAD,
        NpcID.SAILING_TIGER_SHARK_DEAD, NpcID.SAILING_GREAT_WHITE_SHARK_DEAD,
        NpcID.SAILING_NARWHAL_DEAD, NpcID.SAILING_ORCA_DEAD,
        NpcID.SAILING_PYGMY_KRAKEN_DEAD, NpcID.SAILING_SPINED_KRAKEN_DEAD,
        NpcID.SAILING_ARMOURED_KRAKEN_DEAD, NpcID.SAILING_VAMPYRE_KRAKEN_DEAD,
        NpcID.SAILING_EAGLE_RAY_DEAD, NpcID.SAILING_BUTTERFLY_RAY_DEAD,
        NpcID.SAILING_STINGRAY_DEAD, NpcID.SAILING_MANTA_RAY_DEAD,
        NpcID.SAILING_OSPREY_DEAD, NpcID.SAILING_ALBATROSS_DEAD,
        NpcID.SAILING_FRIGATEBIRD_DEAD, NpcID.SAILING_TERN_DEAD,
        NpcID.SAILING_SEA_MOGRE_DEAD, NpcID.SAILING_DOLPHIN_DEAD,
        NpcID.SAILING_VEILED_KRAKEN_DEAD);
    private static final Set<Integer> SPECIAL_NPC_IDS = Set.of(
        NpcID.WHISPERER, NpcID.WHISPERER_MELEE, NpcID.WHISPERER_QUEST,
        NpcID.WHISPERER_MELEE_QUEST, NpcID.ARAXXOR, NpcID.ARAXXOR_DEAD,
        NpcID.RT_FIRE_QUEEN_INACTIVE, NpcID.RT_ICE_KING_INACTIVE,
        NpcID.YAMA, NpcID.HESPORI, NpcID.GRYPHON_BOSS,
        NpcID.GB_HILLGIANT_CHEST, NpcID.GB_MOSSGIANT_CHEST);
    private static final Set<String> SPECIAL_NPC_NAMES = Set.of(
        "The Whisperer", "Araxxor", "Maggot King", "Branda the Fire Queen",
        "Eldric the Ice King", "Crystalline Hunllef", "Corrupted Hunllef",
        "The Gauntlet", "Corrupted Gauntlet", "Shellbane gryphon",
        "Obor (Chest)", "Bryophyta (Chest)");
    private static final Map<String, Integer> CHAT_LOOT = Map.of(
        "You catch a giant blue krill!", ItemID.POH_TROPHYDROP_GIANT_KRILL,
        "You catch a golden haddock!", ItemID.POH_TROPHYDROP_HADDOCK,
        "You catch a orangefin!", ItemID.POH_TROPHYDROP_YELLOWFIN,
        "You catch a huge halibut!", ItemID.POH_TROPHYDROP_HALIBUT,
        "You catch a purplefin!", ItemID.POH_TROPHYDROP_BLUEFIN,
        "You catch a swift marlin!", ItemID.POH_TROPHYDROP_MARLIN,
        "You've received some paint!", ItemID.SAILING_PAINT_ANGLERS);

    private final Client client;
    private final ItemManager itemManager;
    private final BooleanSupplier enabled;
    private final Supplier<Set<Integer>> trackedItems;
    private final Supplier<String> currentRsn;
    private final Consumer<JsonObject> sender;
    private final Map<String, Integer> clogItems = new HashMap<>();
    private final Map<Integer, ObservedInflow> selfDrops = new HashMap<>();
    private final Map<Integer, ObservedInflow> unequips = new HashMap<>();
    private final Map<Integer, Integer> equipment = new HashMap<>();
    private final List<PendingLoot> pending = new ArrayList<>();
    private final Map<String, RecentIdentity> recentIdentities = new HashMap<>();
    private int tick;

    public NightLegionLootPointTracker(Client client, ItemManager itemManager,
        BooleanSupplier enabled, Supplier<Set<Integer>> trackedItems,
        Supplier<String> currentRsn, Consumer<JsonObject> sender)
    {
        this.client = client;
        this.itemManager = itemManager;
        this.enabled = enabled;
        this.trackedItems = trackedItems;
        this.currentRsn = currentRsn;
        this.sender = sender;
    }

    public void onServerNpcLoot(ServerNpcLoot event)
    {
        if (!enabled.getAsBoolean()) return;
        int npcId = event.getComposition().getId();
        String name = event.getComposition().getName();
        if (npcId != NpcID.YAMA && npcId != NpcID.HESPORI && !MAD_ANGEL_IDS.contains(npcId)
            && !SAILING_NPC_IDS.contains(npcId) && !name.startsWith("Hallowed Sepulchre")) return;
        handle(event.getItems(), name, "NPC", npcId, 0);
    }

    public void onNpcLootReceived(NpcLootReceived event)
    {
        if (!enabled.getAsBoolean()) return;
        NPC npc = event.getNpc();
        if (SPECIAL_NPC_IDS.contains(npc.getId()) || MAD_ANGEL_IDS.contains(npc.getId())
            || SAILING_NPC_IDS.contains(npc.getId())) return;
        handle(event.getItems(), npc.getName(), "NPC", npc.getId(), npc.getIndex());
    }

    public void onPlayerLootReceived(PlayerLootReceived event)
    {
        if (!enabled.getAsBoolean()) return;
        String player = event.getPlayer().getName();
        handle(event.getItems(), player, "PLAYER", null, player == null ? 0 : player.hashCode());
    }

    public void onLootReceived(LootReceived event)
    {
        if (!enabled.getAsBoolean()) return;
        if (event.getType() == LootRecordType.EVENT || event.getType() == LootRecordType.PICKPOCKET)
        {
            handle(event.getItems(), event.getName(), "EVENT", null, 0);
        }
        else if (event.getType() == LootRecordType.NPC && SPECIAL_NPC_NAMES.contains(event.getName()))
        {
            String type = "The Gauntlet".equals(event.getName()) || "Corrupted Gauntlet".equals(event.getName())
                ? "EVENT" : "NPC";
            handle(event.getItems(), event.getName(), type, null, 0);
        }
    }

    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (!"Drop".equals(event.getMenuOption()) || event.getItemId() <= 0) return;
        int quantity = 1;
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory != null)
        {
            Item item = inventory.getItem(event.getParam0());
            if (item != null && item.getId() == event.getItemId()) quantity = item.getQuantity();
        }
        record(selfDrops, event.getItemId(), quantity, SELF_DROP_TICKS);
    }

    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() != InventoryID.WORN) return;
        Map<Integer, Integer> current = counts(event.getItemContainer());
        for (Map.Entry<Integer, Integer> old : equipment.entrySet())
        {
            int removed = old.getValue() - current.getOrDefault(old.getKey(), 0);
            if (removed > 0) record(unequips, old.getKey(), removed, UNEQUIP_TICKS);
        }
        equipment.clear();
        equipment.putAll(current);
    }

    public void onGameMessage(String message)
    {
        if (!enabled.getAsBoolean()) return;
        Matcher matcher = CLOG.matcher(message);
        if (matcher.find())
        {
            String itemName = matcher.group("item").trim();
            clogItems.put(itemName.toLowerCase(java.util.Locale.ROOT), tick);
            submitCollectionItem(itemName);
            return;
        }
        if ("You have found the Pharaoh's sceptre!".equals(message)
            || "You have found a Pharaoh's sceptre!".equals(message))
        {
            handle(List.of(new ItemStack(ItemID.PHARAOHS_SCEPTRE, 1)), "Pyramid Plunder", "EVENT", null, 0);
            return;
        }
        Integer itemId = CHAT_LOOT.get(message);
        if (itemId != null) handle(List.of(new ItemStack(itemId, 1)), "Deep sea trawling", "EVENT", null, 0);
    }

    public void onGameTick()
    {
        tick++;
        Iterator<PendingLoot> iterator = pending.iterator();
        while (iterator.hasNext())
        {
            PendingLoot loot = iterator.next();
            if (tick < loot.sendTick) continue;
            iterator.remove();
            JsonArray clean = new JsonArray();
            JsonArray suspected = new JsonArray();
            for (TrackedItem item : loot.items)
            {
                String reason = item.suspectReason == null
                    ? suspect(item.itemId, item.quantity, loot.lootTick) : item.suspectReason;
                JsonObject row = item.json();
                Integer clogTick = clogItems.get(item.name.toLowerCase(java.util.Locale.ROOT));
                row.addProperty("is_new_collection_log_item", clogTick != null && tick - clogTick <= CLOG_TICKS);
                if (reason == null) clean.add(row);
                else
                {
                    row.addProperty("suspect_reason", reason);
                    suspected.add(row);
                }
            }
            loot.payload.add("items", clean);
            if (suspected.size() > 0) loot.payload.add("suspected_items", suspected);
            log.debug("Rank loot flush: eventId={}, clean={}, suspected={}, currentRsn={}",
                loot.payload.get("event_id").getAsString(), clean.size(), suspected.size(), currentRsn.get());
            sender.accept(loot.payload);
        }
        clogItems.values().removeIf(value -> tick - value > CLOG_TICKS);
        selfDrops.values().removeIf(value -> tick - value.tick > SELF_DROP_TICKS + BUFFER_TICKS);
        unequips.values().removeIf(value -> tick - value.tick > UNEQUIP_TICKS + BUFFER_TICKS);
        recentIdentities.values().removeIf(value -> tick - value.tick > BUFFER_TICKS);
    }

    private void handle(Collection<ItemStack> stacks, String source, String sourceType,
        Integer sourceId, int sourceInstance)
    {
        List<TrackedItem> rows = new ArrayList<>();
        Set<Integer> catalogue = trackedItems.get();
        for (ItemStack stack : stacks)
        {
            int itemId = stack.getId();
            int unitValue = Math.max(0, itemManager.getItemPrice(itemId));
            if (unitValue < MIN_VALUE && (catalogue == null || !catalogue.contains(itemId))) continue;
            String name = itemManager.getItemComposition(itemId).getName();
            String reason = suspect(itemId, stack.getQuantity(), tick);
            rows.add(new TrackedItem(itemId, name, stack.getQuantity(), unitValue, reason));
            log.debug("Rank loot received: itemId={}, name={}, quantity={}, unitValue={}, totalValue={}, currentRsn={}, qualifies={}",
                itemId, name, stack.getQuantity(), unitValue, (long) unitValue * stack.getQuantity(),
                currentRsn.get(), unitValue >= MIN_VALUE || catalogue.contains(itemId));
        }
        if (rows.isEmpty()) return;
        String fingerprint = fingerprint(source, sourceType, sourceId, sourceInstance, rows);
        RecentIdentity recent = recentIdentities.get(fingerprint);
        String eventId = recent != null && tick - recent.tick <= BUFFER_TICKS
            ? recent.id : UUID.randomUUID().toString();
        recentIdentities.put(fingerprint, new RecentIdentity(eventId, tick));
        JsonObject payload = new JsonObject();
        payload.addProperty("event_type", "loot");
        payload.addProperty("event_id", eventId);
        payload.addProperty("source", source == null ? "Loot" : source);
        payload.addProperty("source_type", sourceType);
        if (sourceId != null) payload.addProperty("source_id", sourceId);
        pending.add(new PendingLoot(payload, rows, tick, tick + BUFFER_TICKS));
    }

    private void submitCollectionItem(String itemName)
    {
        try
        {
            for (net.runelite.http.api.item.ItemPrice candidate : itemManager.search(itemName))
            {
                if (candidate.getName() == null || !candidate.getName().trim().equalsIgnoreCase(itemName)) continue;
                if (!trackedItems.get().contains(candidate.getId())) return;
                JsonObject payload = new JsonObject();
                payload.addProperty("event_type", "collection_item");
                payload.addProperty("event_id", UUID.randomUUID().toString());
                payload.addProperty("source", "Collection Log");
                JsonArray items = new JsonArray();
                TrackedItem item = new TrackedItem(candidate.getId(), candidate.getName(), 1,
                    Math.max(0, itemManager.getItemPrice(candidate.getId())), null);
                items.add(item.json());
                payload.add("items", items);
                sender.accept(payload);
                return;
            }
        }
        catch (Exception error)
        {
            log.debug("Could not resolve Collection Log item {}", itemName, error);
        }
    }

    private String suspect(int itemId, int quantity, int lootTick)
    {
        ObservedInflow drop = selfDrops.get(itemId);
        if (drop != null && lootTick - drop.tick <= SELF_DROP_TICKS && drop.matches(quantity)) return "self-drop";
        ObservedInflow unequip = unequips.get(itemId);
        if (unequip != null && Math.abs(lootTick - unequip.tick) <= UNEQUIP_TICKS && unequip.matches(quantity)) return "gear-swap";
        return null;
    }

    private void record(Map<Integer, ObservedInflow> values, int itemId, int quantity, int window)
    {
        ObservedInflow value = values.get(itemId);
        if (value == null || tick - value.tick > window)
        {
            value = new ObservedInflow();
            values.put(itemId, value);
        }
        value.tick = tick;
        value.lastQuantity = quantity;
        value.totalQuantity += quantity;
    }

    private static Map<Integer, Integer> counts(ItemContainer container)
    {
        Map<Integer, Integer> values = new HashMap<>();
        if (container != null)
            for (Item item : container.getItems())
                if (item.getId() > 0) values.merge(item.getId(), item.getQuantity(), Integer::sum);
        return values;
    }

    private String fingerprint(String source, String type, Integer id, int instance, List<TrackedItem> items)
    {
        List<String> values = new ArrayList<>();
        for (TrackedItem item : items) values.add(item.itemId + "x" + item.quantity);
        java.util.Collections.sort(values);
        return type + "|" + id + "|" + instance + "|" + source + "|" + String.join(",", values);
    }

    private static final class ObservedInflow
    {
        int tick;
        int lastQuantity;
        int totalQuantity;
        boolean matches(int quantity) { return quantity == lastQuantity || quantity == totalQuantity; }
    }

    private static final class RecentIdentity
    {
        final String id;
        final int tick;
        RecentIdentity(String id, int tick) { this.id = id; this.tick = tick; }
    }

    private static final class PendingLoot
    {
        final JsonObject payload;
        final List<TrackedItem> items;
        final int lootTick;
        final int sendTick;
        PendingLoot(JsonObject payload, List<TrackedItem> items, int lootTick, int sendTick)
        {
            this.payload = payload; this.items = items; this.lootTick = lootTick; this.sendTick = sendTick;
        }
    }

    private static final class TrackedItem
    {
        final int itemId;
        final String name;
        final int quantity;
        final int unitValue;
        final String suspectReason;
        TrackedItem(int itemId, String name, int quantity, int unitValue, String suspectReason)
        {
            this.itemId = itemId; this.name = name; this.quantity = quantity;
            this.unitValue = unitValue; this.suspectReason = suspectReason;
        }
        JsonObject json()
        {
            JsonObject value = new JsonObject();
            value.addProperty("item_id", itemId);
            value.addProperty("name", name);
            value.addProperty("quantity", quantity);
            value.addProperty("unit_value", unitValue);
            value.addProperty("total_value", (long) unitValue * quantity);
            return value;
        }
    }
}
