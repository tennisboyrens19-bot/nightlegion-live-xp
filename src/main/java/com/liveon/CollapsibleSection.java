package com.liveon;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;

/** Direct adaptation of Reval's collapsed-by-default point-source section. */
final class CollapsibleSection extends JPanel
{
	private final JPanel content;
	private final JLabel arrow;
	private final JLabel icon;
	private boolean expanded;

	CollapsibleSection(String title, String subtitle, JPanel content, boolean startExpanded)
	{
		setLayout(new BorderLayout());
		setBackground(RevalUiConstants.BACKGROUND);
		setBorder(new EmptyBorder(0, 0, 3, 0));
		this.content = content;
		expanded = startExpanded;
		content.setVisible(expanded);
		JPanel header = new JPanel(new BorderLayout(2, 0))
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
		header.setBackground(RevalUiConstants.HEADER_BG);
		header.setBorder(new EmptyBorder(6, 8, 6, 8));
		header.setOpaque(false);
		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		left.setOpaque(false);
		arrow = new JLabel(new TriangleIcon(expanded, 9, RevalUiConstants.ACCENT_GOLD));
		left.add(arrow);
		icon = new JLabel();
		icon.setPreferredSize(new Dimension(24, 24));
		left.add(icon);
		JPanel titles = new JPanel();
		titles.setOpaque(false);
		JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(FontManager.getRunescapeBoldFont());
		titleLabel.setForeground(RevalUiConstants.TEXT_PRIMARY);
		if (subtitle != null && !subtitle.isEmpty())
		{
			titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
			titles.add(titleLabel);
			JLabel subtitleLabel = new JLabel(subtitle);
			subtitleLabel.setFont(FontManager.getRunescapeSmallFont());
			subtitleLabel.setForeground(RevalUiConstants.TEXT_SECONDARY);
			titles.add(subtitleLabel);
		}
		else
		{
			titles.setLayout(new BorderLayout());
			titles.add(titleLabel, BorderLayout.CENTER);
		}
		header.add(left, BorderLayout.WEST);
		header.add(titles, BorderLayout.CENTER);
		Clickable.onPress(header, () -> setExpanded(!expanded),
			RevalUiConstants.HEADER_HOVER, RevalUiConstants.HEADER_BG);
		add(header, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);
	}

	void setIcon(Icon value) { icon.setIcon(value); }
	void setExpanded(boolean value)
	{
		expanded = value;
		arrow.setIcon(new TriangleIcon(expanded, 9, RevalUiConstants.ACCENT_GOLD));
		content.setVisible(expanded);
		revalidate();
	}
	boolean isExpanded() { return expanded; }
}
