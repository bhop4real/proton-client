package com.bhop4real.proton.client.gui.theme;

/**
 * Represents a custom theme loaded from a JSON file.
 * This class provides the same interface as ClickGuiTheme but with dynamic values.
 */
public class CustomTheme implements ClickGuiThemeInterface
{
    private static final int DEFAULT_OVERLAY = 0xB013161D;
    private static final int DEFAULT_CONTAINER = 0xF0191F29;
    private static final int DEFAULT_HEADER = 0xFF242E3D;
    private static final int DEFAULT_CATEGORY_BACKGROUND = 0xFF1A222D;
    private static final int DEFAULT_CATEGORY_SELECTED = 0xFF2B3647;
    private static final int DEFAULT_MODULE_BACKGROUND = 0xFF121923;
    private static final int DEFAULT_MODULE_ENABLED = 0xFF273448;
    private static final int DEFAULT_MODULE_HOVER = 0x332D3C52;
    private static final int DEFAULT_SETTING_BACKGROUND = 0xFF181F2C;
    private static final int DEFAULT_ACCENT = 0xFF4DA3FF;
    private static final int DEFAULT_ACCENT_MUTED = 0x804DA3FF;
    private static final int DEFAULT_TEXT_PRIMARY = 0xFFE6EEF9;
    private static final int DEFAULT_TEXT_SECONDARY = 0xFFA8B6C9;
    private static final int DEFAULT_OUTLINE = 0x3327313F;
    
    private final String id;
    private final int overlay;
    private final int container;
    private final int header;
    private final int categoryBackground;
    private final int categorySelected;
    private final int moduleBackground;
    private final int moduleEnabled;
    private final int moduleHover;
    private final int settingBackground;
    private final int accent;
    private final int accentMuted;
    private final int textPrimary;
    private final int textSecondary;
    private final int outline;
    
    /**
     * Creates a CustomTheme with default values (Midnight theme as fallback)
     */
    public CustomTheme(String id)
    {
        this(id, DEFAULT_OVERLAY, DEFAULT_CONTAINER, DEFAULT_HEADER, 
             DEFAULT_CATEGORY_BACKGROUND, DEFAULT_CATEGORY_SELECTED,
             DEFAULT_MODULE_BACKGROUND, DEFAULT_MODULE_ENABLED, DEFAULT_MODULE_HOVER,
             DEFAULT_SETTING_BACKGROUND, DEFAULT_ACCENT, DEFAULT_ACCENT_MUTED,
             DEFAULT_TEXT_PRIMARY, DEFAULT_TEXT_SECONDARY, DEFAULT_OUTLINE);
    }
    
    public CustomTheme(String id,
                      int overlay,
                      int container,
                      int header,
                      int categoryBackground,
                      int categorySelected,
                      int moduleBackground,
                      int moduleEnabled,
                      int moduleHover,
                      int settingBackground,
                      int accent,
                      int accentMuted,
                      int textPrimary,
                      int textSecondary,
                      int outline)
    {
        this.id = id;
        this.overlay = overlay;
        this.container = container;
        this.header = header;
        this.categoryBackground = categoryBackground;
        this.categorySelected = categorySelected;
        this.moduleBackground = moduleBackground;
        this.moduleEnabled = moduleEnabled;
        this.moduleHover = moduleHover;
        this.settingBackground = settingBackground;
        this.accent = accent;
        this.accentMuted = accentMuted;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.outline = outline;
    }
    
    @Override
    public String getId()
    {
        return id;
    }
    
    @Override
    public int overlay()
    {
        return overlay;
    }
    
    @Override
    public int container()
    {
        return container;
    }
    
    @Override
    public int header()
    {
        return header;
    }
    
    @Override
    public int categoryBackground()
    {
        return categoryBackground;
    }
    
    @Override
    public int categorySelected()
    {
        return categorySelected;
    }
    
    @Override
    public int moduleBackground()
    {
        return moduleBackground;
    }
    
    @Override
    public int moduleEnabled()
    {
        return moduleEnabled;
    }
    
    @Override
    public int moduleHover()
    {
        return moduleHover;
    }
    
    @Override
    public int settingBackground()
    {
        return settingBackground;
    }
    
    @Override
    public int accent()
    {
        return accent;
    }
    
    @Override
    public int accentMuted()
    {
        return accentMuted;
    }
    
    @Override
    public int textPrimary()
    {
        return textPrimary;
    }
    
    @Override
    public int textSecondary()
    {
        return textSecondary;
    }
    
    @Override
    public int outline()
    {
        return outline;
    }
}

