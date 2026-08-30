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

final class NightLegionTheme
{
    static final Color BACKGROUND = new Color(14, 8, 20);
    static final Color HEADER = new Color(28, 11, 42);
    static final Color SURFACE = new Color(34, 24, 44);
    static final Color SURFACE_ALT = new Color(47, 31, 61);
    static final Color PURPLE = new Color(146, 70, 255);
    static final Color PURPLE_BRIGHT = new Color(196, 112, 255);
    static final Color SILVER = new Color(232, 226, 239);
    static final Color MUTED = new Color(174, 160, 187);
    static final Color DANGER = new Color(160, 62, 88);
    static final Color SUCCESS = new Color(76, 196, 128);
    static final Color GOLD = new Color(232, 184, 76);
    static final Color BORDER = new Color(79, 53, 98);

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
            BorderFactory.createLineBorder(primary ? PURPLE_BRIGHT : BORDER),
            BorderFactory.createEmptyBorder(5, 9, 5, 9)));
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
        if (value.equals("botw")) return 4151;       // Abyssal whip
        if (value.equals("sotw")) return 13280;      // Max cape
        if (value.equals("giveaway")) return 995;    // Coins
        if (value.contains("group")) return 20997;   // Twisted bow
        return -1;
    }

    static int activityItemId(String label)
    {
        String value = label == null ? "" : label.toLowerCase();
        if (value.contains("chambers of xeric")) return 20997; // Twisted bow
        if (value.contains("theatre of blood")) return 22325; // Scythe of vitur
        if (value.contains("tombs of amascut")) return 27275; // Tumeken's shadow
        if (value.equals("nex")) return 26374; // Zaryte crossbow
        if (value.contains("corporeal beast")) return 11824; // Zamorakian spear
        if (value.contains("vorkath")) return 22978; // Dragon hunter lance
        if (value.contains("zulrah")) return 12926; // Toxic blowpipe
        if (value.contains("general graardor")) return 11804; // Bandos godsword
        if (value.contains("kree'arra") || value.contains("kree’arra")) return 11785; // Armadyl crossbow
        if (value.contains("kril tsutsaroth") || value.contains("k'ril") || value.contains("k’ril")) return 11808; // Zamorak godsword
        if (value.contains("commander zilyana")) return 11806; // Saradomin godsword
        if (value.contains("barbarian assault")) return 10551; // Fighter torso
        if (value.contains("soul wars")) return 25256; // Soul cape
        if (value.contains("agility")) return 11849; // Mark of grace
        if (value.contains("attack")) return 4151; // Abyssal whip
        if (value.contains("strength")) return 6528; // Tzhaar-ket-om
        if (value.contains("defence")) return 11283; // Dragonfire shield
        if (value.contains("ranged")) return 20997; // Twisted bow
        if (value.contains("prayer")) return 536; // Dragon bones
        if (value.contains("magic")) return 12002; // Occult necklace
        if (value.contains("runecraft")) return 7936; // Pure essence
        if (value.contains("construction")) return 8782; // Mahogany plank
        if (value.contains("hitpoints")) return 11936; // Dark crab
        if (value.contains("herblore")) return 257; // Ranarr weed
        if (value.contains("thieving")) return 1523; // Lockpick
        if (value.contains("crafting")) return 6573; // Onyx
        if (value.contains("fletching")) return 861; // Magic shortbow
        if (value.contains("slayer")) return 11864; // Slayer helmet
        if (value.contains("hunter")) return 10033; // Chinchompa
        if (value.contains("mining")) return 451; // Runite ore
        if (value.contains("smithing")) return 2363; // Runite bar
        if (value.contains("fishing")) return 385; // Shark
        if (value.contains("cooking")) return 3144; // Cooked karambwan
        if (value.contains("firemaking")) return 1511; // Logs
        if (value.contains("woodcutting")) return 1513; // Magic logs
        if (value.contains("farming")) return 5316; // Magic seed
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
