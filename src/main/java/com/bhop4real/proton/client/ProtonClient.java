package com.bhop4real.proton.client;

import com.bhop4real.proton.client.module.ModuleManager;
import com.bhop4real.proton.client.settings.SettingsManager;

/**
 * Singleton accessor for client-side managers
 */
public class ProtonClient
{
    private static ProtonClient instance;
    
    private ModuleManager moduleManager;
    private SettingsManager settingsManager;
    
    private ProtonClient()
    {
        // Private constructor for singleton
    }
    
    public static ProtonClient getInstance()
    {
        if (instance == null)
        {
            synchronized (ProtonClient.class)
            {
                if (instance == null)
                {
                    instance = new ProtonClient();
                }
            }
        }
        return instance;
    }
    
    public void setModuleManager(ModuleManager moduleManager)
    {
        this.moduleManager = moduleManager;
    }
    
    public void setSettingsManager(SettingsManager settingsManager)
    {
        this.settingsManager = settingsManager;
    }
    
    public ModuleManager getModuleManager()
    {
        return moduleManager;
    }
    
    public SettingsManager getSettingsManager()
    {
        return settingsManager;
    }
    
    /**
     * Checks if all required managers are initialized
     */
    public boolean isInitialized()
    {
        return moduleManager != null && settingsManager != null;
    }
}
