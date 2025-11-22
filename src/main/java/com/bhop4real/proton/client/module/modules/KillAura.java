package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.event.StrafeEvent;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.DoubleSetting;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import com.bhop4real.proton.client.module.settings.IntSetting;
import com.bhop4real.proton.client.module.settings.SeparatorSetting;
import com.bhop4real.proton.client.util.PointPickingUtil;
import com.bhop4real.proton.client.util.RandomUtil;
import com.bhop4real.proton.client.util.rotation.Rotation;
import com.bhop4real.proton.client.util.rotation.RotationSettings;
import com.bhop4real.proton.client.util.rotation.RotationUtils;
import com.bhop4real.proton.client.module.modules.autoclicker.AutoClickerRandomization;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * KillAura combat module - automatically attacks entities within range
 */
public class KillAura extends Module {
    private static final class Defaults {
        static final int MIN_CPS = 10;
        static final int MAX_CPS = 12;
        static final double RANGE = 3.5;
        static final double SCAN_RANGE = 0.5;
        static final double THROUGH_WALLS_RANGE = 3.0;
        static final String TARGET_MODE = "Single";
        static final String PRIORITY = "Distance";
        static final int FOV = 180;
        static final boolean SWING = true;
        static final boolean KEEP_SPRINT = true;
        static final boolean CLICK_ONLY = false;

        // Rotation defaults
        static final boolean SILENT_ROTATION = false;
        static final boolean MOVEMENT_CORRECTION = false;

        // Point picking defaults
        static final String POINT_PICKING_MODE = "Head";
        static final int SMART_SWITCH_DISTANCE = 3;
        static final String POINT_RANDOMIZATION = "None";
        static final int RANDOMIZATION_STRENGTH = 50;
        static final boolean EXTRA_RANDOM = false;

        // AutoClicker integration defaults
        static final boolean USE_AUTOCLICKER_SETTINGS = false;
    }

    // CPS Settings
    private final IntSetting minCPS;
    private final IntSetting maxCPS;

    // Range Settings
    private final DoubleSetting range;
    private final DoubleSetting scanRange;
    private final DoubleSetting throughWallsRange;

    // Target Settings
    private final EnumSetting targetMode;
    private final EnumSetting priority;
    private final IntSetting fov;

    // Behavior Settings
    private final BooleanSetting swing;
    private final BooleanSetting keepSprint;
    private final BooleanSetting clickOnly;

    // Rotation Settings
    private final BooleanSetting silentRotation;
    private final BooleanSetting movementCorrection;

    // Point Picking Settings
    private final EnumSetting pointPickingMode;
    private final IntSetting smartSwitchDistance;
    private final EnumSetting pointRandomization;
    private final IntSetting randomizationStrength;
    private final BooleanSetting extraRandom;

    // AutoClicker Integration Settings
    private final BooleanSetting useAutoClickerSettings;

    // Internal state
    private EntityLivingBase currentTarget;
    private long nextAttackTime;
    private long attackDelay;
    
    // AutoClicker integration state
    private final AutoClickerRandomization randomization = new AutoClickerRandomization();
    private boolean attackKeyHeld = false;
    private long pendingReleaseTime = -1;

    // Point randomization state
    private double circleAngle = 0.0;
    private double horizontalOffset = 0.0;

