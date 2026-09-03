package com.liveon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;

/** Reval ProfilePanel rank/progress presentation adapted to NightLegion fields. */
final class RevalProfilePanel extends JPanel
{
	private final JLabel icon = new JLabel();
	private final JLabel rsn = new JLabel("Current RuneLite account");
	private final JLabel actualRank = new JLabel("Actual clan rank: loading...");
	private final JLabel progression = new JLabel("Mentor");
	private final JLabel points = new JLabel("0");
	private final JLabel next = new JLabel("Next: Precept at 500");
	private final JLabel maintenance = new JLabel("Monthly maintenance: none");
	private final JLabel recent = new JLabel(" ");
	private final ProgressBar progress = new ProgressBar();
	private final ClanRankIconResolver resolver;

	RevalProfilePanel(ClanRankIconResolver resolver, Runnable refresh)
	{
		this.resolver = resolver;
		setLayout(new BorderLayout());
		setBackground(RevalUiConstants.BACKGROUND);
		setBorder(new EmptyBorder(4, 2, 2, 2));
		JPanel card = new JPanel(new BorderLayout(8, 4))
		{
			@Override protected void paintComponent(Graphics graphics)
			{
				Graphics2D g = (Graphics2D) graphics.create();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(getBackground());
				g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
				g.dispose();
			}
		};
		card.setBackground(RevalUiConstants.CARD_BG);
		card.setBorder(new EmptyBorder(8, 8, 8, 8));
		icon.setPreferredSize(new Dimension(28, 28));
		card.add(icon, BorderLayout.WEST);
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		JPanel identity = new JPanel(new GridLayout(0, 1, 0, 1));
		identity.setOpaque(false);
		rsn.setFont(FontManager.getRunescapeBoldFont());
		rsn.setForeground(RevalUiConstants.TEXT_PRIMARY);
		actualRank.setFont(FontManager.getRunescapeSmallFont());
		actualRank.setForeground(RevalUiConstants.TEXT_SECONDARY);
		identity.add(rsn);
		identity.add(actualRank);
		body.add(identity);
		body.add(Box.createRigidArea(new Dimension(0, 5)));
		JPanel rankRow = new JPanel(new BorderLayout());
		rankRow.setOpaque(false);
		progression.setFont(FontManager.getRunescapeBoldFont());
		progression.setForeground(RevalUiConstants.ACCENT_GOLD);
		points.setFont(FontManager.getRunescapeBoldFont());
		points.setForeground(RevalUiConstants.ACCENT_GREEN);
		points.setHorizontalAlignment(SwingConstants.RIGHT);
		rankRow.add(progression, BorderLayout.WEST);
		rankRow.add(points, BorderLayout.EAST);
		body.add(rankRow);
		body.add(Box.createRigidArea(new Dimension(0, 3)));
		progress.setPreferredSize(new Dimension(100, 5));
		progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
		body.add(progress);
		body.add(Box.createRigidArea(new Dimension(0, 3)));
		next.setFont(FontManager.getRunescapeSmallFont());
		next.setForeground(RevalUiConstants.TEXT_SECONDARY);
		body.add(next);
		maintenance.setFont(FontManager.getRunescapeSmallFont());
		maintenance.setForeground(RevalUiConstants.TEXT_SECONDARY);
		body.add(maintenance);
		recent.setFont(FontManager.getRunescapeSmallFont());
		recent.setForeground(RevalUiConstants.TEXT_MUTED);
		body.add(recent);
		card.add(body, BorderLayout.CENTER);
		Clickable.onPress(card, refresh, RevalUiConstants.CARD_HOVER, RevalUiConstants.CARD_BG);
		card.setToolTipText("Refresh NightLegion rank progress");
		add(card, BorderLayout.CENTER);
	}

	void setProfile(JsonObject profile)
	{
		if (profile == null) return;
		String player = string(profile, "rsn", "Current RuneLite account");
		String actual = string(profile, "actual_clan_rank",
			string(profile, "current_clan_rank", "Not reported"));
		JsonObject rank = object(profile, "progression");
		if (rank == null) rank = object(profile, "rank");
		if (rank == null) return;
		String title = string(rank, "title", "Mentor");
		double total = number(profile, "points", 0);
		double threshold = number(rank, "threshold", 0);
		double nextThreshold = number(rank, "next_threshold", threshold);
		String nextTitle = string(rank, "next_title", "");
		rsn.setText(player);
		actualRank.setText("Actual clan rank: " + (actual.isEmpty() ? "Not reported" : actual));
		progression.setText("Progression: " + title);
		points.setText(String.format(java.util.Locale.ROOT, "%,.0f pts", total));
		if (nextTitle.isEmpty())
		{
			next.setText("Maximum progression reached");
			progress.setValue(1);
		}
		else
		{
			next.setText(String.format(java.util.Locale.ROOT, "Next: %s at %,.0f · %,.0f remaining",
				nextTitle, nextThreshold, Math.max(0, nextThreshold - total)));
			progress.setValue(nextThreshold <= threshold ? 0
				: (total - threshold) / (nextThreshold - threshold));
		}
		JsonObject upkeep = object(profile, "maintenance");
		if (upkeep == null || number(upkeep, "requirement", 0) <= 0)
		{
			maintenance.setText("Monthly maintenance: none");
			maintenance.setForeground(RevalUiConstants.TEXT_SECONDARY);
		}
		else
		{
			double earned = number(upkeep, "points", 0);
			double required = number(upkeep, "requirement", 0);
			maintenance.setText(String.format(java.util.Locale.ROOT,
				"Monthly maintenance: %,.0f / %,.0f%s", earned, required,
				earned >= required ? " ✓" : ""));
			maintenance.setForeground(earned >= required
				? RevalUiConstants.ACCENT_GREEN : RevalUiConstants.ACCENT_GOLD);
		}
		JsonArray rows = profile.has("recent_points") && profile.get("recent_points").isJsonArray()
			? profile.getAsJsonArray("recent_points") : null;
		if (rows != null && rows.size() > 0)
		{
			JsonObject first = rows.get(0).getAsJsonObject();
			recent.setText("Latest: " + string(first, "reason", "Point credit") + " "
				+ String.format(java.util.Locale.ROOT, "%+.0f", number(first, "points", 0)));
		}
		else recent.setText("No point history yet");
		icon.setIcon(null);
		if (resolver != null) resolver.apply(title, icon, 28);
	}

	private static JsonObject object(JsonObject value, String name)
	{
		return value.has(name) && value.get(name).isJsonObject() ? value.getAsJsonObject(name) : null;
	}
	private static String string(JsonObject value, String name, String fallback)
	{
		JsonElement element = value.get(name);
		return element == null || element.isJsonNull() ? fallback : element.getAsString();
	}
	private static double number(JsonObject value, String name, double fallback)
	{
		try { return value.has(name) ? value.get(name).getAsDouble() : fallback; }
		catch (RuntimeException ignored) { return fallback; }
	}

	String actualRankText() { return actualRank.getText(); }

	private static final class ProgressBar extends JPanel
	{
		private double value;
		private ProgressBar() { setOpaque(false); }
		private void setValue(double value)
		{
			this.value = Math.max(0, Math.min(1, value));
			repaint();
		}
		@Override protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setColor(RevalUiConstants.PROGRESS_BG);
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
			g.setColor(RevalUiConstants.ACCENT_GOLD);
			g.fillRoundRect(0, 0, (int) Math.round(getWidth() * value), getHeight(), 4, 4);
			g.dispose();
		}
	}
}
