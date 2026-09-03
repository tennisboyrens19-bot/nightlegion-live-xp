package com.liveon;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;

/** Direct adaptation of Reval's rank row. */
final class RankCard extends JPanel
{
	RankCard(PointsResponse.Rank rank, ClanRankIconResolver resolver)
	{
		setLayout(new BorderLayout(6, 0));
		setBackground(RevalUiConstants.CARD_BG);
		setBorder(new EmptyBorder(8, 6, 8, 10));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(18, 18));
		JLabel name = new JLabel(rank.displayName == null ? rank.name : rank.displayName);
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(RevalUiConstants.ACCENT_GOLD);
		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		left.setOpaque(false);
		left.add(icon);
		left.add(name);

		String text = rank.pointsRequired >= 1000
			? String.format(java.util.Locale.ROOT, "%.1fk", rank.pointsRequired / 1000.0)
			: String.valueOf(rank.pointsRequired);
		if (rank.maintenancePerMonth > 0) text += " (" + rank.maintenancePerMonth + "/mo)";
		JLabel points = new JLabel(text);
		points.setFont(FontManager.getRunescapeBoldFont());
		points.setForeground(RevalUiConstants.POINTS_COLOR);
		add(left, BorderLayout.WEST);
		add(points, BorderLayout.EAST);

		if (rank.additionalRequirements != null && !rank.additionalRequirements.isEmpty())
		{
			StringBuilder tooltip = new StringBuilder();
			for (PointsResponse.Rank.AdditionalRequirement requirement : rank.additionalRequirements)
			{
				if (tooltip.length() > 0) tooltip.append(", ");
				tooltip.append(requirement.description);
			}
			setToolTipText(tooltip.toString());
		}
		if (resolver != null) resolver.apply(rank.name, icon, 18);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(getBackground());
		g.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
		g.dispose();
	}
}
