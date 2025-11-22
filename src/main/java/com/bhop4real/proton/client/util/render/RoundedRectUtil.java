package com.bhop4real.proton.client.util.render;

import com.bhop4real.proton.client.util.render.blur.BlurShader;
import com.bhop4real.proton.client.util.shader.ShaderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

/**
 * Utility for drawing simple rounded rectangles
 * Based on proven client implementations
 */
public final class RoundedRectUtil
{
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final BlurShader roundedShader = new BlurShader("proton:shaders/rrect.frag");
    private static boolean preloaded;
    private RoundedRectUtil() {} // Prevent instantiation

	private static void setShaderUniforms(double x, double y, double width, double height, double radius, int color)
	{
		ScaledResolution sr = new ScaledResolution(mc);
		float scale = (float) sr.getScaleFactor();
		float locX = (float) (x * scale);
		float locY = (float) ((mc.displayHeight - (height * scale)) - (y * scale));
		float sizeW = (float) (width * scale);
		float sizeH = (float) (height * scale);
		float rad = (float) (radius * scale);

		float a = (float)(color >> 24 & 255) / 255.0F;
		float r = (float)(color >> 16 & 255) / 255.0F;
		float g = (float)(color >> 8 & 255) / 255.0F;
		float b = (float)(color & 255) / 255.0F;

		roundedShader.setUniformf("location", locX, locY);
		roundedShader.setUniformf("rectSize", sizeW, sizeH);
		roundedShader.setUniformf("radius", rad);
		roundedShader.setUniformf("color", r, g, b, a);
	}

    /**
     * Draws a rounded rectangle with smooth corners
     * @param x Left position
     * @param y Top position
     * @param width Width
     * @param height Height
     * @param radius Corner radius
     * @param color Color (ARGB format)
     */
	public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color)
    {
		if (radius <= 0)
		{
			RenderUtil.drawRect((int)x, (int)y, (int)(x + width), (int)(y + height), color);
			return;
		}

		// Clamp radius to half of the smaller dimension
		radius = Math.min(radius, Math.min(width, height) / 2.0F);

		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.color(1, 1, 1, 1);

		roundedShader.init();
		setShaderUniforms(x, y, width, height, radius, color);
		// Slightly expand draw area to avoid edge clipping from smoothing
		ShaderUtil.drawQuads(x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F);
		roundedShader.unload();
		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glPopAttrib();
    }

    /**
     * Overload for int coordinates
     */
    public static void drawRoundedRect(int x, int y, int width, int height, int radius, int color)
    {
        drawRoundedRect((float)x, (float)y, (float)width, (float)height, (float)radius, color);
    }

    /**
     * Overload for double coordinates
     */
    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color)
    {
        drawRoundedRect((float)x, (float)y, (float)width, (float)height, (float)radius, color);
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
        }
        finally
        {
            roundedShader.unload();
        }
        preloaded = true;
    }
}
