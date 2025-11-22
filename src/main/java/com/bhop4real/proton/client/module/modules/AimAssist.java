package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import com.bhop4real.proton.client.module.settings.IntSetting;
import com.bhop4real.proton.client.module.settings.SeparatorSetting;
import com.bhop4real.proton.client.util.PointPickingUtil;
import com.bhop4real.proton.client.util.RandomUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

/**
 * AimAssist module - smoothly rotates towards selected targets using configurable speeds.
 */
public class AimAssist extends Module
{
    private static final class Defaults
    {
        static final int MIN_AIM_SPEED = 5;
        static final int MAX_AIM_SPEED = 12;
        static final int MIN_RESPONSE_TIME = 0;
        static final int MAX_RESPONSE_TIME = 100;
        static final int MIN_RANGE = 0;
        static final int MAX_RANGE = 6;
        static final int FOV = 90;
        static final String POINT_PICKING_MODE = "Head";
        static final String POINT_RANDOMIZATION_MODE = "None";
        static final int POINT_RANDOMIZATION_STRENGTH = 50;
        static final boolean POINT_EXTRA_RANDOM = false;
        static final String RANDOMIZATION_MODE = "None";
        static final boolean SMOOTH_MODE = false;
        static final String TARGET_PRIORITY = "Distance";
        static final int SMART_SWITCH_DISTANCE = 3;
        static final boolean ONLY_WHEN_CLICKING = false;
    }

    // Aim Speed settings
    private final IntSetting minAimSpeed;
    private final IntSetting maxAimSpeed;
    
    // Response Time settings
    private final IntSetting minResponseTime;
    private final IntSetting maxResponseTime;
    
    // Range settings
    private final IntSetting minRange;
    private final IntSetting maxRange;
    
    // FOV setting
    private final IntSetting fov;
    
    // Target Priority
    private final EnumSetting targetPriority;
    
    // Point picking mode
    private final EnumSetting pointPickingMode;
    private final IntSetting smartSwitchDistance;
    
    // Point Randomization
    private final EnumSetting pointRandomizationMode;
    private final IntSetting pointRandomizationStrength;
    private final BooleanSetting pointExtraRandom;
    
    // Randomization mode
    private final EnumSetting randomizationMode;
    
    // Smooth mode
    private final BooleanSetting smoothMode;
    
    // Only when clicking
    private final BooleanSetting onlyWhenClicking;

    // Internal state
    private EntityLivingBase currentTarget;
    private long responseReadyAtMs;
    
    // Point randomization state
    private double circleAngle = 0.0;
    private double horizontalOffset = 0.0;

