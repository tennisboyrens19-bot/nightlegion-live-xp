package com.revalclan.util;

import org.junit.Test;

import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.*;

public class UIAssetLoaderTest {
    private static final String ASSETS = "/com/revalclan/ui/assets/";
    private static final String[] BUNDLED_IMAGES = {
        "reval", "checkmark", "info", "discord", "website"
    };

    @Test
    public void bundledImagesKeepTheirPathsAndNormalizedCache() {
        UIAssetLoader loader = new UIAssetLoader();
        for (String name : BUNDLED_IMAGES) {
            BufferedImage image = loader.getImage(name);
            assertNotNull(name, image);
            assertTrue(image.getWidth() > 0);
            assertTrue(image.getHeight() > 0);
            assertSame(image, loader.getImage(name + ".png"));
            assertSame(image, loader.getImage(ASSETS + name + ".png"));
        }
    }

    @Test
    public void bundledIconsLoadWithoutAPrimedImageCacheAndKeepSizeCaches() {
        UIAssetLoader loader = new UIAssetLoader();
        for (String name : BUNDLED_IMAGES) {
            ImageIcon icon = loader.getIcon(name, 24);
            assertNotNull(name, icon);
            assertEquals(24, icon.getIconWidth());
            assertEquals(24, icon.getIconHeight());
            assertSame(icon, loader.getIcon(ASSETS + name + ".png", 24));
            BufferedImage image = loader.getImage(name);
            assertNotNull(image);
            ImageIcon larger = loader.getIcon(name, 32);
            assertNotSame(icon, larger);
            assertEquals(32, larger.getIconWidth());
            assertEquals(32, larger.getIconHeight());
            assertSame(image, loader.getImage(name));
        }
    }

    @Test
    public void nullEmptyAndMissingResourcesKeepTheNullFallback() {
        UIAssetLoader loader = new UIAssetLoader();
        for (String name : new String[] {null, "", "missing-resource-for-test"}) {
            assertNull(loader.getImage(name));
            assertNull(loader.getIcon(name, 24));
        }
    }

    @Test
    public void imagePathUsesAndClosesAStreamThenReusesTheImageCache() throws Exception {
        StreamOnlyClassLoader resources = new StreamOnlyClassLoader(bundledInfoBytes(), false);
        Object loader = resources.newLoader();
        Object image = getImage(loader);
        assertNotNull(image);
        assertTrue(resources.closed);
        assertSame(image, getImage(loader));
        assertNotNull(getIcon(loader));
        assertEquals(1, resources.openCount);
    }

    @Test
    public void iconPathUsesAndClosesAStreamThenReusesBothCaches() throws Exception {
        StreamOnlyClassLoader resources = new StreamOnlyClassLoader(bundledInfoBytes(), false);
        Object loader = resources.newLoader();
        Object icon = getIcon(loader);
        assertNotNull(icon);
        assertTrue(resources.closed);
        assertSame(icon, getIcon(loader));
        assertNotNull(getImage(loader));
        assertEquals(1, resources.openCount);
    }

    @Test
    public void undecodableResourcesCloseStreamsAndAreNotCached() throws Exception {
        assertFailedStreamFallback(new byte[] {1, 2, 3, 4}, false, false);
    }

    @Test
    public void readFailuresCloseStreamsAndKeepTheNullFallback() throws Exception {
        assertFailedStreamFallback(bundledInfoBytes(), true, false);
    }

    @Test
    public void closeFailuresKeepTheNullFallbackAndDoNotPopulateTheImageCache() throws Exception {
        assertFailedStreamFallback(bundledInfoBytes(), false, true);
    }

    private static void assertFailedStreamFallback(byte[] bytes, boolean failReads, boolean failClose) throws Exception {
        for (boolean iconFirst : new boolean[] {false, true}) {
            StreamOnlyClassLoader resources = new StreamOnlyClassLoader(bytes, failReads, failClose);
            Object loader = resources.newLoader();
            assertNull(iconFirst ? getIcon(loader) : getImage(loader));
            assertTrue(resources.closed);
            assertNull(iconFirst ? getIcon(loader) : getImage(loader));
            assertTrue(resources.closed);
            assertEquals(2, resources.openCount);
        }
    }

    private static Object getImage(Object loader) throws Exception {
        return loader.getClass().getMethod("getImage", String.class).invoke(loader, "info");
    }

    private static Object getIcon(Object loader) throws Exception {
        return loader.getClass().getMethod("getIcon", String.class, int.class).invoke(loader, "info", 24);
    }

    private static byte[] bundledInfoBytes() throws IOException {
        try (InputStream stream = UIAssetLoaderTest.class.getResourceAsStream(ASSETS + "info.png")) {
            assertNotNull(stream);
            return stream.readAllBytes();
        }
    }

    /** Loads the real implementation with resources that cannot be accessed by URL. */
    private static final class StreamOnlyClassLoader extends ClassLoader {
        private final byte[] bytes;
        private final boolean failReads;
        private final boolean failClose;
        private int openCount;
        private boolean closed;

        private StreamOnlyClassLoader(byte[] bytes, boolean failReads) {
            this(bytes, failReads, false);
        }

        private StreamOnlyClassLoader(byte[] bytes, boolean failReads, boolean failClose) {
            super(UIAssetLoaderTest.class.getClassLoader());
            this.bytes = bytes;
            this.failReads = failReads;
            this.failClose = failClose;
        }

        private Object newLoader() throws Exception {
            return loadClass(UIAssetLoader.class.getName()).getConstructor().newInstance();
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(UIAssetLoader.class.getName())) {
                return super.loadClass(name, resolve);
            }
            Class<?> type = findLoadedClass(name);
            if (type == null) {
                try (InputStream stream = UIAssetLoader.class.getResourceAsStream("UIAssetLoader.class")) {
                    assertNotNull(stream);
                    byte[] definition = stream.readAllBytes();
                    type = defineClass(name, definition, 0, definition.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
            if (resolve) {
                resolveClass(type);
            }
            return type;
        }

        @Override
        public URL getResource(String name) {
            if (name.startsWith(ASSETS.substring(1))) {
                throw new AssertionError("Bundled images must use Class.getResourceAsStream()");
            }
            return super.getResource(name);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (!name.startsWith(ASSETS.substring(1))) {
                return super.getResourceAsStream(name);
            }
            assertEquals(ASSETS.substring(1) + "info.png", name);
            openCount++;
            closed = false;
            return new FilterInputStream(new ByteArrayInputStream(bytes)) {
                @Override
                public int read() throws IOException {
                    if (failReads) throw new IOException("Fixture read failure");
                    return super.read();
                }

                @Override
                public int read(byte[] target, int offset, int length) throws IOException {
                    if (failReads) throw new IOException("Fixture read failure");
                    return super.read(target, offset, length);
                }

                @Override
                public void close() throws IOException {
                    closed = true;
                    super.close();
                    if (failClose) throw new IOException("Fixture close failure");
                }
            };
        }
    }
}
