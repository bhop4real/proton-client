package com.bhop4real.proton.client.util.audio;

import com.bhop4real.proton.client.ProtonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * Utility for playing sounds
 */
public final class SoundUtil
{
    // Minecraft 1.8.9 button click sound
    private static final ResourceLocation BUTTON_CLICK_SOUND = new ResourceLocation("gui.button.press");
    
    private SoundUtil() {} // Prevent instantiation
    
    /**
     * Play the default Minecraft button click sound
     */
    public static void playButtonClick()
    {
        // Check if sounds are enabled
        boolean soundsEnabled = true;
        ProtonClient client = ProtonClient.getInstance();
        if (client != null && client.getSettingsManager() != null)
        {
            soundsEnabled = client.getSettingsManager().getBooleanSetting("sounds.enabled", true);
        }
        
        if (soundsEnabled)
        {
            Minecraft.getMinecraft().getSoundHandler().playSound(
                PositionedSoundRecord.create(BUTTON_CLICK_SOUND, 1.0F)
            );
        }
    }
    
    /**
     * Play a custom sound by resource location
     * @param soundResource The resource location of the sound (e.g., "mob.chicken.plop")
     * @param volume The volume (0.0 to 1.0)
     */
    public static void playCustomSound(String soundResource, float volume)
    {
        Minecraft.getMinecraft().getSoundHandler().playSound(
            PositionedSoundRecord.create(new ResourceLocation(soundResource), volume)
        );
    }
    
    /**
     * Play enable sound for a module
     * @param moduleName The name of the module
     */
    public static void playEnableSound(String moduleName)
    {
        ProtonClient client = ProtonClient.getInstance();
        if (client != null && client.getSettingsManager() != null)
        {
            // Try to get custom sound from config, fallback to default
            String customSound = client.getSettingsManager().getSetting("sound.enable." + moduleName.toLowerCase(), "");
            
            if (!customSound.isEmpty())
            {
                playCustomSound(customSound, 1.0F);
                return;
            }
        }
        
        playButtonClick();
    }
    
    /**
     * Play disable sound for a module
     * @param moduleName The name of the module
     */
    public static void playDisableSound(String moduleName)
    {
        ProtonClient client = ProtonClient.getInstance();
        if (client != null && client.getSettingsManager() != null)
        {
            // Try to get custom sound from config, fallback to default
            String customSound = client.getSettingsManager().getSetting("sound.disable." + moduleName.toLowerCase(), "");
            
            if (!customSound.isEmpty())
            {
                playCustomSound(customSound, 1.0F);
                return;
            }
        }
        
        playButtonClick();
    }
}

