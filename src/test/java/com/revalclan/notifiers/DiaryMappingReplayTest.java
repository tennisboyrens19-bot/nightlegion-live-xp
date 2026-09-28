package com.revalclan.notifiers;

import com.revalclan.diaries.AchievementDiaryManager;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Current gameval constants independently drive both event and snapshot paths. */
@RunWith(Parameterized.class)
public class DiaryMappingReplayTest {
    @Parameterized.Parameters(name = "{0} {1} ({2})")
    public static Collection<Object[]> mappings() throws Exception {
        List<Object[]> result = new ArrayList<>();
        for (Field field : VarbitID.class.getFields()) {
            if (!field.getName().matches("[A-Z]+_DIARY_(EASY|MEDIUM|HARD|ELITE)_COMPLETE")) continue;
            String[] parts = field.getName().split("_");
            result.add(new Object[]{parts[0], parts[2], field.getInt(null)});
        }
        result.add(new Object[]{"KARAMJA", "EASY", VarbitID.ATJUN_EASY_DONE});
        result.add(new Object[]{"KARAMJA", "MEDIUM", VarbitID.ATJUN_MED_DONE});
        result.add(new Object[]{"KARAMJA", "HARD", VarbitID.ATJUN_HARD_DONE});
        assertEquals("Every current region/tier must be covered", 48, result.size());
        return result;
    }

    private final String region;
    private final String tier;
    private final int varbit;

    public DiaryMappingReplayTest(String region, String tier, int varbit) {
        this.region = region;
        this.tier = tier;
        this.varbit = varbit;
    }

    @Test public void eventAndSnapshotAgreeForCurrentGamevalMapping() throws Exception {
        Client client = mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getIntStack()).thenReturn(new int[]{1});
        List<Map<String, Object>> emitted = new ArrayList<>();
        DiaryNotifier notifier = new DiaryNotifier() {
            @Override public boolean isEnabled() { return true; }
            @Override protected void sendNotification(Map<String, Object> data) { emitted.add(data); }
        };
        notifier.client = client;
        ClientThread thread = mock(ClientThread.class);
        doAnswer(call -> { call.<BooleanSupplier>getArgument(0).getAsBoolean(); return null; })
            .when(thread).invokeLater(any(BooleanSupplier.class));
        inject(notifier, DiaryNotifier.class, "clientThread", thread);
        for (int i = 0; i < 5; i++) notifier.onGameTick();

        int complete = region.equals("KARAMJA") && !tier.equals("ELITE") ? 2 : 1;
        VarbitChanged event = new VarbitChanged();
        event.setVarbitId(varbit);
        event.setValue(complete);
        notifier.onVarbitChanged(event);
        notifier.onVarbitChanged(event);
        assertEquals(1, emitted.size());
        assertEquals(region, emitted.get(0).get("area").toString().toUpperCase(java.util.Locale.ROOT));
        assertEquals(tier, emitted.get(0).get("difficulty").toString().toUpperCase(java.util.Locale.ROOT));
        assertEquals(varbit, emitted.get(0).get("varbitId"));
        assertEquals(1, emitted.get(0).get("totalDiariesCompleted"));

        AchievementDiaryManager manager = new AchievementDiaryManager();
        inject(manager, AchievementDiaryManager.class, "client", client);
        when(client.getVarbitValue(varbit)).thenReturn(complete);
        Map<String, Object> snapshot = manager.sync();
        assertEquals(1, snapshot.get("totalCompleted"));
        Map<String, Map<String, Boolean>> progress = (Map<String, Map<String, Boolean>>) snapshot.get("progress");
        Map<String, Boolean> matchingRegion = progress.entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase(region)).findFirst().orElseThrow().getValue();
        assertEquals(Boolean.TRUE, matchingRegion.get(tier.toLowerCase(java.util.Locale.ROOT)));
    }

    private static void inject(Object target, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
