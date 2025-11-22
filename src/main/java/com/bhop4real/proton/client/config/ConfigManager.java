package com.bhop4real.proton.client.config;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.ModuleManager;
import com.bhop4real.proton.client.settings.SettingsManager;
import com.bhop4real.proton.client.util.io.FileIOHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manages multiple named configuration files.
 * Allows users to save, load, delete, and rename configs.
 */
public class ConfigManager
{
    private static final String CONFIGS_FOLDER = "configs";
    private static final String CONFIG_EXTENSION = ".json";
    
    private final File configsDir;
    private final Gson gson;
    private final SettingsManager settingsManager;
    
    public ConfigManager(SettingsManager settingsManager)
    {
        this.settingsManager = settingsManager;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configsDir = new File(settingsManager.getConfigDirectory(), CONFIGS_FOLDER);
        
        if (!FileIOHelper.ensureDirectory(configsDir, Proton.logger))
        {
            Proton.logger.error("Failed to create configs directory: {}", configsDir.getAbsolutePath());
        }
    }
    
    /**
     * Saves the current configuration to a named config file.
     * This includes all module states and settings.
     */
    public boolean saveConfig(String configName)
    {
        if (configName == null || configName.trim().isEmpty())
        {
            Proton.logger.error("Config name cannot be null or empty");
            return false;
        }
        
        String normalizedName = normalizeConfigName(configName);
        File configFile = new File(configsDir, normalizedName + CONFIG_EXTENSION);
        
        try
        {
            // Export current settings to a JsonObject
            JsonObject configData = exportCurrentConfig();
            
            // Save to file
            boolean saved = FileIOHelper.writeJsonElement(configFile, configData, gson, Proton.logger);
            if (saved)
            {
                Proton.logger.info("Config saved: {}", normalizedName);
            }
            return saved;
        }
        catch (Exception e)
        {
            Proton.logger.error("Failed to save config: {}", normalizedName, e);
            return false;
        }
    }
    
    /**
     * Loads a named config file and applies it to the current settings.
     */
    public boolean loadConfig(String configName)
    {
        if (configName == null || configName.trim().isEmpty())
        {
            Proton.logger.error("Config name cannot be null or empty");
            return false;
        }
        
        String normalizedName = normalizeConfigName(configName);
        File configFile = new File(configsDir, normalizedName + CONFIG_EXTENSION);
        
        if (!configFile.exists())
        {
            Proton.logger.error("Config file does not exist: {}", normalizedName);
            return false;
        }
        
        try
        {
            JsonObject configData = FileIOHelper.readJsonObject(configFile, gson, Proton.logger);
            if (configData == null)
            {
                Proton.logger.error("Failed to read config file: {}", normalizedName);
                return false;
            }
            
            // Import the config data
            importConfig(configData);
            
            Proton.logger.info("Config loaded: {}", normalizedName);
            return true;
        }
        catch (Exception e)
        {
            Proton.logger.error("Failed to load config: {}", normalizedName, e);
            return false;
        }
    }
    
    /**
     * Deletes a named config file.
     */
    public boolean deleteConfig(String configName)
    {
        if (configName == null || configName.trim().isEmpty())
        {
            return false;
        }
        
        String normalizedName = normalizeConfigName(configName);
        File configFile = new File(configsDir, normalizedName + CONFIG_EXTENSION);
        
        if (!configFile.exists())
        {
            return false;
        }
        
        boolean deleted = configFile.delete();
        if (deleted)
        {
            Proton.logger.info("Config deleted: {}", normalizedName);
        }
        return deleted;
    }
    
    /**
     * Renames a config file.
     */
    public boolean renameConfig(String oldName, String newName)
    {
        if (oldName == null || newName == null || oldName.trim().isEmpty() || newName.trim().isEmpty())
        {
            return false;
        }
        
        String normalizedOldName = normalizeConfigName(oldName);
        String normalizedNewName = normalizeConfigName(newName);
        
        File oldFile = new File(configsDir, normalizedOldName + CONFIG_EXTENSION);
        File newFile = new File(configsDir, normalizedNewName + CONFIG_EXTENSION);
        
        if (!oldFile.exists() || newFile.exists())
        {
            return false;
        }
        
        boolean renamed = oldFile.renameTo(newFile);
        if (renamed)
        {
            Proton.logger.info("Config renamed: {} -> {}", normalizedOldName, normalizedNewName);
        }
        return renamed;
    }
    
