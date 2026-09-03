package com.liveon;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Reval RankingPanel adapted to consume NightLegion's backend-owned catalogue.
 * Layout, ordering, spacing, typography, icons and collapsed state are retained.
 */
final class RankingPanel extends JPanel
{
	private final JPanel content = new JPanel(new GridBagLayout());
	private final GridBagConstraints constraints = new GridBagConstraints();
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	private final ClanRankIconResolver rankIcons;
	private int row;

	RankingPanel(ItemManager itemManager, SpriteManager spriteManager, ClanRankIconResolver rankIcons)
	{
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.rankIcons = rankIcons;
		setLayout(new BorderLayout());
		setBackground(RevalUiConstants.BACKGROUND);
		content.setBackground(RevalUiConstants.BACKGROUND);
		content.setBorder(new EmptyBorder(6, 2, 6, 2));
		constraints.gridx = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.insets = new Insets(1, 0, 1, 0);
		JPanel wrapper = new JPanel(new BorderLayout())
		{
			@Override public Dimension getPreferredSize()
			{
				Dimension size = super.getPreferredSize();
				if (getParent() != null) size.width = getParent().getWidth();
				return size;
			}
		};
		wrapper.setBackground(RevalUiConstants.BACKGROUND);
		wrapper.add(content, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(wrapper);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getViewport().setBackground(RevalUiConstants.BACKGROUND);
		add(scroll, BorderLayout.CENTER);
		showMessage("Loading NightLegion points...");
	}

	void setData(PointsResponse response)
	{
		content.removeAll();
		row = 0;
		if (response == null || response.data == null)
		{
			showMessage("Something went wrong");
			return;
		}
		if (response.data.ranks != null && !response.data.ranks.isEmpty())
		{
			add(createHeader("RANKS", "Clan rank progression"));
			for (PointsResponse.Rank rank : response.data.ranks) add(new RankCard(rank, rankIcons));
		}
		space(12);
		add(createHeader("POINT SOURCES", "Ways to earn points"));
		if (response.data.pointSources != null)
		{
			for (Map.Entry<String, List<PointsResponse.PointSource>> entry : response.data.pointSources.entrySet())
			{
				String category = entry.getKey();
				List<PointsResponse.PointSource> sources = entry.getValue();
				if (sources == null || sources.isEmpty() || "UNTRADEABLE_DROPS".equals(category)) continue;
				CollapsibleSection section = new CollapsibleSection(formatCategory(category), null,
					createSources(sources, category, false), false);
				loadSectionIcon(sources, category, section);
				add(section);
			}
			List<PointsResponse.PointSource> untradeable = response.data.pointSources.get("UNTRADEABLE_DROPS");
			if (untradeable != null && !untradeable.isEmpty())
			{
				CollapsibleSection section = new CollapsibleSection("Untradeable Drops", null,
					createUntradeables(untradeable), false);
				loadSectionIcon(untradeable, "UNTRADEABLE_DROPS", section);
				add(section);
			}
		}
		space(16);
		content.revalidate();
		content.repaint();
	}

	private void loadSectionIcon(List<PointsResponse.PointSource> sources, String category, CollapsibleSection section)
	{
		Integer itemId = sources.get(0).metadata == null ? null : sources.get(0).metadata.itemId;
		if (itemId != null && itemManager != null)
		{
			try
			{
				AsyncBufferedImage image = itemManager.getImage(itemId);
				image.onLoaded(() -> SwingUtilities.invokeLater(() -> section.setIcon(
					new ImageIcon(ImageUtil.resizeImage(image, 24, 24)))));
			}
			catch (Exception ignored) { }
		}
		else if ("MISC".equalsIgnoreCase(category))
		{
			SpriteIcons.load(spriteManager, SpriteID.TAB_STATS, 24, section::setIcon);
		}
	}

	private JPanel createSources(List<PointsResponse.PointSource> sources, String category, boolean sourceSubtitle)
	{
		JPanel panel = vertical();
		panel.setBorder(new EmptyBorder(2, 0, 2, 0));
		boolean showIcons = !"VALUABLE_DROPS".equalsIgnoreCase(category)
			&& !"PETS".equalsIgnoreCase(category);
		for (PointsResponse.PointSource source : sources)
		{
			PointSourceCard card = showIcons
				? new PointSourceCard(source, itemManager, sourceSubtitle)
				: new PointSourceCard(source.name, source.description, source.getPointsDisplay());
			card.setAlignmentX(Component.LEFT_ALIGNMENT);
			panel.add(card);
			panel.add(Box.createRigidArea(new Dimension(0, 1)));
		}
		return panel;
	}

	private JPanel createUntradeables(List<PointsResponse.PointSource> sources)
	{
		JPanel panel = vertical();
		panel.setBorder(new EmptyBorder(2, 0, 2, 0));
		Map<String, java.util.List<PointsResponse.PointSource>> grouped = new LinkedHashMap<>();
		for (PointsResponse.PointSource source : sources)
		{
			if (source.metadata != null && source.metadata.category != null)
				grouped.computeIfAbsent(source.metadata.category, ignored -> new java.util.ArrayList<>()).add(source);
		}
		for (Map.Entry<String, java.util.List<PointsResponse.PointSource>> entry : grouped.entrySet())
		{
			JLabel header = new JLabel(entry.getKey());
			header.setFont(FontManager.getRunescapeBoldFont());
			header.setForeground(RevalUiConstants.ACCENT_GOLD);
			header.setBorder(new EmptyBorder(4, 0, 2, 0));
			panel.add(header);
			for (PointsResponse.PointSource source : entry.getValue())
			{
				PointSourceCard card = new PointSourceCard(source, itemManager, true);
				card.setAlignmentX(Component.LEFT_ALIGNMENT);
				panel.add(card);
				panel.add(Box.createRigidArea(new Dimension(0, 1)));
			}
			panel.add(Box.createRigidArea(new Dimension(0, 4)));
		}
		return panel;
	}

	private JPanel vertical()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(RevalUiConstants.BACKGROUND);
		return panel;
	}

