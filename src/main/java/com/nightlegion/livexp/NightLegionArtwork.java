package com.nightlegion.livexp;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

final class NightLegionArtwork
{
    private static final String WELCOME_RESOURCE =
        "/com/nightlegion/livexp/nightlegion-welcome.png";

    private NightLegionArtwork()
    {
    }

    static ImageIcon welcomeIcon(int width)
    {
        try (InputStream stream = NightLegionArtwork.class.getResourceAsStream(WELCOME_RESOURCE))
        {
            if (stream == null)
            {
                return new ImageIcon();
            }

            BufferedImage source = ImageIO.read(stream);
            if (source == null)
            {
                return new ImageIcon();
            }

            int height = Math.max(1, source.getHeight() * width / source.getWidth());
            Image scaled = source.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        catch (Exception ignored)
        {
            return new ImageIcon();
        }
    }
}
