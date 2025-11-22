package com.bhop4real.proton.client.module;

import com.bhop4real.proton.client.module.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all modules with improved lifecycle management
 */
public abstract class Module
{
    protected static final Minecraft mc = Minecraft.getMinecraft();
    
    private final String name;
    private final String[] aliases;
    private final String description;
    private final Category category;
    
    private int keyBind;
    private boolean enabled;
    private boolean visible;
    private final List<Setting> settings;
    
    // Callbacks for module lifecycle events
    private Runnable onEnableCallback;
    private Runnable onDisableCallback;
    private Runnable onToggleCallback;
    
    public Module(String name, String description, Category category, int keyBind)
    {
        this(name, new String[0], description, category, keyBind);
    }
    
    public Module(String name, String[] aliases, String description, Category category, int keyBind)
    {
        if (name == null || name.trim().isEmpty())
        {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        
        this.name = name;
        this.aliases = aliases != null ? aliases : new String[0];
        this.description = description != null ? description : "";
        this.category = category != null ? category : Category.MISC;
        this.keyBind = keyBind;
        this.enabled = false;
        this.visible = true;
        this.settings = new ArrayList<>();
    }
    
    /**
     * Called when the module is enabled
     */
    public void onEnable()
    {
        // Override in subclasses
    }
    
    /**
     * Called when the module is disabled
     */
    public void onDisable()
    {
        // Override in subclasses
    }
    
    /**
     * Called every client tick when the module is enabled
     */
    public void onUpdate()
    {
        // Override in subclasses
    }
    
    /**
     * Called during render events when the module is enabled
     */
    public void onRender()
    {
        // Override in subclasses
    }
    
    /**
     * Called during RenderWorldLastEvent for world-space rendering
     */
    public void onRender3D(float partialTicks)
    {
        // Override in subclasses
    }
    
    /**
     * Toggles the module on/off
     */
    public void toggle()
    {
        setEnabled(!enabled);
    }
    
    /**
     * Sets the enabled state of the module
     */
    public void setEnabled(boolean enabled)
    {
        setEnabled(enabled, true);
    }
    
    /**
     * Sets the enabled state of the module with optional feedback
     * @param enabled Whether to enable or disable
     * @param showFeedback Whether to show notifications and play sounds
     */
    public void setEnabled(boolean enabled, boolean showFeedback)
    {
        if (this.enabled == enabled) return; // No change needed
        
        this.enabled = enabled;
        
        // Save enabled state and all settings to config
        saveState();
        
        // Play sound only if feedback is requested (not during loading)
        if (showFeedback)
        {
            // Play sound only (notifications removed by request)
            if (enabled)
            {
                com.bhop4real.proton.client.util.audio.SoundUtil.playEnableSound(this.name);
            }
            else
            {
                com.bhop4real.proton.client.util.audio.SoundUtil.playDisableSound(this.name);
            }

            // Call toggle callback
            if (onToggleCallback != null)
            {
                try
                {
                    onToggleCallback.run();
                }
                catch (Exception e)
                {
                    com.bhop4real.proton.Proton.logger.error("Error in toggle callback for module: " + name, e);
                }
            }
        }
        
        // Call lifecycle methods
        if (enabled)
        {
            try
        {
            onEnable();
                if (onEnableCallback != null)
                {
                    onEnableCallback.run();
                }
            }
            catch (Exception e)
            {
                com.bhop4real.proton.Proton.logger.error("Error enabling module: " + name, e);
            }
        }
        else
        {
            try
        {
            onDisable();
                if (onDisableCallback != null)
                {
                    onDisableCallback.run();
                }
            }
            catch (Exception e)
            {
                com.bhop4real.proton.Proton.logger.error("Error disabling module: " + name, e);
            }
        }
    }
    
    /**
     * Saves the current state of the module to config
     */
    private void saveState()
    {
        com.bhop4real.proton.client.ProtonClient client = com.bhop4real.proton.client.ProtonClient.getInstance();
        if (client != null && client.getSettingsManager() != null)
        {
            try
            {
                client.getSettingsManager().saveModuleSettings(this);
            }
            catch (Exception e)
            {
                com.bhop4real.proton.Proton.logger.error("Failed to save module settings: " + name, e);
            }
        }
    }
    
    // Getters and Setters
    public String getName()
    {
        return name;
    }
    
    public String[] getAliases()
    {
        return aliases.clone(); // Return copy to prevent external modification
    }
    
    public boolean hasAlias(String alias)
    {
        if (alias == null) return false;
        for (String a : aliases)
        {
            if (alias.equalsIgnoreCase(a))
            {
                return true;
            }
        }
        return false;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public Category getCategory()
    {
        return category;
    }
    
    public int getKeyBind()
    {
        return keyBind;
    }
    
    public void setKeyBind(int keyBind)
    {
        int oldKeyBind = this.keyBind;
        this.keyBind = keyBind;
        
        // Notify ModuleManager of keybind change
        com.bhop4real.proton.client.ProtonClient client = com.bhop4real.proton.client.ProtonClient.getInstance();
        if (client != null && client.getModuleManager() != null)
        {
            client.getModuleManager().updateModuleKeyBind(this, oldKeyBind, keyBind);
        }
        
        // Save keybind to config
        saveState();
    }
    
    public boolean isEnabled()
    {
        return enabled;
    }
    
    public boolean isVisible()
    {
        return visible;
    }
    
    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }
    
    public List<Setting> getSettings()
    {
        return Collections.unmodifiableList(settings);
    }
    
    public Setting getSettingByName(String name)
    {
        if (name == null) return null;
        for (Setting setting : settings)
        {
            if (name.equalsIgnoreCase(setting.getName()))
            {
                return setting;
            }
        }
        return null;
    }
    
    public void addSetting(Setting setting)
    {
        if (setting != null && !settings.contains(setting))
    {
        settings.add(setting);
        }
    }
    
    public void removeSetting(Setting setting)
    {
        settings.remove(setting);
    }
    
    // Callback setters for external integrations
    public void setOnEnableCallback(Runnable callback)
    {
        this.onEnableCallback = callback;
    }
    
    public void setOnDisableCallback(Runnable callback)
    {
        this.onDisableCallback = callback;
    }
    
    public void setOnToggleCallback(Runnable callback)
    {
        this.onToggleCallback = callback;
    }
    
    /**
     * Allows modules to declare their default enabled state when no config exists.
     */
    public boolean isDefaultEnabled()
    {
        return false;
    }

    public enum Category
    {
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        RENDER("Render"),
        PLAYER("Player"),
        WORLD("World"),
        MISC("Misc"),
        THEMES("Themes"),
        CONFIG("Config");
        
        private final String name;
        
        Category(String name)
        {
            this.name = name;
        }
        
        public String getName()
        {
            return name;
        }
    }
}