    public AimAssist()
    {
        super("AimAssist", new String[]{"aim", "aimbot"}, "Smoothly rotates to allowed targets", Module.Category.COMBAT, 0);

        // Aim Speed section
        addSetting(new SeparatorSetting("Aim Speed"));
        minAimSpeed = new IntSetting("Min Speed", "Minimum rotation speed (deg/tick)", Defaults.MIN_AIM_SPEED, 1, 50);
        maxAimSpeed = new IntSetting("Max Speed", "Maximum rotation speed (deg/tick)", Defaults.MAX_AIM_SPEED, 1, 50);
        addSetting(minAimSpeed);
        addSetting(maxAimSpeed);

        // Response Time section
        addSetting(new SeparatorSetting("Response Time"));
        minResponseTime = new IntSetting("Min Response Time", "Minimum reaction delay before rotating (ms)", Defaults.MIN_RESPONSE_TIME, 0, 500);
        maxResponseTime = new IntSetting("Max Response Time", "Maximum reaction delay before rotating (ms)", Defaults.MAX_RESPONSE_TIME, 0, 500);
        addSetting(minResponseTime);
        addSetting(maxResponseTime);

        // Range section
        addSetting(new SeparatorSetting("Range"));
        minRange = new IntSetting("Min Range", "Minimum target distance (blocks)", Defaults.MIN_RANGE, 0, 20);
        maxRange = new IntSetting("Max Range", "Maximum target distance (blocks)", Defaults.MAX_RANGE, 1, 20);
        addSetting(minRange);
        addSetting(maxRange);

        // FOV section
        addSetting(new SeparatorSetting("Targeting"));
        targetPriority = new EnumSetting("Target Priority", "How to prioritize targets", Defaults.TARGET_PRIORITY, "Distance", "Angle");
        addSetting(targetPriority);
        fov = new IntSetting("FOV", "Field of view angle (degrees)", Defaults.FOV, 0, 360);
        addSetting(fov);

        // Point Picking Mode
        addSetting(new SeparatorSetting("Point Picking"));
        pointPickingMode = new EnumSetting("Point Picking Mode", "Which point on the target to aim at", Defaults.POINT_PICKING_MODE, "Head", "Front", "BestHitVec", "ClosestHitVec", "Smart");
        addSetting(pointPickingMode);
        smartSwitchDistance = new IntSetting("Smart Switch Distance", "Distance threshold for Smart mode (blocks)", Defaults.SMART_SWITCH_DISTANCE, 1, 10);
        smartSwitchDistance.setVisibilitySupplier(() -> "Smart".equalsIgnoreCase(pointPickingMode.getValue()));
        addSetting(smartSwitchDistance);

        // Point Randomization
        addSetting(new SeparatorSetting("Point Randomization"));
        pointRandomizationMode = new EnumSetting("Point Randomization", "How to randomize the aim point", Defaults.POINT_RANDOMIZATION_MODE, "None", "Basic", "Circle", "Horizontal");
        addSetting(pointRandomizationMode);
        pointRandomizationStrength = new IntSetting("Randomization Strength", "Strength of point randomization (0-100%)", Defaults.POINT_RANDOMIZATION_STRENGTH, 0, 100);
        pointRandomizationStrength.setVisibilitySupplier(() -> !"None".equalsIgnoreCase(pointRandomizationMode.getValue()));
        addSetting(pointRandomizationStrength);
        pointExtraRandom = new BooleanSetting("Extra Random", "Apply an extra layer of noise to point randomization", Defaults.POINT_EXTRA_RANDOM);
        pointExtraRandom.setVisibilitySupplier(() -> !"None".equalsIgnoreCase(pointRandomizationMode.getValue()));
        addSetting(pointExtraRandom);

        // Randomization Mode
        addSetting(new SeparatorSetting("Rotation Randomization"));
        randomizationMode = new EnumSetting("Randomization Mode", "How to randomize aim behavior", Defaults.RANDOMIZATION_MODE, "None", "Basic", "Advanced");
        addSetting(randomizationMode);

        // Smooth Mode
        smoothMode = new BooleanSetting("Smooth Mode", "Use ease in/out curves instead of linear speed", Defaults.SMOOTH_MODE);
        addSetting(smoothMode);
        
        // Only When Clicking
        addSetting(new SeparatorSetting("Behavior"));
        onlyWhenClicking = new BooleanSetting("Only When Clicking", "Only aim assist when attack key is held", Defaults.ONLY_WHEN_CLICKING);
        addSetting(onlyWhenClicking);
    }

    @Override
    public void onEnable()
    {
        currentTarget = null;
        responseReadyAtMs = 0L;
        circleAngle = 0.0;
        horizontalOffset = 0.0;
    }

    @Override
    public void onDisable()
    {
        currentTarget = null;
        responseReadyAtMs = 0L;
        circleAngle = 0.0;
        horizontalOffset = 0.0;
    }

    @Override
    public void onUpdate()
    {
        if (mc.thePlayer == null || mc.theWorld == null)
        {
            return;
        }

        if (mc.currentScreen != null)
        {
            return;
        }

        // Check if only when clicking is enabled
        if (onlyWhenClicking.getValue() && !isAttackHeld())
        {
            currentTarget = null;
            responseReadyAtMs = 0L;
            return;
        }

        // Enforce value corrections (min <= max)
        enforceValueCorrections();

        // Acquire target
        EntityLivingBase target = acquireTarget();
        if (target == null)
        {
            currentTarget = null;
            responseReadyAtMs = 0L;
            return;
        }

        // Handle response delay
        long now = System.currentTimeMillis();
        if (currentTarget != target)
        {
            currentTarget = target;
            int delay = sampleResponseDelay();
            responseReadyAtMs = now + delay;
        }

        if (now < responseReadyAtMs)
        {
            return;
        }

        // Get aim point and apply rotation
        Vec3 basePoint = PointPickingUtil.getAimPoint(target, pointPickingMode.getValue(), smartSwitchDistance.getValue());
        if (basePoint != null)
        {
            Vec3 aimPoint = applyPointRandomization(target, basePoint);
            if (aimPoint != null)
            {
                applyRotation(aimPoint);
            }
        }
    }

