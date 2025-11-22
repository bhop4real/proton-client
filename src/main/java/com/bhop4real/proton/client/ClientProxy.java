package com.bhop4real.proton.client;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.common.CommonProxy;
import com.bhop4real.proton.client.module.ModuleManager;
import com.bhop4real.proton.client.event.ClientEventHandler;
import com.bhop4real.proton.client.settings.SettingsManager;
import com.bhop4real.proton.client.command.CommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy
{
    private ModuleManager moduleManager;
    private SettingsManager settingsManager;
    private ClientEventHandler eventHandler;
    public void preInit(FMLPreInitializationEvent event)
    {
        Proton.logger.info("Initializing Proton Client...");
		// Use the parent of the config directory (i.e., .minecraft) as the base for Proton config
		java.io.File minecraftRoot = event.getModConfigurationDirectory() != null
				? event.getModConfigurationDirectory().getParentFile()
				: new java.io.File(".");
		settingsManager = new SettingsManager(minecraftRoot);
        
        // Initialize ProtonClient singleton
        ProtonClient.getInstance().setSettingsManager(settingsManager);
    }
    
    public void init(FMLInitializationEvent event)
    {
        moduleManager = new ModuleManager();
        eventHandler = new ClientEventHandler(moduleManager);
        new CommandHandler(); // Initialize for GuiCommand
        
        // Set managers in singleton
        ProtonClient.getInstance().setModuleManager(moduleManager);
        
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(eventHandler);
        MinecraftForge.EVENT_BUS.register(moduleManager);
        // CommandHandler doesn't need to be registered as event handler anymore
        
        Proton.logger.info("Proton Client initialized successfully!");
    }
    
    /**
     * Called when the game is shutting down - save all config
     */
    public void onShutdown()
    {
        Proton.logger.info("Saving Proton config on shutdown...");
        
        // Save all module settings
        if (moduleManager != null && settingsManager != null)
        {
            try
            {
                for (com.bhop4real.proton.client.module.Module module : moduleManager.getModules())
                {
                    settingsManager.saveModuleSettings(module);
                }
                Proton.logger.info("All module settings saved successfully");
            }
            catch (Exception e)
            {
                Proton.logger.error("Error saving module settings on shutdown", e);
            }
        }
        
        // Save any remaining dirty config
        if (settingsManager != null)
        {
            try
            {
                settingsManager.saveIfDirty();
                Proton.logger.info("Config saved successfully on shutdown");
            }
            catch (Exception e)
            {
                Proton.logger.error("Error saving config on shutdown", e);
            }
        }
    }
    
    public ModuleManager getModuleManager()
    {
        return moduleManager;
    }
    
    public SettingsManager getSettingsManager()
    {
        return settingsManager;
    }
    
    public ClientEventHandler getEventHandler()
    {
        return eventHandler;
    }
}

