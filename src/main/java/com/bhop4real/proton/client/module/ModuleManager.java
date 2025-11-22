package com.bhop4real.proton.client.module;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.ProtonClient;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all modules with efficient Map-based lookups
 */
public class ModuleManager {
    private final Map<String, Module> modulesByName;
    private final Map<Integer, List<Module>> modulesByKeyBind;
    private final Map<Module.Category, List<Module>> modulesByCategory;
    private final List<Module> modules;

    public ModuleManager() {
        this.modulesByName = new ConcurrentHashMap<>();
        this.modulesByKeyBind = new ConcurrentHashMap<>();
        this.modulesByCategory = new ConcurrentHashMap<>();
        this.modules = new ArrayList<>();

        // Initialize category maps
        for (Module.Category category : Module.Category.values()) {
            modulesByCategory.put(category, new ArrayList<>());
        }

        // Register all modules
        registerModules();

        // Load module states from config
        loadModuleStates();

        Proton.logger.info("ModuleManager initialized with " + modules.size() + " modules");
    }

    /**
     * Registers all modules
     */
    private void registerModules() {
        // Combat modules
        registerModule(new com.bhop4real.proton.client.module.modules.AutoClicker());
        registerModule(new com.bhop4real.proton.client.module.modules.AimAssist());
        registerModule(new com.bhop4real.proton.client.module.modules.Backtrack());
        registerModule(new com.bhop4real.proton.client.module.modules.Reach());
        registerModule(new com.bhop4real.proton.client.module.modules.KillAura());

        // Movement modules
        registerModule(new com.bhop4real.proton.client.module.modules.Sprint());
        registerModule(new com.bhop4real.proton.client.module.modules.Speed());
        registerModule(new com.bhop4real.proton.client.module.modules.Velocity());

        // Player modules
        registerModule(new com.bhop4real.proton.client.module.modules.Eagle());

        // Render modules
        registerModule(new com.bhop4real.proton.client.module.modules.ESP());
        registerModule(new com.bhop4real.proton.client.module.modules.Fullbright());
        registerModule(new com.bhop4real.proton.client.module.modules.ClickGUI());
        registerModule(new com.bhop4real.proton.client.module.modules.HUD());
        registerModule(new com.bhop4real.proton.client.module.modules.HUDDesigner());

        // Misc modules
        registerModule(new com.bhop4real.proton.client.module.modules.Targets());
        registerModule(new com.bhop4real.proton.client.module.modules.ClickSampler());
        registerModule(new com.bhop4real.proton.client.module.modules.ClickModelTrainerModule());
        registerModule(new com.bhop4real.proton.client.module.modules.MusicPlayer());

        // Other modules (internal)
        registerModule(new com.bhop4real.proton.client.module.impl.other.SlotHandler());

        // Rotation system (must be registered early)
        // registerModule(new
        // com.bhop4real.proton.client.module.modules.other.RotationHandler());

        // Test modules
        // registerModule(new
        // com.bhop4real.proton.client.module.modules.other.SilentSpin());
    }

    /**
     * Registers a module and updates all lookup maps
     */
    public void registerModule(Module module) {
        if (module == null) {
            Proton.logger.warn("Attempted to register null module");
            return;
        }

        String name = module.getName().toLowerCase();

        // Check for duplicate names
        if (modulesByName.containsKey(name)) {
            Proton.logger.warn("Duplicate module name detected: " + name);
            return;
        }

        modules.add(module);
        modulesByName.put(name, module);

        // Register by aliases
        for (String alias : module.getAliases()) {
            String aliasLower = alias.toLowerCase();
            if (!modulesByName.containsKey(aliasLower)) {
                modulesByName.put(aliasLower, module);
            }
        }

        // Register by keybind
        int keyBind = module.getKeyBind();
        if (keyBind != 0) {
            List<Module> keyBindModules = modulesByKeyBind.computeIfAbsent(keyBind, k -> new ArrayList<>());
            // Prevent duplicate entries
            if (!keyBindModules.contains(module)) {
                keyBindModules.add(module);
            }
        }

        // Register by category
        modulesByCategory.get(module.getCategory()).add(module);
    }

