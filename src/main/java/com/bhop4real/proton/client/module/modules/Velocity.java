package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.Proton;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import com.bhop4real.proton.client.module.settings.IntSetting;
import com.bhop4real.proton.client.util.JumpUtil;
import com.bhop4real.proton.client.util.SecureRandomUtil;
import net.minecraft.util.MathHelper;

public class Velocity extends Module
{
    public EnumSetting mode;
    public IntSetting delay;
    public BooleanSetting ignoreFireDamage;
    public IntSetting chance;
    
    private int hitTicks = 0;
    private boolean wasHit = false;
    private boolean groundOnHit = false;
    private int lastHurtTime = 0;
    private int lastHurtResistantTime = 0;
    private float lastHealth = -1.0F;
    private double lastMotionX = 0.0D;
    private double lastMotionZ = 0.0D;
    private boolean pendingVelocityJump = false;
    private long velocityTriggerTime = -1L;
    private long velocityExpireTime = -1L;
    
    public Velocity()
    {
        super("Velocity", new String[]{"velo", "antikb"}, "Reduces knockback taken", Module.Category.COMBAT, 0);
        
        // Add settings
        mode = new EnumSetting("Mode", "Velocity reduction mode", "Legit", "Legit", "Normal");
        delay = new IntSetting("Jump Delay", "Ticks to wait before jumping after hit (0-5)", 0, 0, 5);
        ignoreFireDamage = new BooleanSetting("Ignore Fire Damage", "Do not trigger jumps from fire ticks", true);
        chance = new IntSetting("Chance", "Percent chance to react to velocity (0-100)", 100, 0, 100);
        
        addSetting(mode);
        addSetting(delay);
        addSetting(ignoreFireDamage);
        addSetting(chance);
    }
    
