package com.bhop4real.proton.client.gui.theme;

import java.util.Arrays;

/**
 * Defines the bundled color palettes for the ClickGUI. Each theme exposes all
 * shades used by the interface so the renderer can stay declarative and avoid
 * hard-coded hex values.
 */
public enum ClickGuiTheme implements ClickGuiThemeInterface
{
    MIDNIGHT("Midnight",
        0xB013161D, // overlay
        0xF0191F29, // container
        0xFF242E3D, // header
        0xFF1A222D, // category panel
        0xFF2B3647, // category selected
        0xFF121923, // module panel
        0xFF273448, // module enabled
        0x332D3C52, // module hover
        0xFF181F2C, // setting panel
        0xFF4DA3FF, // accent
        0x804DA3FF, // accent muted
        0xFFE6EEF9, // text primary
        0xFFA8B6C9, // text secondary
        0x3327313F  // outline/shadow
    ),

    AURORA("Aurora",
        0xB0111314,
        0xF0182024,
        0xFF253136,
        0xFF1B2629,
        0xFF2D3A3F,
        0xFF141D21,
        0xFF2F5650,
        0x332F5650,
        0xFF162225,
        0xFF66E0C6,
        0x8066E0C6,
        0xFFE6FBF5,
        0xFFB8D5CC,
        0x332B3A3B
    ),

    CRIMSON("Crimson",
        0xB0150D11,
        0xF0211A22,
        0xFF2A1E27,
        0xFF20161D,
        0xFF35212A,
        0xFF171016,
        0xFF513044,
        0x334B2435,
        0xFF1D141B,
        0xFFFF7AA2,
        0x80FF7AA2,
        0xFFFFECF5,
        0xFFE1BCCA,
        0x332D1C25
    ),

    DAWN("Dawn",
        0xB0101016,
        0xF01B1E28,
        0xFF262738,
        0xFF1B1D2A,
        0xFF30324A,
        0xFF131523,
        0xFF3E3F63,
        0x333E3F63,
        0xFF181A29,
        0xFF9F8CFF,
        0x809F8CFF,
        0xFFF0EEFF,
        0xFFC4C0F3,
        0x33282639
    );

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

    ClickGuiTheme(String id,
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

    public String getId()
    {
        return id;
    }

    public int overlay()
    {
        return overlay;
    }

    public int container()
    {
        return container;
    }

    public int header()
    {
        return header;
    }

    public int categoryBackground()
    {
        return categoryBackground;
    }

    public int categorySelected()
    {
        return categorySelected;
    }

    public int moduleBackground()
    {
        return moduleBackground;
    }

    public int moduleEnabled()
    {
        return moduleEnabled;
    }

    public int moduleHover()
    {
        return moduleHover;
    }

    public int settingBackground()
    {
        return settingBackground;
    }

    public int accent()
    {
        return accent;
    }

    public int accentMuted()
    {
        return accentMuted;
    }

    public int textPrimary()
    {
        return textPrimary;
    }

    public int textSecondary()
    {
        return textSecondary;
    }

    public int outline()
    {
        return outline;
    }

    public static ClickGuiTheme byId(String id)
    {
        if (id != null)
        {
            for (ClickGuiTheme theme : values())
            {
                if (theme.id.equalsIgnoreCase(id))
                {
                    return theme;
                }
            }
        }
        return MIDNIGHT;
    }

    public static String[] ids()
    {
        return Arrays.stream(values()).map(ClickGuiTheme::getId).toArray(String[]::new);
    }
}


