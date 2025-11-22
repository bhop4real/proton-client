package com.bhop4real.proton.client.gui.util;

import com.bhop4real.proton.client.gui.theme.ClickGuiThemeInterface;
import com.bhop4real.proton.client.util.font.FontUtil;
import com.bhop4real.proton.client.util.render.RenderUtil;
import com.bhop4real.proton.client.util.render.RoundedRectUtil;
import com.bhop4real.proton.client.util.render.rrect.RRectUtils;

import java.awt.Color;

/**
 * Utility class for common GUI component rendering
 */
public final class GuiComponent
{
    private GuiComponent() {} // Prevent instantiation
    
    /**
     * Draws a separator line
     */
    public static void drawSeparator(double x, double y, double width, double thickness, int argb)
    {
        if (width <= 0 || thickness <= 0)
        {
            return;
        }
        
        Color outlineColor = new Color(argb, true);
        Color transparentFill = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), 0);
        double radius = Math.max(0.5, thickness / 2.0);
        double outlineThickness = Math.max(0.5, thickness);
        RRectUtils.drawRoundOutline(x, y, width, thickness, radius, outlineThickness, transparentFill, outlineColor);
    }
    
    /**
     * Draws a panel with rounded corners and outline
     */
    public static void drawPanel(int x, int y, int width, int height, float radius, 
                                 int fillColor, int outlineColor, float outlineThickness)
    {
        Color fill = new Color(fillColor, true);
        Color outline = new Color(outlineColor, true);
        RRectUtils.drawRoundOutline(x, y, width, height, radius, outlineThickness, fill, outline);
    }
    
    /**
     * Draws a hoverable item background
     */
    public static void drawItemBackground(int x, int y, int width, int height, 
                                         boolean hovered, boolean selected, ClickGuiThemeInterface theme)
    {
        int baseColor = selected ? theme.categorySelected() : theme.moduleBackground();
        int alpha = hovered ? 200 : 160;
        int color = applyAlpha(baseColor, alpha);
        
        RenderUtil.drawRect(x, y, x + width, y + height, color);
        
        if (selected)
        {
            RenderUtil.drawRect(x, y, x + 3, y + height, theme.accent());
        }
    }
    
    /**
     * Draws a toggle switch
     */
    public static void drawToggle(int x, int y, int width, int height, boolean enabled, ClickGuiThemeInterface theme)
    {
        int radius = Math.min(5, height / 2);
        int trackColor = applyAlpha(theme.moduleHover(), enabled ? 200 : 120);
        RoundedRectUtil.drawRoundedRect(x, y, width, height, radius, enabled ? theme.accentMuted() : trackColor);
        
        int knobX = enabled ? x + width - height : x;
        RoundedRectUtil.drawRoundedRect(knobX, y, height, height, radius, theme.textPrimary());
    }
    
    /**
     * Draws a slider with knob
     */
    public static void drawSlider(int x, int y, int width, int height, float progress, 
                                 boolean hovered, boolean dragging, ClickGuiThemeInterface theme)
    {
        int trackRadius = height / 2;
        int outlineAlpha = hovered || dragging ? 200 : 140;
        int trackAlpha = hovered || dragging ? 255 : 235;
        
        // Outline
        RoundedRectUtil.drawRoundedRect(x - 1, y - 1, width + 2, height + 2,
                trackRadius + 1, applyAlpha(theme.outline(), outlineAlpha));
        
        // Track
        RoundedRectUtil.drawRoundedRect(x, y, width, height,
                trackRadius, applyAlpha(theme.settingBackground(), trackAlpha));
        
        // Fill
        int innerLeft = x + 2;
        int innerTop = y + 2;
        int innerWidth = width - 4;
        int innerHeight = Math.max(3, height - 4);
        int fillWidth = Math.round(innerWidth * Math.max(0.0F, Math.min(1.0F, progress)));
        
        if (fillWidth > 0)
        {
            RoundedRectUtil.drawRoundedRect(innerLeft, innerTop, fillWidth, innerHeight,
                    Math.max(2, innerHeight / 2), theme.accent());
        }
        
        // Knob
        int knobDiameter = Math.max(6, innerHeight + 4);
        int knobCenterX;
        if (fillWidth <= 0)
        {
            knobCenterX = innerLeft;
        }
        else if (fillWidth >= innerWidth)
        {
            knobCenterX = innerLeft + innerWidth;
        }
        else
        {
            knobCenterX = innerLeft + fillWidth;
        }
        int knobX = knobCenterX - knobDiameter / 2;
        int knobY = y + (height - knobDiameter) / 2;
        
        RoundedRectUtil.drawRoundedRect(knobX, knobY, knobDiameter, knobDiameter,
                Math.max(3, knobDiameter / 2), applyAlpha(theme.outline(), hovered || dragging ? 220 : 170));
        RoundedRectUtil.drawRoundedRect(knobX + 1, knobY + 1, knobDiameter - 2, knobDiameter - 2,
                Math.max(2, (knobDiameter - 2) / 2), theme.textPrimary());
    }
    
    /**
     * Draws a pill-shaped button (for enum settings)
     */
    public static void drawPill(int x, int y, int width, int height, String text, 
                               boolean hovered, boolean open, ClickGuiThemeInterface theme)
    {
        int radius = Math.min(6, height / 2);
        int pillColor = open ? theme.accentMuted() : applyAlpha(theme.moduleHover(), hovered ? 180 : 120);
        
        if (height > 0)
        {
            RoundedRectUtil.drawRoundedRect(x, y, width, height, radius, pillColor);
        }
        
        int padding = 6;
        float textY = y + (height - FontUtil.getFontHeight()) / 2.0F + 1;
        FontUtil.drawString(text, x + padding, textY, theme.textPrimary(), true);
    }
    
    /**
     * Applies alpha to a color
     */
    private static int applyAlpha(int color, int alpha)
    {
        return (alpha & 0xFF) << 24 | (color & 0x00FFFFFF);
    }
}

