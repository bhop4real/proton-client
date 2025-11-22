package com.bhop4real.proton.client.util.font;

import com.bhop4real.proton.Proton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * TrueType font renderer for Proton client.
 */
public final class CustomFontRenderer
{
    private static final int ATLAS_SIZE = 1024;
    private static final String COLOR_CODES = "0123456789abcdefklmnor";
    private static final float ADVANCE_FIX = 8.0F;
    private static final float EXTRA_SPACING = 0.65F;
    private static final int DEFAULT_FONT_SIZE = 18;

    private static CustomFontRenderer instance;

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final GlyphAtlas regularAtlas;
    private final GlyphAtlas boldAtlas;
    private final GlyphAtlas italicAtlas;
    private final GlyphAtlas boldItalicAtlas;
    private final int[] colorTable = new int[32];

    private CustomFontRenderer(Font baseFont)
    {
        this.regularAtlas = new GlyphAtlas(baseFont, true, true);
        this.boldAtlas = new GlyphAtlas(baseFont.deriveFont(Font.BOLD), true, true);
        this.italicAtlas = new GlyphAtlas(baseFont.deriveFont(Font.ITALIC), true, true);
        this.boldItalicAtlas = new GlyphAtlas(baseFont.deriveFont(Font.BOLD | Font.ITALIC), true, true);
        buildColorTable();
    }

    public static CustomFontRenderer getInstance()
    {
        if (instance == null)
        {
            Font font = loadFont("proton", "fonts/default.ttf", DEFAULT_FONT_SIZE);
            instance = new CustomFontRenderer(font);
        }
        return instance;
    }

    /**
     * Creates a separate renderer instance for a specific font resource and size.
     */
    public static CustomFontRenderer createFrom(ResourceLocation resourceLocation, int size)
    {
        Font font = loadFont(resourceLocation.getResourceDomain(), resourceLocation.getResourcePath(), size);
        return new CustomFontRenderer(font);
    }

    public boolean isInitialized()
    {
        return regularAtlas.isReady();
    }

    public double drawString(String text, double x, double y, int color)
    {
        return drawString(text, x, y, color, false);
    }

    public double drawString(String text, double x, double y, int color, boolean shadow)
    {
        if (text == null || text.isEmpty())
        {
            return 0.0D;
        }

        ScaledResolution resolution = new ScaledResolution(minecraft);
        double scaleFactor = resolution.getScaleFactor();
        double renderX = (x - 1.0D) * scaleFactor;
        double renderY = (y - 3.0D) * scaleFactor;

        GL11.glPushMatrix();
        double inverseScale = 1.0D / scaleFactor;
        GL11.glScaled(inverseScale, inverseScale, inverseScale);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();

        double width = 0.0D;
        if (shadow)
        {
            int shadowColor = (color & 0xFF000000) | ((color & 0xFCFCFC) >> 2);
            width = renderLine(text, renderX + 1.0D, renderY + 1.0D, shadowColor, true) / scaleFactor;
        }

        width = renderLine(text, renderX, renderY, color, false) / scaleFactor;

        GlStateManager.disableBlend();
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GL11.glPopMatrix();

        return width;
    }

    public int getStringWidth(String text)
    {
        if (text == null || text.isEmpty())
        {
            return 0;
        }

        ScaledResolution resolution = new ScaledResolution(minecraft);
        double scaleFactor = resolution.getScaleFactor();
        boolean bold = false;
        boolean italic = false;
        GlyphAtlas atlas = regularAtlas;

        double width = 0.0D;
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length())
            {
                char modifier = Character.toLowerCase(text.charAt(++i));
                int modifierIndex = COLOR_CODES.indexOf(modifier);
                if (modifierIndex >= 0 && modifierIndex < 16)
                {
                    bold = false;
                    italic = false;
                    atlas = regularAtlas;
                }
                else if (modifier == 'l')
                {
                    bold = true;
                    atlas = resolveAtlas(bold, italic);
                }
                else if (modifier == 'o')
                {
                    italic = true;
                    atlas = resolveAtlas(bold, italic);
                }
                else if (modifier == 'r')
                {
                    bold = false;
                    italic = false;
                    atlas = regularAtlas;
                }
                continue;
            }

