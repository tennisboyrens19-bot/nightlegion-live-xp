package com.liveon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.Icon;

/** Reval's code-drawn expansion icon, avoiding missing RuneScape font glyphs. */
final class TriangleIcon implements Icon
{
	private final boolean down;
	private final int size;
	private final Color color;

	TriangleIcon(boolean down, int size, Color color)
	{
		this.down = down;
		this.size = size;
		this.color = color;
	}

	@Override public int getIconWidth() { return size; }
	@Override public int getIconHeight() { return size; }

	@Override
	public void paintIcon(Component component, Graphics graphics, int x, int y)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(color);
		Polygon triangle = new Polygon();
		if (down)
		{
			triangle.addPoint(x, y + 2);
			triangle.addPoint(x + size - 1, y + 2);
			triangle.addPoint(x + (size - 1) / 2, y + size - 2);
		}
		else
		{
			triangle.addPoint(x, y + size - 2);
			triangle.addPoint(x + size - 1, y + size - 2);
			triangle.addPoint(x + (size - 1) / 2, y + 2);
		}
		g.fillPolygon(triangle);
		g.dispose();
	}
}
