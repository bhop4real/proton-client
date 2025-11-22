package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import com.bhop4real.proton.client.util.JumpUtil;
import net.minecraft.util.MathHelper;

public class Speed extends Module
{
    public EnumSetting mode;
    private boolean wasJumping = false;
    
    public Speed()
    {
        super("Speed", new String[]{"fast"}, "Increases movement speed", Module.Category.MOVEMENT, 0);
        
        mode = new EnumSetting("Mode", "Speed mode", "Legit", "Legit", "Normal");
        addSetting(mode);
    }
    
    @Override
    public void onUpdate()
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }
        
        if (mode.getValue().equalsIgnoreCase("Legit"))
        {
            handleLegitMode();
        }
    }
    
    private void handleLegitMode()
    {
        // Check if player is moving horizontally
        double motionX = mc.thePlayer.motionX;
        double motionZ = mc.thePlayer.motionZ;
        double horizontalSpeed = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
        boolean isMoving = horizontalSpeed > 0.1;
        
        // Ground check - only jump when on ground
        boolean onGround = mc.thePlayer.onGround;
        
        // Release jump key if we were jumping and now we're in the air
        if (wasJumping && !onGround)
        {
            JumpUtil.releaseJumpKey(mc);
            wasJumping = false;
        }
        
        // If player is moving and on ground, make them jump
        if (isMoving && onGround && !wasJumping)
        {
            if (JumpUtil.pressJumpKey(mc))
            {
                wasJumping = true;
            }
        }
        else if (onGround && wasJumping)
        {
            // Release jump key when back on ground to allow next jump
            JumpUtil.releaseJumpKey(mc);
            wasJumping = false;
        }
    }
    
    @Override
    public void onDisable()
    {
        if (mc.thePlayer != null)
        {
            // Release jump key when disabled
            JumpUtil.releaseJumpKey(mc);
            wasJumping = false;
        }
    }
}
