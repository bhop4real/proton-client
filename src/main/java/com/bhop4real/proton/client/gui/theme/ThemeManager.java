package com.bhop4real.proton.client.gui.theme;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.util.io.FileIOHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages custom themes loaded from JSON files.
 * Loads themes from the themes directory and provides access to both built-in and custom themes.
 */
public class ThemeManager
{
    private static final String THEMES_DIR = "themes";
    private static final Gson gson = new Gson();
    private static final Map<String, CustomTheme> customThemes = new ConcurrentHashMap<>();
    private static boolean themesLoaded = false;
    
    /**
     * Loads all custom themes from the themes directory
     */
    public static void loadCustomThemes()
    {
        if (themesLoaded)
        {
            return;
        }
        
        themesLoaded = true;
        customThemes.clear();
        
        try
        {
            ProtonClient client = ProtonClient.getInstance();
            if (client == null || client.getSettingsManager() == null)
            {
                return;
            }
            
            File configDir = client.getSettingsManager().getConfigDirectory();
            File themesDir = new File(configDir, THEMES_DIR);
            
            if (!FileIOHelper.ensureDirectory(themesDir, Proton.logger))
            {
                Proton.logger.warn("Failed to create themes directory: {}", themesDir.getAbsolutePath());
                return;
            }
            
            File[] themeFiles = themesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
            if (themeFiles == null)
            {
                return;
            }
            
            for (File themeFile : themeFiles)
            {
                try
                {
                    CustomTheme theme = loadThemeFromFile(themeFile);
                    if (theme != null)
                    {
                        customThemes.put(theme.getId().toLowerCase(), theme);
                        Proton.logger.info("Loaded custom theme: {} from {}", theme.getId(), themeFile.getName());
                    }
                }
                catch (Exception e)
                {
                    Proton.logger.error("Failed to load theme from file: {}", themeFile.getName(), e);
                }
            }
        }
        catch (Exception e)
        {
            Proton.logger.error("Failed to load custom themes", e);
        }
    }
    
    /**
     * Loads a theme from a JSON file
     */
    private static CustomTheme loadThemeFromFile(File file) throws IOException, JsonParseException
    {
        JsonObject json = FileIOHelper.readJsonObject(file, gson, Proton.logger);
        if (json == null || json.isJsonNull())
        {
            return null;
        }
        
        // Parse theme data
        String id = json.has("id") ? json.get("id").getAsString() : file.getName().replace(".json", "");
        if (id == null || id.trim().isEmpty())
        {
            id = file.getName().replace(".json", "");
        }
        
        // Parse colors (hex strings or integers)
        int overlay = parseColor(json, "overlay", 0xB013161D);
        int container = parseColor(json, "container", 0xF0191F29);
        int header = parseColor(json, "header", 0xFF242E3D);
        int categoryBackground = parseColor(json, "categoryBackground", 0xFF1A222D);
        int categorySelected = parseColor(json, "categorySelected", 0xFF2B3647);
        int moduleBackground = parseColor(json, "moduleBackground", 0xFF121923);
        int moduleEnabled = parseColor(json, "moduleEnabled", 0xFF273448);
        int moduleHover = parseColor(json, "moduleHover", 0x332D3C52);
        int settingBackground = parseColor(json, "settingBackground", 0xFF181F2C);
        int accent = parseColor(json, "accent", 0xFF4DA3FF);
        int accentMuted = parseColor(json, "accentMuted", 0x804DA3FF);
        int textPrimary = parseColor(json, "textPrimary", 0xFFE6EEF9);
        int textSecondary = parseColor(json, "textSecondary", 0xFFA8B6C9);
        int outline = parseColor(json, "outline", 0x3327313F);
        
        return new CustomTheme(id, overlay, container, header, categoryBackground, categorySelected,
                moduleBackground, moduleEnabled, moduleHover, settingBackground, accent, accentMuted,
                textPrimary, textSecondary, outline);
    }
    
    /**
     * Parses a color from JSON (supports hex strings like "#FF0000" or integers)
     */
    private static int parseColor(JsonObject json, String key, int defaultValue)
    {
        if (!json.has(key))
        {
            return defaultValue;
        }
        
        try
        {
            if (json.get(key).isJsonPrimitive())
            {
                com.google.gson.JsonPrimitive primitive = json.get(key).getAsJsonPrimitive();
                if (primitive.isNumber())
                {
                    // It's a number
                    return json.get(key).getAsInt();
                }
                else if (primitive.isString())
                {
                    // It's a string - try parsing as hex or integer
                    String value = json.get(key).getAsString();
                    if (value.startsWith("#"))
                    {
                        // Parse hex string (e.g., "#FF0000" or "#B0FF0000")
                        value = value.substring(1);
                        if (value.length() == 6)
                        {
                            // RGB - add full alpha
                            return 0xFF000000 | Integer.parseInt(value, 16);
                        }
                        else if (value.length() == 8)
                        {
                            // ARGB
                            return (int) Long.parseLong(value, 16);
                        }
                    }
                    else
                    {
                        // Try parsing as integer
                        try
                        {
                            return Integer.parseInt(value);
                        }
                        catch (NumberFormatException e)
                        {
                            // Not a valid number string
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Proton.logger.warn("Failed to parse color for key '{}', using default", key);
        }
        
        return defaultValue;
    }
    
    /**
     * Gets a theme by ID (searches both built-in and custom themes)
     */
    public static ClickGuiThemeInterface getTheme(String id)
    {
        if (id == null || id.trim().isEmpty())
        {
            return ClickGuiTheme.MIDNIGHT;
        }
        
        // Check built-in themes first
        for (ClickGuiTheme builtInTheme : ClickGuiTheme.values())
        {
            if (builtInTheme.getId().equalsIgnoreCase(id))
            {
                return builtInTheme;
            }
        }
        
        // Check custom themes
        loadCustomThemes();
        CustomTheme customTheme = customThemes.get(id.toLowerCase());
        if (customTheme != null)
        {
            return customTheme;
        }
        
        // Fallback to midnight
        return ClickGuiTheme.MIDNIGHT;
    }
    
    /**
     * Gets all available theme IDs (both built-in and custom)
     */
    public static String[] getAllThemeIds()
    {
        loadCustomThemes();
        
        List<String> themeIds = new ArrayList<>();
        
        // Add built-in themes
        themeIds.addAll(Arrays.asList(ClickGuiTheme.ids()));
        
        // Add custom themes
        for (String customId : customThemes.keySet())
        {
            // Only add if not already present (avoid duplicates)
            if (!themeIds.contains(customId))
            {
                themeIds.add(customThemes.get(customId).getId());
            }
        }
        
        return themeIds.toArray(new String[0]);
    }
    
    /**
     * Gets the themes directory
     */
    public static File getThemesDirectory()
    {
        ProtonClient client = ProtonClient.getInstance();
        if (client == null || client.getSettingsManager() == null)
        {
            return null;
        }
        
        File configDir = client.getSettingsManager().getConfigDirectory();
        return new File(configDir, THEMES_DIR);
    }
    
    /**
     * Reloads custom themes from disk
     */
    public static void reloadCustomThemes()
    {
        themesLoaded = false;
        loadCustomThemes();
    }
    
    /**
     * Checks if a custom theme with the given ID exists
     */
    public static boolean hasCustomTheme(String id)
    {
        loadCustomThemes();
        return customThemes.containsKey(id.toLowerCase());
    }
}