    @Override
    public void onUpdate()
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }

        double motionX = mc.thePlayer.motionX;
        double motionZ = mc.thePlayer.motionZ;
        double horizontalMotion = MathHelper.sqrt_double(motionX * motionX + motionZ * motionZ);
        double motionDelta = MathHelper.sqrt_double(Math.pow(motionX - lastMotionX, 2) + Math.pow(motionZ - lastMotionZ, 2));

        boolean legitMode = mode.getValue().equalsIgnoreCase("Legit");
        int currentHurtTime = mc.thePlayer.hurtTime;
        int currentHurtResistant = mc.thePlayer.hurtResistantTime;
        float currentHealth = mc.thePlayer.getHealth();
        int maxHurtTime = mc.thePlayer.maxHurtTime;

        boolean hurtTimeSpike = currentHurtTime > 0 && currentHurtTime > lastHurtTime;
        boolean firstHurtFrame = maxHurtTime > 0 && currentHurtTime == maxHurtTime;
        boolean resistReset = currentHurtResistant > lastHurtResistantTime;
        boolean healthDrop = lastHealth >= 0.0F && currentHealth < lastHealth - 0.05F;
        boolean velocityImpulse = motionDelta > 0.08D && currentHurtTime > 0;

        boolean justHit = hurtTimeSpike || firstHurtFrame || resistReset || healthDrop || velocityImpulse;
        if (justHit && ignoreFireDamage.getValue() && mc.thePlayer.isBurning() && !velocityImpulse)
        {
            justHit = false;
        }

        if (legitMode)
        {
            if (justHit)
            {
                if (rollChance())
                {
                    wasHit = true;
                    hitTicks = 0;
                    groundOnHit = mc.thePlayer.onGround || mc.thePlayer.isCollidedVertically;
                    Proton.logger.debug("[Velocity] Hit detected via onUpdate (delay={} ticks, hurtTime={}, motionDelta={})",
                        delay.getValue(), currentHurtTime, String.format("%.4f", motionDelta));
                }
                else
                {
                    Proton.logger.debug("[Velocity] Hit ignored due to chance roll (chance={}%)", chance.getValue());
                }
            }

            if (wasHit)
            {
                if (hitTicks >= delay.getValue())
                {
                if (attemptJump())
                    {
                        wasHit = false;
                        groundOnHit = false;
                        hitTicks = 0;
                    }
                }

                hitTicks++;

                if (hitTicks > delay.getValue() + 10)
                {
                    // Expire the hit if we never got the chance to jump (e.g. stayed airborne)
                    wasHit = false;
                    hitTicks = 0;
                    groundOnHit = false;
                }
            }
            else if (currentHurtTime == 0)
            {
                hitTicks = 0;
                groundOnHit = false;
            }
        }
        else
        {
            if (horizontalMotion > 0.05)
            {
                mc.thePlayer.motionX *= 0.3;
                mc.thePlayer.motionZ *= 0.3;
            }

            if (mc.thePlayer.motionY > 0.3)
            {
                mc.thePlayer.motionY *= 0.6;
            }

            groundOnHit = false;
            wasHit = false;
            hitTicks = 0;
        }

        long now = System.currentTimeMillis();
        if (pendingVelocityJump)
        {
            if (now >= velocityTriggerTime)
            {
                if (attemptJump())
                {
                    pendingVelocityJump = false;
                    wasHit = false;
                    groundOnHit = false;
                }
                else if (velocityExpireTime > 0 && now > velocityExpireTime)
                {
                    pendingVelocityJump = false;
                }
            }
            else if (velocityExpireTime > 0 && now > velocityExpireTime)
            {
                pendingVelocityJump = false;
            }
        }

        if (!wasHit && currentHurtTime == 0)
        {
            hitTicks = 0;
        }

        lastHurtTime = currentHurtTime;
        lastHurtResistantTime = currentHurtResistant;
        lastHealth = currentHealth;
        lastMotionX = motionX;
        lastMotionZ = motionZ;
    }

    public void onVelocityPacket(double packetMotionX, double packetMotionY, double packetMotionZ)
    {
        if (!isEnabled() || !mode.getValue().equalsIgnoreCase("Legit") || mc.thePlayer == null)
        {
            return;
        }

        double horizontal = MathHelper.sqrt_double(packetMotionX * packetMotionX + packetMotionZ * packetMotionZ);
        if (horizontal < 0.03D && Math.abs(packetMotionY) < 0.03D)
        {
            return;
        }

        if (ignoreFireDamage.getValue() && mc.thePlayer.isBurning())
        {
            return;
        }

        if (!rollChance())
        {
            Proton.logger.debug("[Velocity] Velocity packet ignored due to chance roll (chance={}%)", chance.getValue());
            return;
        }

        wasHit = true;
        hitTicks = 0;
        groundOnHit = mc.thePlayer.onGround || mc.thePlayer.isCollidedVertically;
        pendingVelocityJump = true;
        long now = System.currentTimeMillis();
        long delayMs = Math.max(0, delay.getValue()) * 50L;
        velocityTriggerTime = now + delayMs;
        velocityExpireTime = velocityTriggerTime + 250L;
        Proton.logger.debug("[Velocity] Velocity packet received (delay={}ms, motionY={}, horizontal={})",
            delayMs, String.format("%.4f", packetMotionY), String.format("%.4f", horizontal));
    }

    @Override
    public void onEnable()
    {
        hitTicks = 0;
        wasHit = false;
        groundOnHit = false;
        lastHurtTime = mc.thePlayer != null ? mc.thePlayer.hurtTime : 0;
        lastHurtResistantTime = mc.thePlayer != null ? mc.thePlayer.hurtResistantTime : 0;
        lastHealth = mc.thePlayer != null ? mc.thePlayer.getHealth() : -1.0F;
        lastMotionX = mc.thePlayer != null ? mc.thePlayer.motionX : 0.0D;
        lastMotionZ = mc.thePlayer != null ? mc.thePlayer.motionZ : 0.0D;
        pendingVelocityJump = false;
        velocityTriggerTime = -1L;
        velocityExpireTime = -1L;
    }
    
    @Override
    public void onDisable()
    {
        wasHit = false;
        hitTicks = 0;
        groundOnHit = false;
        lastHurtTime = 0;
        lastHurtResistantTime = 0;
        lastHealth = -1.0F;
        lastMotionX = 0.0D;
        lastMotionZ = 0.0D;
        pendingVelocityJump = false;
        velocityTriggerTime = -1L;
        velocityExpireTime = -1L;
    }

    private boolean attemptJump()
    {
        if (mc.thePlayer == null)
        {
            return false;
        }

        boolean wasOnGround = mc.thePlayer.onGround;
        double beforeMotionY = mc.thePlayer.motionY;

        if (!JumpUtil.pressJumpKey(mc))
        {
            Proton.logger.debug("[Velocity] Jump key unavailable, aborting attempt");
            return false;
        }

        if (!wasOnGround && groundOnHit)
        {
            mc.thePlayer.motionY = Math.max(mc.thePlayer.motionY, 0.42F);
            mc.thePlayer.isAirBorne = true;
            mc.thePlayer.onGround = false;
        }

        // Release jump key after a short delay to allow proper jump processing
        // The key will be released naturally in the next tick
        
        Proton.logger.debug("[Velocity] attemptJump triggered (wasOnGround={}, groundOnHit={}, beforeY={}, afterY={})",
            wasOnGround, groundOnHit, String.format("%.4f", beforeMotionY), String.format("%.4f", mc.thePlayer.motionY));
        return true;
    }

    private boolean rollChance()
    {
        int percent = chance.getValue();
        if (percent >= 100)
        {
            return true;
        }
        if (percent <= 0)
        {
            return false;
        }
        int roll = SecureRandomUtil.nextInt(0, 100);
        return roll < percent;
    }
}
