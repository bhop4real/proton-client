package com.bhop4real.proton.client.gui;

import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.config.ConfigManager;
import com.bhop4real.proton.client.gui.config.ClickGuiPreferences;
import com.bhop4real.proton.client.gui.theme.ClickGuiTheme;
import com.bhop4real.proton.client.gui.theme.ClickGuiThemeInterface;
import com.bhop4real.proton.client.gui.util.GuiAnimation;
import com.bhop4real.proton.client.settings.SettingsManager;
import com.bhop4real.proton.client.util.animation.Easing;
import com.bhop4real.proton.client.util.font.FontUtil;
import com.bhop4real.proton.client.util.render.RenderUtil;
import com.bhop4real.proton.client.util.render.rrect.RRectUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI for managing configuration files.
 * Allows users to create, load, delete, and rename configs.
 */
public class GuiConfigManager extends GuiScreen
{
    private static final int GUI_WIDTH = 520;
    private static final int GUI_HEIGHT = 320;
    private static final int HEADER_HEIGHT = 28;
    private static final int ITEM_HEIGHT = 18;
    private static final int PANEL_PADDING = 8;
    private static final int PANEL_RADIUS = 8;
    
    private ConfigManager configManager;
    private SettingsManager settingsManager;
    private ClickGuiThemeInterface activeTheme = ClickGuiTheme.MIDNIGHT;
    private ClickGuiPreferences activePreferences = ClickGuiPreferences.defaults();
    
    private int guiLeft;
    private int guiTop;
    private int configScroll = 0;
    private List<String> configs = new ArrayList<>();
    private String selectedConfig = null;
    private String inputText = "";
    private boolean isCreating = false;
    private boolean isRenaming = false;
    private GuiAnimation fadeAnimation;
    private GuiAnimation zoomAnimation;
    
    @Override
    public void initGui()
    {
        super.initGui();
        
        ProtonClient client = ProtonClient.getInstance();
        if (client != null && client.getSettingsManager() != null)
        {
            settingsManager = client.getSettingsManager();
            configManager = new ConfigManager(settingsManager);
        }
        
        updateGuiPositions();
        refreshConfigList();
        
        fadeAnimation = new GuiAnimation(Easing.EASE_OUT_EXPO, 280L);
        fadeAnimation.setStartValue(0.0);
        fadeAnimation.animateTo(1.0);
        
        zoomAnimation = new GuiAnimation(Easing.EASE_OUT_EXPO, 280L);
        zoomAnimation.setStartValue(0.85);
        zoomAnimation.animateTo(1.0);
        
        Keyboard.enableRepeatEvents(true);
    }
    
    private void updateGuiPositions()
    {
        ScaledResolution sr = new ScaledResolution(mc);
        this.guiLeft = (sr.getScaledWidth() - GUI_WIDTH) / 2;
        this.guiTop = (sr.getScaledHeight() - GUI_HEIGHT) / 2;
        
        if (this.guiLeft < 0) this.guiLeft = 0;
        if (this.guiTop < 0) this.guiTop = 0;
    }
    
