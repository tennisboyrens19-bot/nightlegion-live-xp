package com.revalclan;

import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

public class BankOwnershipTest {
    private long account = 1;
    private boolean open = true;
    private boolean seasonal;

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return -1;
        if (type == double.class) return 0.0;
        return null;
    }

    private static void inject(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    @SuppressWarnings("unchecked")
    private Set<Integer> owned(PlayerDataCollector collector) throws Exception {
        Field field = PlayerDataCollector.class.getDeclaredField("bankOwnedItems");
        field.setAccessible(true);
        return (Set<Integer>) field.get(collector);
    }

    @Test
    public void bankEvidenceExcludesPlaceholdersAndIsScopedToAccountAndWorld() throws Exception {
        ItemContainer container = (ItemContainer) Proxy.newProxyInstance(ItemContainer.class.getClassLoader(),
            new Class<?>[]{ItemContainer.class}, (p, m, a) -> m.getName().equals("getItems")
                ? new Item[]{new Item(30793, 1), new Item(6570, 0), new Item(999, 1)} : null);
        Widget bank = (Widget) Proxy.newProxyInstance(Widget.class.getClassLoader(), new Class<?>[]{Widget.class},
            (p, m, a) -> m.getName().equals("isHidden") ? !open : defaultValue(m.getReturnType()));
        Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class},
            (p, m, a) -> {
                switch (m.getName()) {
                    case "getAccountHash": return account;
                    case "getWorldType": return seasonal ? EnumSet.of(WorldType.SEASONAL) : EnumSet.noneOf(WorldType.class);
                    case "getWidget": return bank;
                    case "getItemContainer": return container;
                    case "isClientThread": return true;
                    case "getItemDefinition":
                        int id = (int) a[0];
                        return Proxy.newProxyInstance(ItemComposition.class.getClassLoader(),
                            new Class<?>[]{ItemComposition.class}, (ip, im, ia) -> {
                                if (im.getName().equals("getId")) return id;
                                if (im.getName().equals("getPlaceholderTemplateId")) return id == 999 ? 14401 : -1;
                                return defaultValue(im.getReturnType());
                            });
                    default: return defaultValue(m.getReturnType());
                }
            });
        // Use RuneLite's actual canonicalization with a game-client fixture;
        // the executor does not launch network price refreshes in this test.
        Constructor<?> constructor = ItemManager.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Object[] args = new Object[constructor.getParameterCount()];
        Class<?>[] types = constructor.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (types[i] == Client.class) args[i] = client;
            else if (types[i] == EventBus.class) args[i] = new EventBus();
            else if (types[i].isInterface()) args[i] = Proxy.newProxyInstance(types[i].getClassLoader(),
                new Class<?>[]{types[i]}, (p, m, a) -> defaultValue(m.getReturnType()));
        }
        PlayerDataCollector collector = new PlayerDataCollector();
        inject(collector, "client", client);
        inject(collector, "itemManager", constructor.newInstance(args));
        collector.captureBankItems();
        assertEquals(java.util.Collections.singleton(30793), owned(collector));
        open = false;
        collector.captureBankItems();
        assertTrue(owned(collector).contains(30793));
        account = 2;
        collector.captureBankItems();
        assertTrue(owned(collector).isEmpty());
        open = true;
        seasonal = true;
        collector.captureBankItems();
        assertTrue(owned(collector).isEmpty());
    }
}
