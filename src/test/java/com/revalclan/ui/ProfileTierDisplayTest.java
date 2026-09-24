package com.revalclan.ui;

import com.google.gson.Gson;
import com.revalclan.RevalClanConfig;
import com.revalclan.api.account.AccountResponse;
import com.revalclan.api.points.PointsResponse;
import org.junit.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Exercises displayed labels using unchanged NightLegionBot 7a22c35 catalogue rows. */
public class ProfileTierDisplayTest {
    @Test
    public void combatTierLabelsShowTheRewardAddedAtEachTier() throws Exception {
        assertEquals(Arrays.asList("+33 pts", "+67 pts", "+150 pts", "+350 pts", "+600 pts", "+1800 pts"),
            rewardLabels(render("COMBAT_ACHIEVEMENTS", 0, false)));
    }

    @Test
    public void collectionTierLabelsShowTheRewardAddedAtEachTier() throws Exception {
        assertEquals(Arrays.asList("+200 pts", "+400 pts", "+400 pts", "+400 pts", "+400 pts",
                                  "+200 pts", "+200 pts", "+200 pts", "+600 pts"),
            rewardLabels(render("COLLECTION_LOG", 0, false)));
    }

    @Test
    public void hidingCompletedTiersDoesNotTurnNextRewardIntoItsRunningTotal() throws Exception {
        JPanel section = render("COMBAT_ACHIEVEMENTS", 436, true);
        assertEquals(Arrays.asList("+350 pts", "+600 pts", "+1800 pts"), rewardLabels(section));
        assertFalse(allLabels(section).contains("Hard tier"));
    }

    @Test
    public void accountThresholdOverridesCatalogueForCombatCompletion() throws Exception {
        assertTrue(allLabels(render("COMBAT_ACHIEVEMENTS", 2691, true)).contains("Grandmaster tier"));
        assertFalse(allLabels(render("COMBAT_ACHIEVEMENTS", 2691, true,
            Collections.singletonMap("grandmaster", 2691))).contains("Grandmaster tier"));
        assertTrue(allLabels(render("COMBAT_ACHIEVEMENTS", 2697, true,
            Collections.singletonMap("grandmaster", 2800))).contains("Grandmaster tier"));
    }

    @Test
    public void invalidOrUnrelatedThresholdsKeepCatalogueCompletion() throws Exception {
        for (Integer invalid : Arrays.asList(null, 0, -1)) {
            assertTrue(allLabels(render("COMBAT_ACHIEVEMENTS", 2691, true,
                Collections.singletonMap("grandmaster", invalid))).contains("Grandmaster tier"));
        }
        assertEquals(rewardLabels(render("COLLECTION_LOG", 0, true)),
            rewardLabels(render("COLLECTION_LOG", 0, true, Collections.singletonMap("bronze", 1))));
    }

    private static JPanel render(String category, int progress, boolean hideCompleted) throws Exception {
        return render(category, progress, hideCompleted, null);
    }

    private static JPanel render(String category, int progress, boolean hideCompleted,
                                Map<String, Integer> thresholds) throws Exception {
        PointsResponse response;
        try (InputStreamReader reader = new InputStreamReader(
                ProfileTierDisplayTest.class.getResourceAsStream("/fixtures/nightlegion-tier-catalog.json"),
                StandardCharsets.UTF_8)) {
            response = new Gson().fromJson(reader, PointsResponse.class);
        }
        AtomicReference<JPanel> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                ProfilePanel panel = new ProfilePanel();
                AccountResponse.AccountData account = new Gson().fromJson(
                    "{\"combatAchievementThresholds\":" + new Gson().toJson(thresholds) + "}",
                    AccountResponse.AccountData.class);
                Field currentAccount = ProfilePanel.class.getDeclaredField("currentAccount");
                currentAccount.setAccessible(true);
                currentAccount.set(panel, account);
                Field pointsData = ProfilePanel.class.getDeclaredField("pointsData");
                pointsData.setAccessible(true);
                pointsData.set(panel, response.getData());
                Field config = ProfilePanel.class.getDeclaredField("config");
                config.setAccessible(true);
                config.set(panel, new RevalClanConfig() {
                    @Override
                    public boolean hideCompletedItems() {
                        return hideCompleted;
                    }
                });
                Method build = ProfilePanel.class.getDeclaredMethod("buildTierSection", String.class, String.class, Integer.class);
                build.setAccessible(true);
                result.set((JPanel) build.invoke(panel, "Tiers", category, progress));
            } catch (ReflectiveOperationException error) {
                throw new AssertionError(error);
            }
        });
        return result.get();
    }

    private static List<String> rewardLabels(Container container) {
        List<String> labels = allLabels(container);
        labels.removeIf(text -> !text.matches("\\+\\d+ pts"));
        return labels;
    }

    private static List<String> allLabels(Container container) {
        List<String> labels = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) labels.add(((JLabel) component).getText());
            if (component instanceof Container) labels.addAll(allLabels((Container) component));
        }
        return labels;
    }
}
