package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.IntSetting;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

/**
 * Eagle module - automatically sneaks when player is on the edge of a block
 * Based on FDPClient and RavenAPlus implementations
 */
public class Eagle extends Module
{
    private final IntSetting releaseDelay;
    
    private int sneakReleaseTimer = 0;
    private boolean sneaking = false;
    
    public Eagle()
    {
        super("Eagle", new String[]{"eg"}, "Automatically shifts on block edges", Module.Category.PLAYER, 0);
        
        releaseDelay = new IntSetting("Release Delay", "Ticks to keep sneaking after leaving edge", 3, 0, 10);
        addSetting(releaseDelay);
    }
    
    @Override
    public void onEnable()
    {
        sneakReleaseTimer = 0;
        sneaking = false;
    }
    
    /**
     * Checks if the block directly below the player is air
     */
    private boolean overAir()
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return false;
        }
        BlockPos blockBelow = new BlockPos(
            mc.thePlayer.posX,
            mc.thePlayer.posY - 1.0,
            mc.thePlayer.posZ
        );
        return mc.theWorld.isAirBlock(blockBelow);
    }
    
    /**
     * Checks if player is on an edge by looking ahead in movement direction
     * Based on RavenAPlus Utils.onEdge() implementation
     */
    private boolean onEdge()
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return false;
        }
        
        // Check if there are any colliding bounding boxes when offset by motion
        // This checks if there's a block ahead in the direction the player is moving
        AxisAlignedBB boundingBox = mc.thePlayer.getEntityBoundingBox();
        AxisAlignedBB offsetBox = boundingBox.offset(
            mc.thePlayer.motionX / 3.0D,
            -1.0D,
            mc.thePlayer.motionZ / 3.0D
        );
        
        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, offsetBox).isEmpty();
    }
    
    @Override
    public void onUpdate()
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }
        
        // Respect manual sneaking - if player is manually holding sneak, don't interfere
        if (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
        {
            return;
        }
        
        // Check if player is on ground and either over air or on edge
        boolean shouldSneakNow = mc.thePlayer.onGround && (overAir() || onEdge());
        
        if (shouldSneakNow)
        {
            // Player is on edge - sneak immediately
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
            sneaking = true;
            sneakReleaseTimer = 0;
        }
        else if (sneaking)
        {
            // Player is not on edge but was sneaking - use timer to delay releasing
            int maxDelay = releaseDelay.getValue();
            if (sneakReleaseTimer >= maxDelay)
            {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                sneaking = false;
                sneakReleaseTimer = 0;
            }
            else
            {
                // Keep sneak pressed during timer delay
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                sneakReleaseTimer++;
            }
        }
        else
        {
            // Not sneaking and not on edge - ensure sneak is released
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
    }
    
    @Override
    public void onDisable()
    {
        sneakReleaseTimer = 0;
        sneaking = false;
        
        // Only release sneak if player is not manually holding it
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
        {
            if (mc.thePlayer != null)
            {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }
        }
    }
}