    public KillAura() {
        super("KillAura", new String[] { "killaura", "ka", "aura" }, "Automatically attacks entities within range",
                Module.Category.COMBAT, 0);

        // CPS Settings
        addSetting(new SeparatorSetting("Attack Speed"));
        minCPS = new IntSetting("Min CPS", "Minimum clicks per second", Defaults.MIN_CPS, 1, 20);
        maxCPS = new IntSetting("Max CPS", "Maximum clicks per second", Defaults.MAX_CPS, 1, 20);
        addSetting(minCPS);
        addSetting(maxCPS);

        // Range Settings
        addSetting(new SeparatorSetting("Range"));
        range = new DoubleSetting("Range", "Maximum attack range in blocks", Defaults.RANGE, 3.0, 6.0, 0.1);
        scanRange = new DoubleSetting("Scan Range", "Additional scan distance for target detection",
                Defaults.SCAN_RANGE, 0.0, 2.0, 0.1);
        throughWallsRange = new DoubleSetting("Through Walls Range", "Attack through walls range",
                Defaults.THROUGH_WALLS_RANGE, 0.0, 6.0, 0.1);
        addSetting(range);
        addSetting(scanRange);
        addSetting(throughWallsRange);

        // Target Settings
        addSetting(new SeparatorSetting("Targeting"));
        targetMode = new EnumSetting("Target Mode", "How to select targets", Defaults.TARGET_MODE, "Single", "Switch",
                "Multi");
        priority = new EnumSetting("Priority", "Target priority mode", Defaults.PRIORITY, "Distance", "Health",
                "Direction", "Armor");
        fov = new IntSetting("FOV", "Field of view for targeting (degrees)", Defaults.FOV, 30, 360);
        addSetting(targetMode);
        addSetting(priority);
        addSetting(fov);

        // Behavior Settings
        addSetting(new SeparatorSetting("Behavior"));
        swing = new BooleanSetting("Swing", "Swing arm when attacking", Defaults.SWING);
        keepSprint = new BooleanSetting("Keep Sprint", "Maintain sprint while attacking", Defaults.KEEP_SPRINT);
        clickOnly = new BooleanSetting("Click Only", "Only attack when attack key is held", Defaults.CLICK_ONLY);
        addSetting(swing);
        addSetting(keepSprint);
        addSetting(clickOnly);

        // Rotation Settings
        addSetting(new SeparatorSetting("Rotations"));
        silentRotation = new BooleanSetting("Silent Rotation", "Show rotations on player model (realistic mode)",
                Defaults.SILENT_ROTATION);
        movementCorrection = new BooleanSetting("Movement Correction", "Fix movement direction when rotating",
                Defaults.MOVEMENT_CORRECTION);
        addSetting(silentRotation);
        addSetting(movementCorrection);

        // Point Picking Settings
        addSetting(new SeparatorSetting("Point Picking"));
        pointPickingMode = new EnumSetting("Point Picking Mode", "Which point on the target to aim at",
                Defaults.POINT_PICKING_MODE,
                "Head", "Front", "BestHitVec", "ClosestHitVec", "Smart");
        addSetting(pointPickingMode);
        smartSwitchDistance = new IntSetting("Smart Switch Distance", "Distance threshold for Smart mode (blocks)",
                Defaults.SMART_SWITCH_DISTANCE, 1, 10);
        smartSwitchDistance.setVisibilitySupplier(() -> "Smart".equalsIgnoreCase(pointPickingMode.getValue()));
        addSetting(smartSwitchDistance);

        // Point Randomization Settings
        addSetting(new SeparatorSetting("Point Randomization"));
        pointRandomization = new EnumSetting("Point Randomization", "How to randomize the aim point",
                Defaults.POINT_RANDOMIZATION,
                "None", "Basic", "Circle", "Horizontal");
        addSetting(pointRandomization);
        randomizationStrength = new IntSetting("Randomization Strength", "Strength of point randomization (0-100%)",
                Defaults.RANDOMIZATION_STRENGTH, 0, 100);
        randomizationStrength.setVisibilitySupplier(() -> !"None".equalsIgnoreCase(pointRandomization.getValue()));
        addSetting(randomizationStrength);
        extraRandom = new BooleanSetting("Extra Random", "Apply an extra layer of noise to point randomization",
                Defaults.EXTRA_RANDOM);
        extraRandom.setVisibilitySupplier(() -> !"None".equalsIgnoreCase(pointRandomization.getValue()));
        addSetting(extraRandom);

        // AutoClicker Integration Settings
        addSetting(new SeparatorSetting("AutoClicker Integration"));
        useAutoClickerSettings = new BooleanSetting("Use AutoClicker Settings",
                "Use AutoClicker module settings for attack timing and clicking", Defaults.USE_AUTOCLICKER_SETTINGS);
        addSetting(useAutoClickerSettings);
        
        // Hide CPS settings when using AutoClicker settings
        minCPS.setVisibilitySupplier(() -> !useAutoClickerSettings.getValue());
        maxCPS.setVisibilitySupplier(() -> !useAutoClickerSettings.getValue());
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        nextAttackTime = 0L;
        calculateAttackDelay();
        circleAngle = 0.0;
        horizontalOffset = 0.0;
        randomization.resetAll();
        releaseAttackKey();
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        nextAttackTime = 0L;
        circleAngle = 0.0;
        horizontalOffset = 0.0;
        randomization.resetAll();
        releaseAttackKey();
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        if (mc.currentScreen != null) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Handle AutoClicker key release if needed
        if (attackKeyHeld && pendingReleaseTime > 0 && currentTime >= pendingReleaseTime) {
            releaseAttackKey();
        }

        // Check if click-only mode is enabled
        if (clickOnly.getValue() && !mc.gameSettings.keyBindAttack.isKeyDown()) {
            currentTarget = null;
            return;
        }

        // Update target
        updateTarget();

        // Attack if we have a target and enough time has passed
        if (currentTarget != null && currentTime >= nextAttackTime) {
            attackEntity(currentTarget);
            nextAttackTime = currentTime + attackDelay;
            calculateAttackDelay(); // Recalculate for next attack
        }
    }

