package com.revalclan.ui;

import com.revalclan.api.common.MonthlyMvp;
import com.revalclan.ui.components.Badge;
import com.revalclan.ui.constants.UIConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;

/** Compact presentation of the existing single-winner monthly MVP rules. */
public final class MonthlyMvpPanel extends JPanel {
    public MonthlyMvpPanel(MonthlyMvp snapshot) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UIConstants.CARD_BG);
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        add(text("MONTHLY MVP", UIConstants.ACCENT_GOLD, true));
        add(Box.createVerticalStrut(4));
        if (snapshot == null || !snapshot.isReady()) {
            add(wrapped("Monthly data unavailable. Refresh to try again; no winner is assigned from incomplete data.", 5));
        } else if (snapshot.getWinner() == null || snapshot.getWinner().getPlayerName() == null) {
            add(wrapped("No qualifying activity this month yet.", 2));
        } else {
            JPanel winner = row(24);
            winner.add(new Badge("MVP", UIConstants.ACCENT_GOLD), BorderLayout.WEST);
            JLabel name = text(snapshot.getWinner().getPlayerName(), UIConstants.ACCENT_GOLD, true);
            name.setToolTipText(snapshot.getWinner().getPlayerName());
            winner.add(name, BorderLayout.CENTER);
            add(winner);
            add(text(snapshot.getWinner().getPoints() + " MVP points", UIConstants.TEXT_SECONDARY, false));
        }
        add(Box.createVerticalStrut(4));
        add(wrapped("Top three in Drops, EHB and EHP earn 3 / 2 / 1. One overall winner. Calendar month in UTC.", 5));
        if (snapshot != null && snapshot.getPeriodStart() != null) {
            String period = snapshot.getPeriodStart();
            add(text(period.length() >= 7 ? period.substring(0, 7) + " (UTC)" : period,
                UIConstants.TEXT_MUTED, false));
        }
        if (snapshot != null && snapshot.getBoards() != null) {
            for (String key : new String[]{"drops", "ehb", "ehp"}) {
                add(Box.createVerticalStrut(6));
                add(text(key.toUpperCase(Locale.ROOT), UIConstants.ACCENT_GOLD, false));
                List<MonthlyMvp.Entry> rows = snapshot.getBoards().getOrDefault(key, Collections.emptyList());
                if (rows == null || rows.isEmpty()) {
                    add(text(snapshot.isReady() ? "No qualifying entries" : "Data unavailable", UIConstants.TEXT_MUTED, false));
                    continue;
                }
                for (MonthlyMvp.Entry entry : rows.subList(0, Math.min(3, rows.size()))) {
                    if (entry == null) continue;
                    JPanel line = row(20);
                    JLabel name = text(entry.getPosition() + ". " + (entry.getRsn() == null ? "Unknown" : entry.getRsn()), UIConstants.TEXT_PRIMARY, false);
                    name.setToolTipText(entry.getRsn());
                    line.add(name, BorderLayout.CENTER);
                    line.add(text(formatValue(key, entry.getValue()), UIConstants.TEXT_SECONDARY, false), BorderLayout.EAST);
                    add(line);
                }
            }
        }
    }

    @Override public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    static String formatValue(String board, double value) {
        if (!Double.isFinite(value) || value < 0) return "--";
        if (!"drops".equals(board)) return String.format(Locale.ROOT, "%.2f", value);
        if (value >= 1_000_000_000) return String.format(Locale.ROOT, "%.2fb", value / 1_000_000_000);
        return String.format(Locale.ROOT, "%.1fm", value / 1_000_000);
    }

    private static JPanel row(int height) {
        JPanel line = new JPanel(new BorderLayout(4, 0));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        line.setOpaque(false);
        line.setMinimumSize(new Dimension(0, height));
        line.setPreferredSize(new Dimension(0, height));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return line;
    }

    private static JLabel text(String value, Color color, boolean bold) {
        JLabel label = new JLabel(value);
        label.setFont(bold ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMinimumSize(new Dimension(0, label.getPreferredSize().height));
        return label;
    }

    private static JTextArea wrapped(String value, int rows) {
        JTextArea area = new JTextArea(value);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(rows);
        area.setFont(FontManager.getRunescapeSmallFont());
        area.setForeground(UIConstants.TEXT_SECONDARY);
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        int height = area.getFontMetrics(area.getFont()).getHeight() * rows + 4;
        area.setMinimumSize(new Dimension(0, height));
        area.setPreferredSize(new Dimension(0, height));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return area;
    }
}