    /**
     * Ensures min values are not greater than max values (value correction)
     */
    private void enforceValueCorrections()
    {
        // Aim Speed correction
        if (minAimSpeed.getValue() > maxAimSpeed.getValue())
        {
            minAimSpeed.setValue(maxAimSpeed.getValue());
        }

        // Response Time correction
        if (minResponseTime.getValue() > maxResponseTime.getValue())
        {
            minResponseTime.setValue(maxResponseTime.getValue());
        }

        // Range correction
        if (minRange.getValue() > maxRange.getValue())
        {
            minRange.setValue(maxRange.getValue());
        }
    }

    /**
     * Acquires the best target based on priority mode
     */
    private EntityLivingBase acquireTarget()
    {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;
        
        int minRangeValue = Math.min(minRange.getValue(), maxRange.getValue());
        int maxRangeValue = Math.max(minRange.getValue(), maxRange.getValue());
        float fovHalfAngle = fov.getValue() / 2.0F;
        
        Targets targetsModule = Targets.get();
        EntityLivingBase bestTarget = null;
        double bestScore = Double.MAX_VALUE;
        
        for (Entity entity : mc.theWorld.loadedEntityList)
        {
            if (!(entity instanceof EntityLivingBase))
            {
                continue;
            }
            
            EntityLivingBase candidate = (EntityLivingBase) entity;
            
            if (candidate == mc.thePlayer)
            {
                continue;
            }
            
            if (!isEntityAllowed(candidate, targetsModule))
            {
                continue;
            }
            
            double distance = mc.thePlayer.getDistanceToEntity(candidate);
            if (distance < minRangeValue || distance > maxRangeValue)
            {
                continue;
            }
            
            // Check FOV
            Vec3 targetPos = new Vec3(candidate.posX, candidate.posY + candidate.getEyeHeight(), candidate.posZ);
            float[] rotation = PointPickingUtil.computeRotation(eyes, targetPos);
            if (rotation == null)
            {
                continue;
            }
            
            float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotation[0] - currentYaw));
            if (yawDiff > fovHalfAngle)
            {
                continue;
            }
            
            // Calculate score based on priority mode
            double score;
            String priority = targetPriority.getValue();
            if ("Angle".equalsIgnoreCase(priority))
            {
                // Score based on angle difference (smaller is better)
                float pitchDiff = Math.abs(rotation[1] - currentPitch);
                score = yawDiff + pitchDiff * 0.5;
            }
            else // Distance
            {
                // Score based on distance (smaller is better)
                score = distance;
            }
            