    /**
     * Unregisters a module (useful for dynamic module loading)
     */
    public void unregisterModule(Module module) {
        if (module == null)
            return;

        String name = module.getName().toLowerCase();
        modules.remove(module);
        modulesByName.remove(name);

        // Remove aliases
        for (String alias : module.getAliases()) {
            modulesByName.remove(alias.toLowerCase());
        }

        // Remove from keybind map
        int keyBind = module.getKeyBind();
        if (keyBind != 0) {
            List<Module> keyBindModules = modulesByKeyBind.get(keyBind);
            if (keyBindModules != null) {
                keyBindModules.remove(module);
                if (keyBindModules.isEmpty()) {
                    modulesByKeyBind.remove(keyBind);
                }
            }
        }

        // Remove from category map
        List<Module> categoryModules = modulesByCategory.get(module.getCategory());
        if (categoryModules != null) {
            categoryModules.remove(module);
        }
    }

    private void loadModuleStates() {
        ProtonClient client = ProtonClient.getInstance();
        com.bhop4real.proton.client.settings.SettingsManager settings = client != null ? client.getSettingsManager()
                : null;

        if (settings == null) {
            Proton.logger.warn("SettingsManager not available, skipping module state loading");
            return;
        }

        for (Module module : modules) {
            try {
                settings.loadModuleSettings(module);
            } catch (Exception e) {
                Proton.logger.error("Failed to load settings for module: " + module.getName(), e);
            }
        }
    }

    /**
     * Gets all registered modules
     */
    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * Gets a module by name (case-insensitive) or alias
     */
    public Module getModuleByName(String name) {
        if (name == null)
            return null;
        return modulesByName.get(name.toLowerCase());
    }

    /**
     * Gets all modules in a category
     */
    public List<Module> getModulesByCategory(Module.Category category) {
        List<Module> categoryModules = modulesByCategory.get(category);
        return categoryModules != null ? Collections.unmodifiableList(categoryModules) : Collections.emptyList();
    }

    /**
     * Gets all modules bound to a specific key
     */
    public List<Module> getModulesByKeyBind(int keyCode) {
        List<Module> keyBindModules = modulesByKeyBind.get(keyCode);
        return keyBindModules != null ? Collections.unmodifiableList(keyBindModules) : Collections.emptyList();
    }

    /**
     * Updates a module's keybind in the lookup maps
     */
    public void updateModuleKeyBind(Module module, int oldKeyBind, int newKeyBind) {
        if (module == null)
            return;

        // Remove from old keybind
        if (oldKeyBind != 0) {
            List<Module> oldKeyBindModules = modulesByKeyBind.get(oldKeyBind);
            if (oldKeyBindModules != null) {
                oldKeyBindModules.remove(module);
                if (oldKeyBindModules.isEmpty()) {
                    modulesByKeyBind.remove(oldKeyBind);
                }
            }
        }

        // Add to new keybind
        if (newKeyBind != 0) {
            List<Module> newKeyBindModules = modulesByKeyBind.computeIfAbsent(newKeyBind, k -> new ArrayList<>());
            // Prevent duplicate entries
            if (!newKeyBindModules.contains(module)) {
                newKeyBindModules.add(module);
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (Module module : modules) {
                if (module.isEnabled()) {
                    try {
                        module.onUpdate();
                    } catch (Exception e) {
                        Proton.logger.error("Error in module update: " + module.getName(), e);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (!Keyboard.getEventKeyState())
            return;

        int keyCode = Keyboard.getEventKey();
        if (keyCode == Keyboard.KEY_NONE)
            return;

        // Open click GUI with RSHIFT (default keybind)
        if (keyCode == Keyboard.KEY_RSHIFT) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc.thePlayer != null && mc.theWorld != null) {
                com.bhop4real.proton.client.gui.GuiClickGUI.preloadResources();
                mc.displayGuiScreen(new com.bhop4real.proton.client.gui.GuiClickGUI());
            }
            return;
        }

        // Handle module keybinds (more efficient lookup)
        List<Module> keyBindModules = getModulesByKeyBind(keyCode);
        for (Module module : keyBindModules) {
            try {
                module.toggle();
            } catch (Exception e) {
                Proton.logger.error("Error toggling module: " + module.getName(), e);
            }
        }
    }
}
