package com.bhop4real.proton.client.util.render;

import com.bhop4real.proton.client.util.font.FontUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Core rendering utilities for drawing basic shapes and text
 * All text rendering uses custom font
 */
public final class RenderUtil
{
    private RenderUtil() {} // Prevent instantiation
    
    /**
     * Draws a filled rectangle
     */
    public static void drawRect(int left, int top, int right, int bottom, int color)
    {
        // Normalize coordinates
        if (left < right)
        {
            int i = left;
            left = right;
            right = i;
        }
        
        if (top < bottom)
        {
            int j = top;
            top = bottom;
            bottom = j;
        }
        
        float alpha = (float)(color >> 24 & 255) / 255.0F;
        float red = (float)(color >> 16 & 255) / 255.0F;
        float green = (float)(color >> 8 & 255) / 255.0F;
        float blue = (float)(color & 255) / 255.0F;
        
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d((double)left, (double)bottom);
        GL11.glVertex2d((double)right, (double)bottom);
        GL11.glVertex2d((double)right, (double)top);
        GL11.glVertex2d((double)left, (double)top);
        GL11.glEnd();
        
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
    
    /**
     * Draws a rectangle using Minecraft's Gui.drawRect (faster but less control)
     */
    public static void drawRectFast(int left, int top, int right, int bottom, int color)
    {
        Gui.drawRect(left, top, right, bottom, color);
    }
    
    /**
     * Draws a bordered rectangle
     */
    public static void drawBorderedRect(int x, int y, int width, int height, float borderWidth, int borderColor, int fillColor)
    {
        // Draw fill
        drawRect(x, y, x + width, y + height, fillColor);
        
        // Draw borders
        drawRect(x, y, x + width, y + (int)borderWidth, borderColor); // Top
        drawRect(x, y + height - (int)borderWidth, x + width, y + height, borderColor); // Bottom
        drawRect(x, y, x + (int)borderWidth, y + height, borderColor); // Left
        drawRect(x + width - (int)borderWidth, y, x + width, y + height, borderColor); // Right
    }
    
    /**
     * Draws a horizontal line
     */
    public static void drawHorizontalLine(int startX, int endX, int y, int color)
    {
        if (endX < startX)
        {
            int temp = startX;
            startX = endX;
            endX = temp;
        }
        
        drawRect(startX, y, endX + 1, y + 1, color);
    }
    
    /**
     * Draws a vertical line
     */
    public static void drawVerticalLine(int x, int startY, int endY, int color)
    {
        if (endY < startY)
        {
            int temp = startY;
            startY = endY;
            endY = temp;
        }
        
        drawRect(x, startY + 1, x + 1, endY, color);
    }
    
    /**
     * Draws a gradient rectangle
     */
    public static void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor)
    {
        float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
        float startRed = (float)(startColor >> 16 & 255) / 255.0F;
        float startGreen = (float)(startColor >> 8 & 255) / 255.0F;
        float startBlue = (float)(startColor & 255) / 255.0F;
        
        float endAlpha = (float)(endColor >> 24 & 255) / 255.0F;
        float endRed = (float)(endColor >> 16 & 255) / 255.0F;
        float endGreen = (float)(endColor >> 8 & 255) / 255.0F;
        float endBlue = (float)(endColor & 255) / 255.0F;
        
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos((double)right, (double)top, 0.0D).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldRenderer.pos((double)left, (double)top, 0.0D).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        worldRenderer.pos((double)left, (double)bottom, 0.0D).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        worldRenderer.pos((double)right, (double)bottom, 0.0D).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        tessellator.draw();
        
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
    
    /**
     * Draws text with shadow using custom font
     */
    public static void drawString(String text, int x, int y, int color)
    {
        FontUtil.drawString(text, x, y, color, true);
    }
    
    /**
     * Draws text with optional shadow using custom font
     */
    public static void drawString(String text, int x, int y, int color, boolean shadow)
    {
        FontUtil.drawString(text, x, y, color, shadow);
    }
    
    /**
     * Gets the width of a string using custom font
     */
    public static int getStringWidth(String text)
    {
        return FontUtil.getStringWidth(text);
    }
    
    /**
     * Gets the font height using custom font
     */
    public static int getFontHeight()
    {
        return FontUtil.getFontHeight();
    }
}

