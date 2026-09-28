package com.revalclan.ui;

import com.google.gson.Gson;
import com.revalclan.api.RevalApiService;
import com.revalclan.api.achievements.AchievementsResponse;
import com.revalclan.api.diaries.DiariesResponse;
import com.revalclan.ui.components.LoginPrompt;
import net.runelite.api.Client;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Replays real progress models and Swing callbacks across logout, login and refresh. */
public class ProgressPanelSessionBoundaryTest {
    private final Api api = new Api();
    private final Client client = mock(Client.class);
    private final AchievementsPanel achievements = new AchievementsPanel();
    private final DiaryPanel diaries = new DiaryPanel();

    @Before public void setup() throws Exception {
        when(client.getAccountHash()).thenReturn(11L);
        SwingUtilities.invokeAndWait(() -> {
            achievements.init(api, client);
            diaries.init(api, client, null);
        });
    }

    @Test public void lateOldAccountSuccessAndErrorCannotOverwriteCurrentProgress() throws Exception {
        request();
        logout();
        when(client.getAccountHash()).thenReturn(22L);
        request();
        deliver(1, "current-account");
        deliver(0, "old-account");
        api.achievementErrors.get(0).accept(new RuntimeException("Previous account timeout"));
        api.diaryErrors.get(0).accept(new RuntimeException("Previous account timeout"));
        flush();
        assertCurrent("current-account");
    }

    @Test public void logoutClearsModelsSelectedDetailAndLateCallbacksPreserveLoginPrompt() throws Exception {
        request();
        deliver(0, "old-account");
        SwingUtilities.invokeAndWait(() -> {
            try {
                Method detail = DiaryPanel.class.getDeclaredMethod("showDiaryDetail", DiariesResponse.Diary.class);
                detail.setAccessible(true);
                detail.invoke(diaries, ((List<?>) get(diaries, "allDiaries")).get(0));
                ((Set<String>) get(diaries, "expandedTiers")).add("old-account_easy");
            } catch (Exception error) { throw new AssertionError(error); }
        });
        request();
        logout();
        assertTrue(((List<?>) get(achievements, "achievements")).isEmpty());
        assertTrue(((List<?>) get(diaries, "allDiaries")).isEmpty());
        assertNull(get(diaries, "selectedDiary"));
        assertTrue(((Set<?>) get(diaries, "expandedTiers")).isEmpty());
        assertEquals(1, ((JPanel) get(diaries, "mainContainer")).getComponentCount());
        deliver(1, "old-account");
        api.achievementErrors.get(1).accept(new RuntimeException("Previous account timeout"));
        api.diaryErrors.get(1).accept(new RuntimeException("Previous account timeout"));
        flush();
        assertTrue(((List<?>) get(achievements, "achievements")).isEmpty());
        assertTrue(((List<?>) get(diaries, "allDiaries")).isEmpty());
        assertTrue(containsPrompt(achievements));
        assertTrue(containsPrompt(diaries));
    }

    @Test public void refreshSupersedesEarlierProgressRequestsWithinSameAccount() throws Exception {
        request();
        request();
        deliver(1, "refreshed");
        deliver(0, "previous");
        api.achievementErrors.get(0).accept(new RuntimeException("Previous refresh timeout"));
        api.diaryErrors.get(0).accept(new RuntimeException("Previous refresh timeout"));
        flush();
        assertCurrent("refreshed");
    }

    private void request() throws Exception { SwingUtilities.invokeAndWait(() -> { achievements.refresh(); diaries.refresh(); }); }
    private void logout() throws Exception { SwingUtilities.invokeAndWait(() -> { achievements.onLoggedOut(); diaries.onLoggedOut(); }); }
    private void deliver(int request, String id) throws Exception {
        Gson gson = new Gson();
        api.achievements.get(request).accept(gson.fromJson("{\"status\":\"success\",\"data\":{\"achievements\":[{\"id\":\"" + id
            + "\",\"name\":\"Fixture milestone\",\"rarity\":\"common\",\"progress\":{\"isCompleted\":true}}]}}", AchievementsResponse.class));
        api.diaries.get(request).accept(gson.fromJson("{\"status\":\"success\",\"data\":{\"diaries\":[{\"id\":\"" + id
            + "\",\"name\":\"Fixture diary\",\"tiers\":[]}]}}", DiariesResponse.class));
        flush();
    }
    private void assertCurrent(String id) throws Exception {
        List<?> achievementRows = (List<?>) get(achievements, "achievements");
        List<?> diaryRows = (List<?>) get(diaries, "allDiaries");
        assertEquals(1, achievementRows.size()); assertEquals(1, diaryRows.size());
        assertEquals(id, ((AchievementsResponse.Achievement) achievementRows.get(0)).getId());
        assertEquals(id, ((DiariesResponse.Diary) diaryRows.get(0)).getId());
    }
    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    private static boolean containsPrompt(Component component) {
        if (component instanceof LoginPrompt) return true;
        if (component instanceof Container) for (Component child : ((Container) component).getComponents()) if (containsPrompt(child)) return true;
        return false;
    }
    private static void flush() throws Exception { SwingUtilities.invokeAndWait(() -> {}); }
    private static class Api extends RevalApiService {
        final List<Consumer<AchievementsResponse>> achievements = new ArrayList<>();
        final List<Consumer<DiariesResponse>> diaries = new ArrayList<>();
        final List<Consumer<Exception>> achievementErrors = new ArrayList<>(), diaryErrors = new ArrayList<>();
        Api() { super(null, new Gson()); }
        @Override public void fetchAchievementDefinitions(Long hash, Consumer<AchievementsResponse> ok, Consumer<Exception> error) {
            achievements.add(ok); achievementErrors.add(error);
        }
        @Override public void fetchDiaries(Long hash, Consumer<DiariesResponse> ok, Consumer<Exception> error) {
            diaries.add(ok); diaryErrors.add(error);
        }
    }
}