    /**
     * Updates the current target based on targeting settings
     */
    private void updateTarget() {
        String mode = targetMode.getValue();

        if ("Single".equalsIgnoreCase(mode)) {
            // Single mode: keep the same target until it's invalid
            if (currentTarget != null && isValidTarget(currentTarget)) {
                return;
            }
            currentTarget = findBestTarget();
        } else if ("Switch".equalsIgnoreCase(mode)) {
            // Switch mode: always find the best target
            currentTarget = findBestTarget();
        } else if ("Multi".equalsIgnoreCase(mode)) {
            // Multi mode: attack all valid targets (for now, just attack best target each
            // tick)
            currentTarget = findBestTarget();
        }
    }

    /**
     * Finds the best target based on priority settings
     */
    private EntityLivingBase findBestTarget() {
        Targets targetsModule = Targets.get();
        List<EntityLivingBase> candidates = new ArrayList<>();

        double scanDist = range.getValue() + scanRange.getValue();
        float fovHalfAngle = fov.getValue() / 2.0F;

        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        float currentYaw = mc.thePlayer.rotationYaw;

        // Collect valid targets
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase living = (EntityLivingBase) entity;

            if (living == mc.thePlayer) {
                continue;
            }

            // Check if entity is allowed by Targets module
            if (targetsModule != null && !targetsModule.isEntityAllowed(living)) {
                continue;
            }

            // Check distance
            double distance = mc.thePlayer.getDistanceToEntity(living);
            if (distance > scanDist) {
                continue;
            }

            // Check FOV
            Vec3 targetPos = new Vec3(living.posX, living.posY + living.getEyeHeight(), living.posZ);
            float[] rotation = computeRotation(eyes, targetPos);
            if (rotation == null) {
                continue;
            }

            float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotation[0] - currentYaw));
            if (yawDiff > fovHalfAngle) {
                continue;
            }

            candidates.add(living);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Sort by priority
        candidates.sort(getComparator());

        return candidates.get(0);
    }

    /**
     * Gets the comparator for sorting targets based on priority mode
     */
    private Comparator<EntityLivingBase> getComparator() {
        String priorityMode = priority.getValue();

        if ("Health".equalsIgnoreCase(priorityMode)) {
            return Comparator.comparingDouble(EntityLivingBase::getHealth);
        } else if ("Direction".equalsIgnoreCase(priorityMode)) {
            return (a, b) -> {
                Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
                float currentYaw = mc.thePlayer.rotationYaw;
                float currentPitch = mc.thePlayer.rotationPitch;

                Vec3 posA = new Vec3(a.posX, a.posY + a.getEyeHeight(), a.posZ);
                Vec3 posB = new Vec3(b.posX, b.posY + b.getEyeHeight(), b.posZ);

                float[] rotA = computeRotation(eyes, posA);
                float[] rotB = computeRotation(eyes, posB);

                if (rotA == null || rotB == null) {
                    return 0;
                }

                float angleA = Math.abs(MathHelper.wrapAngleTo180_float(rotA[0] - currentYaw))
                        + Math.abs(rotA[1] - currentPitch);
                float angleB = Math.abs(MathHelper.wrapAngleTo180_float(rotB[0] - currentYaw))
                        + Math.abs(rotB[1] - currentPitch);

                return Float.compare(angleA, angleB);
            };
        } else if ("Armor".equalsIgnoreCase(priorityMode)) {
            return Comparator.comparingInt(EntityLivingBase::getTotalArmorValue);
        } else // Distance
        {
            return Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e));
        }
    }

    /**
     * Checks if a target is valid for attacking
     */
    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity == null || !entity.isEntityAlive()) {
            return false;
        }

        Targets targetsModule = Targets.get();
        if (targetsModule != null && !targetsModule.isEntityAllowed(entity)) {
            return false;
        }

        double distance = mc.thePlayer.getDistanceToEntity(entity);
        double maxRange = range.getValue() + scanRange.getValue();

        if (distance > maxRange) {
            return false;
        }

        // Check FOV
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 targetPos = new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        float[] rotation = computeRotation(eyes, targetPos);
        if (rotation == null) {
            return false;
        }

        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(rotation[0] - mc.thePlayer.rotationYaw));
        float fovHalfAngle = fov.getValue() / 2.0F;

        return yawDiff <= fovHalfAngle;
    }

    /**
     * Applies point randomization based on mode (from AimAssist)
     */
    private Vec3 applyPointRandomization(EntityLivingBase target, Vec3 basePoint) {
        String mode = pointRandomization.getValue();
        if ("None".equalsIgnoreCase(mode) || basePoint == null) {
            return basePoint;
        }

        double strength = randomizationStrength.getValue() / 100.0D;
        if (strength <= 0.0) {
            return basePoint;
        }

        AxisAlignedBB box = target.getEntityBoundingBox();
        if (box == null) {
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

        if ("Basic".equalsIgnoreCase(mode)) {
            // Random offset in all directions
            offsetX = RandomUtil.nextDouble(-maxOffset, maxOffset);
            offsetY = RandomUtil.nextDouble(-maxOffset, maxOffset);
            offsetZ = RandomUtil.nextDouble(-maxOffset, maxOffset);
        } else if ("Circle".equalsIgnoreCase(mode)) {
            // Circular motion on-screen (2D circle in view plane)
            circleAngle += 0.1D; // Increment angle
            if (circleAngle > Math.PI * 2.0D) {
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
                    basePoint.zCoord - eyes.zCoord);
            double dist = Math.sqrt(toTarget.xCoord * toTarget.xCoord + toTarget.zCoord * toTarget.zCoord);
            if (dist > 1.0E-6D) {
                // Perpendicular vector
                double perpX = -toTarget.zCoord / dist;
                double perpZ = toTarget.xCoord / dist;

                offsetX = perpX * circleOffsetX + (toTarget.xCoord / dist) * circleOffsetZ;
                offsetZ = perpZ * circleOffsetX + (toTarget.zCoord / dist) * circleOffsetZ;
                offsetY = 0.0; // Circle mode only affects horizontal plane
            }
        } else if ("Horizontal".equalsIgnoreCase(mode)) {
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
                    basePoint.zCoord - eyes.zCoord);
            double dist = Math.sqrt(toTarget.xCoord * toTarget.xCoord + toTarget.zCoord * toTarget.zCoord);
            if (dist > 1.0E-6D) {
                // Perpendicular vector
                double perpX = -toTarget.zCoord / dist;
                double perpZ = toTarget.xCoord / dist;

                offsetX = perpX * horizontalOffset;
                offsetZ = perpZ * horizontalOffset;
                offsetY = 0.0; // Horizontal mode only affects horizontal plane
            }
        }

        // Apply extra random layer if enabled
        if (extraRandom.getValue()) {
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
                basePoint.zCoord + offsetZ);
    }

    /**
     * Attacks the specified entity
     */
    private void attackEntity(EntityLivingBase target) {
        if (target == null) {
            return;
        }

        // Always prevent breaking blocks - check if we're hovering a block
        if (mc.objectMouseOver != null && 
            mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            // Don't attack if hovering a block
            return;
        }

        // Get aim point using point picking mode
        Vec3 basePoint = PointPickingUtil.getAimPoint(target, pointPickingMode.getValue(),
                smartSwitchDistance.getValue());

        // Apply point randomization if enabled
        Vec3 aimPoint = applyPointRandomization(target, basePoint);

        if (aimPoint != null) {
            // Calculate rotation to aim point
            Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
            float[] rotation = PointPickingUtil.computeRotation(eyes, aimPoint);

            if (rotation != null) {
                // Set rotation using RotationUtils
                RotationSettings rotationSettings = new RotationSettings();
                rotationSettings.strafe.setValue(movementCorrection.getValue());
                Rotation targetRotation = new Rotation(rotation[0], rotation[1]);
                RotationUtils.setTargetRotation(targetRotation, rotationSettings);

                // Apply silent rotation (realistic mode) if enabled
                if (silentRotation.getValue() && RotationUtils.currentRotation != null) {
                    // Set head and body rotation to match server rotation for realistic appearance
                    mc.thePlayer.rotationYawHead = RotationUtils.currentRotation.getYaw();
                    mc.thePlayer.renderYawOffset = RotationUtils.currentRotation.getYaw();
                }
            }
        }

        // Swing arm if enabled (client-side animation)
        if (swing.getValue()) {
            mc.thePlayer.swingItem(); // This plays the client-side swing animation
        }

        // Use AutoClicker's click method if enabled, otherwise use standard attack
        if (useAutoClickerSettings.getValue()) {
            // AutoClicker method: simulate key press (will attack what player is looking at, which is our target)
            performAutoClickerClick();
        } else {
            // Standard attack method: send attack packet directly
            mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));
        }

        // Handle sprint
        if (!keepSprint.getValue() && mc.thePlayer.isSprinting()) {
            mc.thePlayer.setSprinting(false);
        }
    }

    /**
     * Calculates the delay until the next attack based on CPS settings or AutoClicker settings
     */
    private void calculateAttackDelay() {
        if (useAutoClickerSettings.getValue()) {
            // Use AutoClicker's delay calculation
            AutoClicker autoClicker = AutoClicker.get();
            if (autoClicker != null) {
                AutoClickerRandomization.Profile profile = AutoClickerRandomization.Profile.fromLabel(
                    autoClicker.randomizationMode.getValue());
                AutoClickerRandomization.Config config = new AutoClickerRandomization.Config(
                    autoClicker.leftMinCPS.getValue(),
                    autoClicker.leftMaxCPS.getValue(),
                    autoClicker.leftMinDelayMs.getValue(),
                    autoClicker.leftMaxDelayMs.getValue(),
                    autoClicker.advancedOffsetMinMs.getValue(),
                    autoClicker.advancedOffsetMaxMs.getValue()
                );

                AutoClickerRandomization.Mode mode = AutoClickerRandomization.Mode.fromLabel(
                    autoClicker.cpsMode.getValue());
                attackDelay = randomization.nextDelay(profile, mode, config);
            } else {
                // Fallback to default if AutoClicker not found
                int min = Math.min(minCPS.getValue(), maxCPS.getValue());
                int max = Math.max(minCPS.getValue(), maxCPS.getValue());
                int cps = min + (int) (Math.random() * (max - min + 1));
                attackDelay = 1000L / Math.max(1, cps);
            }
        } else {
            // Use KillAura's own CPS settings
            int min = Math.min(minCPS.getValue(), maxCPS.getValue());
            int max = Math.max(minCPS.getValue(), maxCPS.getValue());

            // Randomly select a CPS value
            int cps = min + (int) (Math.random() * (max - min + 1));

            // Convert CPS to delay in milliseconds
            attackDelay = 1000L / Math.max(1, cps);
        }
    }

    /**
     * Performs a click using AutoClicker's method (simulates key press)
     */
    private void performAutoClickerClick() {
        // Check again for blocks before clicking
        if (mc.objectMouseOver != null && 
            mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        long holdReference = Math.max(1L, attackDelay);
        releaseAttackKey();

        // Simulate key press using KeyBinding (like AutoClicker)
        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.setKeyBindState(attackKey, true);
        KeyBinding.onTick(attackKey); // This is crucial - it processes the key press
        scheduleReleaseForDelay(holdReference);
    }

    /**
     * Schedules the release of the attack key after a delay (from AutoClicker)
     */
    private void scheduleReleaseForDelay(long referenceDelay) {
        long safeDelay = Math.max(25L, referenceDelay);
        int minHold = (int) Math.max(14L, Math.round(safeDelay * 0.16));
        int maxHold = (int) Math.max(minHold + 6, Math.round(safeDelay * 0.33) + 14);
        if (maxHold <= minHold) {
            maxHold = minHold + 4;
        }

        int hold = RandomUtil.nextInt(minHold, maxHold + 1);

        if (RandomUtil.nextDouble() < 0.08) {
            hold += RandomUtil.nextInt(12, 40);
        }

        if (RandomUtil.nextDouble() < 0.03) {
            hold += RandomUtil.nextInt(40, 90);
        }

        pendingReleaseTime = System.currentTimeMillis() + hold;
        attackKeyHeld = true;
    }

    /**
     * Releases the attack key (from AutoClicker)
     */
    private void releaseAttackKey() {
        if (!attackKeyHeld) {
            pendingReleaseTime = -1;
            return;
        }

        if (mc != null && mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        }
        attackKeyHeld = false;
        pendingReleaseTime = -1;
    }

    /**
     * Computes rotation from one position to another
     */
    private float[] computeRotation(Vec3 from, Vec3 to) {
        if (from == null || to == null) {
            return null;
        }

        double diffX = to.xCoord - from.xCoord;
        double diffY = to.yCoord - from.yCoord;
        double diffZ = to.zCoord - from.zCoord;

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0 / Math.PI));

        return new float[] { yaw, pitch };
    }

    /**
     * Public getter for silent rotation setting (used by mixin)
     */
    public boolean isSilentRotationEnabled() {
        return silentRotation.getValue();
    }

    /**
     * Handles strafe event for movement correction
     * Delegates to RotationUtils which applies the correction
     */
    @SubscribeEvent
    public void onStrafe(StrafeEvent event) {
        // Movement correction is handled by RotationUtils.onStrafe()
        // when the strafe setting is enabled in RotationSettings
        // This event handler is here for potential future custom logic
    }
}
