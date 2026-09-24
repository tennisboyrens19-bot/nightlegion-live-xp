package com.revalclan.ui;

import com.google.gson.Gson;
import com.revalclan.api.account.AccountResponse;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Render/pipeline replay using synthetic pixels, not a claim of live game sprites. */
public class PointsAlbumRenderingTest {
    private static class PendingImage extends AsyncBufferedImage {
        Runnable callback;
        PendingImage() { super(null, 36, 36, BufferedImage.TYPE_INT_ARGB); }
        @Override public synchronized void onLoaded(Runnable callback) { this.callback = callback; }
        void complete(int color) {
            Graphics2D graphics = createGraphics();
            graphics.setColor(new Color(color, true));
            graphics.fillRect(0, 0, getWidth(), getHeight());
            graphics.dispose();
            callback.run();
        }
    }

    @Test
    public void itemIdsSurvivePetFilteringAndAsyncCardsRebuildWithLoadedPixels() throws Exception {
        int[] ids = {21273, 20693, 28947, 27281, 27381, 27380, 27378, 27377};
        List<AccountResponse.PointsLogEntry> entries = new ArrayList<>();
        ItemManager itemManager = mock(ItemManager.class);
        PendingImage[] images = new PendingImage[ids.length];
        for (int i = 0; i < ids.length; i++) {
            images[i] = new PendingImage();
            when(itemManager.getImage(ids[i])).thenReturn(images[i]);
            entries.add(new Gson().fromJson("{\"itemId\":" + ids[i]
                + ",\"pointsChange\":100,\"sourceType\":\"" + (i < 2 ? "pet" : "drop")
                + "\",\"sourceDescription\":\"Fixture " + ids[i] + "\"}", AccountResponse.PointsLogEntry.class));
        }
        // Avoid a native JFrame in headless CI; exercise its actual rebuild/card code.
        PointsAlbumWindow window = mock(PointsAlbumWindow.class, CALLS_REAL_METHODS);
        JPanel grid = new JPanel();
        SwingUtilities.invokeAndWait(() -> {
            try {
                set(window, "allEntries", entries);
                set(window, "itemManager", itemManager);
                set(window, "sourceCombo", new JComboBox<>(new String[]{"All", "Drops", "Pets", "Milestones", "Diaries", "Challenges", "Events", "Misc"}));
                set(window, "sortCombo", new JComboBox<>(new String[]{"Newest"}));
                set(window, "searchField", new JTextField());
                set(window, "groupToggle", new JCheckBox());
                set(window, "summaryLabel", new JLabel());
                set(window, "pageLabel", new JLabel());
                set(window, "prevButton", new JButton());
                set(window, "nextButton", new JButton());
                set(window, "gridPanel", grid);
                rebuild(window);
            } catch (Exception error) { throw new AssertionError(error); }
        });
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(8, grid.getComponentCount());
        for (int id : ids) verify(itemManager).getImage(id);
        for (int i = 0; i < images.length; i++) images[i].complete(0xff000000 | ids[i]);
        SwingUtilities.invokeAndWait(() -> {});
        for (int i = 0; i < ids.length; i++) assertEquals(0xff000000 | ids[i], pixel(iconLabel((Container) grid.getComponent(i))));

        for (int pass = 0; pass < 2; pass++) {
            SwingUtilities.invokeAndWait(() -> {
                window.selectSource("pet");
                rebuild(window);
            });
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(2, grid.getComponentCount());
            assertEquals(0xff000000 | ids[0], pixel(iconLabel((Container) grid.getComponent(0))));
            assertEquals(0xff000000 | ids[1], pixel(iconLabel((Container) grid.getComponent(1))));
            SwingUtilities.invokeAndWait(() -> { window.selectSource(null); rebuild(window); });
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(8, grid.getComponentCount());
        }
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = PointsAlbumWindow.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
    private static void rebuild(PointsAlbumWindow window) {
        try {
            Method method = PointsAlbumWindow.class.getDeclaredMethod("rebuild");
            method.setAccessible(true);
            method.invoke(window);
        } catch (Exception error) { throw new AssertionError(error); }
    }
    private static JLabel iconLabel(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JLabel && ((JLabel) child).getIcon() != null) return (JLabel) child;
            if (child instanceof Container) {
                JLabel found = iconLabel((Container) child);
                if (found != null) return found;
            }
        }
        return null;
    }
    private static int pixel(JLabel label) {
        assertNotNull(label);
        BufferedImage image = new BufferedImage(36, 36, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        label.getIcon().paintIcon(label, graphics, 0, 0);
        graphics.dispose();
        return image.getRGB(18, 18);
    }
}
