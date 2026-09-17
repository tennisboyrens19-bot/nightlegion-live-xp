package com.revalclan.ui;

import com.google.gson.Gson;
import com.revalclan.api.account.AccountResponse;
import com.revalclan.api.common.MonthlyMvp;
import com.revalclan.api.points.PointsResponse;
import com.revalclan.ui.components.Badge;
import com.revalclan.ui.constants.UIConstants;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProfileRegressionTest {
    private static final Gson GSON = new Gson();

    @Test public void unknownEfficiencyDoesNotBecomeZero() {
        for (Double value : new Double[]{null, Double.NaN, Double.POSITIVE_INFINITY, -1.0})
            assertEquals("--", ProfilePanel.formatEfficiency(value));
        assertEquals("0.0", ProfilePanel.formatEfficiency(0.0));
        assertEquals("123.5", ProfilePanel.formatEfficiency(123.456));
    }

    @Test public void oldResponseRemainsCompatibleAndNewResponseKeepsUnknowns() {
        AccountResponse old = GSON.fromJson("{\"data\":{\"osrsAccount\":{\"osrsNickname\":\"Test player\"}}}", AccountResponse.class);
        assertNull(old.getData().getMonthlyMvp());
        assertFalse(old.getData().isMonthlyMvpWinner());
        assertNull(old.getData().getOsrsAccount().getEhp());
        AccountResponse value = GSON.fromJson("{\"data\":{\"osrsAccount\":{\"ehp\":null,\"ehb\":0,\"calculatedRank\":\"prefect\"},\"pointsBreakdown\":{\"misc\":-40},\"monthlyMvpWinner\":true}}", AccountResponse.class);
        assertEquals("--", ProfilePanel.formatEfficiency(value.getData().getOsrsAccount().getEhp()));
        assertEquals("0.0", ProfilePanel.formatEfficiency(value.getData().getOsrsAccount().getEhb()));
        assertEquals(Integer.valueOf(-40), value.getData().getPointsBreakdown().getMisc());
        assertTrue(value.getData().isMonthlyMvpWinner());
    }

    @Test public void hidesOnlyTheFourRequestedDisplaySections() {
        for (String key : new String[]{"CLAN_ACTIVITY", "DISCORD_ACTIVITY", "CLAN_EVENTS", "WOM_ACTIVITY"})
            assertFalse(RankingPanel.isPointSectionVisible(key));
        for (String key : new String[]{"MILESTONES", "PETS", "VALUABLE_DROPS", "MISC", "UNTRADEABLE_DROPS"})
            assertTrue(RankingPanel.isPointSectionVisible(key));
    }

    @Test public void websiteIconIsConfiguredVisibleAndClickable() throws Exception {
        assertEquals("https://nightlegion-web.vercel.app/", RevalPanel.WEBSITE_URL);
        SwingUtilities.invokeAndWait(() -> {
            try {
                RevalPanel panel = new RevalPanel();
                Method method = RevalPanel.class.getDeclaredMethod("createSocialIcon", String.class, String.class);
                method.setAccessible(true);
                JLabel website = (JLabel) method.invoke(panel, RevalPanel.WEBSITE_URL, "Website");
                assertTrue(website.isVisible());
                assertTrue(website.getMouseListeners().length > 0);
                JLabel empty = (JLabel) method.invoke(panel, "", "Website");
                assertFalse(empty.isVisible());
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
        assertNotNull(getClass().getResource("/com/revalclan/ui/assets/website.png"));
    }

    @Test public void actualMilestoneRendererUsesCompletionEvidence() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                ProfilePanel panel = new ProfilePanel();
                PointsResponse data = GSON.fromJson("{\"data\":{\"pointSources\":{\"MILESTONES\":[{\"id\":\"dragon_defender\",\"name\":\"Dragon Defender\",\"description\":\"First Defender\",\"points\":10}]}}}", PointsResponse.class);
                Field definitions = ProfilePanel.class.getDeclaredField("pointsData");
                definitions.setAccessible(true);
                definitions.set(panel, data.getData());
                AccountResponse.Milestone done = GSON.fromJson("{\"type\":\"dragon_defender\",\"achievedAt\":\"2026-09-17T09:00:00Z\",\"pointsAwarded\":10}", AccountResponse.Milestone.class);
                Method render = ProfilePanel.class.getDeclaredMethod("buildMilestonesSection", List.class);
                render.setAccessible(true);
                JPanel completed = (JPanel) render.invoke(panel, List.of(done));
                JPanel pending = (JPanel) render.invoke(panel, List.of());
                JLabel yes = labels(completed).stream().filter(l -> "First Defender".equals(l.getText())).findFirst().orElseThrow();
                JLabel no = labels(pending).stream().filter(l -> "First Defender".equals(l.getText())).findFirst().orElseThrow();
                assertEquals(UIConstants.TEXT_PRIMARY, yes.getForeground());
                assertEquals(UIConstants.TEXT_SECONDARY, no.getForeground());
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
    }

    @Test public void tokenResetClearsProfileAndAdminState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                RevalPanel shell = new RevalPanel();
                ProfilePanel profile = shell.getProfilePanel();
                Field account = ProfilePanel.class.getDeclaredField("currentAccount");
                account.setAccessible(true);
                AccountResponse.AccountData data = new AccountResponse.AccountData();
                data.setOsrsAccount(new AccountResponse.OsrsAccount());
                account.set(profile, data);
                assertTrue(profile.isAccountLoaded());
                shell.onConnectionChanged();
                assertFalse(profile.isAccountLoaded());
                assertEquals("PROFILE", shell.getActiveTab());
            } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        });
    }

    @Test public void mvpPanelRendersOneGoldBadgeAndFitsNarrowWidths() throws Exception {
        MonthlyMvp snapshot = GSON.fromJson("{\"status\":\"ready\",\"periodStart\":\"2026-09-01T00:00:00Z\",\"winner\":{\"playerName\":\"Twelve chars\",\"points\":7},\"boards\":{\"drops\":[{\"position\":1,\"rsn\":\"Twelve chars\",\"value\":1234567890}],\"ehb\":[],\"ehp\":[]}}", MonthlyMvp.class);
        SwingUtilities.invokeAndWait(() -> {
            for (int width : new int[]{180, 220, 250}) {
                MonthlyMvpPanel panel = new MonthlyMvpPanel(snapshot);
                List<JLabel> badges = labels(panel).stream().filter(l -> l instanceof Badge).collect(java.util.stream.Collectors.toList());
                assertEquals(1, badges.size());
                assertEquals(UIConstants.ACCENT_GOLD, badges.get(0).getForeground());
                panel.setSize(width, 500);
                layout(panel);
                for (Component child : panel.getComponents()) {
                    assertTrue("child overflows " + width, child.getX() >= 0 && child.getX() + child.getWidth() <= width);
                }
                BufferedImage image = new BufferedImage(width, 500, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D graphics = image.createGraphics();
                panel.printAll(graphics);
                graphics.dispose();
                try {
                    Path directory = Path.of("build/reports/ui");
                    Files.createDirectories(directory);
                    ImageIO.write(image, "png", directory.resolve("mvp-" + width + ".png").toFile());
                } catch (java.io.IOException e) { throw new AssertionError(e); }
            }
        });
    }

    @Test public void unavailableMvpNeverShowsAStaleWinnerBadge() throws Exception {
        MonthlyMvp snapshot = GSON.fromJson("{\"status\":\"unavailable\",\"winner\":{\"playerName\":\"Old winner\",\"points\":9}}", MonthlyMvp.class);
        SwingUtilities.invokeAndWait(() -> {
            for (MonthlyMvp value : new MonthlyMvp[]{null, snapshot, new MonthlyMvp()}) {
                assertFalse(labels(new MonthlyMvpPanel(value)).stream().anyMatch(l -> l instanceof Badge));
            }
        });
        assertEquals("--", MonthlyMvpPanel.formatValue("ehb", Double.NaN));
        assertEquals("1.00b", MonthlyMvpPanel.formatValue("drops", 1_000_000_000));
    }

    private static List<JLabel> labels(Container parent) {
        List<JLabel> found = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (child instanceof JLabel) found.add((JLabel) child);
            if (child instanceof Container) found.addAll(labels((Container) child));
        }
        return found;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component c : root.getComponents()) if (c instanceof Container) layout((Container)c);
    }
}
