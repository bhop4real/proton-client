package com.bhop4real.proton.client.settings;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.*;
import com.bhop4real.proton.client.util.io.FileIOHelper;
import com.google.gson.*;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * Central configuration manager backed by JSON files stored under the player's .minecraft directory.
 * Supports hierarchical access (e.g. "client.ui.theme"), module persistence, and typed helpers.
 */
public class SettingsManager
{
    private static final String ROOT_FOLDER = "proton";
    private static final String CONFIG_FILE = "config.json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configDir;
    private final File configFile;

    private JsonObject root;
    private boolean dirty;

    public SettingsManager()
    {
		// Derive base .minecraft directory without relying on Minecraft.mcDataDir (obf-unsafe)
		File configDirectory = Loader.instance().getConfigDir(); // points to .minecraft/config
		File minecraftRoot = configDirectory != null ? configDirectory.getParentFile() : new File(".");
		this.configDir = new File(minecraftRoot, ROOT_FOLDER);
        if (!FileIOHelper.ensureDirectory(configDir, Proton.logger))
        {
            Proton.logger.error("Failed to initialise Proton config directory: {}", configDir.getAbsolutePath());
        }

        this.configFile = new File(configDir, CONFIG_FILE);
        load();
    }

	/**
	 * Preferred constructor for environments where the base directory is known.
	 * Pass `.minecraft` as baseDir (e.g., event.getModConfigurationDirectory().getParentFile()).
	 */
	public SettingsManager(File baseDir)
	{
		File minecraftRoot = baseDir != null ? baseDir : new File(".");
		this.configDir = new File(minecraftRoot, ROOT_FOLDER);
		if (!FileIOHelper.ensureDirectory(configDir, Proton.logger))
		{
			Proton.logger.error("Failed to initialise Proton config directory: {}", configDir.getAbsolutePath());
		}

		this.configFile = new File(configDir, CONFIG_FILE);
		load();
	}

    public File getConfigDirectory()
    {
        return configDir;
    }

    private synchronized void load()
    {
        if (!configFile.exists())
        {
            root = new JsonObject();
            save();
            return;
        }

        try
        {
            JsonObject loaded = FileIOHelper.readJsonObject(configFile, gson, Proton.logger);
            root = loaded != null ? loaded : new JsonObject();
        }
        catch (IOException | JsonParseException e)
        {
            Proton.logger.error("Failed to read config, creating fresh file", e);
            createCorruptionBackup();
            root = new JsonObject();
            save();
        }
    }

    public synchronized boolean save()
    {
        if (configFile == null)
        {
            Proton.logger.error("Config file is null, cannot save");
            return false;
        }

        // Ensure directory exists first (like ProfileManager.java)
        File parentDir = configFile.getParentFile();
        if (parentDir != null && !parentDir.exists())
        {
            boolean dirCreated = parentDir.mkdirs();
            if (!dirCreated && !parentDir.exists())
            {
                Proton.logger.error("Failed to create config directory: {}", parentDir.getAbsolutePath());
                return false;
            }
        }

        // Try FileIOHelper first (atomic write with fallback)
        boolean saved = FileIOHelper.writeJsonElement(configFile, root != null ? root : new JsonObject(), gson, Proton.logger);
        
        if (!saved)
        {
            // Fallback: direct write like reference ProfileManager.java
            try (FileWriter fileWriter = new FileWriter(configFile))
            {
                gson.toJson(root != null ? root : new JsonObject(), fileWriter);
                fileWriter.flush();
                saved = true;
                Proton.logger.debug("Successfully saved config file using direct write");
            }
            catch (Exception e)
            {
                Proton.logger.error("Failed to save config file: {}", configFile.getAbsolutePath(), e);
                saved = false;
            }
        }

        if (saved)
        {
            dirty = false;
        }
        return saved;
    }

    public synchronized void saveIfDirty()
    {
        if (dirty)
        {
            save();
        }
    }

    private void markDirty()
    {
        dirty = true;
    }

    private void createCorruptionBackup()
    {
        FileIOHelper.createTimestampedCopy(configFile, "corrupt", Proton.logger);
    }

    // ---------------------------------------------------------------------
    // Generic path helpers
    // ---------------------------------------------------------------------

    private JsonObject getRoot()
    {
        if (root == null)
        {
            root = new JsonObject();
        }
        return root;
    }