    private void refreshConfigList()
    {
        if (configManager != null)
        {
            configs = configManager.listConfigs();
        }
    }
    
    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            if (isCreating || isRenaming)
            {
                isCreating = false;
                isRenaming = false;
                inputText = "";
                selectedConfig = null;
            }
            else
            {
                this.mc.displayGuiScreen(null);
            }
            return;
        }
        
        if (isCreating || isRenaming)
        {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)
            {
                if (isCreating)
                {
                    if (!inputText.trim().isEmpty() && configManager != null)
                    {
                        if (configManager.saveConfig(inputText.trim()))
                        {
                            refreshConfigList();
                            isCreating = false;
                            inputText = "";
                        }
                    }
                }
                else if (isRenaming && selectedConfig != null)
                {
                    if (!inputText.trim().isEmpty() && configManager != null)
                    {
                        if (configManager.renameConfig(selectedConfig, inputText.trim()))
                        {
                            refreshConfigList();
                            isRenaming = false;
                            inputText = "";
                            selectedConfig = null;
                        }
                    }
                }
                return;
            }
            else if (keyCode == Keyboard.KEY_BACK)
            {
                if (inputText.length() > 0)
                {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
                return;
            }
            else if (typedChar >= 32 && typedChar < 127)
            {
                inputText += typedChar;
                return;
            }
        }
        
        super.keyTyped(typedChar, keyCode);
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        int contentLeft = guiLeft + PANEL_PADDING;
        int contentRight = guiLeft + GUI_WIDTH - PANEL_PADDING;
        int contentTop = guiTop + HEADER_HEIGHT + PANEL_PADDING;
        int contentBottom = guiTop + GUI_HEIGHT - PANEL_PADDING;
        
        // Button area at bottom
        int buttonY = contentBottom - 30;
        int buttonHeight = 20;
        int buttonWidth = 100;
        int buttonSpacing = 10;
        
        int createButtonX = contentLeft;
        int loadButtonX = createButtonX + buttonWidth + buttonSpacing;
        int deleteButtonX = loadButtonX + buttonWidth + buttonSpacing;
        int renameButtonX = deleteButtonX + buttonWidth + buttonSpacing;
        
        if (mouseY >= buttonY && mouseY <= buttonY + buttonHeight)
        {
            if (mouseButton == 0)
            {
                if (mouseX >= createButtonX && mouseX <= createButtonX + buttonWidth)
                {
                    // Create new config
                    isCreating = true;
                    isRenaming = false;
                    inputText = "";
                    selectedConfig = null;
                    return;
                }
                else if (mouseX >= loadButtonX && mouseX <= loadButtonX + buttonWidth && selectedConfig != null)
                {
                    // Load config
                    if (configManager != null)
                    {
                        if (configManager.loadConfig(selectedConfig))
                        {
                            mc.displayGuiScreen(null);
                        }
                    }
                    return;
                }
                else if (mouseX >= deleteButtonX && mouseX <= deleteButtonX + buttonWidth && selectedConfig != null)
                {
                    // Delete config
                    if (configManager != null)
                    {
                        if (configManager.deleteConfig(selectedConfig))
                        {
                            refreshConfigList();
                            selectedConfig = null;
                        }
                    }
                    return;
                }
                else if (mouseX >= renameButtonX && mouseX <= renameButtonX + buttonWidth && selectedConfig != null)
                {
                    // Rename config
                    isRenaming = true;
                    isCreating = false;
                    inputText = selectedConfig;
                    return;
                }
            }
        }
        
        // Config list area
        if (mouseX >= contentLeft && mouseX <= contentRight &&
            mouseY >= contentTop && mouseY <= buttonY - 10)
        {
            int configY = contentTop - configScroll;
            for (String config : configs)
            {
                int rowTop = configY;
                int rowBottom = configY + ITEM_HEIGHT;
                
                if (mouseY >= rowTop && mouseY < rowBottom)
                {
                    if (mouseButton == 0)
                    {
                        selectedConfig = config;
                    }
                    return;
                }
                
                configY += ITEM_HEIGHT + 2;
            }
        }
        
        // Handle mouse wheel scrolling
        int mouseWheel = Mouse.getDWheel();
        if (mouseWheel != 0 && mouseX >= contentLeft && mouseX <= contentRight &&
            mouseY >= contentTop && mouseY <= buttonY - 10)
        {
            int scrollDelta = mouseWheel > 0 ? -12 : 12;
            configScroll = Math.max(0, configScroll + scrollDelta);
        }
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        updateGuiPositions();
        
        // Update animations
        if (fadeAnimation != null)
        {
            fadeAnimation.animateTo(1.0);
        }
        if (zoomAnimation != null)
        {
            zoomAnimation.animateTo(1.0);
        }
        
        // Draw background overlay
        float fadeProgress = fadeAnimation != null ? (float) fadeAnimation.getValue() : 1.0F;
        int overlayAlpha = activePreferences.backgroundBrightness();
        int finalAlpha = (int) (overlayAlpha * fadeProgress);
        RenderUtil.drawRect(0, 0, this.width, this.height, 
                applyAlpha(activeTheme.overlay(), finalAlpha));
        
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        
        // Apply zoom animation
        float zoom = zoomAnimation != null ? (float) zoomAnimation.getValue() : 1.0F;
        int centerX = guiLeft + GUI_WIDTH / 2;
        int centerY = guiTop + GUI_HEIGHT / 2;
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(zoom, zoom, 1.0F);
        GlStateManager.translate(-centerX, -centerY, 0);
        
        // Draw main container
        drawMainContainer(mouseX, mouseY);
        
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private void drawMainContainer(int mouseX, int mouseY)
    {
        // Main container
        Color containerColor = new Color(activeTheme.container(), true);
        Color frameOutlineColor = new Color(applyAlpha(activeTheme.outline(), 160), true);
        RRectUtils.drawRoundOutline(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, PANEL_RADIUS,
                1.5F, containerColor, frameOutlineColor);
        
        // Header
        drawHeader();
        
        // Content area
        drawContent(mouseX, mouseY);
        
        // Buttons
        drawButtons(mouseX, mouseY);
    }
    
    private void drawHeader()
    {
        int headerLeft = guiLeft + PANEL_PADDING;
        int headerRight = guiLeft + GUI_WIDTH - PANEL_PADDING;
        int headerTop = guiTop + PANEL_PADDING / 2;
        int headerHeight = HEADER_HEIGHT - PANEL_PADDING / 2;
        
        Color headerColor = new Color(activeTheme.header(), true);
        Color outlineColor = new Color(applyAlpha(activeTheme.outline(), 120), true);
        RRectUtils.drawRoundOutline(headerLeft, headerTop, headerRight - headerLeft, headerHeight,
                PANEL_RADIUS, 1.0F, headerColor, outlineColor);
        
        String title = "Config Manager";
        float titleX = headerLeft + 12;
        float titleY = headerTop + 5;
        FontUtil.drawString(title, titleX, titleY, activeTheme.textPrimary(), true);
    }
    
    private void drawContent(int mouseX, int mouseY)
    {
        int contentLeft = guiLeft + PANEL_PADDING;
        int contentRight = guiLeft + GUI_WIDTH - PANEL_PADDING;
        int contentTop = guiTop + HEADER_HEIGHT + PANEL_PADDING;
        int contentBottom = guiTop + GUI_HEIGHT - PANEL_PADDING - 40;
        
        // Draw config list
        int configY = contentTop - configScroll;
        for (String config : configs)
        {
            int rowTop = configY;
            int rowBottom = configY + ITEM_HEIGHT;
            
            if (rowBottom >= contentTop && rowTop <= contentBottom)
            {
                boolean selected = config.equals(selectedConfig);
                boolean hovered = mouseX >= contentLeft && mouseX <= contentRight &&
                        mouseY >= rowTop && mouseY < rowBottom;
                
                int bgColor = selected ? applyAlpha(activeTheme.categorySelected(), 220) :
                        hovered ? applyAlpha(activeTheme.moduleHover(), 160) :
                        applyAlpha(activeTheme.settingBackground(), 200);
                
                RenderUtil.drawRect(contentLeft, rowTop, contentRight, rowBottom, bgColor);
                
                if (selected)
                {
                    RenderUtil.drawRect(contentLeft, rowTop, contentLeft + 3, rowBottom,
                            activeTheme.accent());
                }
                
                float textX = contentLeft + 6;
                float textY = rowTop + 4;
                FontUtil.drawString(config, textX, textY,
                        selected ? activeTheme.textPrimary() : activeTheme.textSecondary(), true);
            }
            
            configY += ITEM_HEIGHT + 2;
        }
        
        // Draw input field if creating or renaming
        if (isCreating || isRenaming)
        {
            int inputY = contentBottom + 5;
            int inputHeight = 20;
            String prompt = isCreating ? "Enter config name:" : "Rename to:";
            
            FontUtil.drawString(prompt, contentLeft, inputY - 15, activeTheme.textSecondary(), true);
            
            RenderUtil.drawRect(contentLeft, inputY, contentRight, inputY + inputHeight,
                    applyAlpha(activeTheme.settingBackground(), 255));
            RenderUtil.drawBorderedRect(contentLeft, inputY, contentRight - contentLeft, inputHeight,
                    1.0F, applyAlpha(activeTheme.outline(), 200), 0);
            
            String displayText = inputText + (System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
            FontUtil.drawString(displayText, contentLeft + 4, inputY + 5, activeTheme.textPrimary(), true);
        }
    }
    
    private void drawButtons(int mouseX, int mouseY)
    {
        int contentLeft = guiLeft + PANEL_PADDING;
        int contentBottom = guiTop + GUI_HEIGHT - PANEL_PADDING;
        
        int buttonY = contentBottom - 30;
        int buttonHeight = 20;
        int buttonWidth = 100;
        int buttonSpacing = 10;
        
        int createButtonX = contentLeft;
        int loadButtonX = createButtonX + buttonWidth + buttonSpacing;
        int deleteButtonX = loadButtonX + buttonWidth + buttonSpacing;
        int renameButtonX = deleteButtonX + buttonWidth + buttonSpacing;
        
        drawButton("Create", createButtonX, buttonY, buttonWidth, buttonHeight, 
                mouseX, mouseY, true);
        drawButton("Load", loadButtonX, buttonY, buttonWidth, buttonHeight,
                mouseX, mouseY, selectedConfig != null);
        drawButton("Delete", deleteButtonX, buttonY, buttonWidth, buttonHeight,
                mouseX, mouseY, selectedConfig != null);
        drawButton("Rename", renameButtonX, buttonY, buttonWidth, buttonHeight,
                mouseX, mouseY, selectedConfig != null);
    }
    
    private void drawButton(String text, int x, int y, int width, int height,
                           int mouseX, int mouseY, boolean enabled)
    {
        boolean hovered = enabled && mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
        
        int bgColor = enabled ? 
                (hovered ? applyAlpha(activeTheme.moduleHover(), 200) : 
                        applyAlpha(activeTheme.settingBackground(), 255)) :
                applyAlpha(activeTheme.settingBackground(), 150);
        
        RenderUtil.drawRect(x, y, x + width, y + height, bgColor);
        RenderUtil.drawBorderedRect(x, y, width, height, 1.0F,
                applyAlpha(activeTheme.outline(), enabled ? 200 : 100), 0);
        
        int textColor = enabled ? activeTheme.textPrimary() : activeTheme.textSecondary();
        int textWidth = FontUtil.getStringWidth(text);
        float textX = x + (width - textWidth) / 2.0F;
        float textY = y + (height - FontUtil.getFontHeight()) / 2.0F + 1;
        FontUtil.drawString(text, textX, textY, textColor, true);
    }
    
    private static int applyAlpha(int color, int alpha)
    {
        return (alpha & 0xFF) << 24 | (color & 0x00FFFFFF);
    }
    
    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
}

