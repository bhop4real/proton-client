package com.bhop4real.proton.client.util.font;

/**
 * Utility class for accessing custom font renderer
 * Provides a simple interface that falls back to default font if custom font fails
 */
public final class FontUtil
{
    private static CustomFontRenderer customFont;
    private static boolean fontEnabled = true;
    
    static
    {
        try
        {
            customFont = CustomFontRenderer.getInstance();
        }
        catch (Exception e)
        {
            customFont = null;
            fontEnabled = false;
        }
    }
    
    private FontUtil() {} // Prevent instantiation
    
    /**
     * Draws a string using custom font (with fallback)
     */
    public static void drawString(String text, float x, float y, int color)
    {
        drawString(text, x, y, color, true);
    }
    
    /**
     * Draws a string using custom font (with fallback)
     */
    public static void drawString(String text, float x, float y, int color, boolean shadow)
    {
        // Try custom font first (it will initialize lazily if needed)
        if (fontEnabled && customFont != null)
        {
            try
            {
                // CustomFontRenderer.drawString will initialize if needed
                customFont.drawString(text, x, y, color, shadow);
                
                // If it's initialized, assume it rendered (even if it returned early due to empty text)
                // If text is empty or all color codes, that's fine - nothing to render
                if (customFont.isInitialized())
                {
                    // It's initialized, assume it rendered (even if it returned early due to empty text)
                    // If text is empty or all color codes, that's fine - nothing to render
                    return;
                }
                // If not initialized, fall through to default
            }
            catch (Exception e)
            {
                // Log error but continue to fallback
                com.bhop4real.proton.Proton.logger.warn("Custom font rendering failed, using fallback: " + e.getMessage(), e);
            }
        }
        
        // Fallback to default font
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc != null && mc.fontRendererObj != null)
        {
            try
            {
                if (shadow)
                {
                    mc.fontRendererObj.drawStringWithShadow(text, (int)x, (int)y, color);
                }
                else
                {
                    mc.fontRendererObj.drawString(text, (int)x, (int)y, color);
                }
            }
            catch (Exception e)
            {
                // If even fallback fails, log it
                com.bhop4real.proton.Proton.logger.error("Even fallback font rendering failed", e);
            }
        }
    }
    
    /**
     * Gets string width using custom font (with fallback)
     */
    public static int getStringWidth(String text)
    {
        // Try custom font first (it will initialize lazily if needed)
        if (fontEnabled && customFont != null)
        {
            try
            {
                // CustomFontRenderer.getStringWidth will initialize if needed
                int width = customFont.getStringWidth(text);
                
                // If initialized and returned a valid width, use it
                if (customFont.isInitialized())
                {
                    return width;
                }
            }
            catch (Exception e)
            {
                // Fall through to default
            }
        }
        
        // Fallback to default font
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc != null && mc.fontRendererObj != null)
        {
            return mc.fontRendererObj.getStringWidth(text);
        }
        return 0;
    }
    
    /**
     * Gets font height using custom font (with fallback)
     */
    public static int getFontHeight()
    {
        // Try custom font first (it will initialize lazily if needed)
        if (fontEnabled && customFont != null)
        {
            try
            {
                // CustomFontRenderer.getFontHeight will work even if not fully initialized
                int height = customFont.getFontHeight();
                
                // If initialized, use it; otherwise fallback
                if (customFont.isInitialized())
                {
                    return height;
                }
            }
            catch (Exception e)
            {
                // Fall through to default
            }
        }
        
        // Fallback to default font
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc != null && mc.fontRendererObj != null)
        {
            return mc.fontRendererObj.FONT_HEIGHT;
        }
        return 9; // Safe default
    }
    
    /**
     * Enable or disable custom font rendering
     */
    public static void setFontEnabled(boolean enabled)
    {
        fontEnabled = enabled;
    }
    
    /**
     * Check if custom font is enabled and available
     */
    public static boolean isFontEnabled()
    {
        return fontEnabled && customFont != null;
    }
}

