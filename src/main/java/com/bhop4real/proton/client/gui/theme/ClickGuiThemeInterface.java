package com.bhop4real.proton.client.gui.theme;

/**
 * Interface for theme color palettes used by the ClickGUI.
 * Both built-in themes (ClickGuiTheme enum) and custom themes (CustomTheme) implement this.
 */
public interface ClickGuiThemeInterface
{
    String getId();
    int overlay();
    int container();
    int header();
    int categoryBackground();
    int categorySelected();
    int moduleBackground();
    int moduleEnabled();
    int moduleHover();
    int settingBackground();
    int accent();
    int accentMuted();
    int textPrimary();
    int textSecondary();
    int outline();
}

