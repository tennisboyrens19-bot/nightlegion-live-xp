package com.nightlegion.livexp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;

/** Shared colors/widgets intentionally matched to the Live On Clan RuneLite UI. */
final class NightLegionTheme
{
    static final Color BACKGROUND = new Color(36, 36, 36);
    static final Color HEADER = new Color(35, 35, 35);
    static final Color SURFACE = new Color(43, 43, 43);
    static final Color SURFACE_ALT = new Color(50, 50, 50);
    // Existing NightLegion views use the PURPLE names. Keep the API but map the
    // accent to Live On's orange so BOTW/SOTW/Giveaway/Groups match the shell.
    static final Color PURPLE = new Color(190, 104, 0);
    static final Color PURPLE_BRIGHT = new Color(255, 152, 0);
    static final Color SILVER = new Color(225, 225, 225);
    static final Color MUTED = new Color(160, 160, 160);
    static final Color DANGER = new Color(150, 55, 55);
    static final Color SUCCESS = new Color(70, 220, 100);
    static final Color GOLD = new Color(235, 190, 45);
    static final Color BORDER = new Color(62, 62, 62);
    static final Color BLUE = new Color(90, 190, 245);
    static final Color BRONZE = new Color(190, 110, 55);

    private NightLegionTheme()
    {
    }

    static void styleButton(JButton button, boolean primary, boolean danger)
    {
        Color background = danger ? DANGER : primary ? PURPLE : SURFACE_ALT;
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(primary ? PURPLE_BRIGHT.darker() : BORDER),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    static void styleCombo(JComboBox<?> combo)
    {
        combo.setOpaque(true);
        combo.setBackground(SURFACE_ALT);
        combo.setForeground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(BORDER));
    }

    static void styleField(JComponent component)
    {
        component.setOpaque(true);
        component.setBackground(SURFACE_ALT);
        component.setForeground(Color.WHITE);
        component.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
    }

    static int sectionItemId(String section)
    {
        String value = section == null ? "" : section.toLowerCase();
        if (value.equals("botw")) return 4151;
        if (value.equals("sotw")) return 13280;
        if (value.equals("giveaway")) return 995;
        if (value.contains("group")) return 20997;
        return -1;
    }

    static int activityItemId(String label)
    {
        String value = label == null ? "" : label.toLowerCase();
        if (value.contains("chambers of xeric")) return 20997;
        if (value.contains("theatre of blood")) return 22325;
        if (value.contains("tombs of amascut")) return 27275;
        if (value.equals("nex")) return 26374;
        if (value.contains("corporeal beast")) return 11824;
        if (value.contains("vorkath")) return 22978;
        if (value.contains("zulrah")) return 12926;
        if (value.contains("general graardor")) return 11804;
        if (value.contains("kree'arra") || value.contains("kree’arra")) return 11785;
        if (value.contains("kril tsutsaroth") || value.contains("k'ril") || value.contains("k’ril")) return 11808;
        if (value.contains("commander zilyana")) return 11806;
        if (value.contains("barbarian assault")) return 10551;
        if (value.contains("soul wars")) return 25256;
        if (value.contains("agility")) return 11849;
        if (value.contains("attack")) return 4151;
        if (value.contains("strength")) return 6528;
        if (value.contains("defence")) return 11283;
        if (value.contains("ranged")) return 20997;
        if (value.contains("prayer")) return 536;
        if (value.contains("magic")) return 12002;
        if (value.contains("runecraft")) return 7936;
        if (value.contains("construction")) return 8782;
        if (value.contains("hitpoints")) return 11936;
        if (value.contains("herblore")) return 257;
        if (value.contains("thieving")) return 1523;
        if (value.contains("crafting")) return 6573;
        if (value.contains("fletching")) return 861;
        if (value.contains("slayer")) return 11864;
        if (value.contains("hunter")) return 10033;
        if (value.contains("mining")) return 451;
        if (value.contains("smithing")) return 2363;
        if (value.contains("fishing")) return 385;
        if (value.contains("cooking")) return 3144;
        if (value.contains("firemaking")) return 1511;
        if (value.contains("woodcutting")) return 1513;
        if (value.contains("farming")) return 5316;
        return -1;
    }

    static boolean wildernessActivity(String label)
    {
        String value = label == null ? "" : label.toLowerCase();
        return value.contains("wilderness")
            || value.contains("revenant")
            || value.contains("chaos elemental")
            || value.contains("callisto")
            || value.contains("vet'ion")
            || value.contains("vet’ion")
            || value.contains("venenatis")
            || value.equals("pvp");
    }

    static ImageIcon markIcon(int size, Color color)
    {
        return new ImageIcon(markImage(size, color));
    }

    static BufferedImage markImage(int size, Color color)
    {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            float scale = size / 16f;
            g.setStroke(new BasicStroke(2.25f * scale, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

            Path2D left = new Path2D.Double();
            left.moveTo(2.0 * scale, 3.0 * scale);
            left.lineTo(6.4 * scale, 7.4 * scale);
            left.lineTo(4.0 * scale, 9.8 * scale);
            left.lineTo(1.2 * scale, 7.0 * scale);
            g.draw(left);

            Path2D right = new Path2D.Double();
            right.moveTo(14.0 * scale, 3.0 * scale);
            right.lineTo(9.6 * scale, 7.4 * scale);
            right.lineTo(12.0 * scale, 9.8 * scale);
            right.lineTo(14.8 * scale, 7.0 * scale);
            g.draw(right);

            Path2D lower = new Path2D.Double();
            lower.moveTo(5.4 * scale, 8.5 * scale);
            lower.lineTo(8.0 * scale, 11.2 * scale);
            lower.lineTo(10.6 * scale, 8.5 * scale);
            g.draw(lower);
        }
        finally
        {
            g.dispose();
        }
        return image;
    }

    static void applyForeground(Component component, Color color)
    {
        component.setForeground(color);
    }
}