    private JsonObject ensureObject(JsonObject parent, String key)
    {
        if (!parent.has(key) || !parent.get(key).isJsonObject())
        {
            JsonObject child = new JsonObject();
            parent.add(key, child);
            return child;
        }
        return parent.getAsJsonObject(key);
    }

    private JsonObject getParentForPath(String path, boolean create)
    {
        if (path == null || path.trim().isEmpty())
        {
            return null;
        }

        String[] segments = path.split("\\.");
        JsonObject current = getRoot();
        for (int i = 0; i < segments.length - 1; i++)
        {
            String segment = segments[i];
            if (!current.has(segment) || !current.get(segment).isJsonObject())
            {
                if (!create)
                {
                    return null;
                }
                current.add(segment, new JsonObject());
            }
            current = current.getAsJsonObject(segment);
        }
        return current;
    }

    private String lastSegment(String path)
    {
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private JsonElement getElement(String path)
    {
        JsonObject parent = getParentForPath(path, false);
        if (parent == null)
        {
            return null;
        }
        return parent.get(lastSegment(path));
    }

    private void setValue(String path, JsonElement value)
    {
        if (path == null || value == null)
        {
            return;
        }
        JsonObject parent = getParentForPath(path, true);
        if (parent == null)
        {
            return;
        }
        parent.add(lastSegment(path), value);
        markDirty();
    }

    private static String normalize(String key)
    {
        return key == null ? "" : key.toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    // ---------------------------------------------------------------------
    // Primitive helpers
    // ---------------------------------------------------------------------

    public void setString(String path, String value)
    {
        setValue(path, new JsonPrimitive(value));
        saveIfDirty();
    }

    public void setBoolean(String path, boolean value)
    {
        setValue(path, new JsonPrimitive(value));
        saveIfDirty();
    }

    public void setInt(String path, int value)
    {
        setValue(path, new JsonPrimitive(value));
        saveIfDirty();
    }

    public void setFloat(String path, float value)
    {
        setValue(path, new JsonPrimitive(value));
        saveIfDirty();
    }

    public String getString(String path, String defaultValue)
    {
        JsonElement element = getElement(path);
        if (element != null && element.isJsonPrimitive())
        {
            try
            {
                return element.getAsString();
            }
            catch (Exception ignored)
            {
            }
        }
        return defaultValue;
    }

    public boolean getBoolean(String path, boolean defaultValue)
    {
        JsonElement element = getElement(path);
        if (element != null && element.isJsonPrimitive())
        {
            try
            {
                return element.getAsBoolean();
            }
            catch (Exception ignored)
            {
            }
        }
        return defaultValue;
    }

    public int getInt(String path, int defaultValue)
    {
        JsonElement element = getElement(path);
        if (element != null && element.isJsonPrimitive())
        {
            try
            {
                return element.getAsInt();
            }
            catch (Exception ignored)
            {
            }
        }
        return defaultValue;
    }

    public float getFloat(String path, float defaultValue)
    {
        JsonElement element = getElement(path);
        if (element != null && element.isJsonPrimitive())
        {
            try
            {
                return element.getAsFloat();
            }
            catch (Exception ignored)
            {
            }
        }
        return defaultValue;
    }

    // ---------------------------------------------------------------------
    // Legacy-style helpers used throughout the codebase
    // ---------------------------------------------------------------------

    public void setSetting(String key, String value)
    {
        setValue("settings." + normalize(key), new JsonPrimitive(value));
        saveIfDirty();
    }

    public String getSetting(String key, String defaultValue)
    {
        return getString("settings." + normalize(key), defaultValue);
    }

    public boolean getBooleanSetting(String key, boolean defaultValue)
    {
        return getBoolean("settings." + normalize(key), defaultValue);
    }

    public int getIntSetting(String key, int defaultValue)
    {
        return getInt("settings." + normalize(key), defaultValue);
    }

    public float getFloatSetting(String key, float defaultValue)
    {
        return getFloat("settings." + normalize(key), defaultValue);
    }

    public void setKeyBind(String moduleName, int keyCode)
    {
        setValue("modules." + normalize(moduleName) + ".keybind", new JsonPrimitive(keyCode));
        saveIfDirty();
    }

    public int getKeyBind(String moduleName, int defaultValue)
    {
        return getInt("modules." + normalize(moduleName) + ".keybind", defaultValue);
    }

    public void setModuleEnabled(String moduleName, boolean enabled)
    {
        setValue("modules." + normalize(moduleName) + ".enabled", new JsonPrimitive(enabled));
        saveIfDirty();
    }

    public boolean getModuleEnabled(String moduleName, boolean defaultValue)
    {
        return getBoolean("modules." + normalize(moduleName) + ".enabled", defaultValue);
    }

    // ---------------------------------------------------------------------
    // Module persistence
    // ---------------------------------------------------------------------

    public synchronized void saveModuleSettings(Module module)
    {
        if (module == null)
        {
            return;
        }

        JsonObject modulesObj = ensureObject(getRoot(), "modules");
        String key = normalize(module.getName());
        JsonObject moduleObj = modulesObj.has(key) && modulesObj.get(key).isJsonObject()
                ? modulesObj.getAsJsonObject(key)
                : new JsonObject();

        moduleObj.addProperty("enabled", module.isEnabled());
        // Always save keybind, even if it's 0 (to clear keybind)
        moduleObj.addProperty("keybind", module.getKeyBind());

        JsonObject settingsObj = new JsonObject();
        for (Setting setting : module.getSettings())
        {
            try
            {
                String settingKey = normalize(setting.getName());
                if (setting instanceof BooleanSetting)
                {
                    settingsObj.addProperty(settingKey, ((BooleanSetting) setting).getValue());
                }
                else if (setting instanceof IntSetting)
                {
                    settingsObj.addProperty(settingKey, ((IntSetting) setting).getValue());
                }
                        else if (setting instanceof com.bhop4real.proton.client.module.settings.DoubleSetting)
                        {
                            settingsObj.addProperty(settingKey, ((com.bhop4real.proton.client.module.settings.DoubleSetting) setting).getValue());
                        }
                else if (setting instanceof EnumSetting)
                {
                    settingsObj.addProperty(settingKey, ((EnumSetting) setting).getValue());
                }
                else if (setting instanceof ColorSetting)
                {
                    settingsObj.addProperty(settingKey, ((ColorSetting) setting).getColor());
                }
                else if (setting instanceof StringSetting)
                {
                    settingsObj.addProperty(settingKey, ((StringSetting) setting).getValue());
                }
            }
            catch (Exception e)
            {
                Proton.logger.warn("Failed to persist setting '" + setting.getName() + "' for module " + module.getName(), e);
            }
        }

        if (settingsObj.entrySet().isEmpty())
        {
            moduleObj.remove("settings");
        }
        else
        {
            moduleObj.add("settings", settingsObj);
        }

        modulesObj.add(key, moduleObj);
        
        // Mark as dirty and save using the centralized save() method
        markDirty();
        save();
    }

    public void loadModuleSettings(Module module)
    {
        if (module == null)
        {
            return;
        }

        JsonObject modulesObj = getRoot().has("modules") && getRoot().get("modules").isJsonObject()
                ? getRoot().getAsJsonObject("modules")
                : null;

        if (modulesObj == null)
        {
            if (module.isDefaultEnabled() && !module.isEnabled())
            {
                module.setEnabled(true, false);
            }
            return;
        }

        JsonElement moduleElement = modulesObj.get(normalize(module.getName()));
        if (moduleElement == null || !moduleElement.isJsonObject())
        {
            if (module.isDefaultEnabled() && !module.isEnabled())
            {
                module.setEnabled(true, false);
            }
            return;
        }

        JsonObject moduleObj = moduleElement.getAsJsonObject();

        try
        {
            if (moduleObj.has("enabled"))
            {
                boolean enabled = moduleObj.get("enabled").getAsBoolean();
                if (enabled != module.isEnabled())
                {
                    module.setEnabled(enabled, false);
                }
            }

            if (moduleObj.has("keybind"))
            {
                int key = moduleObj.get("keybind").getAsInt();
                // Always update keybind if it's different, even if it's 0 (to clear keybind)
                int currentKey = module.getKeyBind();
                if (key != currentKey)
                {
                    // Get ModuleManager to ensure it's available
                    com.bhop4real.proton.client.ProtonClient client = com.bhop4real.proton.client.ProtonClient.getInstance();
                    if (client != null && client.getModuleManager() != null)
                    {
                        // Directly update the keybind field first to avoid issues with setKeyBind's saveState call
                        // Then manually update the ModuleManager map
                        try
                        {
                            java.lang.reflect.Field keyBindField = module.getClass().getSuperclass().getDeclaredField("keyBind");
                            keyBindField.setAccessible(true);
                            keyBindField.setInt(module, key);
                            
                            // Now update the ModuleManager map with the correct old and new values
                            client.getModuleManager().updateModuleKeyBind(module, currentKey, key);
                        }
                        catch (Exception e)
                        {
                            // Fallback to using setKeyBind if reflection fails
                            Proton.logger.warn("Failed to set keybind via reflection for module: " + module.getName() + ", using setKeyBind instead", e);
                            module.setKeyBind(key);
                        }
                    }
                    else
                    {
                        // ModuleManager not available yet - just set the field directly
                        // It will be registered when ModuleManager is initialized
                        try
                        {
                            java.lang.reflect.Field keyBindField = module.getClass().getSuperclass().getDeclaredField("keyBind");
                            keyBindField.setAccessible(true);
                            keyBindField.setInt(module, key);
                        }
                        catch (Exception e)
                        {
                            Proton.logger.error("Failed to set keybind for module: " + module.getName(), e);
                        }
                    }
                }
            }

            if (moduleObj.has("settings"))
            {
                JsonObject settingsObj = moduleObj.getAsJsonObject("settings");
                for (Setting setting : module.getSettings())
                {
                    String settingKey = normalize(setting.getName());
                    if (!settingsObj.has(settingKey))
                    {
                        continue;
                    }

                    JsonElement value = settingsObj.get(settingKey);
                    try
                    {
                        if (setting instanceof BooleanSetting && value.isJsonPrimitive())
                        {
                            ((BooleanSetting) setting).setValue(value.getAsBoolean());
                        }
                        else if (setting instanceof IntSetting && value.isJsonPrimitive())
                        {
                            ((IntSetting) setting).setValue(value.getAsInt());
                        }
                        else if (setting instanceof com.bhop4real.proton.client.module.settings.DoubleSetting && value.isJsonPrimitive())
                        {
                            ((com.bhop4real.proton.client.module.settings.DoubleSetting) setting).setValue(value.getAsDouble());
                        }
                        else if (setting instanceof EnumSetting && value.isJsonPrimitive())
                        {
                            ((EnumSetting) setting).setValue(value.getAsString());
                        }
                        else if (setting instanceof ColorSetting && value.isJsonPrimitive())
                        {
                            ((ColorSetting) setting).setColor(value.getAsInt());
                        }
                        else if (setting instanceof StringSetting && value.isJsonPrimitive())
                        {
                            ((StringSetting) setting).setValue(value.getAsString());
                        }
                    }
                    catch (Exception e)
                    {
                        Proton.logger.warn("Failed to load setting '" + setting.getName() + "' for module " + module.getName(), e);
                    }
                }
            }
        }
        catch (Exception e)
        {
            Proton.logger.error("Failed to load module data for " + module.getName(), e);
        }
    }
    
    /**
     * Exports the full configuration as a JsonObject.
     * This includes all settings, modules, and other configuration data.
     */
    public JsonObject exportFullConfig()
    {
        // Return a deep copy of the root to avoid external modifications
        if (root == null)
        {
            return new JsonObject();
        }
        
        try
        {
            String json = gson.toJson(root);
            JsonObject copy = gson.fromJson(json, JsonObject.class);
            return copy;
        }
        catch (Exception e)
        {
            Proton.logger.error("Failed to export full config", e);
            return new JsonObject();
        }
    }
    
    /**
     * Imports a full configuration from a JsonObject.
     * This will merge the imported config with the current config.
     */
    public void importFullConfig(JsonObject configData)
    {
        if (configData == null)
        {
            return;
        }
        
        try
        {
            // Merge the imported config with current root
            JsonObject currentRoot = getRoot();
            
            // Copy all non-module settings from configData
            for (java.util.Map.Entry<String, JsonElement> entry : configData.entrySet())
            {
                String key = entry.getKey();
                if (!key.equals("modules"))
                {
                    currentRoot.add(key, entry.getValue());
                }
            }
            
            markDirty();
            save();
        }
        catch (Exception e)
        {
            Proton.logger.error("Failed to import full config", e);
        }
    }
}
