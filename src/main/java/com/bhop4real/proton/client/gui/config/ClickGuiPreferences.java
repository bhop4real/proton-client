package com.bhop4real.proton.client.gui.config;

import com.bhop4real.proton.client.gui.theme.ClickGuiTheme;
import com.bhop4real.proton.client.gui.theme.ClickGuiThemeInterface;

import java.util.Locale;

/**
 * Immutable snapshot of all runtime preferences that influence the Click GUI rendering.
 * This allows different modules to expose the same contract without the GUI needing
 * to know about their internal setting structures.
 */
public final class ClickGuiPreferences
{
    private final ClickGuiThemeInterface theme;
    private final int backgroundBrightness;
    private final boolean blurEnabled;
    private final int blurRadius;
    private final BlurMode blurMode;
    private final int openAnimationDuration;
    private final int expandAnimationDuration;

    private ClickGuiPreferences(Builder builder)
    {
        this.theme = builder.theme;
        this.backgroundBrightness = builder.backgroundBrightness;
        this.blurEnabled = builder.blurEnabled;
        this.blurRadius = builder.blurRadius;
        this.blurMode = builder.blurMode;
        this.openAnimationDuration = builder.openAnimationDuration;
        this.expandAnimationDuration = builder.expandAnimationDuration;
    }

    public ClickGuiThemeInterface theme()
    {
        return theme;
    }

    public int backgroundBrightness()
    {
        return backgroundBrightness;
    }

    public boolean blurEnabled()
    {
        return blurEnabled;
    }

    public int blurRadius()
    {
        return blurRadius;
    }

    public BlurMode blurMode()
    {
        return blurMode;
    }
    
    public int openAnimationDuration()
    {
        return openAnimationDuration;
    }
    
    public int expandAnimationDuration()
    {
        return expandAnimationDuration;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static ClickGuiPreferences defaults()
    {
        return builder().build();
    }

    public static final class Builder
    {
        private ClickGuiThemeInterface theme = ClickGuiTheme.MIDNIGHT;
        private int backgroundBrightness = 136;
        private boolean blurEnabled = true;
        private int blurRadius = 12;
        private BlurMode blurMode = BlurMode.GAUSSIAN;
        private int openAnimationDuration = 280;
        private int expandAnimationDuration = 200;

        private Builder()
        {
        }

        public Builder theme(ClickGuiThemeInterface theme)
        {
            if (theme != null)
            {
                this.theme = theme;
            }
            return this;
        }

        public Builder backgroundBrightness(int brightness)
        {
            this.backgroundBrightness = Math.max(0, Math.min(255, brightness));
            return this;
        }

        public Builder blurEnabled(boolean enabled)
        {
            this.blurEnabled = enabled;
            return this;
        }

        public Builder blurRadius(int radius)
        {
            this.blurRadius = Math.max(0, radius);
            return this;
        }

        public Builder blurMode(BlurMode mode)
        {
            if (mode != null)
            {
                this.blurMode = mode;
            }
            return this;
        }
        
        public Builder openAnimationDuration(int duration)
        {
            this.openAnimationDuration = Math.max(100, Math.min(1000, duration));
            return this;
        }
        
        public Builder expandAnimationDuration(int duration)
        {
            this.expandAnimationDuration = Math.max(50, Math.min(500, duration));
            return this;
        }

        public ClickGuiPreferences build()
        {
            return new ClickGuiPreferences(this);
        }
    }

    public enum BlurMode
    {
        GAUSSIAN("Gaussian"),
        KAWASE("Kawase");

        private final String id;

        BlurMode(String id)
        {
            this.id = id;
        }

        public String getId()
        {
            return id;
        }

        public static BlurMode byId(String id)
        {
            if (id == null || id.isEmpty())
            {
                return GAUSSIAN;
            }

            String normalized = id.trim().toLowerCase(Locale.ROOT);
            for (BlurMode mode : values())
            {
                if (mode.id.toLowerCase(Locale.ROOT).equals(normalized))
                {
                    return mode;
                }
            }
            return GAUSSIAN;
        }

        public static String[] ids()
        {
            BlurMode[] modes = values();
            String[] ids = new String[modes.length];
            for (int i = 0; i < modes.length; i++)
            {
                ids[i] = modes[i].id;
            }
            return ids;
        }
    }
}

