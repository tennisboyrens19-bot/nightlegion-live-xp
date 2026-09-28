package com.revalclan.ui;

import com.google.gson.Gson;
import com.revalclan.api.account.AccountResponse;
import com.revalclan.api.points.PointsResponse;
import com.revalclan.util.NumberFmt;
import org.junit.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Fractional staff adjustments travel through the actual account and Swing models. */
public class PointsPrecisionTest {
    @Test public void fractionalAdjustmentsSurviveAccountCardsGroupingAndSorting() throws Exception {
        AccountResponse response = new Gson().fromJson("{\"data\":{"
            + "\"osrsAccount\":{\"osrsNickname\":\"Fixture\",\"clanRank\":\"bronze\",\"activityPoints\":1.5,\"maintenancePoints\":0.125},"
            + "\"pointsBreakdown\":{\"drops\":2,\"total\":1.5},\"pointsLog\":["
            + "{\"pointsChange\":1,\"pointsAfter\":1,\"sourceType\":\"manual\",\"sourceDescription\":\"Staff adjustment\"},"
            + "{\"pointsChange\":-0.5,\"pointsAfter\":0.5,\"sourceType\":\"manual\",\"sourceDescription\":\"Staff adjustment\"},"
            + "{\"pointsChange\":1,\"pointsAfter\":1.5,\"sourceType\":\"manual\",\"sourceDescription\":\"Staff adjustment\"}"
            + "]}}", AccountResponse.class);
        AccountResponse.AccountData data = response.getData();
        assertEquals(1.5, data.getOsrsAccount().getActivityPoints(), 0);
        assertEquals(0.125, data.getOsrsAccount().getMaintenancePoints(), 0);
        assertEquals(1.5, data.getPointsBreakdown().getTotal(), 0);
        assertEquals(-0.5, data.getPointsLog().get(1).getPointsChange(), 0);
        assertEquals(0.5, data.getPointsLog().get(1).getPointsAfter(), 0);
        assertEquals(data.getOsrsAccount().getActivityPoints(),
            data.getPointsLog().stream().mapToDouble(AccountResponse.PointsLogEntry::getPointsChange).sum(), 0);

        SwingUtilities.invokeAndWait(() -> {
            try {
                ProfilePanel profile = new ProfilePanel();
                List<PointsResponse.Rank> ranks = List.of(
                    new Gson().fromJson("{\"name\":\"bronze\",\"displayName\":\"Bronze\",\"pointsRequired\":0}", PointsResponse.Rank.class),
                    new Gson().fromJson("{\"name\":\"iron\",\"displayName\":\"Iron\",\"pointsRequired\":2}", PointsResponse.Rank.class));
                set(profile, "ranks", ranks);
                List<String> header = labels((Container) invoke(profile, "buildHeaderSection", AccountResponse.OsrsAccount.class, data.getOsrsAccount()));
                assertTrue(header.toString(), header.contains("1.5"));
                assertTrue(header.toString(), header.contains("0.5 pts to"));
                List<String> breakdown = labels((Container) invoke(profile, "buildPointsSection", AccountResponse.PointsBreakdown.class, data.getPointsBreakdown()));
                assertTrue(breakdown.toString(), breakdown.contains("-0.5"));

                PointsAlbumWindow album = album(data.getPointsLog());
                invoke(album, "rebuild");
                assertEquals("3 entries - 1.5 pts", ((JLabel) get(album, "summaryLabel")).getText());
                List<?> cards = (List<?>) get(album, "filtered");
                assertEquals(-0.5, (double) get(cards.get(0), "points"), 0);
                assertTrue(labels((Container) get(album, "gridPanel")).contains("-0.5 pts"));
                ((JCheckBox) get(album, "groupToggle")).setSelected(true);
                invoke(album, "rebuild");
                cards = (List<?>) get(album, "filtered");
                assertEquals(1, cards.size());
                assertEquals(1.5, (double) get(cards.get(0), "points"), 0);
                assertTrue(labels((Container) get(album, "gridPanel")).contains("+1.5 pts"));
            } catch (Exception error) { throw new AssertionError(error); }
        });
    }

