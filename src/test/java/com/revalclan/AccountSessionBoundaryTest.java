package com.revalclan;

import com.revalclan.collectionlog.CollectionLogManager;
import com.revalclan.collectionlog.ObtainedCollectionItem;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import org.junit.Test;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AccountSessionBoundaryTest {
    @Test public void actualLogoutAndFreshLoginClearCollectionOwnership() throws Exception {
        RevalClanPlugin plugin = new RevalClanPlugin();
        for (String name : new String[]{"diaryNotifier", "eventFilterManager", "revalApiService", "clanMembership",
                "announcementService", "leaguesNotifier", "leaguesSyncNotifier", "lootNotifier", "varbitNotifier",
                "raidPartyTracker", "sessionTracker"}) {
            Field field = RevalClanPlugin.class.getDeclaredField(name);
            field.setAccessible(true); field.set(plugin, mock(field.getType()));
        }
        CollectionLogManager collection = new CollectionLogManager();
        Field field = RevalClanPlugin.class.getDeclaredField("collectionLogManager");
        field.setAccessible(true); field.set(plugin, collection);
        collection.getObtainedItems().put(21273, new ObtainedCollectionItem(21273, "Skotos", 1));
        GameStateChanged event = new GameStateChanged();
        event.setGameState(GameState.LOGIN_SCREEN);
        plugin.onGameStateChanged(event);
        assertTrue("Logout must clear the departing account immediately", collection.getObtainedItems().isEmpty());
        collection.getObtainedItems().put(20693, new ObtainedCollectionItem(20693, "Phoenix", 1));
        event.setGameState(GameState.LOGGED_IN);
        plugin.onGameStateChanged(event);
        assertTrue("Fresh login must not inherit previous observations", collection.getObtainedItems().isEmpty());
    }
}