	private JPanel createHeader(String title, String subtitle)
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(RevalUiConstants.BACKGROUND);
		header.setBorder(new EmptyBorder(6, 2, 4, 2));
		JPanel text = vertical();
		JLabel heading = new JLabel(title);
		heading.setFont(FontManager.getRunescapeBoldFont());
		heading.setForeground(RevalUiConstants.ACCENT_GOLD);
		text.add(heading);
		JLabel detail = new JLabel(subtitle);
		detail.setFont(FontManager.getRunescapeSmallFont());
		detail.setForeground(RevalUiConstants.TEXT_SECONDARY);
		text.add(detail);
		header.add(text, BorderLayout.WEST);
		return header;
	}

	private void add(JComponent component)
	{
		constraints.gridy = row++;
		content.add(component, constraints);
	}

	private void space(int height)
	{
		constraints.insets = new Insets(height / 2, 0, height / 2, 0);
		add(Box.createVerticalStrut(height));
		constraints.insets = new Insets(1, 0, 1, 0);
	}

	private void showMessage(String value)
	{
		content.removeAll();
		row = 0;
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panel.setBackground(RevalUiConstants.BACKGROUND);
		panel.setBorder(new EmptyBorder(30, 0, 0, 0));
		JLabel label = new JLabel(value, SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(RevalUiConstants.TEXT_SECONDARY);
		panel.add(label);
		add(panel);
		content.revalidate();
		content.repaint();
	}

	private static String formatCategory(String value)
	{
		if ("VALUABLE_DROPS".equals(value)) return "Valuable Drops";
		StringBuilder result = new StringBuilder();
		for (String word : value.split("[_\\s]+"))
		{
			if (result.length() > 0) result.append(' ');
			if (!word.isEmpty()) result.append(Character.toUpperCase(word.charAt(0)))
				.append(word.substring(1).toLowerCase(java.util.Locale.ROOT));
		}
		return result.toString();
	}

	int displayedRankCount() { return content.getComponentCount(); }
}
