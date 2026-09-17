package com.revalclan.sync;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;

/** Only observed one-time milestone item IDs are uploaded, never a bank dump. */
@Singleton
public class MilestoneEvidence {
    private static final Set<Integer> ITEMS = Set.of(12954, 22322, 6570, 21295, 21439,
        28947, 27372, 27255, 27248, 30793, 28336, 9813, 19476, 13280);
    @Inject private Client client;
    @Inject private ItemManager itemManager;
    private long account = -1;
    private final Set<Integer> observed = new HashSet<>();
    private boolean bankSeen;

    public void reset() { account = -1; observed.clear(); bankSeen = false; }
    public List<Integer> collect() {
        if (account != client.getAccountHash()) {
            reset();
            account = client.getAccountHash();
        }
        inspect(client.getItemContainer(93));
        inspect(client.getItemContainer(94));
        if (client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER) != null
            && !client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER).isHidden()) {
            ItemContainer bank = client.getItemContainer(95);
            if (bank != null) { inspect(bank); bankSeen = true; }
        }
        List<Integer> result = new ArrayList<>(observed);
        result.sort(Integer::compareTo);
        return result;
    }
    private void inspect(ItemContainer container) {
        if (container == null || container.getItems() == null) return;
        for (Item item : container.getItems()) {
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0) continue;
            ItemComposition composition = itemManager.getItemComposition(item.getId());
            if (composition == null || composition.getPlaceholderTemplateId() != -1) continue;
            int canonical = itemManager.canonicalize(item.getId());
            if (ITEMS.contains(canonical)) observed.add(canonical);
        }
    }
    public boolean hasSeenBank() { return bankSeen; }
}
