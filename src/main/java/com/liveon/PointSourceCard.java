package com.liveon;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/** Direct adaptation of Reval's point-source row. */
final class PointSourceCard extends JPanel
{
	private final JLabel icon = new JLabel();

	PointSourceCard(PointsResponse.PointSource source, ItemManager itemManager, boolean useSourceSubtitle)
	{
		Integer itemId = source.metadata == null ? null : source.metadata.itemId;
		String description = useSourceSubtitle && source.metadata != null
			? source.metadata.source : source.description;
		init(source.name, description, source.getPointsDisplay(), itemId, itemManager);
	}

	PointSourceCard(String name, String description, String points)
	{
		init(name, description, points, null, null);
	}

	private void init(String name, String description, String points, Integer itemId, ItemManager itemManager)
	{
		setLayout(new BorderLayout(4, 0));
		setBackground(RevalUiConstants.ROW_BG);
		setBorder(new EmptyBorder(5, 8, 5, 8));
		setToolTipText(name);
		boolean hasIcon = itemId != null && itemManager != null;
		JPanel left = new JPanel(new BorderLayout(hasIcon ? 6 : 0, 0));
		left.setOpaque(false);
		if (hasIcon)
		{
			icon.setPreferredSize(new Dimension(24, 24));
			left.add(icon, BorderLayout.WEST);
			try
			{
				AsyncBufferedImage image = itemManager.getImage(itemId);
				image.onLoaded(() -> SwingUtilities.invokeLater(() ->
					icon.setIcon(new ImageIcon(ImageUtil.resizeImage(image, 24, 24)))));
			}
			catch (Exception ignored) { }
		}
		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		JLabel title = new JLabel(name);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(RevalUiConstants.TEXT_PRIMARY);
		text.add(title);
		if (description != null && !description.isEmpty())
		{
			JTextArea detail = new JTextArea(description);
			detail.setFont(FontManager.getRunescapeSmallFont());
			detail.setForeground(RevalUiConstants.TEXT_SECONDARY);
			detail.setEditable(false);
			detail.setFocusable(false);
			detail.setLineWrap(true);
			detail.setWrapStyleWord(true);
			detail.setOpaque(false);
			detail.setBorder(null);
			text.add(detail);
		}
		left.add(text, BorderLayout.CENTER);
		JLabel value = new JLabel(points == null ? "" : points);
		value.setFont(FontManager.getRunescapeSmallFont());
		value.setForeground(RevalUiConstants.ACCENT_GREEN);
		value.setVerticalAlignment(SwingConstants.TOP);
		add(left, BorderLayout.CENTER);
		add(value, BorderLayout.EAST);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(getBackground());
		g.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
		g.dispose();
	}
}
