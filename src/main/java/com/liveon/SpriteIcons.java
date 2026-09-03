package com.liveon;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.ImageUtil;

/** Reval's aspect-preserving async game sprite loader. */
final class SpriteIcons
{
	private SpriteIcons() { }

	static void load(SpriteManager manager, int spriteId, int size, Consumer<ImageIcon> consumer)
	{
		if (manager == null || consumer == null) return;
		manager.getSpriteAsync(spriteId, 0, sprite ->
		{
			if (sprite != null) SwingUtilities.invokeLater(() -> consumer.accept(fit(sprite, size)));
		});
	}

	static void apply(SpriteManager manager, int spriteId, JLabel target, int size)
	{
		if (target != null) load(manager, spriteId, size, target::setIcon);
	}

	private static ImageIcon fit(BufferedImage image, int size)
	{
		BufferedImage scaled = image;
		if (scaled.getWidth() > size || scaled.getHeight() > size)
		{
			double scale = Math.min((double) size / scaled.getWidth(), (double) size / scaled.getHeight());
			scaled = ImageUtil.resizeImage(scaled,
				Math.max(1, (int) Math.round(scaled.getWidth() * scale)),
				Math.max(1, (int) Math.round(scaled.getHeight() * scale)));
		}
		return new ImageIcon(ImageUtil.resizeCanvas(scaled, size, size));
	}
}
