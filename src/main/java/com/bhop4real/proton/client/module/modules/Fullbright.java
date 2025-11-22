package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

/**
 * Fullbright module - makes everything bright
 * Supports two modes: Gamma and Night Vision
 */
public class Fullbright extends Module
{
    public EnumSetting mode;
    private float originalGamma;
    
    public Fullbright()
    {
        super("Fullbright", new String[]{"fb", "bright"}, "Makes everything bright", Module.Category.RENDER, 0);
        
        mode = new EnumSetting("Mode", "Fullbright mode", "Gamma", "Gamma", "Night Vision");
        addSetting(mode);
        
        originalGamma = 1.0F;
    }
    
    @Override
    public void onEnable()
    {
        if (mc.gameSettings != null)
        {
            originalGamma = mc.gameSettings.gammaSetting;
        }
    }
    
    @Override
    public void onUpdate()
    {
        if (mc.thePlayer == null || mc.gameSettings == null)
        {
            return;
        }
        
        String currentMode = mode.getValue();
        
        if ("Gamma".equals(currentMode))
        {
            // Set gamma to maximum brightness
            mc.gameSettings.gammaSetting = 1000.0F;
            
            // Remove night vision if it was active
            if (mc.thePlayer.isPotionActive(Potion.nightVision))
            {
                mc.thePlayer.removePotionEffect(Potion.nightVision.id);
            }
        }
        else if ("Night Vision".equals(currentMode))
        {
            // Reset gamma to original value
            mc.gameSettings.gammaSetting = originalGamma;
            
            // Apply night vision potion effect if not already active
            // Duration: 1000000 ticks (very long), Amplifier: 0 (level 1)
            if (!mc.thePlayer.isPotionActive(Potion.nightVision))
            {
                mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 1000000, 0));
            }
            else
            {
                // Refresh the effect to ensure it doesn't expire
                PotionEffect activeEffect = mc.thePlayer.getActivePotionEffect(Potion.nightVision);
                if (activeEffect != null && activeEffect.getDuration() < 100000)
                {
                    mc.thePlayer.removePotionEffect(Potion.nightVision.id);
                    mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 1000000, 0));
                }
            }
        }
    }
    
    @Override
    public void onDisable()
    {
        if (mc.thePlayer != null)
        {
            // Remove night vision potion effect
            if (mc.thePlayer.isPotionActive(Potion.nightVision))
            {
                mc.thePlayer.removePotionEffect(Potion.nightVision.id);
            }
        }
        
        if (mc.gameSettings != null)
        {
            // Restore original gamma
            mc.gameSettings.gammaSetting = originalGamma;
        }
    }
}

