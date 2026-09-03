package com.liveon;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/** Reval's mouse-press click behavior, including its trackpad-safe semantics. */
final class Clickable
{
	private Clickable() { }

	static void onPress(JComponent component, Runnable action, Color hover, Color resting)
	{
		component.addMouseListener(new MouseAdapter()
		{
			@Override public void mouseEntered(MouseEvent event) { component.setBackground(hover); }
			@Override public void mouseExited(MouseEvent event) { component.setBackground(resting); }
			@Override public void mousePressed(MouseEvent event)
			{
				if (SwingUtilities.isLeftMouseButton(event)) action.run();
			}
		});
		component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}
}