    /**
     * Lists all available config files.
     */
    public List<String> listConfigs()
    {
        List<String> configs = new ArrayList<>();
        
        if (!configsDir.exists() || !configsDir.isDirectory())
        {
            return configs;
        }
        
        File[] files = configsDir.listFiles((dir, name) -> name.endsWith(CONFIG_EXTENSION));
        if (files != null)
        {
            for (File file : files)
            {
                String name = file.getName();
                // Remove .json extension
                if (name.endsWith(CONFIG_EXTENSION))
                {
                    configs.add(name.substring(0, name.length() - CONFIG_EXTENSION.length()));
                }
            }
        }
        
        return configs;
    }
    
    /**
     * Exports the current configuration (all modules and settings) to a JsonObject.
     */
    private JsonObject exportCurrentConfig()
    {
        JsonObject configData = new JsonObject();
        
        // Export all module settings
        ProtonClient client = ProtonClient.getInstance();
        if (client != null && client.getModuleManager() != null)
        {
            ModuleManager moduleManager = client.getModuleManager();
            JsonObject modulesObj = new JsonObject();
            
            for (Module module : moduleManager.getModules())
            {
                JsonObject moduleObj = new JsonObject();
                moduleObj.addProperty("enabled", module.isEnabled());
                moduleObj.addProperty("keybind", module.getKeyBind());
                
                // Export module settings
                JsonObject settingsObj = new JsonObject();
                for (com.bhop4real.proton.client.module.settings.Setting setting : module.getSettings())
                {
                    try
                    {
                        String settingKey = normalize(setting.getName());
                        if (setting instanceof com.bhop4real.proton.client.module.settings.BooleanSetting)
                        {
                            settingsObj.addProperty(settingKey, ((com.bhop4real.proton.client.module.settings.BooleanSetting) setting).getValue());
                        }
                        else if (setting instanceof com.bhop4real.proton.client.module.settings.IntSetting)
                        {
                            settingsObj.addProperty(settingKey, ((com.bhop4real.proton.client.module.settings.IntSetting) setting).getValue());
                        }
                        else if (setting instanceof com.bhop4real.proton.client.module.settings.DoubleSetting)
                        {
                            settingsObj.addProperty(settingKey, ((com.bhop4real.proton.client.module.settings.DoubleSetting) setting).getValue());
                        }
                        else if (setting instanceof com.bhop4real.proton.client.module.settings.EnumSetting)
                        {
                            settingsObj.addProperty(settingKey, ((com.bhop4real.proton.client.module.settings.EnumSetting) setting).getValue());
                        }
                        else if (setting instanceof com.bhop4real.proton.client.module.settings.ColorSetting)
                        {
                            settingsObj.addProperty(settingKey, ((com.bhop4real.proton.client.module.settings.ColorSetting) setting).getColor());
                        }
                        else if (setting instanceof com.bhop4real.proton.client.module.settings.StringSetting)
                        {
                            settingsObj.addProperty(settingKey, ((com.bhop4real.proton.client.module.settings.StringSetting) setting).getValue());
                        }
                    }
                    catch (Exception e)
                    {
                        Proton.logger.warn("Failed to export setting '{}' for module {}", setting.getName(), module.getName(), e);
                    }
                }
                
                if (settingsObj.entrySet().size() > 0)
                {
                    moduleObj.add("settings", settingsObj);
                }
                
                modulesObj.add(normalize(module.getName()), moduleObj);
            }
            
            configData.add("modules", modulesObj);
        }
        
        // Export other settings from SettingsManager
        // This includes things like theme preferences, etc.
        try
        {
            JsonObject currentRoot = settingsManager.exportFullConfig();
            if (currentRoot != null)
            {
                // Copy non-module settings
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : currentRoot.entrySet())
                {
                    String key = entry.getKey();
                    if (!key.equals("modules"))
                    {
                        configData.add(key, entry.getValue());
                    }
                }
            }
        }
        catch (Exception e)
        {
            Proton.logger.warn("Failed to export additional settings", e);
        }
        
