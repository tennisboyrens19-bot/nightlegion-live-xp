package com.revalclan.ui;

import com.revalclan.api.common.MonthlyMvp;
import com.revalclan.ui.components.Badge;
import com.revalclan.util.RankNames;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class MvpLayoutTest {
    @Test public void tallPanelsKeepTheGoldBadgeCompact() throws Exception {
        MonthlyMvp snapshot = new MonthlyMvp();
        snapshot.setStatus("ready");
        MonthlyMvp.Winner winner = new MonthlyMvp.Winner();
        winner.setPlayerName("Twelve chars");
        winner.setPoints(7);
        snapshot.setWinner(winner);
        SwingUtilities.invokeAndWait(() -> {
            for (int width : new int[]{180,220,250}) {
                MonthlyMvpPanel panel = new MonthlyMvpPanel(snapshot);
                panel.setSize(width,700);
                layout(panel);
                assertEquals(1, checkBadges(panel));
            }
        });
    }

    @Test public void retiredChatCollectorAndSettingAreNotPackaged() throws Exception {
        Path root = Path.of("src/main/java/com/revalclan");
        assertFalse(Files.exists(root.resolve("notifiers/ChatNotifier.java")));
        assertFalse(Files.readString(root.resolve("RevalClanPlugin.java")).contains("chatNotifier"));
        assertFalse(Files.readString(root.resolve("RevalClanConfig.java")).contains("notifyChat"));
        assertEquals("Prefect", RankNames.display("prefect"));
    }

    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents())
            if (child instanceof Container) layout((Container)child);
    }

    private static int checkBadges(Container parent) {
        int count = 0;
        for (Component child : parent.getComponents()) {
            if (child instanceof Badge) {
                assertTrue("Badge stretched vertically", child.getHeight() > 0 && child.getHeight() <= 24);
                assertTrue("Badge stretched horizontally", child.getWidth() > 0 && child.getWidth() <= 50);
                count++;
            } else if (child instanceof Container) count += checkBadges((Container)child);
        }
        return count;
    }
}
