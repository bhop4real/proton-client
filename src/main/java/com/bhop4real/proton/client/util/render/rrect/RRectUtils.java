package com.bhop4real.proton.client.util.render.rrect;

import com.bhop4real.proton.client.util.render.blur.BlurShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class RRectUtils
{
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final BlurShader roundedShader = new BlurShader("proton:shaders/rrect.frag");
    public static final BlurShader roundedOutlineShader = new BlurShader("proton:shaders/rrectOutline.frag");
    private static final BlurShader roundedGradientShader = new BlurShader("proton:shaders/rrectGradient.frag");
    private static boolean preloaded;

    private RRectUtils() {}

    private static void setupRoundedRectUniforms(double x, double y, double width, double height, double radius, BlurShader shader)
    {
        ScaledResolution sr = new ScaledResolution(mc);
        shader.setUniformf("location", (float)(x * sr.getScaleFactor()), (float)((mc.displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor())));
        shader.setUniformf("rectSize", (float)(width * sr.getScaleFactor()), (float)(height * sr.getScaleFactor()));
        shader.setUniformf("radius", (float)(radius * sr.getScaleFactor()));
    }

    public static void drawRound(double x, double y, double width, double height, double radius, Color color)
    {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        roundedShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        com.bhop4real.proton.client.util.shader.ShaderUtil.drawQuads((float)(x - 1), (float)(y - 1), (float)(width + 2), (float)(height + 2));
        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawRoundOutline(double x, double y, double width, double height, double radius, double outlineThickness, Color color, Color outlineColor)
    {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ScaledResolution sr = new ScaledResolution(mc);
        roundedOutlineShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedOutlineShader);
        roundedOutlineShader.setUniformf("outlineThickness", (float)(outlineThickness * sr.getScaleFactor()));
        roundedOutlineShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        roundedOutlineShader.setUniformf("outlineColor", outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f, outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f);

        com.bhop4real.proton.client.util.shader.ShaderUtil.drawQuads((float)(x - (2 + outlineThickness)), (float)(y - (2 + outlineThickness)), (float)(width + (4 + outlineThickness * 2)), (float)(height + (4 + outlineThickness * 2)));
        roundedOutlineShader.unload();
        GlStateManager.disableBlend();
    }

    public static void drawGradientRoundCorner(double x, double y, double width, double height, double radius, Color c1, Color c2, Color c3, Color c4)
    {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        roundedGradientShader.init();
        setupRoundedRectUniforms(x, y, width, height, radius, roundedGradientShader);
        roundedGradientShader.setUniformf("color1", c1.getRed() / 255f, c1.getGreen() / 255f, c1.getBlue() / 255f, c1.getAlpha() / 255f);
        roundedGradientShader.setUniformf("color2", c2.getRed() / 255f, c2.getGreen() / 255f, c2.getBlue() / 255f, c2.getAlpha() / 255f);
        roundedGradientShader.setUniformf("color3", c3.getRed() / 255f, c3.getGreen() / 255f, c3.getBlue() / 255f, c3.getAlpha() / 255f);
        roundedGradientShader.setUniformf("color4", c4.getRed() / 255f, c4.getGreen() / 255f, c4.getBlue() / 255f, c4.getAlpha() / 255f);

        com.bhop4real.proton.client.util.shader.ShaderUtil.drawQuads((float)(x - 1), (float)(y - 1), (float)(width + 2), (float)(height + 2));
        roundedGradientShader.unload();
        GlStateManager.disableBlend();
    }

    public static void preload()
    {
        if (preloaded)
        {
            return;
        }

        try
        {
            roundedShader.init();
            roundedShader.unload();
            roundedOutlineShader.init();
            roundedOutlineShader.unload();
            roundedGradientShader.init();
        }
        finally
        {
            roundedGradientShader.unload();
        }
        preloaded = true;
    }
}


