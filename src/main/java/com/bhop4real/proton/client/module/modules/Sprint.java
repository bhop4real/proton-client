package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.module.Module;
import net.minecraft.client.settings.KeyBinding;

public class Sprint extends Module
{
    public Sprint()
    {
        super("Sprint", new String[]{"s", "run"}, "Automatically sprints when moving forward", Module.Category.MOVEMENT, 0);
    }
    
    @Override
    public void onUpdate()
    {
        if (mc.thePlayer != null && mc.theWorld != null)
        {
            // Only sprint when moving forward and conditions are met
            boolean isMovingForward = mc.thePlayer.movementInput.moveForward > 0;
            boolean hasEnoughFood = mc.thePlayer.getFoodStats().getFoodLevel() > 6;
            boolean notSneaking = !mc.thePlayer.isSneaking();
            boolean notInWater = !mc.thePlayer.isInWater();

            if (isMovingForward && hasEnoughFood && notSneaking && notInWater)
            {
                // Set sprint key to pressed state (more legitimate than directly setting sprinting)
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
            }
            else
            {
                // Release sprint key if conditions aren't met
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
            }
        }
    }
    
    @Override
    public void onDisable()
    {
        if (mc.thePlayer != null)
        {
            // Release the sprint key binding
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        }
    }
}
