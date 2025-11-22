package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.learning.click.ClickMLService;
import com.bhop4real.proton.client.module.modules.autoclicker.AutoClickerLearningBridge;
import com.bhop4real.proton.client.module.modules.autoclicker.AutoClickerRandomization;
import com.bhop4real.proton.client.module.modules.autoclicker.AutoClickerPatternScript;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import com.bhop4real.proton.client.module.settings.IntSetting;
import com.bhop4real.proton.client.module.settings.StringSetting;
import com.bhop4real.proton.client.util.RandomUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.util.function.Supplier;

public class AutoClicker extends Module
{
    // Global Left settings
    public IntSetting leftMinCPS;
    public IntSetting leftMaxCPS;
    public BooleanSetting leftRequireHolding;
    public BooleanSetting leftBreakBlocks;
    public IntSetting leftBreakBlockMinDelay;
    public IntSetting leftBreakBlockMaxDelay;

    // Delay mode settings
    public IntSetting leftMinDelayMs;
    public IntSetting leftMaxDelayMs;
    
    // CPS/Delay mode selectors
    public EnumSetting cpsMode;
    public EnumSetting randomizationMode;
    public IntSetting advancedOffsetMinMs;
    public IntSetting advancedOffsetMaxMs;
    public StringSetting patternFilePath;
    
    // Internal state
    private long nextClickDelay = 0;
    private long lastClickTime = 0;
    private int ticksButtonDown = 0;
    private long blockHoverStartTime = -1;
    private boolean wasHoveringBlock = false;
    private final AutoClickerRandomization randomization = new AutoClickerRandomization();
    private AutoClickerPatternScript patternScript;
    private long pendingReleaseTime = -1;
    private boolean attackKeyHeld = false;
    private final AutoClickerLearningBridge learningBridge = AutoClickerLearningBridge.get();
    private boolean lastDelayOverrideApplied;
    private int clickCount = 0;
    private long sessionStartTime = 0;
    private String lastPatternPath = "";
    
    public AutoClicker()
    {
        super("AutoClicker", new String[]{"ac", "autoclick"}, "Automatically clicks for you", Module.Category.COMBAT, 0);
        
        // Mode selectors
        cpsMode = new EnumSetting("CPS Mode", "How click speed is calculated", "Classic", "Classic", "Delay", "ML");
        randomizationMode = new EnumSetting("Randomization", "Randomization profile for delays", "Basic", "Basic", "Open");

        // Global Left settings
        leftMinCPS = new IntSetting("Left Min CPS", "Minimum left clicks per second", 8, 1, 20);
        leftMaxCPS = new IntSetting("Left Max CPS", "Maximum left clicks per second", 12, 1, 20);
        leftRequireHolding = new BooleanSetting("Require Holding Button", "Only click if left mouse button is held down", true);
        leftBreakBlocks = new BooleanSetting("Break Blocks", "Stop clicking when hovering blocks", false);
        leftBreakBlockMinDelay = new IntSetting("Block Min Delay", "Minimum ticks to wait after hovering block (0-20)", 0, 0, 20);
        leftBreakBlockMaxDelay = new IntSetting("Block Max Delay", "Maximum ticks to wait after hovering block (0-20)", 0, 0, 20);

        leftMinDelayMs = new IntSetting("Left Min Delay", "Minimum delay between clicks (ms)", 85, 0, 1000);
        leftMaxDelayMs = new IntSetting("Left Max Delay", "Maximum delay between clicks (ms)", 135, 0, 1000);
        advancedOffsetMinMs = new IntSetting("Advanced Offset Min", "Minimum random offset applied to the previous delay (ms)", -50, -250, 250);
        advancedOffsetMaxMs = new IntSetting("Advanced Offset Max", "Maximum random offset applied to the previous delay (ms)", 125, -250, 250);
        patternFilePath = new StringSetting("Pattern File", "Path to JavaScript pattern script file", "");
        
        addSetting(cpsMode);
        addSetting(randomizationMode);
        addSetting(patternFilePath);
        addSetting(leftMinCPS);
        addSetting(leftMaxCPS);
        addSetting(leftRequireHolding);
        addSetting(leftBreakBlocks);
        addSetting(leftBreakBlockMinDelay);
        addSetting(leftBreakBlockMaxDelay);
        addSetting(leftMinDelayMs);
        addSetting(leftMaxDelayMs);
        addSetting(advancedOffsetMinMs);
        addSetting(advancedOffsetMaxMs);

        // Unique visibility: show block delay settings only when Break Blocks is enabled
        leftMinCPS.setVisibilitySupplier(() -> !isMlMode() && cpsMode.getValue().equalsIgnoreCase("Classic"));
        leftMaxCPS.setVisibilitySupplier(() -> !isMlMode() && cpsMode.getValue().equalsIgnoreCase("Classic"));
        leftBreakBlockMinDelay.setVisibilitySupplier(() -> leftBreakBlocks.getValue());
        leftBreakBlockMaxDelay.setVisibilitySupplier(() -> leftBreakBlocks.getValue());
        leftMinDelayMs.setVisibilitySupplier(() -> !isMlMode() && cpsMode.getValue().equalsIgnoreCase("Delay"));
        leftMaxDelayMs.setVisibilitySupplier(() -> !isMlMode() && cpsMode.getValue().equalsIgnoreCase("Delay"));
        randomizationMode.setVisibilitySupplier(() -> !isMlMode());
        Supplier<Boolean> advancedVisible = () -> !isMlMode() && randomizationMode.getValue().equalsIgnoreCase("Basic");
        advancedOffsetMinMs.setVisibilitySupplier(advancedVisible);
        advancedOffsetMaxMs.setVisibilitySupplier(advancedVisible);
        patternFilePath.setVisibilitySupplier(() -> !isMlMode() && randomizationMode.getValue().equalsIgnoreCase("Open"));
        
        // Initialize pattern script engine if available
        try
        {
            patternScript = new AutoClickerPatternScript();
        }
        catch (IllegalStateException e)
        {
            Proton.logger.warn("Pattern scripting not available: " + e.getMessage());
            patternScript = null;
        }
    }
    
