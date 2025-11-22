package com.bhop4real.proton.client.gui;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.gui.config.ClickGuiPreferences;
import com.bhop4real.proton.client.gui.theme.ClickGuiTheme;
import com.bhop4real.proton.client.gui.theme.ClickGuiThemeInterface;
import com.bhop4real.proton.client.gui.util.GuiAnimation;
import com.bhop4real.proton.client.gui.util.GuiComponent;
import com.bhop4real.proton.client.util.animation.Easing;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.ModuleManager;
import com.bhop4real.proton.client.module.modules.ClickGUI;
import com.bhop4real.proton.client.module.settings.*;
import com.bhop4real.proton.client.settings.SettingsManager;
import com.bhop4real.proton.client.util.font.FontUtil;
import com.bhop4real.proton.client.util.render.RenderUtil;
import com.bhop4real.proton.client.util.render.RoundedRectUtil;
import com.bhop4real.proton.client.util.render.blur.GaussianBlur;
import com.bhop4real.proton.client.util.render.blur.KawaseBlur;
import com.bhop4real.proton.client.util.render.rrect.RRectUtils;
import com.bhop4real.proton.client.gui.util.PatternFilePicker;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact, animated Click GUI with modern design
 */
public class GuiClickGUI extends GuiScreen
{
    private static boolean resourcesPreloaded;
    
    // Core components
    private ModuleManager moduleManager;
    private Module.Category selectedCategory = Module.Category.COMBAT;
    
    // Animations
    private GuiAnimation fadeAnimation;
    private GuiAnimation zoomAnimation;
    private Map<Module, GuiAnimation> expandAnimations = new HashMap<>();
    
    // GUI dimensions (compact)
    private static final int GUI_WIDTH = 520;
    private static final int GUI_HEIGHT = 320;
    private static final int CATEGORY_WIDTH = 130;
    private static final int HEADER_HEIGHT = 28;
    private static final int ITEM_HEIGHT = 18;
    private static final int SETTING_ITEM_HEIGHT = 16;
    private static final int PANEL_PADDING = 8;
    private static final int PANEL_RADIUS = 8;
    
    // State
    private int guiLeft;
    private int guiTop;
    private int categoryScroll = 0;
    private int moduleScroll = 0;
    private Map<Module, Integer> expandedModules = new HashMap<>();
    
    // Settings interaction
    private IntSetting draggingSetting = null;
    private com.bhop4real.proton.client.module.settings.DoubleSetting draggingDoubleSetting = null;
    private Module draggingModule;
    private EnumSetting dropdownSetting = null;
    private Module dropdownModule = null; // Track which module the dropdown belongs to
    private Module bindingModule;
    private int dropdownSettingY = 0; // Store dropdown Y position for rendering after scissor
    
    // Theme and preferences
    private ClickGuiThemeInterface activeTheme = ClickGuiTheme.MIDNIGHT;
    private ClickGuiPreferences activePreferences = ClickGuiPreferences.defaults();
    
    // Color picker
    private com.bhop4real.proton.client.module.settings.ColorSetting activeColorSetting;
    private Module colorPickerModule;
    private boolean colorPickerOpen;
    private int colorPickerX = 0;
    private int colorPickerY = 0;
    private boolean draggingRed = false;
    private boolean draggingGreen = false;
    private boolean draggingBlue = false;
    private boolean draggingAlpha = false;
    
    @Override
    public void initGui()
    {
        super.initGui();
        
        this.moduleManager = ProtonClient.getInstance().getModuleManager();
        updateGuiPositions();
        
        // Initialize fade-in and zoom animations (will be updated with preferences)
        this.fadeAnimation = new GuiAnimation(Easing.EASE_OUT_EXPO, 280L);
        this.fadeAnimation.setStartValue(0.0);
        this.fadeAnimation.animateTo(1.0);
        
        this.zoomAnimation = new GuiAnimation(Easing.EASE_OUT_EXPO, 280L);
        this.zoomAnimation.setStartValue(0.85);
        this.zoomAnimation.animateTo(1.0);
        
        Keyboard.enableRepeatEvents(true);
    }
    
    private void updateGuiPositions()
    {
        int screenWidth = this.width;
        int screenHeight = this.height;
        
        if (screenWidth <= 0 || screenHeight <= 0)
        {
            ScaledResolution sr = new ScaledResolution(mc);
            screenWidth = sr.getScaledWidth();
            screenHeight = sr.getScaledHeight();
        }
        
        this.guiLeft = (screenWidth - GUI_WIDTH) / 2;
        this.guiTop = (screenHeight - GUI_HEIGHT) / 2;
        
        if (this.guiLeft < 0) this.guiLeft = 0;
        if (this.guiTop < 0) this.guiTop = 0;
    }
    
