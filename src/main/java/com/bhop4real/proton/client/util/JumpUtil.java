package com.bhop4real.proton.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

/**
 * Utility helpers for simulating jump key presses in a consistent,
 * input-safe way. Modules that need to queue legitimate jumps should
 * call {@link #pressJumpKey(Minecraft)} instead of tinkering with the
 * player's motion directly so we stay in sync with Minecraft's input
 * pipeline.
 */
public final class JumpUtil
{
    private JumpUtil() {}

    /**
     * Simulates a single jump key press for the supplied client instance.
     * The key is pressed and kept pressed for the current tick to ensure
     * Minecraft processes the jump correctly.
     *
     * @return {@code true} if the key binding was available and the press
     *         was simulated, {@code false} otherwise.
     */
    public static boolean pressJumpKey(Minecraft mc)
    {
        if (mc == null || mc.thePlayer == null || mc.gameSettings == null || mc.gameSettings.keyBindJump == null)
        {
            return false;
        }

        // Only jump if player is on ground
        if (!mc.thePlayer.onGround)
        {
            return false;
        }

        KeyBinding jumpKey = mc.gameSettings.keyBindJump;
        int keyCode = jumpKey.getKeyCode();
        
        // Set the key state to pressed
        KeyBinding.setKeyBindState(keyCode, true);
        
        // Process the key press - this triggers the jump
        KeyBinding.onTick(keyCode);
        
        // Keep the key pressed for this tick - it will be released naturally
        // or can be released in the next tick if needed
        return true;
    }
    
    /**
     * Releases the jump key. Should be called after a jump has been processed.
     */
    public static void releaseJumpKey(Minecraft mc)
    {
        if (mc == null || mc.gameSettings == null || mc.gameSettings.keyBindJump == null)
        {
            return;
        }

        KeyBinding jumpKey = mc.gameSettings.keyBindJump;
        int keyCode = jumpKey.getKeyCode();
        KeyBinding.setKeyBindState(keyCode, false);
    }
}