            if (score < bestScore)
            {
                bestScore = score;
                bestTarget = candidate;
            }
        }
        
        return bestTarget;
    }


    /**
     * Applies point randomization based on mode
     */
    private Vec3 applyPointRandomization(EntityLivingBase target, Vec3 basePoint)
    {
        String mode = pointRandomizationMode.getValue();
        if ("None".equalsIgnoreCase(mode) || basePoint == null)
        {
            return basePoint;
        }
        
        double strength = pointRandomizationStrength.getValue() / 100.0D;
        if (strength <= 0.0)
        {
            return basePoint;
        }
        
        AxisAlignedBB box = target.getEntityBoundingBox();
        if (box == null)
        {
            return basePoint;
        }
        
        double boxWidth = box.maxX - box.minX;
        double boxHeight = box.maxY - box.minY;
        double boxDepth = box.maxZ - box.minZ;
        double maxOffset = Math.max(boxWidth, Math.max(boxHeight, boxDepth)) * strength * 0.5D;
        
        // Calculate offsets based on mode
        double offsetX = 0.0;
        double offsetY = 0.0;
        double offsetZ = 0.0;
        
        if ("Basic".equalsIgnoreCase(mode))
        {
            // Random offset in all directions
            offsetX = RandomUtil.nextDouble(-maxOffset, maxOffset);
            offsetY = RandomUtil.nextDouble(-maxOffset, maxOffset);
            offsetZ = RandomUtil.nextDouble(-maxOffset, maxOffset);
        }
        else if ("Circle".equalsIgnoreCase(mode))
        {
            // Circular motion on-screen (2D circle in view plane)
            circleAngle += 0.1D; // Increment angle
            if (circleAngle > Math.PI * 2.0D)
            {
                circleAngle -= Math.PI * 2.0D;
            }
            
            double radius = maxOffset;
            double circleOffsetX = Math.cos(circleAngle) * radius;
            double circleOffsetZ = Math.sin(circleAngle) * radius;
            
            // Apply offset perpendicular to view direction
            Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
            Vec3 toTarget = new Vec3(
                basePoint.xCoord - eyes.xCoord,
                0.0,
                basePoint.zCoord - eyes.zCoord
            );
            double dist = Math.sqrt(toTarget.xCoord * toTarget.xCoord + toTarget.zCoord * toTarget.zCoord);
            if (dist > 1.0E-6D)
            {
                // Perpendicular vector
                double perpX = -toTarget.zCoord / dist;
                double perpZ = toTarget.xCoord / dist;
                
                offsetX = perpX * circleOffsetX + (toTarget.xCoord / dist) * circleOffsetZ;
                offsetZ = perpZ * circleOffsetX + (toTarget.zCoord / dist) * circleOffsetZ;
                offsetY = 0.0; // Circle mode only affects horizontal plane
            }
        }
        else if ("Horizontal".equalsIgnoreCase(mode))
        {
            // Smooth horizontal movement
            double targetOffset = RandomUtil.nextDouble(-maxOffset, maxOffset);
            
            // Smooth interpolation to avoid sudden changes
            double smoothing = 0.15D; // How fast to approach target
            horizontalOffset = horizontalOffset * (1.0 - smoothing) + targetOffset * smoothing;
            
            // Apply horizontal offset (perpendicular to view direction)
            Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
            Vec3 toTarget = new Vec3(
                basePoint.xCoord - eyes.xCoord,
                0.0,
                basePoint.zCoord - eyes.zCoord
            );
            double dist = Math.sqrt(toTarget.xCoord * toTarget.xCoord + toTarget.zCoord * toTarget.zCoord);
            if (dist > 1.0E-6D)
            {
                // Perpendicular vector
                double perpX = -toTarget.zCoord / dist;
                double perpZ = toTarget.xCoord / dist;
                
                offsetX = perpX * horizontalOffset;
                offsetZ = perpZ * horizontalOffset;
                offsetY = 0.0; // Horizontal mode only affects horizontal plane
            }
        }
        
        // Apply extra random layer if enabled
        if (pointExtraRandom.getValue())
        {
            // Apply an extra layer of basic randomization with 0.15-0.2 strength
            double extraStrength = RandomUtil.nextDouble(0.15D, 0.2D);
            double extraMaxOffset = Math.max(boxWidth, Math.max(boxHeight, boxDepth)) * extraStrength * 0.5D;
            
            double extraOffsetX = RandomUtil.nextDouble(-extraMaxOffset, extraMaxOffset);
            double extraOffsetY = RandomUtil.nextDouble(-extraMaxOffset, extraMaxOffset);
            double extraOffsetZ = RandomUtil.nextDouble(-extraMaxOffset, extraMaxOffset);
            
            offsetX += extraOffsetX;
            offsetY += extraOffsetY;
            offsetZ += extraOffsetZ;
        }
        
        return new Vec3(
            basePoint.xCoord + offsetX,
            basePoint.yCoord + offsetY,
            basePoint.zCoord + offsetZ
        );
    }


    /**
     * Applies rotation to look at the target point
     */
    private void applyRotation(Vec3 targetPoint)
    {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        float[] rotation = PointPickingUtil.computeRotation(eyes, targetPoint);
        if (rotation == null)
        {
            return;
        }
        
        float targetYaw = rotation[0];
        float targetPitch = rotation[1];
        float currentYaw = mc.thePlayer.rotationYaw;
        float currentPitch = mc.thePlayer.rotationPitch;
        
        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;
        
        // Calculate speed
        int minSpeed = Math.min(minAimSpeed.getValue(), maxAimSpeed.getValue());
        int maxSpeed = Math.max(minAimSpeed.getValue(), maxAimSpeed.getValue());
        float speed = (float) RandomUtil.nextDouble(minSpeed, maxSpeed + 1.0D);
        
        // Apply smooth mode if enabled
        if (smoothMode.getValue())
        {
            // TODO: Implement ease in/out curves
            // For now, just use the speed
        }
        
        // Clamp rotation change
        float yawChange = MathHelper.clamp_float(yawDiff, -speed, speed);
        float pitchChange = MathHelper.clamp_float(pitchDiff, -speed, speed);
        
        // Calculate new rotations
        float newYaw = currentYaw + yawChange;
        float newPitch = MathHelper.clamp_float(currentPitch + pitchChange, -90.0F, 90.0F);
        
        // Apply GCD fix to quantize rotations
        float[] gcdAdjusted = applyMouseGcd(newYaw, newPitch, currentYaw, currentPitch);
        newYaw = gcdAdjusted[0];
        newPitch = gcdAdjusted[1];
        
        // Re-clamp after GCD adjustment to ensure we don't exceed speed limits
        float finalYawDiff = MathHelper.wrapAngleTo180_float(newYaw - currentYaw);
        float finalPitchDiff = newPitch - currentPitch;
        
        if (Math.abs(finalYawDiff) > speed)
        {
            finalYawDiff = MathHelper.clamp_float(finalYawDiff, -speed, speed);
            newYaw = currentYaw + finalYawDiff;
        }
        
        if (Math.abs(finalPitchDiff) > speed)
        {
            finalPitchDiff = MathHelper.clamp_float(finalPitchDiff, -speed, speed);
            newPitch = MathHelper.clamp_float(currentPitch + finalPitchDiff, -90.0F, 90.0F);
        }
        
        // Apply rotation
        mc.thePlayer.rotationYaw = newYaw;
        mc.thePlayer.rotationYawHead = newYaw;
        mc.thePlayer.renderYawOffset = newYaw;
        mc.thePlayer.rotationPitch = newPitch;
    }

    /**
     * Applies mouse GCD (Greatest Common Divisor) fix to quantize rotations
     * This ensures rotations match the game's sensitivity quantization
     * Matches reference implementation exactly
     */
    private float[] applyMouseGcd(float targetYaw, float targetPitch, float currentYaw, float currentPitch)
    {
        // Calculate GCD factor based on sensitivity (matches reference exactly)
        float f = (float) (mc.gameSettings.mouseSensitivity * 0.6F + 0.2F);
        float multiplier = f * f * f * 1.2F;
        
        // Quantize yaw delta
        float yaw = currentYaw + (float) (Math.round((targetYaw - currentYaw) / multiplier) * multiplier);
        
        // Quantize pitch delta
        float pitch = currentPitch + (float) (Math.round((targetPitch - currentPitch) / multiplier) * multiplier);
        pitch = MathHelper.clamp_float(pitch, -90.0F, 90.0F);
        
        return new float[]{yaw, pitch};
    }

    /**
     * Samples a random response delay
     */
    private int sampleResponseDelay()
    {
        int min = Math.min(minResponseTime.getValue(), maxResponseTime.getValue());
        int max = Math.max(minResponseTime.getValue(), maxResponseTime.getValue());
        if (min <= 0 && max <= 0)
        {
            return 0;
        }
        return RandomUtil.nextInt(min, max + 1);
    }

    /**
     * Checks if an entity is allowed as a target
     */
    private boolean isEntityAllowed(EntityLivingBase entity, Targets targetsModule)
    {
        if (targetsModule != null)
        {
            return targetsModule.isEntityAllowed(entity);
        }
        
        if (entity instanceof EntityPlayer)
        {
            return entity.isEntityAlive() && !entity.isInvisible();
        }
        
        return false;
    }

    /**
     * Checks if the attack key is currently held
     */
    private boolean isAttackHeld()
    {
        return mc.gameSettings.keyBindAttack.isKeyDown() || Mouse.isButtonDown(0);
    }
}