    @Override
    public void onUpdate()
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }
        
        // Don't auto-click if in a GUI
        if (mc.currentScreen != null)
        {
            return;
        }
        
        long currentTime = System.currentTimeMillis();

        if (attackKeyHeld && pendingReleaseTime > 0 && currentTime >= pendingReleaseTime)
        {
            releaseAttackKey();
        }
        
        // Track how long button has been held down
        boolean buttonDown = Mouse.isButtonDown(0) || mc.gameSettings.keyBindAttack.isKeyDown();
        if (buttonDown || !leftRequireHolding.getValue())
        {
            if (leftRequireHolding.getValue())
            {
                ticksButtonDown++;
            }
            else
            {
                // If not requiring holding, simulate button always being down
                ticksButtonDown = 2; // Set to > 1 so clicking works
            }
        }
        else
        {
            ticksButtonDown = 0;
        }
        
        // Calculate if we should click now
        if (currentTime - lastClickTime >= nextClickDelay)
        {
            // Check if we should perform click
            if (shouldClick())
            {
                long interClickDelay = lastClickTime == 0 ? 0 : currentTime - lastClickTime;
                boolean buttonState = buttonDown;
                boolean hoveringBlockNow = mc.objectMouseOver != null &&
                    mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;

                performClick();
                clickCount++;

                AutoClickerRandomization.Profile profile = AutoClickerRandomization.Profile.fromLabel(randomizationMode.getValue());
                AutoClickerRandomization.Config config = new AutoClickerRandomization.Config(
                    leftMinCPS.getValue(),
                    leftMaxCPS.getValue(),
                    leftMinDelayMs.getValue(),
                    leftMaxDelayMs.getValue(),
                    advancedOffsetMinMs.getValue(),
                    advancedOffsetMaxMs.getValue()
                );

                long scheduledDelay = calculateNextDelay(profile, config);
                lastClickTime = currentTime;

                learningBridge.recordObservation(AutoClickerLearningBridge.Observation.create(
                    interClickDelay,
                    scheduledDelay,
                    cpsMode.getValue(),
                    randomizationMode.getValue(),
                    leftMinCPS.getValue(),
                    leftMaxCPS.getValue(),
                    leftMinDelayMs.getValue(),
                    leftMaxDelayMs.getValue(),
                    advancedOffsetMinMs.getValue(),
                    advancedOffsetMaxMs.getValue(),
                    leftRequireHolding.getValue(),
                    ticksButtonDown,
                    buttonState,
                    leftBreakBlocks.getValue(),
                    hoveringBlockNow,
                    attackKeyHeld,
                    lastDelayOverrideApplied
                ));
            }
        }
    }
    
    private boolean shouldClick()
    {
        // Need button to be held for at least 2 ticks (like reference)
        if (leftRequireHolding.getValue() && ticksButtonDown <= 1)
        {
            return false;
        }
        
        // Check break blocks logic
        if (leftBreakBlocks.getValue())
        {
            MovingObjectPosition mouseOver = mc.objectMouseOver;
            boolean isHoveringBlock = mouseOver != null && 
                                     mouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
            
            if (isHoveringBlock)
            {
                // Start tracking when we started hovering
                if (!wasHoveringBlock)
                {
                    blockHoverStartTime = System.currentTimeMillis();
                    wasHoveringBlock = true;
                }
                
                // Calculate delay in milliseconds
                int minDelay = leftBreakBlockMinDelay.getValue() * 50; // Convert ticks to ms
                int maxDelay = leftBreakBlockMaxDelay.getValue() * 50;
                
                if (maxDelay > 0 || minDelay > 0)
                {
                    long delay = maxDelay > minDelay ? 
                        RandomUtil.nextInt(minDelay, maxDelay + 1) : minDelay;
                    
                    // Don't click if we're still within the delay period
                    if (System.currentTimeMillis() - blockHoverStartTime < delay)
                    {
                        return false;
                    }
                }
                
                // After delay period, stop clicking
                return false;
            }
            else
            {
                // Reset block hover tracking
                wasHoveringBlock = false;
                blockHoverStartTime = -1;
            }
        }
        
        return true;
    }
    
    private void performClick()
    {
        // Handle block breaking logic
        if (mc.objectMouseOver != null && 
            mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
        {
            if (leftBreakBlocks.getValue())
            {
                // Don't click blocks if break blocks is enabled
                return;
            }
        }
        
        long holdReference = Math.max(1L, nextClickDelay);
        releaseAttackKey();
        
        // Simulate key press using KeyBinding (like reference)
        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.setKeyBindState(attackKey, true);
        KeyBinding.onTick(attackKey); // This is crucial - it processes the key press
        scheduleReleaseForDelay(holdReference);
    }
    
    private long calculateNextDelay(AutoClickerRandomization.Profile profile,
                                    AutoClickerRandomization.Config config)
    {
        Long override = learningBridge.pollDelayOverride();
            if (override != null)
        {
            long candidate = Math.max(1L, override);
            if (!isMlMode() && cpsMode.getValue().equalsIgnoreCase("Delay"))
            {
                int minDelay = Math.min(config.minDelayMs(), config.maxDelayMs());
                int maxDelay = Math.max(config.minDelayMs(), config.maxDelayMs());
                candidate = Math.max(minDelay, Math.min(maxDelay, candidate));
            }
            nextClickDelay = candidate;
            lastDelayOverrideApplied = true;
        }
        else
        {
            if (isMlMode())
            {
                long mlDelay = ClickMLService.get()
                    .sampleDelay()
                    .orElseGet(() -> defaultMlFallback(config));
                nextClickDelay = mlDelay;
            }
            else if (profile == AutoClickerRandomization.Profile.OPEN)
            {
                if (patternScript == null)
                {
                    // Pattern scripting not available, fall back to basic randomization
                    Proton.logger.warn("Pattern script engine not available, falling back to basic randomization");
                    AutoClickerRandomization.Mode mode = AutoClickerRandomization.Mode.fromLabel(cpsMode.getValue());
                    nextClickDelay = randomization.nextDelay(AutoClickerRandomization.Profile.BASIC, mode, config);
                }
                else
                {
                    // Check if script path has changed and reload if needed
                    String currentPath = patternFilePath.getValue();
                    if (currentPath != null && !currentPath.equals(lastPatternPath))
                    {
                        patternScript.loadScript(currentPath);
                        lastPatternPath = currentPath;
                    }
                    
                    // Use pattern script for Open mode
                    long sessionTime = sessionStartTime > 0 ? System.currentTimeMillis() - sessionStartTime : 0L;
                    AutoClickerPatternScript.PatternState state = new AutoClickerPatternScript.PatternState(
                        clickCount,
                        sessionTime,
                        lastClickTime
                    );
                    nextClickDelay = patternScript.getNextDelay(lastClickTime > 0 ? nextClickDelay : 50L, config, state);
                }
            }
            else
            {
                AutoClickerRandomization.Mode mode = AutoClickerRandomization.Mode.fromLabel(cpsMode.getValue());
                nextClickDelay = randomization.nextDelay(profile, mode, config);
            }
            if (nextClickDelay < 1L)
            {
                nextClickDelay = 1L;
            }
            lastDelayOverrideApplied = false;
        }
        return nextClickDelay;
    }

    private long defaultMlFallback(AutoClickerRandomization.Config config)
    {
        int min = Math.min(config.minDelayMs(), config.maxDelayMs());
        int max = Math.max(config.minDelayMs(), config.maxDelayMs());
        if (min <= 0 && max <= 0)
        {
            min = 70;
            max = 140;
        }
        if (min == max)
        {
            return Math.max(25, min);
        }
        return Math.max(25, RandomUtil.nextInt(min, max + 1));
    }

    private boolean isMlMode()
    {
        return cpsMode.getValue().equalsIgnoreCase("ML");
    }
    
    @Override
    public void onEnable()
    {
        lastClickTime = 0;
        nextClickDelay = 0;
        ticksButtonDown = 0;
        blockHoverStartTime = -1;
        wasHoveringBlock = false;
        randomization.resetAll();
        releaseAttackKey();
        learningBridge.reset();
        lastDelayOverrideApplied = false;
        clickCount = 0;
        sessionStartTime = System.currentTimeMillis();
        
        // Load pattern script if Open mode is selected
        if (randomizationMode.getValue().equalsIgnoreCase("Open") && patternScript != null)
        {
            String path = patternFilePath.getValue();
            patternScript.loadScript(path);
            patternScript.startSession();
            lastPatternPath = path != null ? path : "";
        }
        else if (patternScript != null)
        {
            patternScript.reset();
            lastPatternPath = "";
        }
        
        // Calculate initial delay
        AutoClickerRandomization.Profile profile = AutoClickerRandomization.Profile.fromLabel(randomizationMode.getValue());
        AutoClickerRandomization.Config config = new AutoClickerRandomization.Config(
            leftMinCPS.getValue(),
            leftMaxCPS.getValue(),
            leftMinDelayMs.getValue(),
            leftMaxDelayMs.getValue(),
            advancedOffsetMinMs.getValue(),
            advancedOffsetMaxMs.getValue()
        );
        calculateNextDelay(profile, config);
    }

    private void scheduleReleaseForDelay(long referenceDelay)
    {
        long safeDelay = Math.max(25L, referenceDelay);
        int minHold = (int)Math.max(14L, Math.round(safeDelay * 0.16));
        int maxHold = (int)Math.max(minHold + 6, Math.round(safeDelay * 0.33) + 14);
        if (maxHold <= minHold)
        {
            maxHold = minHold + 4;
        }

        int hold = RandomUtil.nextInt(minHold, maxHold + 1);

        if (RandomUtil.nextDouble() < 0.08)
        {
            hold += RandomUtil.nextInt(12, 40);
        }

        if (RandomUtil.nextDouble() < 0.03)
        {
            hold += RandomUtil.nextInt(40, 90);
        }

        pendingReleaseTime = System.currentTimeMillis() + hold;
        attackKeyHeld = true;
    }

    private void releaseAttackKey()
    {
        if (!attackKeyHeld)
        {
            pendingReleaseTime = -1;
            return;
        }

        if (mc != null && mc.gameSettings != null)
        {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        }
        attackKeyHeld = false;
        pendingReleaseTime = -1;
    }

    @Override
    public void onDisable()
    {
        // Release attack key when disabled
        if (mc.thePlayer != null && mc.gameSettings != null)
        {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        }
        
        lastClickTime = 0;
        nextClickDelay = 0;
        ticksButtonDown = 0;
        blockHoverStartTime = -1;
        wasHoveringBlock = false;
        randomization.resetAll();
        releaseAttackKey();
        attackKeyHeld = false;
        pendingReleaseTime = -1;
        learningBridge.reset();
        lastDelayOverrideApplied = false;
        if (patternScript != null)
        {
            patternScript.reset();
        }
        clickCount = 0;
        sessionStartTime = 0;
        lastPatternPath = "";
    }
    
    /**
     * Gets the AutoClicker module instance
     */
    public static AutoClicker get()
    {
        com.bhop4real.proton.client.ProtonClient client = com.bhop4real.proton.client.ProtonClient.getInstance();
        if (client == null || client.getModuleManager() == null) return null;
        Module m = client.getModuleManager().getModuleByName("AutoClicker");
        return m instanceof AutoClicker ? (AutoClicker) m : null;
    }
}