    public static void preloadResources()
    {
        if (resourcesPreloaded)
        {
            return;
        }
        
        resourcesPreloaded = true;
        try
        {
            FontUtil.getFontHeight();
            GaussianBlur.preload();
            KawaseBlur.preload();
        }
        catch (Exception exception)
        {
            Proton.logger.warn("Failed to preload ClickGUI resources", exception);
        }
    }
    
    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        draggingSetting = null;
        draggingModule = null;
        dropdownSetting = null;
        dropdownModule = null;
        bindingModule = null;
        expandedModules.clear();
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (bindingModule != null)
        {
            // Allow ESC to cancel binding
            if (keyCode == Keyboard.KEY_ESCAPE)
            {
                bindingModule = null;
                return;
            }
            
            // Use the keyCode parameter directly - it should match what onKeyInput receives
            // Don't bind if it's 0 or invalid
            if (keyCode != 0 && keyCode != Keyboard.KEY_NONE)
            {
                // setKeyBind() already calls updateModuleKeyBind internally
                bindingModule.setKeyBind(keyCode);
            }
            bindingModule = null;
            return;
        }
        
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            if (dropdownSetting != null)
            {
                dropdownSetting = null;
                dropdownModule = null;
            }
            else if (colorPickerOpen)
            {
                colorPickerOpen = false;
                activeColorSetting = null;
                colorPickerModule = null;
            }
            else
            {
                this.mc.displayGuiScreen(null);
            }
            return;
        }
        
        super.keyTyped(typedChar, keyCode);
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Handle category selection
        int categoryPanelLeft = guiLeft + PANEL_PADDING;
        int categoryPanelRight = guiLeft + CATEGORY_WIDTH - PANEL_PADDING / 2;
        int categoryInnerTop = categoryPanelTopBound() + PANEL_PADDING;
        int categoryInnerBottom = categoryPanelBottomBound() - PANEL_PADDING / 2;
        
        if (mouseX >= categoryPanelLeft && mouseX <= categoryPanelRight &&
            mouseY >= categoryInnerTop && mouseY <= categoryInnerBottom)
        {
            Module.Category[] categories = Module.Category.values();
            int categoryY = categoryPanelTopBound() + PANEL_PADDING + categoryScroll;
            
            for (Module.Category category : categories)
            {
                int rowTop = categoryY;
                int rowBottom = rowTop + ITEM_HEIGHT;
                if (mouseY >= rowTop && mouseY < rowBottom)
                {
                    if (selectedCategory != category)
                    {
                        selectedCategory = category;
                        moduleScroll = 0;
                        dropdownSetting = null;
                        dropdownModule = null;
                        
                        // Open config manager if CONFIG category is selected
                        if (category == Module.Category.CONFIG)
                        {
                            this.mc.displayGuiScreen(new GuiConfigManager());
                            return;
                        }
                    }
                    return;
                }
                categoryY += ITEM_HEIGHT + 2;
            }
            return;
        }
        
        // Handle module interaction
        int modulePanelLeft = moduleAreaLeft();
        int modulePanelRight = moduleAreaRight();
        int modulePanelTop = modulePanelTopBound();
        int modulePanelBottom = modulePanelBottomBound();
        
        if (mouseX >= modulePanelLeft && mouseX <= modulePanelRight &&
            mouseY >= modulePanelTop && mouseY <= modulePanelBottom)
        {
            List<Module> modules = moduleManager.getModulesByCategory(selectedCategory);
            int currentY = moduleContentTop() - moduleScroll;
            
            for (Module module : modules)
            {
                int moduleY = currentY;
                
                // Check if clicking on module row
                if (mouseY >= moduleY && mouseY < moduleY + ITEM_HEIGHT)
                {
                    if (mouseButton == 0) // Left click - toggle module
                    {
                        module.toggle();
                    }
                    else if (mouseButton == 1) // Right click - toggle settings expansion
                    {
                        // Always allow expansion to show keybind option
                        if (expandedModules.containsKey(module))
                        {
                            expandedModules.remove(module);
                        }
                        else
                        {
                            expandedModules.put(module, 0);
                            dropdownSetting = null;
                            dropdownModule = null;
                        }
                    }
                    else if (mouseButton == 2) // Middle click - start keybind binding
                    {
                        bindingModule = module;
                    }
                    return;
                }
                
                currentY += ITEM_HEIGHT;
                
                    // Check if clicking on expanded settings
                    if (expandedModules.containsKey(module))
                    {
                        List<Setting> settings = module.getSettings();
                        int settingsY = currentY;
                        int settingsScroll = expandedModules.get(module);
                        
                        int visibleIndex = 0;
                        
                        // Check keybind entry first
                        int keybindY = settingsY - settingsScroll + visibleIndex * SETTING_ITEM_HEIGHT;
                        if (mouseY >= keybindY && mouseY < keybindY + SETTING_ITEM_HEIGHT)
                        {
                            if (mouseButton == 0) // Left click - start binding
                            {
                                bindingModule = module;
                            }
                            return;
                        }
                        visibleIndex++;
                        
                        for (Setting setting : settings)
                        {
                            if (!setting.isVisible()) continue;
                            int settingY = settingsY - settingsScroll + visibleIndex * SETTING_ITEM_HEIGHT;
                            
                            if (mouseY >= settingY && mouseY < settingY + SETTING_ITEM_HEIGHT)
                            {
                                handleSettingClick(module, setting, mouseX, mouseY, mouseButton, settingY);
                                return;
                            }
                            
                            // Check dropdown if open
                            if (setting instanceof EnumSetting && dropdownSetting == setting)
                            {
                                EnumSetting enumSetting = (EnumSetting) setting;
                                int dropdownY = settingY + SETTING_ITEM_HEIGHT;
                                List<String> modes = enumSetting.getModes();
                                for (int k = 0; k < modes.size(); k++)
                                {
                                    if (mouseY >= dropdownY + k * SETTING_ITEM_HEIGHT &&
                                        mouseY < dropdownY + (k + 1) * SETTING_ITEM_HEIGHT)
                                    {
                                        enumSetting.setValue(modes.get(k));
                                        persistSettingChange(module);
                                        dropdownSetting = null;
                                        dropdownModule = null;
                                        return;
                                    }
                                }
                            }
                            visibleIndex++;
                        }
                        
                        currentY += calculateSettingsHeight(module, settings, false);
                    }
            }
        }
        else
        {
            // Check if click is within dropdown bounds before closing
            if (dropdownSetting != null && dropdownSetting instanceof EnumSetting)
            {
                EnumSetting enumSetting = (EnumSetting) dropdownSetting;
                int dropdownLeft = moduleContentLeft();
                int dropdownRight = moduleContentRight();
                int dropdownTop = dropdownSettingY + SETTING_ITEM_HEIGHT;
                List<String> modes = enumSetting.getModes();
                int dropdownBottom = dropdownTop + modes.size() * SETTING_ITEM_HEIGHT;
                
                // If click is within dropdown bounds, don't close it
                if (mouseX >= dropdownLeft && mouseX <= dropdownRight &&
                    mouseY >= dropdownTop && mouseY <= dropdownBottom)
                {
                    // Handle dropdown option click
                    for (int k = 0; k < modes.size(); k++)
                    {
                        if (mouseY >= dropdownTop + k * SETTING_ITEM_HEIGHT &&
                            mouseY < dropdownTop + (k + 1) * SETTING_ITEM_HEIGHT)
                        {
                            enumSetting.setValue(modes.get(k));
                            persistSettingChange(dropdownModule);
                            dropdownSetting = null;
                            dropdownModule = null;
                            return;
                        }
                    }
                    return; // Click is in dropdown area but not on an option
                }
            }
            
            // Check if clicking on color picker
            if (colorPickerOpen)
            {
                int pickerLeft = colorPickerX;
                int pickerRight = colorPickerX + 220;
                int pickerTop = colorPickerY;
                int pickerBottom = colorPickerY + 180;
                
                if (mouseX >= pickerLeft && mouseX <= pickerRight && mouseY >= pickerTop && mouseY <= pickerBottom)
                {
                    // Handle color picker interaction
                    handleColorPickerClick(mouseX, mouseY, mouseButton);
                    return;
                }
                else
                {
                    // Click outside - close color picker
                    colorPickerOpen = false;
                    activeColorSetting = null;
                    colorPickerModule = null;
                }
            }
            
            // Click outside - close dropdown
            dropdownSetting = null;
            dropdownModule = null;
        }
    }
    
    private void handleColorPickerClick(int mouseX, int mouseY, int mouseButton)
    {
        if (activeColorSetting == null || mouseButton != 0) return;
        
        int pickerLeft = colorPickerX;
        int pickerTop = colorPickerY;
        
        // Calculate slider positions (matching drawColorPicker layout)
        int previewHeight = 40;
        int sliderStartY = pickerTop + 10 + previewHeight + 15;
        int sliderHeight = 8;
        int sliderSpacing = 22;
        int labelWidth = 25; // Space for label
        int valueWidth = 35; // Space for value text
        int sliderX = pickerLeft + 10 + labelWidth; // Start after label
        int sliderWidth = 180 - labelWidth - valueWidth; // Actual slider width
        
        // Red slider
        int redY = sliderStartY + (18 - sliderHeight) / 2; // Center vertically in row
        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= redY && mouseY <= redY + sliderHeight)
        {
            draggingRed = true;
            updateColorFromRed(mouseX - sliderX, sliderWidth);
            return;
        }
        
        // Green slider
        int greenY = sliderStartY + sliderSpacing + (18 - sliderHeight) / 2;
        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= greenY && mouseY <= greenY + sliderHeight)
        {
            draggingGreen = true;
            updateColorFromGreen(mouseX - sliderX, sliderWidth);
            return;
        }
        
        // Blue slider
        int blueY = sliderStartY + sliderSpacing * 2 + (18 - sliderHeight) / 2;
        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= blueY && mouseY <= blueY + sliderHeight)
        {
            draggingBlue = true;
            updateColorFromBlue(mouseX - sliderX, sliderWidth);
            return;
        }
        
        // Alpha slider
        int alphaY = sliderStartY + sliderSpacing * 3 + (18 - sliderHeight) / 2;
        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= alphaY && mouseY <= alphaY + sliderHeight)
        {
            draggingAlpha = true;
            updateColorFromAlpha(mouseX - sliderX, sliderWidth);
            return;
        }
    }
    
    private void updateColorFromRed(int x, int width)
    {
        if (activeColorSetting == null) return;

        int red = (int)((float)x / (float)width * 255.0F);
        red = Math.max(0, Math.min(255, red));

        int currentColor = activeColorSetting.getColor();
        int alpha = (currentColor >> 24) & 0xFF;
        int green = (currentColor >> 8) & 0xFF;
        int blue = currentColor & 0xFF;

        int newColor = (alpha << 24) | (red << 16) | (green << 8) | blue;
        activeColorSetting.setColor(newColor);
        // Save immediately when color changes to prevent data loss
        persistSettingChange(colorPickerModule);
    }
    
    private void updateColorFromGreen(int x, int width)
    {
        if (activeColorSetting == null) return;

        int green = (int)((float)x / (float)width * 255.0F);
        green = Math.max(0, Math.min(255, green));

        int currentColor = activeColorSetting.getColor();
        int alpha = (currentColor >> 24) & 0xFF;
        int red = (currentColor >> 16) & 0xFF;
        int blue = currentColor & 0xFF;

        int newColor = (alpha << 24) | (red << 16) | (green << 8) | blue;
        activeColorSetting.setColor(newColor);
        // Save immediately when color changes to prevent data loss
        persistSettingChange(colorPickerModule);
    }
    
    private void updateColorFromBlue(int x, int width)
    {
        if (activeColorSetting == null) return;

        int blue = (int)((float)x / (float)width * 255.0F);
        blue = Math.max(0, Math.min(255, blue));

        int currentColor = activeColorSetting.getColor();
        int alpha = (currentColor >> 24) & 0xFF;
        int red = (currentColor >> 16) & 0xFF;
        int green = (currentColor >> 8) & 0xFF;

        int newColor = (alpha << 24) | (red << 16) | (green << 8) | blue;
        activeColorSetting.setColor(newColor);
        // Save immediately when color changes to prevent data loss
        persistSettingChange(colorPickerModule);
    }
    
    private void updateColorFromAlpha(int x, int width)
    {
        if (activeColorSetting == null) return;

        int alpha = (int)((float)x / (float)width * 255.0F);
        alpha = Math.max(0, Math.min(255, alpha));

        int currentColor = activeColorSetting.getColor();
        int rgb = currentColor & 0x00FFFFFF;
        int newColor = (alpha << 24) | rgb;
        activeColorSetting.setColor(newColor);
        // Save immediately when color changes to prevent data loss
        persistSettingChange(colorPickerModule);
    }
    
    private void handleSettingClick(Module module, Setting setting, int mouseX, int mouseY, int mouseButton, int settingY)
    {
        if (!setting.isVisible()) return;
        if (setting instanceof SeparatorSetting) return;
        
        if (setting instanceof BooleanSetting)
        {
            BooleanSetting boolSetting = (BooleanSetting) setting;
            boolSetting.toggle();
            persistSettingChange(module);
        }
        else if (setting instanceof IntSetting)
        {
            IntSetting intSetting = (IntSetting) setting;
            int sliderX = moduleContentRight() - 100;
            int sliderHeight = 8;
            int sliderY = settingY + (SETTING_ITEM_HEIGHT - sliderHeight) / 2;
            
            if (mouseX >= sliderX && mouseX <= sliderX + 100 &&
                mouseY >= sliderY && mouseY <= sliderY + sliderHeight)
            {
                draggingSetting = intSetting;
                draggingModule = module;
                updateSliderValue(intSetting, mouseX, sliderX);
            }
        }
        else if (setting instanceof com.bhop4real.proton.client.module.settings.DoubleSetting)
        {
            com.bhop4real.proton.client.module.settings.DoubleSetting doubleSetting =
                    (com.bhop4real.proton.client.module.settings.DoubleSetting) setting;
            int sliderX = moduleContentRight() - 100;
            int sliderHeight = 8;
            int sliderY = settingY + (SETTING_ITEM_HEIGHT - sliderHeight) / 2;
            
            if (mouseX >= sliderX && mouseX <= sliderX + 100 &&
                mouseY >= sliderY && mouseY <= sliderY + sliderHeight)
            {
                draggingDoubleSetting = doubleSetting;
                draggingModule = module;
                updateSliderValue(doubleSetting, mouseX, sliderX);
            }
        }
        else if (setting instanceof EnumSetting)
        {
            EnumSetting enumSetting = (EnumSetting) setting;
            String valueText = enumSetting.getValue();
            int valueWidth = FontUtil.getStringWidth(valueText);
            int pillPadding = 6;
            int pillLeft = moduleContentRight() - valueWidth - pillPadding * 2;
            int pillTop = settingY + 1;
            int pillBottom = settingY + SETTING_ITEM_HEIGHT - 1;
            
            if (mouseX >= pillLeft && mouseX <= moduleContentRight() && mouseY >= pillTop && mouseY <= pillBottom)
            {
                if (dropdownSetting == enumSetting)
                {
                    dropdownSetting = null;
                    dropdownModule = null;
                }
                else
                {
                    dropdownSetting = enumSetting;
                    dropdownModule = module;
                }
            }
            else
            {
                enumSetting.cycle();
                persistSettingChange(module);
            }
        }
        else if (setting instanceof ColorSetting)
        {
            ColorSetting colorSetting = (ColorSetting) setting;
            int swatchX = moduleContentRight() - 24;
            int swatchY = settingY + (SETTING_ITEM_HEIGHT - 10) / 2;
            
            // Check if clicking on color swatch
            if (mouseX >= swatchX && mouseX <= swatchX + 18 && mouseY >= swatchY && mouseY <= swatchY + 10)
            {
                if (colorPickerOpen && activeColorSetting == colorSetting)
                {
                    // Close color picker
                    colorPickerOpen = false;
                    activeColorSetting = null;
                    colorPickerModule = null;
                }
                else
                {
                    // Open color picker
                    colorPickerOpen = true;
                    activeColorSetting = colorSetting;
                    colorPickerModule = module;
                    dropdownSetting = null;
                    dropdownModule = null;
                    
                    // Position color picker near the swatch
                    colorPickerX = swatchX - 220;
                    colorPickerY = settingY;
                    
                    // Keep within screen bounds
                    if (colorPickerX < 0) colorPickerX = swatchX + 25;
                    if (colorPickerY + 180 > mc.displayHeight) colorPickerY = mc.displayHeight - 180;
                }
            }
        }
        else if (setting instanceof StringSetting)
        {
            StringSetting stringSetting = (StringSetting) setting;
            // Check if this is the pattern file setting
            if (stringSetting.getName().equalsIgnoreCase("Pattern File"))
            {
                // Open file picker for pattern file selection
                PatternFilePicker.pickPatternFile(selectedPath -> {
                    if (selectedPath != null)
                    {
                        stringSetting.setValue(selectedPath);
                        persistSettingChange(module);
                    }
                });
            }
        }
    }
    
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        
        if ((draggingSetting != null || draggingDoubleSetting != null) && clickedMouseButton == 0)
        {
            int sliderX = moduleContentRight() - 100;
            if (draggingSetting != null)
            {
                updateSliderValue(draggingSetting, mouseX, sliderX);
            }
            else if (draggingDoubleSetting != null)
            {
                updateSliderValue(draggingDoubleSetting, mouseX, sliderX);
            }
        }
        
        if (colorPickerOpen && clickedMouseButton == 0)
        {
            int pickerLeft = colorPickerX;
            int labelWidth = 25;
            int valueWidth = 35;
            int sliderX = pickerLeft + 10 + labelWidth;
            int sliderWidth = 180 - labelWidth - valueWidth;
            
            if (draggingRed)
            {
                updateColorFromRed(mouseX - sliderX, sliderWidth);
            }
            else if (draggingGreen)
            {
                updateColorFromGreen(mouseX - sliderX, sliderWidth);
            }
            else if (draggingBlue)
            {
                updateColorFromBlue(mouseX - sliderX, sliderWidth);
            }
            else if (draggingAlpha)
            {
                updateColorFromAlpha(mouseX - sliderX, sliderWidth);
            }
        }
    }
    
    private void updateSliderValue(IntSetting setting, int mouseX, int sliderX)
    {
        float ratio = (float)(mouseX - sliderX) / 100.0F;
        ratio = Math.max(0.0F, Math.min(1.0F, ratio));

        int range = setting.getMax() - setting.getMin();
        int value = setting.getMin() + (int)(range * ratio);
        if (value != setting.getValue())
        {
            setting.setValue(value);
            // Save immediately when value changes to prevent data loss
            persistSettingChange(draggingModule);
        }
    }
    
    private void updateSliderValue(com.bhop4real.proton.client.module.settings.DoubleSetting setting, int mouseX, int sliderX)
    {
        float ratio = (float)(mouseX - sliderX) / 100.0F;
        ratio = Math.max(0.0F, Math.min(1.0F, ratio));
        
        double range = setting.getMax() - setting.getMin();
        double value = setting.getMin() + (range * ratio);
        setting.setValue(value);
        persistSettingChange(draggingModule);
    }
    
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
        if (draggingModule != null && (draggingSetting != null || draggingDoubleSetting != null))
        {
            persistSettingChange(draggingModule);
        }
        draggingSetting = null;
        draggingDoubleSetting = null;
        draggingModule = null;
        
        if (colorPickerOpen && (draggingRed || draggingGreen || draggingBlue || draggingAlpha))
        {
            if (colorPickerModule != null)
            {
                persistSettingChange(colorPickerModule);
            }
        }
        draggingRed = false;
        draggingGreen = false;
        draggingBlue = false;
        draggingAlpha = false;
    }
    
    private void persistSettingChange(Module module)
    {
        if (module == null) return;
        
        ProtonClient client = ProtonClient.getInstance();
        if (client == null) return;
        
        SettingsManager settingsManager = client.getSettingsManager();
        if (settingsManager != null)
        {
            settingsManager.saveModuleSettings(module);
        }
    }
    
    private ClickGuiPreferences resolvePreferences()
    {
        if (moduleManager == null)
        {
            return ClickGuiPreferences.defaults();
        }
        
        Module moduleRef = moduleManager.getModuleByName("ClickGUI");
        if (moduleRef instanceof ClickGUI)
        {
            return ((ClickGUI) moduleRef).snapshotPreferences();
        }
        
        return ClickGuiPreferences.defaults();
    }
    
    private static int applyAlpha(int color, int alpha)
    {
        return (alpha & 0xFF) << 24 | (color & 0x00FFFFFF);
    }
    
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        preloadResources();
        updateGuiPositions();
        
        // Resolve preferences first (needed for animation durations)
        activePreferences = resolvePreferences();
        activeTheme = activePreferences.theme();
        
        // Update animation durations from preferences
        if (fadeAnimation != null)
        {
            fadeAnimation.setDuration(activePreferences.openAnimationDuration());
        }
        if (zoomAnimation != null)
        {
            zoomAnimation.setDuration(activePreferences.openAnimationDuration());
        }
        
        // Update animations
        if (fadeAnimation != null)
        {
            fadeAnimation.animateTo(1.0);
        }
        if (zoomAnimation != null)
        {
            zoomAnimation.animateTo(1.0);
        }
        
        // Update expand/collapse animations
        for (Module module : moduleManager.getModulesByCategory(selectedCategory))
        {
            boolean isExpanded = expandedModules.containsKey(module);
            GuiAnimation anim = expandAnimations.get(module);
            
            if (isExpanded)
            {
                // Expanding - animate to 1.0
                if (anim == null)
                {
                    // Create new animation starting from 0.0 (collapsed state)
                    anim = GuiAnimation.createFade(activePreferences.expandAnimationDuration());
                    // Initialize animation value to 0.0 (collapsed)
                    // The animateTo method will handle setting up the animation properly
                    expandAnimations.put(module, anim);
                }
                else
                {
                    // Update duration if preferences changed
                    anim.setDuration(activePreferences.expandAnimationDuration());
                }
                // Animate to expanded state (1.0)
                // This will interpolate from current value (0.0 for new, or current for existing) to 1.0
                anim.animateTo(1.0);
            }
            else
            {
                // Collapsing - animate to 0.0
                if (anim != null)
                {
                    // Animation exists - animate to collapsed state
                    // Update duration if preferences changed
                    anim.setDuration(activePreferences.expandAnimationDuration());
                    // Animate to collapsed state (0.0)
                    // This will interpolate from current value to 0.0
                    anim.animateTo(0.0);
                }
                // If anim is null, module was never expanded or animation already finished and removed
                // No need to create animation for modules that were never expanded
            }
        }
        
        // Remove animations for modules that are fully collapsed and not in expandedModules
        expandAnimations.entrySet().removeIf(e -> {
            Module module = e.getKey();
            GuiAnimation anim = e.getValue();
            boolean notExpanded = !expandedModules.containsKey(module);
            boolean fullyCollapsed = anim.getValue() <= 0.001;
            boolean finished = anim.isFinished();
            return notExpanded && fullyCollapsed && finished;
        });
        
        // Apply blur if enabled
        int blurStrength = activePreferences.blurRadius();
        if (activePreferences.blurEnabled() && blurStrength > 0)
        {
            switch (activePreferences.blurMode())
            {
                case KAWASE:
                {
                    KawaseBlur.startBlur();
                    RenderUtil.drawRect(0, 0, this.width, this.height, 0xFFFFFFFF);
                    int iterations = Math.max(1, blurStrength / 4);
                    float offset = 0.75F + blurStrength * 0.125F;
                    KawaseBlur.endBlur(iterations, offset);
                    break;
                }
                case GAUSSIAN:
                default:
                {
                    GaussianBlur.startBlur();
                    RenderUtil.drawRect(0, 0, this.width, this.height, 0xFFFFFFFF);
                    GaussianBlur.endBlur(Math.min(blurStrength, 32), 1.0f);
                    break;
                }
            }
        }
        
        // Draw overlay
        int overlayAlpha = activePreferences.backgroundBrightness();
        float fadeProgress = fadeAnimation != null ? (float) fadeAnimation.getValue() : 1.0F;
        int finalAlpha = (int) (overlayAlpha * fadeProgress);
        RenderUtil.drawRect(0, 0, this.width, this.height, applyAlpha(activeTheme.overlay(), finalAlpha));
        
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        
        // Apply scissor clipping for entire GUI panel AFTER pushAttrib
        // Scissor is in screen coordinates, so apply after pushAttrib to ensure it's not restored
        boolean guiClipEnabled = pushScissor(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
        
        // Apply zoom animation
        float zoom = zoomAnimation != null ? (float) zoomAnimation.getValue() : 1.0F;
        int centerX = guiLeft + GUI_WIDTH / 2;
        int centerY = guiTop + GUI_HEIGHT / 2;
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(zoom, zoom, 1.0F);
        GlStateManager.translate(-centerX, -centerY, 0);
        
        // Re-apply scissor after transformation to ensure it's still active
        // (scissor is in screen coordinates, so it should work, but re-applying ensures it's set)
        if (guiClipEnabled)
        {
            pushScissor(guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT);
        }
        
        // Draw main GUI container
        drawMainContainer(mouseX, mouseY);
        
        // Pop scissor for GUI panel (before popAttrib to avoid state conflicts)
        if (guiClipEnabled)
        {
            popScissor();
        }
        
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
        
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        // Render notifications on top
        NotificationManager.renderOnGui(partialTicks);
    }
    
    private void drawMainContainer(int mouseX, int mouseY)
    {
        int frameLeft = guiLeft;
        int frameTop = guiTop;
        int frameRight = guiLeft + GUI_WIDTH;
        int frameBottom = guiTop + GUI_HEIGHT;
        
        // Main container
        Color containerColor = new Color(activeTheme.container(), true);
        Color frameOutlineColor = new Color(applyAlpha(activeTheme.outline(), 160), true);
        
        RRectUtils.drawRoundOutline(frameLeft, frameTop, GUI_WIDTH, GUI_HEIGHT, PANEL_RADIUS,
                1.5F, containerColor, frameOutlineColor);
        
        // Header
        drawHeader(frameLeft, frameTop, frameRight);
        
        // Category panel
        drawCategoryPanel(frameLeft, frameTop, mouseX, mouseY);
        
        // Module panel
        drawModulePanel(frameLeft, frameTop, frameRight, frameBottom, mouseX, mouseY);
    }
    
    private void drawHeader(int frameLeft, int frameTop, int frameRight)
    {
        int headerLeft = frameLeft + PANEL_PADDING;
        int headerRight = frameRight - PANEL_PADDING;
        int headerTop = frameTop + PANEL_PADDING / 2;
        int headerHeight = HEADER_HEIGHT - PANEL_PADDING / 2;
        
        Color headerColor = new Color(activeTheme.header(), true);
        Color outlineColor = new Color(applyAlpha(activeTheme.outline(), 120), true);
        RRectUtils.drawRoundOutline(headerLeft, headerTop, headerRight - headerLeft, headerHeight,
                PANEL_RADIUS, 1.0F, headerColor, outlineColor);
        
        // Title
        String title = "Proton Client";
        float titleX = headerLeft + 12;
        float titleY = headerTop + 5;
        FontUtil.drawString(title, titleX, titleY, activeTheme.textPrimary(), true);
        
        // Category info
        String headerInfo;
        if (selectedCategory == Module.Category.CONFIG)
        {
            headerInfo = selectedCategory.getName();
        }
        else
        {
            List<Module> modules = moduleManager.getModulesByCategory(selectedCategory);
            headerInfo = selectedCategory.getName() + " • " + modules.size() + " modules";
        }
        int headerInfoWidth = FontUtil.getStringWidth(headerInfo);
        FontUtil.drawString(headerInfo, headerRight - headerInfoWidth - 12, titleY, activeTheme.textSecondary(), true);
    }
    
    // Helper methods for layout calculations
    private int categoryPanelTopBound()
    {
        return guiTop + HEADER_HEIGHT + PANEL_PADDING;
    }
    
    private int categoryPanelBottomBound()
    {
        return guiTop + GUI_HEIGHT - PANEL_PADDING - PANEL_PADDING / 2;
    }
    
    private int moduleAreaLeft()
    {
        return guiLeft + CATEGORY_WIDTH + 10;
    }
    
    private int moduleAreaRight()
    {
        return guiLeft + GUI_WIDTH - PANEL_PADDING;
    }
    
    private int modulePanelTopBound()
    {
        return guiTop + HEADER_HEIGHT + PANEL_PADDING;
    }
    
    private int modulePanelBottomBound()
    {
        return guiTop + GUI_HEIGHT - PANEL_PADDING - PANEL_PADDING / 2;
    }
    
    private int moduleContentLeft()
    {
        return moduleAreaLeft() + PANEL_PADDING + 6;
    }
    
    private int moduleContentRight()
    {
        return moduleAreaRight() - PANEL_PADDING - 12;
    }
    
    private int moduleContentTop()
    {
        return modulePanelTopBound() + PANEL_PADDING;
    }
    
    private int moduleContentBottom()
    {
        return modulePanelBottomBound() - PANEL_PADDING;
    }
    
    private void drawCategoryPanel(int frameLeft, int frameTop, int mouseX, int mouseY)
    {
        int categoryPanelLeft = frameLeft + PANEL_PADDING;
        int categoryPanelRight = frameLeft + CATEGORY_WIDTH - PANEL_PADDING / 2;
        int categoryPanelTop = categoryPanelTopBound();
        int categoryPanelBottom = categoryPanelBottomBound();
        
        // Draw category panel background
        Color panelColor = new Color(activeTheme.categoryBackground(), true);
        Color outlineColor = new Color(applyAlpha(activeTheme.outline(), 120), true);
        RRectUtils.drawRoundOutline(categoryPanelLeft, categoryPanelTop,
                categoryPanelRight - categoryPanelLeft, categoryPanelBottom - categoryPanelTop,
                PANEL_RADIUS, 1.0F, panelColor, outlineColor);
        
        // Draw separator
        GuiComponent.drawSeparator(categoryPanelLeft + PANEL_PADDING / 2.0, categoryPanelTop + 2,
                categoryPanelRight - categoryPanelLeft - PANEL_PADDING, 1.0,
                applyAlpha(activeTheme.outline(), 50));
        
        // Scissor for clipping
        int categoryInnerLeft = categoryPanelLeft + PANEL_PADDING;
        int categoryInnerRight = categoryPanelRight - PANEL_PADDING;
        int categoryClipTop = categoryPanelTop + PANEL_PADDING;
        int categoryClipBottom = categoryPanelBottom - PANEL_PADDING;
        boolean clipEnabled = pushScissor(categoryInnerLeft, categoryClipTop,
                categoryInnerRight - categoryInnerLeft, categoryClipBottom - categoryClipTop);
        
        // Draw categories
        Module.Category[] categories = Module.Category.values();
        int categoryY = categoryPanelTop + PANEL_PADDING + categoryScroll;
        
        for (Module.Category category : categories)
        {
            int rowTop = categoryY;
            int rowBottom = categoryY + ITEM_HEIGHT;
            boolean selected = category == selectedCategory;
            boolean hovered = mouseX >= categoryInnerLeft && mouseX <= categoryInnerRight &&
                    mouseY >= rowTop && mouseY < rowBottom;
            
            int drawTop = Math.max(rowTop, categoryClipTop);
            int drawBottom = Math.min(rowBottom, categoryClipBottom);
            
            if (drawBottom > drawTop)
            {
                if (selected)
                {
                    RenderUtil.drawRect(categoryInnerLeft, drawTop, categoryInnerRight, drawBottom,
                            applyAlpha(activeTheme.categorySelected(), 220));
                    RenderUtil.drawRect(categoryInnerLeft, drawTop, categoryInnerLeft + 3, drawBottom,
                            activeTheme.accent());
                }
                else if (hovered)
                {
                    RenderUtil.drawRect(categoryInnerLeft, drawTop, categoryInnerRight, drawBottom,
                            applyAlpha(activeTheme.moduleHover(), 160));
                }
            }
            
            float textX = categoryInnerLeft + 6;
            float textY = Math.max(drawTop + 4, rowTop + 4);
            FontUtil.drawString(category.getName(), textX, textY,
                    selected ? activeTheme.textPrimary() : activeTheme.textSecondary(), true);
            
            categoryY += ITEM_HEIGHT + 2;
        }
        
        if (clipEnabled)
        {
            popScissor();
        }
    }
    
    private void drawModulePanel(int frameLeft, int frameTop, int frameRight, int frameBottom, int mouseX, int mouseY)
    {
        int modulePanelLeft = moduleAreaLeft();
        int modulePanelRight = moduleAreaRight();
        int modulePanelTop = modulePanelTopBound();
        int modulePanelBottom = modulePanelBottomBound();
        
        // Draw module panel background
        Color panelColor = new Color(activeTheme.moduleBackground(), true);
        Color outlineColor = new Color(applyAlpha(activeTheme.outline(), 120), true);
        RRectUtils.drawRoundOutline(modulePanelLeft, modulePanelTop,
                modulePanelRight - modulePanelLeft, modulePanelBottom - modulePanelTop,
                PANEL_RADIUS, 1.0F, panelColor, outlineColor);
        
        // Draw separator
        GuiComponent.drawSeparator(modulePanelLeft + PANEL_PADDING / 2.0, modulePanelTop + 2,
                modulePanelRight - modulePanelLeft - PANEL_PADDING, 1.0,
                applyAlpha(activeTheme.outline(), 50));
        
        // Scissor for clipping
        int moduleListLeft = modulePanelLeft + PANEL_PADDING;
        int moduleListRight = modulePanelRight - PANEL_PADDING;
        int moduleClipTop = moduleContentTop();
        int moduleClipBottom = moduleContentBottom();
        // Use moduleListLeft and moduleListRight for scissor to match where module items are drawn
        // Intersect with main GUI bounds to ensure nothing renders outside the panel
        int clipLeft = Math.max(moduleListLeft, guiLeft);
        int clipRight = Math.min(moduleListRight, guiLeft + GUI_WIDTH);
        int clipTop = Math.max(moduleClipTop, guiTop);
        int clipBottom = Math.min(moduleClipBottom, guiTop + GUI_HEIGHT);
        boolean clipEnabled = pushScissor(clipLeft, clipTop,
                clipRight - clipLeft, clipBottom - clipTop);
        
        // Don't draw modules if CONFIG category is selected (should open GuiConfigManager instead)
        if (selectedCategory == Module.Category.CONFIG)
        {
            if (clipEnabled)
            {
                popScissor();
            }
            return;
        }
        
        // Calculate scroll bounds
        List<Module> modules = moduleManager.getModulesByCategory(selectedCategory);
        int totalHeight = calculateTotalModulesHeight(modules);
        int visibleHeight = Math.max(1, moduleClipBottom - moduleClipTop);
        int maxScroll = Math.max(0, totalHeight - visibleHeight);
        moduleScroll = Math.max(0, Math.min(moduleScroll, maxScroll));
        
        int moduleY = moduleClipTop - moduleScroll;
        
        // Find first visible module for rounded corners
        int firstVisibleModuleY = -1;
        int tempY = moduleClipTop - moduleScroll;
        for (Module m : modules)
        {
            if (tempY + ITEM_HEIGHT >= moduleClipTop && tempY < moduleClipBottom)
            {
                if (firstVisibleModuleY == -1) firstVisibleModuleY = tempY;
            }
            tempY += ITEM_HEIGHT;
            // Add animated height
            GuiAnimation anim = expandAnimations.get(m);
            if (anim != null)
            {
                double animValue = anim.getValue();
                float expandProgress = (float) Math.max(0.0, Math.min(1.0, animValue));
                if (expandProgress > 0.001F)
                {
                    int settingsHeight = calculateSettingsHeight(m, m.getSettings(), false);
                    int animatedHeight = Math.max(0, Math.round(settingsHeight * expandProgress));
                    tempY += animatedHeight;
                }
            }
        }
        
        // Calculate which modules are first and last visible (considering expanded options)
        tempY = moduleClipTop - moduleScroll;
        for (Module m : modules)
        {
            tempY += ITEM_HEIGHT;
            // Add animated height
            GuiAnimation anim = expandAnimations.get(m);
            if (anim != null)
            {
                double animValue = anim.getValue();
                float expandProgress = (float) Math.max(0.0, Math.min(1.0, animValue));
                if (expandProgress > 0.001F)
                {
                    int settingsHeight = calculateSettingsHeight(m, m.getSettings(), false);
                    int animatedHeight = Math.max(0, Math.round(settingsHeight * expandProgress));
                    tempY += animatedHeight;
                }
            }
        }
        
        // Draw modules
        int moduleIndex = 0;
        int moduleItemSpacing = 2; // Spacing between module items
        int moduleItemPadding = 3; // Padding on left and right sides
        float moduleOutlineWidth = 1.0F; // Outline width for module items
        
        for (Module module : modules)
        {
            boolean enabled = module.isEnabled();
            int rowTop = moduleY + moduleItemSpacing / 2;
            int rowBottom = moduleY + ITEM_HEIGHT - moduleItemSpacing / 2;
            int itemWidth = moduleListRight - moduleListLeft - (moduleItemPadding * 2);
            int itemLeft = moduleListLeft + moduleItemPadding;
            int itemRight = moduleListRight - moduleItemPadding;
            
            boolean hovered = mouseX >= itemLeft && mouseX <= itemRight &&
                    mouseY >= rowTop && mouseY < rowBottom;
            
            int drawTop = Math.max(rowTop, moduleClipTop);
            int drawBottom = Math.min(rowBottom, moduleClipBottom);
            int drawHeight = drawBottom - drawTop;
            
            if (drawBottom > drawTop && drawHeight > 0)
            {
                // Determine base color with better contrast
                int baseColor;
                int moduleOutlineColor;
                if (enabled)
                {
                    // Enabled modules - brighter, more distinct
                    baseColor = applyAlpha(activeTheme.moduleEnabled(), 255);
                    moduleOutlineColor = applyAlpha(activeTheme.outline(), hovered ? 200 : 180);
                }
                else
                {
                    // Disabled modules - still visible but darker
                    baseColor = applyAlpha(activeTheme.settingBackground(), 245);
                    moduleOutlineColor = applyAlpha(activeTheme.outline(), hovered ? 160 : 120);
                }
                
                // Draw module as regular rectangle with outline
                RenderUtil.drawBorderedRect(itemLeft, drawTop, itemWidth, drawHeight,
                        moduleOutlineWidth, moduleOutlineColor, baseColor);
                
                // Draw hover effect overlay - subtle highlight
                if (hovered)
                {
                    int hoverColor = applyAlpha(activeTheme.moduleHover(), 100);
                    RenderUtil.drawRect(itemLeft, drawTop, itemRight, drawBottom, hoverColor);
                }
                
                // Draw accent bar for enabled modules (left side indicator)
                if (enabled)
                {
                    int accentBarWidth = 3;
                    RenderUtil.drawRect(itemLeft + 1, drawTop + 1,
                            itemLeft + 1 + accentBarWidth, drawBottom - 1, activeTheme.accent());
                }
            }
            
            // Only render module name if it's within main panel bounds
            int mainPanelLeft = guiLeft;
            int mainPanelRight = guiLeft + GUI_WIDTH;
            int mainPanelTop = guiTop;
            int mainPanelBottom = guiTop + GUI_HEIGHT;
            
            // Check if module row is fully outside main panel - skip rendering if so
            if (rowBottom < mainPanelTop || rowTop > mainPanelBottom || 
                moduleContentRight() < mainPanelLeft || moduleContentLeft() > mainPanelRight)
            {
                // Module is fully outside main panel, skip rendering text
            }
            else
            {
                // Adjust text position to account for module item padding
                float textX = itemLeft + 6; // Slightly more padding from left edge
                float textY = Math.max(drawTop + 4, rowTop + 4);
                
                // Calculate available width for module name (accounting for keybind and padding)
                int availableWidth = itemRight - (int)textX - 6; // Account for padding
                if (module.getKeyBind() != 0)
                {
                    String keyName = Keyboard.getKeyName(module.getKeyBind());
                    String keyLabel = "[" + keyName + "]";
                    int keyWidth = FontUtil.getStringWidth(keyLabel);
                    availableWidth -= keyWidth + 8; // 8px spacing between name and keybind
                }
                
                // Truncate module name if it's too long
                String moduleName = module.getName();
                int moduleNameWidth = FontUtil.getStringWidth(moduleName);
                if (moduleNameWidth > availableWidth)
                {
                    // Truncate with ellipsis
                    int ellipsisWidth = FontUtil.getStringWidth("...");
                    int maxTextWidth = availableWidth - ellipsisWidth;
                    
                    while (moduleNameWidth > maxTextWidth && moduleName.length() > 0)
                    {
                        moduleName = moduleName.substring(0, moduleName.length() - 1);
                        moduleNameWidth = FontUtil.getStringWidth(moduleName);
                    }
                    moduleName = moduleName + "...";
                }
                
                FontUtil.drawString(moduleName, textX, textY,
                        enabled ? activeTheme.textPrimary() : activeTheme.textSecondary(), true);
                
                // Draw keybind (no icons/symbols)
                if (module.getKeyBind() != 0)
                {
                    String keyName = Keyboard.getKeyName(module.getKeyBind());
                    String keyLabel = "[" + keyName + "]";
                    int keyWidth = FontUtil.getStringWidth(keyLabel);
                    FontUtil.drawString(keyLabel, itemRight - keyWidth - 6,
                            textY, activeTheme.textSecondary(), true);
                }
            }
            
            moduleY += ITEM_HEIGHT;
            
            // Draw expanded settings with animation
            GuiAnimation expandAnim = expandAnimations.get(module);
            if (expandAnim != null)
            {
                double animValue = expandAnim.getValue();
                float expandProgress = (float) Math.max(0.0, Math.min(1.0, animValue));
                
                // Only render if there's meaningful progress
                if (expandProgress > 0.001F)
                {
                    List<Setting> settings = module.getSettings();
                    int settingsScroll = expandedModules.getOrDefault(module, 0);
                    int fullSettingsHeight = calculateSettingsHeight(module, settings, false);
                    int animatedHeight = Math.max(0, Math.round(fullSettingsHeight * expandProgress));
                    int settingsTop = moduleY;
                    
                    // Only proceed if we have actual height to render
                    if (animatedHeight > 0)
                    {
                        // Calculate visible bounds
                        int settingsBottom = settingsTop + animatedHeight;
                        int visibleTop = Math.max(settingsTop, moduleClipTop);
                        int visibleBottom = Math.min(settingsBottom, moduleClipBottom);
                        
                        // Draw background if there's visible area
                        if (visibleBottom > visibleTop)
                        {
                            int settingsColor = applyAlpha(activeTheme.settingBackground(), 220);
                            
                            // Draw options menu background
                            RenderUtil.drawRect(moduleListLeft, visibleTop, moduleListRight, visibleBottom, settingsColor);
                            
                            if (module.isEnabled())
                            {
                                RenderUtil.drawRect(moduleListLeft, visibleTop, moduleListLeft + 2, visibleBottom,
                                        activeTheme.accent());
                            }
                        }
                        
                        // Apply scissor for clipping animated content
                        int scissorTop = Math.max(settingsTop, moduleClipTop);
                        int scissorBottom = Math.min(settingsTop + animatedHeight, moduleClipBottom);
                        int scissorHeight = Math.max(0, scissorBottom - scissorTop);
                        
                        boolean settingsClipEnabled = false;
                        if (scissorHeight > 0 && scissorBottom > scissorTop)
                        {
                            settingsClipEnabled = pushScissor(moduleListLeft, scissorTop, 
                                    moduleListRight - moduleListLeft, scissorHeight);
                        }
                        
                        // Draw settings with proper clipping
                        int settingsY = settingsTop - settingsScroll;
                        int visibleIndex = 0;
                        
                        // Draw keybind entry first
                        int keybindY = settingsY + visibleIndex * SETTING_ITEM_HEIGHT;
                        int keybindBottom = keybindY + SETTING_ITEM_HEIGHT;
                        if (keybindBottom > settingsTop && keybindY < settingsTop + animatedHeight)
                        {
                            if (keybindBottom >= moduleClipTop && keybindY <= moduleClipBottom)
                            {
                                drawKeybindSetting(module, keybindY, mouseX, mouseY, expandProgress);
                            }
                        }
                        visibleIndex++;
                        
                        for (Setting setting : settings)
                        {
                            if (!setting.isVisible()) continue;
                            
                            int settingY = settingsY + visibleIndex * SETTING_ITEM_HEIGHT;
                            int settingBottom = settingY + SETTING_ITEM_HEIGHT;
                            
                            // Only draw if setting is within animated bounds and visible area
                            if (settingBottom > settingsTop && settingY < settingsTop + animatedHeight)
                            {
                                if (settingBottom >= moduleClipTop && settingY <= moduleClipBottom)
                                {
                                    drawSetting(setting, settingY, mouseX, mouseY, expandProgress);
                                    
                                    // Store dropdown position for rendering after scissor
                                    if (setting instanceof EnumSetting && dropdownSetting == setting)
                                    {
                                        dropdownSettingY = settingY;
                                    }
                                }
                            }
                            
                            visibleIndex++;
                        }
                        
                        if (settingsClipEnabled)
                        {
                            popScissor();
                        }
                    }
                    
                    // Increment moduleY with animated height for proper spacing
                    moduleY += animatedHeight;
                }
            }
            
            // Draw separator line at the bottom of the module (between modules)
            // Draw after the module and its expanded settings, but before the next module
            // Calculate the actual bottom of this module (including expanded settings)
            int moduleBottomY = moduleY;
            int separatorY = moduleBottomY;
            int separatorDrawY = Math.max(separatorY, moduleClipTop);
            int separatorDrawBottom = Math.min(separatorY + 1, moduleClipBottom);
            
            // Only draw separator if it's within the visible area and not the last module
            if (separatorDrawBottom > separatorDrawY && separatorDrawY >= moduleClipTop && separatorDrawY < moduleClipBottom)
            {
                // Check if this is not the last module
                boolean isLastModule = (moduleIndex == modules.size() - 1);
                if (!isLastModule)
                {
                    GuiComponent.drawSeparator(moduleListLeft + 4, separatorY, 
                            moduleListRight - moduleListLeft - 8, 1.0,
                            applyAlpha(activeTheme.outline(), 50));
                }
            }
            
            moduleIndex++;
        }
        
        if (clipEnabled)
        {
            popScissor();
        }
        
        // Draw dropdown after scissor is popped so it renders on top
        if (dropdownSetting != null)
        {
            int dropdownClipTop = moduleContentTop();
            int dropdownClipBottom = moduleContentBottom();
            drawDropdown(dropdownSetting, dropdownSettingY, dropdownClipTop, dropdownClipBottom, mouseX, mouseY);
        }
        
        // Draw color picker if open
        if (colorPickerOpen && activeColorSetting != null)
        {
            drawColorPicker(mouseX, mouseY);
        }
        
        // Handle mouse wheel scrolling
        int mouseWheel = Mouse.getDWheel();
        if (mouseWheel != 0)
        {
            if (mouseX >= modulePanelLeft && mouseX <= modulePanelRight &&
                    mouseY >= modulePanelTop && mouseY <= modulePanelBottom)
            {
                int scrollDelta = mouseWheel > 0 ? -12 : 12;
                moduleScroll = Math.max(0, Math.min(moduleScroll + scrollDelta, maxScroll));
            }
        }
    }
    
    private int calculateTotalModulesHeight(List<Module> modules)
    {
        int height = 0;
        for (Module module : modules)
        {
            height += ITEM_HEIGHT;
            // Add animated height
            GuiAnimation anim = expandAnimations.get(module);
            if (anim != null)
            {
                double animValue = anim.getValue();
                float expandProgress = (float) Math.max(0.0, Math.min(1.0, animValue));
                if (expandProgress > 0.001F)
                {
                    int settingsHeight = calculateSettingsHeight(module, module.getSettings(), false);
                    int animatedHeight = Math.max(0, Math.round(settingsHeight * expandProgress));
                    height += animatedHeight;
                }
            }
        }
        return height;
    }
    
    private int calculateSettingsHeight(Module module, List<Setting> settings, boolean includeDropdown)
    {
        int height = 0;
        // Add keybind entry height
        height += SETTING_ITEM_HEIGHT;
        for (Setting setting : settings)
        {
            if (!setting.isVisible()) continue;
            height += SETTING_ITEM_HEIGHT;
            // Don't include dropdown in height calculation to prevent blank space
            if (includeDropdown && setting instanceof EnumSetting && dropdownSetting == setting)
            {
                height += ((EnumSetting) setting).getModes().size() * SETTING_ITEM_HEIGHT;
            }
        }
        return height;
    }
    
    private void drawKeybindSetting(Module module, int y, int mouseX, int mouseY, float alpha)
    {
        int rowLeft = moduleAreaLeft() + PANEL_PADDING;
        int rowRight = moduleAreaRight() - PANEL_PADDING;
        int contentTop = moduleContentTop();
        int contentBottom = moduleContentBottom();
        int drawTop = Math.max(y, contentTop);
        int drawBottom = Math.min(y + SETTING_ITEM_HEIGHT, contentBottom);
        
        if (drawBottom <= drawTop) return;
        
        boolean isBinding = bindingModule == module;
        boolean hovered = mouseX >= rowLeft && mouseX <= rowRight && 
                mouseY >= drawTop && mouseY < drawBottom;
        
        int baseColor = applyAlpha(activeTheme.settingBackground(), (int) (hovered ? 230 : 200 * alpha));
        RenderUtil.drawRect(rowLeft, drawTop, rowRight, drawBottom, baseColor);
        if (hovered || isBinding)
        {
            // Full width hover effect
            RenderUtil.drawRect(rowLeft, drawTop, rowRight, drawBottom,
                    applyAlpha(activeTheme.moduleHover(), (int) (120 * alpha)));
        }
        
        float labelY = Math.max(drawTop + 3, y + 3);
        FontUtil.drawString("Keybind", moduleContentLeft() + 4, labelY, activeTheme.textSecondary(), true);
        
        // Draw keybind value
        String keyText;
        if (isBinding)
        {
            keyText = EnumChatFormatting.YELLOW + "Press a key...";
        }
        else if (module.getKeyBind() != 0)
        {
            String keyName = Keyboard.getKeyName(module.getKeyBind());
            keyText = EnumChatFormatting.AQUA + "[" + keyName + "]";
        }
        else
        {
            keyText = EnumChatFormatting.GRAY + "None";
        }
        
        int keyTextWidth = FontUtil.getStringWidth(keyText);
        int keyTextX = moduleContentRight() - keyTextWidth - 6;
        FontUtil.drawString(keyText, keyTextX, labelY, 0xFFFFFF, true);
    }
    
    private void drawSetting(Setting setting, int y, int mouseX, int mouseY, float alpha)
    {
        int rowLeft = moduleAreaLeft() + PANEL_PADDING;
        int rowRight = moduleAreaRight() - PANEL_PADDING;
        int contentTop = moduleContentTop();
        int contentBottom = moduleContentBottom();
        int drawTop = Math.max(y, contentTop);
        int drawBottom = Math.min(y + SETTING_ITEM_HEIGHT, contentBottom);
        
        if (drawBottom <= drawTop) return;
        
        // Check if dropdown is open and mouse is over it (prevent hover on items underneath)
        boolean dropdownOpen = dropdownSetting != null;
        boolean mouseOverDropdown = false;
        if (dropdownOpen && dropdownSetting instanceof EnumSetting)
        {
            int dropdownTop = dropdownSettingY + SETTING_ITEM_HEIGHT;
            EnumSetting enumSetting = (EnumSetting) dropdownSetting;
            int dropdownBottom = dropdownTop + enumSetting.getModes().size() * SETTING_ITEM_HEIGHT;
            mouseOverDropdown = mouseX >= moduleContentLeft() && mouseX <= moduleContentRight() &&
                    mouseY >= dropdownTop && mouseY <= dropdownBottom;
        }
        
        // Only show hover if dropdown is not open or mouse is not over dropdown
        boolean hovered = !mouseOverDropdown && mouseX >= rowLeft && mouseX <= rowRight && 
                mouseY >= drawTop && mouseY < drawBottom;
        
        if (setting instanceof SeparatorSetting)
        {
            int accentAlpha = (int) (hovered ? 200 : 140 * alpha);
            int lineColor = applyAlpha(activeTheme.outline(), accentAlpha);
            RenderUtil.drawRect(rowLeft, drawBottom - 1, rowRight, drawBottom, lineColor);
            FontUtil.drawString(setting.getName(), moduleContentLeft(), Math.max(drawTop + 2, y + 2),
                    activeTheme.textPrimary(), true);
            return;
        }
        
        int baseColor = applyAlpha(activeTheme.settingBackground(), (int) (hovered ? 230 : 200 * alpha));
        RenderUtil.drawRect(rowLeft, drawTop, rowRight, drawBottom, baseColor);
        if (hovered)
        {
            // Full width hover effect
            RenderUtil.drawRect(rowLeft, drawTop, rowRight, drawBottom,
                    applyAlpha(activeTheme.moduleHover(), (int) (120 * alpha)));
        }
        
        float labelY = Math.max(drawTop + 3, y + 3);
        FontUtil.drawString(setting.getName(), moduleContentLeft() + 4, labelY, activeTheme.textSecondary(), true);
        
        if (setting instanceof BooleanSetting)
        {
            BooleanSetting boolSetting = (BooleanSetting) setting;
            int toggleWidth = 24;
            int toggleHeight = 10;
            int toggleX = moduleContentRight() - toggleWidth;
            int toggleY = y + (SETTING_ITEM_HEIGHT - toggleHeight) / 2;
            GuiComponent.drawToggle(toggleX, toggleY, toggleWidth, toggleHeight, boolSetting.getValue(), activeTheme);
        }
        else if (setting instanceof IntSetting)
        {
            IntSetting intSetting = (IntSetting) setting;
            int sliderX = moduleContentRight() - 100;
            int sliderHeight = 8;
            int sliderY = y + (SETTING_ITEM_HEIGHT - sliderHeight) / 2;
            boolean sliderHovered = mouseX >= sliderX && mouseX <= sliderX + 100 &&
                    mouseY >= sliderY && mouseY <= sliderY + sliderHeight;
            boolean isDragging = draggingSetting == intSetting;
            
            int range = intSetting.getMax() - intSetting.getMin();
            float progress = range <= 0 ? 1.0F :
                    (float)(intSetting.getValue() - intSetting.getMin()) / (float)range;
            progress = Math.max(0.0F, Math.min(1.0F, progress));
            
            GuiComponent.drawSlider(sliderX, sliderY, 100, sliderHeight, progress, sliderHovered, isDragging, activeTheme);
            
            String valueText = String.valueOf(intSetting.getValue());
            int textWidth = FontUtil.getStringWidth(valueText);
            FontUtil.drawString(valueText, sliderX - textWidth - 6, labelY, activeTheme.textSecondary(), true);
        }
        else if (setting instanceof com.bhop4real.proton.client.module.settings.DoubleSetting)
        {
            com.bhop4real.proton.client.module.settings.DoubleSetting doubleSetting =
                    (com.bhop4real.proton.client.module.settings.DoubleSetting) setting;
            int sliderX = moduleContentRight() - 100;
            int sliderHeight = 8;
            int sliderY = y + (SETTING_ITEM_HEIGHT - sliderHeight) / 2;
            boolean sliderHovered = mouseX >= sliderX && mouseX <= sliderX + 100 &&
                    mouseY >= sliderY && mouseY <= sliderY + sliderHeight;
            boolean isDragging = draggingDoubleSetting == doubleSetting;
            
            double range = doubleSetting.getMax() - doubleSetting.getMin();
            float progress = range <= 0.0 ? 1.0F :
                    (float)((doubleSetting.getValue() - doubleSetting.getMin()) / range);
            progress = Math.max(0.0F, Math.min(1.0F, progress));
            
            GuiComponent.drawSlider(sliderX, sliderY, 100, sliderHeight, progress, sliderHovered, isDragging, activeTheme);
            
            String valueText = String.format(java.util.Locale.US, "%.2f", doubleSetting.getValue());
            int textWidth = FontUtil.getStringWidth(valueText);
            FontUtil.drawString(valueText, sliderX - textWidth - 6, labelY, activeTheme.textSecondary(), true);
        }
        else if (setting instanceof EnumSetting)
        {
            EnumSetting enumSetting = (EnumSetting) setting;
            String valueText = enumSetting.getValue();
            int valueWidth = FontUtil.getStringWidth(valueText);
            int pillPadding = 6;
            int pillLeft = moduleContentRight() - valueWidth - pillPadding * 2;
            int pillTop = Math.max(drawTop + 1, y + 1);
            int pillBottom = Math.min(drawBottom - 1, y + SETTING_ITEM_HEIGHT - 1);
            int pillHeight = pillBottom - pillTop;
            boolean isOpen = dropdownSetting == enumSetting;
            
            if (pillHeight > 0)
            {
                GuiComponent.drawPill(pillLeft, pillTop, valueWidth + pillPadding * 2, pillHeight,
                        valueText, hovered, isOpen, activeTheme);
            }
        }
        else if (setting instanceof ColorSetting)
        {
            ColorSetting colorSetting = (ColorSetting) setting;
            int swatchX = moduleContentRight() - 24;
            int swatchY = y + (SETTING_ITEM_HEIGHT - 10) / 2;
            swatchY = Math.max(drawTop + 1, swatchY);
            int swatchBottom = Math.min(swatchY + 10, drawBottom - 1);
            int color = colorSetting.getColor();
            if (swatchBottom > swatchY)
            {
                RenderUtil.drawRect(swatchX - 1, swatchY - 1, swatchX + 19, swatchBottom + 1,
                        applyAlpha(activeTheme.outline(), 45));
                RoundedRectUtil.drawRoundedRect(swatchX, swatchY, 18, swatchBottom - swatchY, 3, color);
            }
        }
        else if (setting instanceof StringSetting)
        {
            StringSetting stringSetting = (StringSetting) setting;
            String value = stringSetting.getValue();
            if (value == null || value.isEmpty())
            {
                value = "None selected";
            }
            else
            {
                // Show only filename if path is long
                int lastSeparator = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
                if (lastSeparator >= 0 && lastSeparator < value.length() - 1)
                {
                    value = value.substring(lastSeparator + 1);
                }
            }
            
            int valueWidth = FontUtil.getStringWidth(value);
            int maxWidth = moduleContentRight() - moduleContentLeft() - 8;
            if (valueWidth > maxWidth)
            {
                // Truncate if too long
                while (valueWidth > maxWidth && value.length() > 0)
                {
                    value = value.substring(0, value.length() - 1);
                    valueWidth = FontUtil.getStringWidth(value);
                }
                value = value + "...";
            }
            
            int valueX = moduleContentRight() - valueWidth - 6;
            int textColor = hovered ? activeTheme.textPrimary() : activeTheme.textSecondary();
            FontUtil.drawString(value, valueX, labelY, textColor, true);
        }
    }
    
    private void drawDropdown(EnumSetting enumSetting, int settingY, int clipTop, int clipBottom, int mouseX, int mouseY)
    {
        int dropdownLeft = moduleContentLeft();
        int dropdownRight = moduleContentRight();
        int dropdownTop = settingY + SETTING_ITEM_HEIGHT;
        List<String> modes = enumSetting.getModes();
        int dropdownBottom = dropdownTop + modes.size() * SETTING_ITEM_HEIGHT;
        
        int drawTop = Math.max(dropdownTop, clipTop);
        int drawBottom = Math.min(dropdownBottom, clipBottom);
        
        if (drawBottom <= drawTop) return;
        
        int dropdownWidth = dropdownRight - dropdownLeft;
        int dropdownHeight = drawBottom - drawTop;
        Color fillColor = new Color(applyAlpha(activeTheme.settingBackground(), 255), true);
        Color outlineColor = new Color(applyAlpha(activeTheme.outline(), 220), true);
        RRectUtils.drawRoundOutline(dropdownLeft, drawTop, dropdownWidth, dropdownHeight,
                PANEL_RADIUS, 1.2F, fillColor, outlineColor);
        
        // Check if mouse is over dropdown area (to prevent hover on options underneath)
        boolean mouseOverDropdown = mouseX >= dropdownLeft && mouseX <= dropdownRight &&
                mouseY >= dropdownTop && mouseY <= dropdownBottom;
        
        for (int i = 0; i < modes.size(); i++)
        {
            String mode = modes.get(i);
            int optionTop = dropdownTop + i * SETTING_ITEM_HEIGHT;
            int optionBottom = optionTop + SETTING_ITEM_HEIGHT;
            
            if (optionBottom <= drawTop || optionTop >= drawBottom) continue;
            
            int optDrawTop = Math.max(optionTop, drawTop);
            int optDrawBottom = Math.min(optionBottom, drawBottom);
            // Only hover if mouse is actually over dropdown area
            boolean hovered = mouseOverDropdown && mouseX >= dropdownLeft && mouseX <= dropdownRight &&
                    mouseY >= optDrawTop && mouseY < optDrawBottom;
            
            if (i > 0)
            {
                RenderUtil.drawRect(dropdownLeft + 4, optionTop, dropdownRight - 4, optionTop + 1,
                        applyAlpha(activeTheme.outline(), 30));
            }
            
            if (hovered)
            {
                RenderUtil.drawRect(dropdownLeft + 4, optDrawTop, dropdownRight - 4, optDrawBottom,
                        applyAlpha(activeTheme.moduleHover(), 200));
            }
            
            boolean selected = mode.equals(enumSetting.getValue());
            float textX = dropdownLeft + 6;
            float textY = Math.max(optDrawTop + 3, optionTop + 3);
            FontUtil.drawString(mode, textX, textY,
                    selected ? activeTheme.textPrimary() : activeTheme.textSecondary(), true);
        }
    }
    
    private void drawColorPicker(int mouseX, int mouseY)
    {
        if (activeColorSetting == null) return;
        
        int pickerLeft = colorPickerX;
        int pickerTop = colorPickerY;
        int pickerWidth = 220;
        int pickerHeight = 180;
        
        // Draw background with rounded corners (matching GUI theme)
        int bgColor = applyAlpha(activeTheme.settingBackground(), 255);
        int outlineColor = applyAlpha(activeTheme.outline(), 220);
        Color fillColor = new Color(bgColor, true);
        Color outlineColorObj = new Color(outlineColor, true);
        RRectUtils.drawRoundOutline(pickerLeft, pickerTop, pickerWidth, pickerHeight, PANEL_RADIUS, 1.2F, fillColor, outlineColorObj);
        
        // Get current color
        int currentColor = activeColorSetting.getColor();
        int alpha = (currentColor >> 24) & 0xFF;
        int red = (currentColor >> 16) & 0xFF;
        int green = (currentColor >> 8) & 0xFF;
        int blue = currentColor & 0xFF;
        
        // Draw color preview box (larger, at the top)
        int previewX = pickerLeft + 10;
        int previewY = pickerTop + 10;
        int previewWidth = pickerWidth - 20;
        int previewHeight = 40;
        
        // Draw checkerboard pattern for transparency
        int checkerSize = 4;
        for (int cx = 0; cx < previewWidth; cx += checkerSize)
        {
            for (int cy = 0; cy < previewHeight; cy += checkerSize)
            {
                boolean isWhite = ((cx / checkerSize) + (cy / checkerSize)) % 2 == 0;
                int checkerColor = isWhite ? 0xFFFFFFFF : 0xFFCCCCCC;
                RenderUtil.drawRect(previewX + cx, previewY + cy, previewX + cx + checkerSize, previewY + cy + checkerSize, checkerColor);
            }
        }
        
        // Draw color preview on top
        RoundedRectUtil.drawRoundedRect(previewX, previewY, previewWidth, previewHeight, 4, currentColor);
        
        // Draw outline around preview
        RenderUtil.drawBorderedRect(previewX, previewY, previewWidth, previewHeight, 1.0F, outlineColor, 0);
        
        // Draw sliders below preview
        int sliderStartY = previewY + previewHeight + 15;
        int sliderSpacing = 22;
        int labelWidth = 25;
        int valueWidth = 35;
        int sliderX = pickerLeft + 10 + labelWidth;
        int sliderWidth = 180 - labelWidth - valueWidth;
        int sliderHeight = 8;
        
        // Draw Red slider
        drawColorSliderWithGuiComponent(pickerLeft + 10, sliderStartY, sliderX, sliderWidth, sliderHeight, red, 0xFFFF0000, "R", draggingRed);
        
        // Draw Green slider
        drawColorSliderWithGuiComponent(pickerLeft + 10, sliderStartY + sliderSpacing, sliderX, sliderWidth, sliderHeight, green, 0xFF00FF00, "G", draggingGreen);
        
        // Draw Blue slider
        drawColorSliderWithGuiComponent(pickerLeft + 10, sliderStartY + sliderSpacing * 2, sliderX, sliderWidth, sliderHeight, blue, 0xFF0000FF, "B", draggingBlue);
        
        // Draw Alpha slider
        drawAlphaSliderWithGuiComponent(pickerLeft + 10, sliderStartY + sliderSpacing * 3, sliderX, sliderWidth, sliderHeight, alpha, currentColor & 0x00FFFFFF, draggingAlpha);
    }
    
    private void drawColorSliderWithGuiComponent(int labelX, int rowY, int sliderX, int sliderWidth, int sliderHeight, int value, int baseColor, String label, boolean dragging)
    {
        // Draw label
        FontUtil.drawString(label, labelX, rowY + 5, activeTheme.textSecondary(), true);
        
        int sliderY = rowY + (18 - sliderHeight) / 2;
        
        // Draw gradient background (from black to base color) behind slider
        int startColor = 0xFF000000; // Black
        int endColor = baseColor & 0x00FFFFFF | 0xFF000000; // Base color with full alpha
        RenderUtil.drawGradientRect(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, startColor, endColor);
        
        // Calculate progress
        float progress = (float)value / 255.0F;
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        
        // Draw slider outline and knob (matching GuiComponent style, but without track background)
        boolean hovered = dragging;
        int trackRadius = sliderHeight / 2;
        int outlineAlpha = hovered || dragging ? 200 : 140;
        
        // Draw outline
        RoundedRectUtil.drawRoundedRect(sliderX - 1, sliderY - 1, sliderWidth + 2, sliderHeight + 2,
                trackRadius + 1, applyAlpha(activeTheme.outline(), outlineAlpha));
        
        // Draw knob (same as GuiComponent)
        int innerLeft = sliderX + 2;
        int innerWidth = sliderWidth - 4;
        int innerHeight = Math.max(3, sliderHeight - 4);
        int fillWidth = Math.round(innerWidth * progress);
        
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
        int knobY = sliderY + (sliderHeight - knobDiameter) / 2;
        
        RoundedRectUtil.drawRoundedRect(knobX, knobY, knobDiameter, knobDiameter,
                Math.max(3, knobDiameter / 2), applyAlpha(activeTheme.outline(), hovered || dragging ? 220 : 170));
        RoundedRectUtil.drawRoundedRect(knobX + 1, knobY + 1, knobDiameter - 2, knobDiameter - 2,
                Math.max(2, (knobDiameter - 2) / 2), activeTheme.textPrimary());
        
        // Draw value text
        String valueText = String.valueOf(value);
        FontUtil.drawString(valueText, sliderX + sliderWidth + 8, rowY + 5, activeTheme.textSecondary(), true);
    }
    
    private void drawAlphaSliderWithGuiComponent(int labelX, int rowY, int sliderX, int sliderWidth, int sliderHeight, int alphaValue, int rgbColor, boolean dragging)
    {
        // Draw label
        FontUtil.drawString("A", labelX, rowY + 5, activeTheme.textSecondary(), true);
        
        int sliderY = rowY + (18 - sliderHeight) / 2;
        
        // Draw checkerboard pattern for transparency
        int checkerSize = 4;
        for (int cx = 0; cx < sliderWidth; cx += checkerSize)
        {
            for (int cy = 0; cy < sliderHeight; cy += checkerSize)
            {
                boolean isWhite = ((cx / checkerSize) + (cy / checkerSize)) % 2 == 0;
                int checkerColor = isWhite ? 0xFFFFFFFF : 0xFFCCCCCC;
                RenderUtil.drawRect(sliderX + cx, sliderY + cy, sliderX + cx + checkerSize, sliderY + cy + checkerSize, checkerColor);
            }
        }
        
        // Draw alpha gradient
        for (int cx = 0; cx < sliderWidth; cx++)
        {
            int alpha = (int)((float)cx / (float)sliderWidth * 255.0F);
            alpha = Math.max(0, Math.min(255, alpha));
            int color = (alpha << 24) | rgbColor;
            RenderUtil.drawRect(sliderX + cx, sliderY, sliderX + cx + 1, sliderY + sliderHeight, color);
        }
        
        // Calculate progress
        float progress = (float)alphaValue / 255.0F;
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        
        // Draw slider outline and knob (matching GuiComponent style, but without track background)
        boolean hovered = dragging;
        int trackRadius = sliderHeight / 2;
        int outlineAlpha = hovered || dragging ? 200 : 140;
        
        // Draw outline
        RoundedRectUtil.drawRoundedRect(sliderX - 1, sliderY - 1, sliderWidth + 2, sliderHeight + 2,
                trackRadius + 1, applyAlpha(activeTheme.outline(), outlineAlpha));
        
        // Draw knob (same as GuiComponent)
        int innerLeft = sliderX + 2;
        int innerWidth = sliderWidth - 4;
        int innerHeight = Math.max(3, sliderHeight - 4);
        int fillWidth = Math.round(innerWidth * progress);
        
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
        int knobY = sliderY + (sliderHeight - knobDiameter) / 2;
        
        RoundedRectUtil.drawRoundedRect(knobX, knobY, knobDiameter, knobDiameter,
                Math.max(3, knobDiameter / 2), applyAlpha(activeTheme.outline(), hovered || dragging ? 220 : 170));
        RoundedRectUtil.drawRoundedRect(knobX + 1, knobY + 1, knobDiameter - 2, knobDiameter - 2,
                Math.max(2, (knobDiameter - 2) / 2), activeTheme.textPrimary());
        
        // Draw value text
        String valueText = String.valueOf(alphaValue);
        FontUtil.drawString(valueText, sliderX + sliderWidth + 8, rowY + 5, activeTheme.textSecondary(), true);
    }
    
    private boolean pushScissor(int x, int y, int width, int height)
    {
        if (width <= 0 || height <= 0) return false;
        
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        int scissorX = x * scale;
        int scissorY = mc.displayHeight - (y + height) * scale;
        int scissorWidth = width * scale;
        int scissorHeight = height * scale;
        
        if (scissorWidth <= 0 || scissorHeight <= 0) return false;
        
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        return true;
    }
    
    private void popScissor()
    {
        if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST))
        {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }
    
    @Override
    public void updateScreen()
    {
        super.updateScreen();
    }
    
    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
}

