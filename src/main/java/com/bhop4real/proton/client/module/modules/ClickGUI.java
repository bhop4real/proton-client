package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.gui.GuiClickGUI;
import com.bhop4real.proton.client.gui.config.ClickGuiPreferences;
import com.bhop4real.proton.client.gui.theme.ClickGuiTheme;
import com.bhop4real.proton.client.gui.theme.ThemeManager;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import com.bhop4real.proton.client.module.settings.IntSetting;

/**
 * Primary Click GUI module that exposes user preferences for the interface.
 */
public class ClickGUI extends Module
{
    private final DisplaySettings displaySettings;
    private final BlurSettings blurSettings;
    private final ThemeSettings themeSettings;
    private final AnimationSettings animationSettings;

    public ClickGUI()
    {
        super("ClickGUI", new String[]{"clickgui", "gui"}, "Opens the Click GUI", Category.RENDER, 0);

        this.displaySettings = new DisplaySettings();
        this.blurSettings = new BlurSettings();
        this.themeSettings = new ThemeSettings();
        this.animationSettings = new AnimationSettings();

        // Load custom themes on initialization
        ThemeManager.loadCustomThemes();
        
        registerSettings();
    }

    private void registerSettings()
    {
        addSetting(displaySettings.backgroundBrightness);
        addSetting(blurSettings.enabled);
        addSetting(blurSettings.radius);
        addSetting(blurSettings.mode);
        addSetting(themeSettings.theme);
        addSetting(animationSettings.openDuration);
        addSetting(animationSettings.expandDuration);
    }

    @Override
    public void onEnable()
    {
        if (mc.thePlayer != null && mc.theWorld != null)
        {
            GuiClickGUI.preloadResources();
            mc.displayGuiScreen(new GuiClickGUI());
        }
    }

    @Override
    public void onDisable()
    {
        // GUI cleans itself up when closed
    }

    public ClickGuiPreferences snapshotPreferences()
    {
        // Get theme from ThemeManager (supports both built-in and custom themes)
        String themeId = themeSettings.theme.getValue();
        return ClickGuiPreferences.builder()
                .theme(ThemeManager.getTheme(themeId))
                .backgroundBrightness(displaySettings.backgroundBrightness.getValue())
                .blurEnabled(blurSettings.enabled.getValue())
                .blurRadius(blurSettings.radius.getValue())
                .blurMode(blurSettings.mode())
                .openAnimationDuration(animationSettings.openDuration.getValue())
                .expandAnimationDuration(animationSettings.expandDuration.getValue())
                .build();
    }

    private static final class DisplaySettings
    {
        private final IntSetting backgroundBrightness = new IntSetting(
                "Background Brightness",
                "Screen darkening (0-255)",
                136,
                0,
                255
        );
    }

    private final class BlurSettings
    {
        private final BooleanSetting enabled = new BooleanSetting(
                "Background Blur",
                "Blur the world behind the Click GUI",
                true
        );

        private final IntSetting radius = new IntSetting(
                "Blur Strength",
                "Controls the intensity of background blur",
                12,
                0,
                32
        );

        private final EnumSetting mode = new EnumSetting(
                "Blur Mode",
                "Select blur algorithm",
                ClickGuiPreferences.BlurMode.GAUSSIAN.getId(),
                ClickGuiPreferences.BlurMode.ids()
        );

        private BlurSettings()
        {
            radius.setVisibilitySupplier(enabled::getValue);
            mode.setVisibilitySupplier(enabled::getValue);
        }

        private ClickGuiPreferences.BlurMode mode()
        {
            return ClickGuiPreferences.BlurMode.byId(mode.getValue());
        }
    }

    private static final class ThemeSettings
    {
        private final EnumSetting theme;
        
        private ThemeSettings()
        {
            // Get all available theme IDs (both built-in and custom)
            String[] themeIds = ThemeManager.getAllThemeIds();
            if (themeIds.length == 0)
            {
                // Fallback to built-in themes if ThemeManager hasn't loaded yet
                themeIds = ClickGuiTheme.ids();
            }
            
            this.theme = new EnumSetting(
                    "Theme",
                    "Color palette for the Click GUI",
                    ClickGuiTheme.MIDNIGHT.getId(),
                    themeIds
            );
        }
    }
    
    private static final class AnimationSettings
    {
        private final IntSetting openDuration = new IntSetting(
                "Open Animation Duration",
                "Duration of open/close animation in milliseconds",
                280,
                100,
                1000
        );
        
        private final IntSetting expandDuration = new IntSetting(
                "Expand Animation Duration",
                "Duration of expand/collapse animation in milliseconds",
                200,
                50,
                500
        );
    }
}

