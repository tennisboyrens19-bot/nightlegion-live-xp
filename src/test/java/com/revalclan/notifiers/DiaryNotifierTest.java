package com.revalclan.notifiers;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DiaryNotifierTest {
    private static class RecordingNotifier extends DiaryNotifier {
        final List<Map<String, Object>> notifications = new ArrayList<>();
        @Override public boolean isEnabled() { return true; }
        @Override protected void sendNotification(Map<String, Object> data) { notifications.add(data); }
    }

    private RecordingNotifier notifier() throws Exception {
        RecordingNotifier notifier = new RecordingNotifier();
        notifier.client = mock(Client.class);
        when(notifier.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(notifier.client.getIntStack()).thenReturn(new int[]{1});
        ClientThread thread = mock(ClientThread.class);
        doAnswer(invocation -> {
            invocation.<BooleanSupplier>getArgument(0).getAsBoolean();
            return null;
        }).when(thread).invokeLater(any(BooleanSupplier.class));
        Field field = DiaryNotifier.class.getDeclaredField("clientThread");
        field.setAccessible(true);
        field.set(notifier, thread);
        for (int i = 0; i < 5; i++) notifier.onGameTick();
        return notifier;
    }

    private static void change(RecordingNotifier notifier, int id, int value) {
        VarbitChanged event = new VarbitChanged();
        event.setVarbitId(id);
        event.setValue(value);
        notifier.onVarbitChanged(event);
    }

    @Test
    public void everyCurrentRuneLiteDiaryCompletionEmitsTheCorrectAreaAndTierOnce() throws Exception {
        RecordingNotifier notifier = notifier();
        int count = 0;
        for (Field field : Varbits.class.getFields()) {
            if (!field.getName().matches("DIARY_[A-Z]+_(EASY|MEDIUM|HARD|ELITE)")) continue;
            int id = field.getInt(null);
            String[] parts = field.getName().split("_");
            change(notifier, id, 2);
            change(notifier, id, 2);
            assertEquals(++count, notifier.notifications.size());
            Map<String, Object> event = notifier.notifications.get(count - 1);
            assertEquals(parts[1], event.get("area").toString().toUpperCase(java.util.Locale.ROOT));
            assertEquals(parts[2], event.get("difficulty").toString().toUpperCase(java.util.Locale.ROOT));
            assertEquals(id, event.get("varbitId"));
            assertEquals(count, event.get("totalDiariesCompleted"));
        }
        assertEquals(48, count);
    }

    @Test
    public void karamjaStartedIsNotCompleteAndUnrelatedVarbitsDoNotEmit() throws Exception {
        RecordingNotifier notifier = notifier();
        for (int id : new int[]{Varbits.DIARY_KARAMJA_EASY, Varbits.DIARY_KARAMJA_MEDIUM, Varbits.DIARY_KARAMJA_HARD}) {
            change(notifier, id, 1);
        }
        change(notifier, 3577, 1);
        assertTrue(notifier.notifications.isEmpty());
        change(notifier, Varbits.DIARY_KARAMJA_EASY, 2);
        assertEquals(1, notifier.notifications.size());
        notifier.reset();
        change(notifier, Varbits.DIARY_ARDOUGNE_EASY, 1);
        assertEquals(1, notifier.notifications.size());
    }
}