    @Test public void fourDecimalTotalsAndNegativeDeltasRemainVisible() throws Exception {
        AccountResponse response = new Gson().fromJson("{\"data\":{"
            + "\"osrsAccount\":{\"activityPoints\":0.2344},\"pointsBreakdown\":{\"drops\":0.2345,\"total\":0.2344},"
            + "\"pointsLog\":[{\"pointsChange\":0.1234,\"pointsAfter\":0.1234,\"sourceType\":\"manual\"},"
            + "{\"pointsChange\":0.1111,\"pointsAfter\":0.2345,\"sourceType\":\"manual\"},"
            + "{\"pointsChange\":-0.0001,\"pointsAfter\":0.2344,\"sourceType\":\"manual\"}]}}", AccountResponse.class);
        assertEquals(0.2344, response.getData().getOsrsAccount().getActivityPoints(), 0);
        assertEquals(0.2344, response.getData().getPointsLog().get(2).getPointsAfter(), 0);
        SwingUtilities.invokeAndWait(() -> {
            try {
                PointsAlbumWindow album = album(response.getData().getPointsLog());
                invoke(album, "rebuild");
                assertEquals("3 entries - 0.2344 pts", ((JLabel) get(album, "summaryLabel")).getText());
                assertTrue(labels((Container) get(album, "gridPanel")).contains("-0.0001 pts"));
                ProfilePanel profile = new ProfilePanel();
                List<String> breakdown = labels((Container) invoke(profile, "buildPointsSection", AccountResponse.PointsBreakdown.class, response.getData().getPointsBreakdown()));
                assertTrue(breakdown.toString(), breakdown.contains("0.2345"));
                assertTrue(breakdown.toString(), breakdown.contains("-0.0001"));
            } catch (Exception error) { throw new AssertionError(error); }
        });
    }

    @Test public void pointFormattingKeepsPrecisionAndAvoidsNegativeZeroInAnyLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("1 234.5678", NumberFmt.group(1234.5678));
            assertEquals("-0.0001", NumberFmt.group(-0.0001));
            assertEquals("0", NumberFmt.group(-0.0000000001));
            assertEquals("2", NumberFmt.group(2.0));
            assertEquals("1 234", NumberFmt.group(1234L));
        } finally { Locale.setDefault(original); }
    }

    private static PointsAlbumWindow album(List<AccountResponse.PointsLogEntry> entries) throws Exception {
        PointsAlbumWindow album = mock(PointsAlbumWindow.class, CALLS_REAL_METHODS);
        set(album, "allEntries", entries);
        set(album, "sourceCombo", new JComboBox<>(new String[]{"All"}));
        JComboBox<String> sort = new JComboBox<>(new String[]{"Newest", "Oldest", "Highest", "Lowest"});
        sort.setSelectedIndex(3);
        set(album, "sortCombo", sort);
        set(album, "searchField", new JTextField());
        set(album, "groupToggle", new JCheckBox());
        set(album, "summaryLabel", new JLabel());
        set(album, "pageLabel", new JLabel());
        set(album, "prevButton", new JButton());
        set(album, "nextButton", new JButton());
        set(album, "gridPanel", new JPanel());
        return album;
    }

    private static Object invoke(Object target, String method) throws Exception {
        Method callable = target instanceof PointsAlbumWindow ? PointsAlbumWindow.class.getDeclaredMethod(method) : target.getClass().getDeclaredMethod(method);
        callable.setAccessible(true);
        return callable.invoke(target);
    }

    private static Object invoke(Object target, String method, Class<?> argumentType, Object argument) throws Exception {
        Method callable = target.getClass().getDeclaredMethod(method, argumentType);
        callable.setAccessible(true);
        return callable.invoke(target, argument);
    }

    private static Field field(Object target, String name) throws Exception {
        Class<?> type = target instanceof PointsAlbumWindow ? PointsAlbumWindow.class : target.getClass();
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    private static Object get(Object target, String name) throws Exception { return field(target, name).get(target); }
    private static void set(Object target, String name, Object value) throws Exception { field(target, name).set(target, value); }

    private static List<String> labels(Container container) {
        List<String> result = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) result.add(((JLabel) component).getText());
            if (component instanceof Container) result.addAll(labels((Container) component));
        }
        return result;
    }
}