        return configData;
    }
    
    /**
     * Imports a configuration from a JsonObject and applies it.
     */
    private void importConfig(JsonObject configData)
    {
        ProtonClient client = ProtonClient.getInstance();
        if (client == null || client.getModuleManager() == null)
        {
            Proton.logger.error("Cannot import config: ModuleManager not available");
            return;
        }
        
        ModuleManager moduleManager = client.getModuleManager();
        
        // Import module settings
        if (configData.has("modules") && configData.get("modules").isJsonObject())
        {
            JsonObject modulesObj = configData.getAsJsonObject("modules");
            
            for (Module module : moduleManager.getModules())
            {
                String moduleKey = normalize(module.getName());
                if (!modulesObj.has(moduleKey) || !modulesObj.get(moduleKey).isJsonObject())
                {
                    continue;
                }
                
                JsonObject moduleObj = modulesObj.getAsJsonObject(moduleKey);
                
                try
                {
                    // Load module enabled state
                    if (moduleObj.has("enabled"))
                    {
                        boolean enabled = moduleObj.get("enabled").getAsBoolean();
                        if (enabled != module.isEnabled())
                        {
                            module.setEnabled(enabled, false);
                        }
                    }
                    
                    // Load keybind
                    if (moduleObj.has("keybind"))
                    {
                        int keybind = moduleObj.get("keybind").getAsInt();
                        module.setKeyBind(keybind);
                    }
                    
                    // Load module settings
                    if (moduleObj.has("settings") && moduleObj.get("settings").isJsonObject())
                    {
                        JsonObject settingsObj = moduleObj.getAsJsonObject("settings");
                        for (com.bhop4real.proton.client.module.settings.Setting setting : module.getSettings())
                        {
                            String settingKey = normalize(setting.getName());
                            if (!settingsObj.has(settingKey))
                            {
                                continue;
                            }
                            
                            try
                            {
                                com.google.gson.JsonElement value = settingsObj.get(settingKey);
                                if (setting instanceof com.bhop4real.proton.client.module.settings.BooleanSetting && value.isJsonPrimitive())
                                {
                                    ((com.bhop4real.proton.client.module.settings.BooleanSetting) setting).setValue(value.getAsBoolean());
                                }
                                else if (setting instanceof com.bhop4real.proton.client.module.settings.IntSetting && value.isJsonPrimitive())
                                {
                                    ((com.bhop4real.proton.client.module.settings.IntSetting) setting).setValue(value.getAsInt());
                                }
                                else if (setting instanceof com.bhop4real.proton.client.module.settings.DoubleSetting && value.isJsonPrimitive())
                                {
                                    ((com.bhop4real.proton.client.module.settings.DoubleSetting) setting).setValue(value.getAsDouble());
                                }
                                else if (setting instanceof com.bhop4real.proton.client.module.settings.EnumSetting && value.isJsonPrimitive())
                                {
                                    ((com.bhop4real.proton.client.module.settings.EnumSetting) setting).setValue(value.getAsString());
                                }
                                else if (setting instanceof com.bhop4real.proton.client.module.settings.ColorSetting && value.isJsonPrimitive())
                                {
                                    ((com.bhop4real.proton.client.module.settings.ColorSetting) setting).setColor(value.getAsInt());
                                }
                                else if (setting instanceof com.bhop4real.proton.client.module.settings.StringSetting && value.isJsonPrimitive())
                                {
                                    ((com.bhop4real.proton.client.module.settings.StringSetting) setting).setValue(value.getAsString());
                                }
                            }
                            catch (Exception e)
                            {
                                Proton.logger.warn("Failed to import setting '{}' for module {}", setting.getName(), module.getName(), e);
                            }
                        }
                    }
                    
                    // Save the module settings after loading
                    settingsManager.saveModuleSettings(module);
                }
                catch (Exception e)
                {
                    Proton.logger.error("Failed to import module: {}", module.getName(), e);
                }
            }
        }
        
        // Import other settings
        try
        {
            settingsManager.importFullConfig(configData);
        }
        catch (Exception e)
        {
            Proton.logger.warn("Failed to import additional settings", e);
        }
    }
    
    private static String normalizeConfigName(String name)
    {
        if (name == null)
        {
            return "";
        }
        // Remove invalid filename characters and normalize
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_\\-]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "");
    }
    
    private static String normalize(String key)
    {
        return key == null ? "" : key.toLowerCase(Locale.ROOT).replace(" ", "_");
    }
}

