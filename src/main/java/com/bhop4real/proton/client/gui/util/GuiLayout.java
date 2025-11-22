package com.bhop4real.proton.client.gui.util;

/**
 * Utility class for GUI layout calculations
 */
public final class GuiLayout
{
    private GuiLayout() {} // Prevent instantiation
    
    /**
     * Calculates centered position for a component
     */
    public static int centerX(int screenWidth, int componentWidth)
    {
        return (screenWidth - componentWidth) / 2;
    }
    
    /**
     * Calculates centered position for a component
     */
    public static int centerY(int screenHeight, int componentHeight)
    {
        return (screenHeight - componentHeight) / 2;
    }
    
    /**
     * Clamps a value between min and max
     */
    public static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Clamps a float value between min and max
     */
    public static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Calculates scroll bounds
     */
    public static int calculateScroll(int currentScroll, int contentHeight, int visibleHeight)
    {
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        return Math.max(0, Math.min(currentScroll, maxScroll));
    }
    
    /**
     * Checks if a point is within a rectangle
     */
    public static boolean isPointInRect(int x, int y, int rectX, int rectY, int rectWidth, int rectHeight)
    {
        return x >= rectX && x <= rectX + rectWidth && y >= rectY && y <= rectY + rectHeight;
    }
}