            Glyph glyph = atlas.getGlyph(c);
            width += glyph.getAdvance(EXTRA_SPACING);
        }
        return (int) Math.round(width / scaleFactor);
    }

    public int getFontHeight()
    {
        ScaledResolution resolution = new ScaledResolution(minecraft);
        return (int) Math.round(regularAtlas.getFontHeight() / resolution.getScaleFactor());
    }

    private double renderLine(String text, double startX, double startY, int color, boolean shadow)
    {
        GlyphAtlas atlas = regularAtlas;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;

        float baseAlpha = ((color >> 24) & 255) / 255.0F;
        if (baseAlpha <= 0.0F)
        {
            baseAlpha = 1.0F;
        }
        float defaultRed = (color >> 16 & 255) / 255.0F;
        float defaultGreen = (color >> 8 & 255) / 255.0F;
        float defaultBlue = (color & 255) / 255.0F;

        float currentRed = defaultRed;
        float currentGreen = defaultGreen;
        float currentBlue = defaultBlue;
        float currentAlpha = baseAlpha;

        applyColor(currentRed, currentGreen, currentBlue, currentAlpha);
        atlas.bind();

        double drawX = startX;
        double drawY = startY;

        GL11.glBegin(GL11.GL_QUADS);

        for (int i = 0; i < text.length(); i++)
        {
            char character = text.charAt(i);
            if (character == '§' && i + 1 < text.length())
            {
                char modifier = Character.toLowerCase(text.charAt(++i));
                int modifierIndex = COLOR_CODES.indexOf(modifier);

                if (modifierIndex >= 0 && modifierIndex < 16)
                {
                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    atlas = regularAtlas;

                    int lookupIndex = modifierIndex + (shadow ? 16 : 0);
                    int rgb = colorTable[Math.max(0, Math.min(lookupIndex, colorTable.length - 1))];
                    currentRed = (rgb >> 16 & 255) / 255.0F;
                    currentGreen = (rgb >> 8 & 255) / 255.0F;
                    currentBlue = (rgb & 255) / 255.0F;
                    currentAlpha = baseAlpha;
                    applyColor(currentRed, currentGreen, currentBlue, currentAlpha);
                    atlas.bind();
                    continue;
                }

                switch (modifier)
                {
                    case 'l':
                        bold = true;
                        atlas = resolveAtlas(bold, italic);
                        atlas.bind();
                        break;
                    case 'o':
                        italic = true;
                        atlas = resolveAtlas(bold, italic);
                        atlas.bind();
                        break;
                    case 'm':
                        strikethrough = true;
                        break;
                    case 'n':
                        underline = true;
                        break;
                    case 'r':
                        bold = false;
                        italic = false;
                        underline = false;
                        strikethrough = false;
                        currentRed = defaultRed;
                        currentGreen = defaultGreen;
                        currentBlue = defaultBlue;
                        currentAlpha = baseAlpha;
                        applyColor(currentRed, currentGreen, currentBlue, currentAlpha);
                        atlas = regularAtlas;
                        atlas.bind();
                        break;
                    default:
                        break;
                }
                continue;
            }

            Glyph glyph = atlas.getGlyph(character);
            double glyphY = drawY - glyph.height + atlas.getFontHeight();

            atlas.emitGlyph(character, drawX, glyphY);

            if (strikethrough || underline)
            {
                GL11.glEnd();
                GlStateManager.disableTexture2D();
                applyColor(currentRed, currentGreen, currentBlue, currentAlpha);
                GL11.glLineWidth(1.0F);
                GL11.glBegin(GL11.GL_LINES);
                double left = drawX;
                double right = drawX + glyph.width;
                if (strikethrough)
                {
                    double yMid = glyphY + glyph.height / 2.0D;
                    GL11.glVertex2d(left, yMid);
                    GL11.glVertex2d(right, yMid);
                }
                if (underline)
                {
                    double yBottom = glyphY + glyph.height - 1.0D;
                    GL11.glVertex2d(left, yBottom);
                    GL11.glVertex2d(right, yBottom);
                }
                GL11.glEnd();
                GlStateManager.enableTexture2D();
                applyColor(currentRed, currentGreen, currentBlue, currentAlpha);
                atlas.bind();
                GL11.glBegin(GL11.GL_QUADS);
            }

            drawX += atlas.getAdvance(character, EXTRA_SPACING);
        }

        GL11.glEnd();
        return drawX - startX;
    }

    private GlyphAtlas resolveAtlas(boolean bold, boolean italic)
    {
        if (bold && italic)
        {
            return boldItalicAtlas;
        }
        if (bold)
        {
            return boldAtlas;
        }
        if (italic)
        {
            return italicAtlas;
        }
        return regularAtlas;
    }

    private void applyColor(float red, float green, float blue, float alpha)
    {
        GlStateManager.color(red, green, blue, alpha);
    }

    private void buildColorTable()
    {
        for (int index = 0; index < 32; index++)
        {
            int j = (index >> 3 & 1) * 85;
            int r = (index >> 2 & 1) * 170 + j;
            int g = (index >> 1 & 1) * 170 + j;
            int b = (index & 1) * 170 + j;

            if (index == 6)
            {
                r += 85;
            }

            if (index >= 16)
            {
                r /= 4;
                g /= 4;
                b /= 4;
            }

            colorTable[index] = (r & 255) << 16 | (g & 255) << 8 | b & 255;
        }
    }

    private static Font loadFont(String domain, String path, int size)
    {
        try
        {
            ResourceLocation location = new ResourceLocation(domain, path);
            InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
            Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
            stream.close();
            return font.deriveFont(Font.PLAIN, size);
        }
        catch (Exception exception)
        {
            Proton.logger.warn("Falling back to system font: {}", exception.getMessage());
            return new Font("SansSerif", Font.PLAIN, size);
        }
    }

    private static final class GlyphAtlas
    {
        private final Glyph[] glyphs = new Glyph[256];
        private final DynamicTexture texture;
        private final int fontHeight;

        private GlyphAtlas(Font font, boolean antiAlias, boolean fractionalMetrics)
        {
            BufferedImage image = generate(font, antiAlias, fractionalMetrics);
            this.texture = new DynamicTexture(image);
            this.fontHeight = calculateFontHeight();
        }

        private boolean isReady()
        {
            return texture != null;
        }

        private int getFontHeight()
        {
            return fontHeight;
        }

        private Glyph getGlyph(char character)
        {
            if (character >= glyphs.length)
            {
                return glyphs['?'];
            }
            Glyph glyph = glyphs[character];
            return glyph != null ? glyph : glyphs['?'];
        }

        private void emitGlyph(char character, double x, double y)
        {
            Glyph glyph = getGlyph(character);
            double x1 = x;
            double y1 = y;
            double x2 = x + glyph.width;
            double y2 = y + glyph.height;

            float u1 = glyph.storedX;
            float v1 = glyph.storedY;
            float u2 = u1 + glyph.width;
            float v2 = v1 + glyph.height;

            u1 /= ATLAS_SIZE;
            v1 /= ATLAS_SIZE;
            u2 /= ATLAS_SIZE;
            v2 /= ATLAS_SIZE;

            GL11.glTexCoord2f(u2, v1);
            GL11.glVertex2d(x2, y1);
            GL11.glTexCoord2f(u1, v1);
            GL11.glVertex2d(x1, y1);
            GL11.glTexCoord2f(u1, v2);
            GL11.glVertex2d(x1, y2);
            GL11.glTexCoord2f(u2, v2);
            GL11.glVertex2d(x2, y2);
        }

        private double getAdvance(char character, float extraSpacing)
        {
            Glyph glyph = getGlyph(character);
            return glyph.width - ADVANCE_FIX + extraSpacing;
        }

        private void bind()
        {
            GlStateManager.bindTexture(texture.getGlTextureId());
        }

        private BufferedImage generate(Font font, boolean antiAlias, boolean fractionalMetrics)
        {
            BufferedImage image = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setFont(font);
            graphics.setColor(new Color(255, 255, 255, 0));
            graphics.fillRect(0, 0, ATLAS_SIZE, ATLAS_SIZE);
            graphics.setColor(Color.WHITE);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

            FontMetrics metrics = graphics.getFontMetrics();
            int positionX = 0;
            int positionY = 1;
            int rowHeight = 0;

            for (int index = 0; index < glyphs.length; index++)
            {
                char character = (char) index;
                Rectangle2D bounds = metrics.getStringBounds(String.valueOf(character), graphics);
                int glyphWidth = (int) Math.ceil(bounds.getWidth()) + 8;
                int glyphHeight = (int) Math.ceil(bounds.getHeight());

                if (positionX + glyphWidth >= ATLAS_SIZE)
                {
                    positionX = 0;
                    positionY += rowHeight;
                    rowHeight = 0;
                }

                if (glyphHeight > rowHeight)
                {
                    rowHeight = glyphHeight;
                }

                Glyph glyph = new Glyph();
                glyph.width = glyphWidth;
                glyph.height = glyphHeight;
                glyph.storedX = positionX;
                glyph.storedY = positionY;
                glyphs[index] = glyph;

                graphics.drawString(String.valueOf(character), positionX + 2, positionY + metrics.getAscent());
                positionX += glyphWidth;
            }

            graphics.dispose();
            return image;
        }

        private int calculateFontHeight()
        {
            int height = 0;
            for (Glyph glyph : glyphs)
            {
                if (glyph != null && glyph.height > height)
                {
                    height = glyph.height;
                }
            }
            return height;
        }
    }

    private static final class Glyph
    {
        int width;
        int height;
        int storedX;
        int storedY;

        double getAdvance(float extraSpacing)
        {
            return width - ADVANCE_FIX + extraSpacing;
        }
    }
}
