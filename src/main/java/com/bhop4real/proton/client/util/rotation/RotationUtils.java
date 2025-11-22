package com.bhop4real.proton.client.util.rotation;

import com.bhop4real.proton.client.event.PacketEvent;
import com.bhop4real.proton.client.event.StrafeEvent;
import com.bhop4real.proton.client.util.MinecraftInstance;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.vecmath.Vector2f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RotationUtils implements MinecraftInstance {

    public static Rotation targetRotation;
    public static Rotation currentRotation;
    public static Rotation serverRotation = new Rotation(0f, 0f);

    public static float renderYaw;
    public static float renderPitch;
    public static float prevRenderYaw;
    public static float prevRenderPitch;

    private static final int MAX_CAPTURE_TICKS = 3;
    public static List<Rotation> lastRotations = new ArrayList<>();

    static {
        for (int i = 0; i < MAX_CAPTURE_TICKS; i++) {
            lastRotations.add(Rotation.ZERO);
        }
    }

    public static RotationSettings activeSettings;
    public static int resetTicks = 0;

    // ShortStop logic simulation
    private static long shortStopEndTime = 0;

    public static void setServerRotation(Rotation rotation) {
        List<Rotation> updatedList = new ArrayList<>(MAX_CAPTURE_TICKS);
        updatedList.add(rotation);
        for (int i = 0; i < MAX_CAPTURE_TICKS - 1; i++) {
            updatedList.add(lastRotations.get(i));
        }
        lastRotations = updatedList;
        serverRotation = rotation;
    }

    public static VecRotation faceBlock(BlockPos blockPos, boolean throughWalls, boolean targetUpperFace,
            double hRangeMin, double hRangeMax) {
        if (mc.theWorld == null || mc.thePlayer == null || blockPos == null)
            return null;

        Vec3 eyesPos = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 startPos = new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        VecRotation visibleVec = null;
        VecRotation invisibleVec = null;

        double yRangeMin = 0.0;
        double yRangeMax = targetUpperFace ? 0.01 : 1.0;
        double step = 0.1; // Simplified iteration step

        for (double x = hRangeMin; x <= hRangeMax; x += step) {
            for (double y = yRangeMin; y <= yRangeMax; y += step) {
                for (double z = hRangeMin; z <= hRangeMax; z += step) {
                    Vec3 posVec = startPos.addVector(x, y, z);
                    double dist = eyesPos.distanceTo(posVec);

                    double diffX = posVec.xCoord - eyesPos.xCoord;
                    double diffY = posVec.yCoord - eyesPos.yCoord;
                    double diffZ = posVec.zCoord - eyesPos.zCoord;
                    double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

                    Rotation rotation = new Rotation(
                            MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f),
                            MathHelper.wrapAngleTo180_float((float) -Math.toDegrees(Math.atan2(diffY, diffXZ))))
                            .fixedSensitivity(mc.gameSettings.mouseSensitivity);

                    Vec3 rotationVector = getVectorForRotation(rotation);
                    Vec3 vector = eyesPos.addVector(rotationVector.xCoord * dist, rotationVector.yCoord * dist,
                            rotationVector.zCoord * dist);

                    VecRotation currentVec = new VecRotation(posVec, rotation);
                    MovingObjectPosition raycast = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, true, false);

                    Rotation currRot = currentRotation != null ? currentRotation
                            : new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);

                    if (raycast != null && raycast.getBlockPos().equals(blockPos)
                            && (!targetUpperFace || raycast.sideHit == EnumFacing.UP)) {
                        if (visibleVec == null || rotationDifference(currentVec.getRotation(),
                                currRot) < rotationDifference(visibleVec.getRotation(), currRot)) {
                            visibleVec = currentVec;
                        }
                    } else if (throughWalls) {
                        MovingObjectPosition invisibleRaycast = performRaytrace(blockPos, rotation);
                        if (invisibleRaycast == null || !invisibleRaycast.getBlockPos().equals(blockPos)) {
                            continue;
                        }

                        if (invisibleVec == null || rotationDifference(currentVec.getRotation(),
                                currRot) < rotationDifference(invisibleVec.getRotation(), currRot)) {
                            invisibleVec = currentVec;
                        }
                    }
                }
            }
        }

        return visibleVec != null ? visibleVec : invisibleVec;
    }

    public static Rotation toRotation(Vec3 vec, boolean predict) {
        EntityPlayerSP player = mc.thePlayer;
        Vec3 eyesPos = player.getPositionEyes(1.0f);
        if (predict)
            eyesPos = eyesPos.addVector(player.motionX, player.motionY, player.motionZ);

        double diffX = vec.xCoord - eyesPos.xCoord;
        double diffY = vec.yCoord - eyesPos.yCoord;
        double diffZ = vec.zCoord - eyesPos.zCoord;

        return new Rotation(
                MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f),
                MathHelper.wrapAngleTo180_float(
                        (float) -Math.toDegrees(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)))));
    }

    public static float rotationDifference(Rotation a, Rotation b) {
        return (float) Math.hypot(angleDifference(a.getYaw(), b.getYaw()), a.getPitch() - b.getPitch());
    }

    public static float rotationDifference(Rotation a) {
        return rotationDifference(a, serverRotation);
    }

    private static Rotation limitAngleChange(Rotation currentRotation, Rotation targetRotation,
            RotationSettings settings) {
        float hSpeed = settings.getHorizontalSpeed();
        float vSpeed = settings.getVerticalSpeed();

        if (settings.instant.getValue()) {
            hSpeed = 180f;
            vSpeed = 180f;
        }

        return performAngleChange(
                currentRotation,
                targetRotation,
                hSpeed,
                vSpeed,
                !settings.instant.getValue() && settings.legitimize.getValue(),
                (float) settings.minRotationDifference.getValue(),
                settings.minRotationDifferenceResetTiming.getValue());
    }

    public static Rotation performAngleChange(Rotation currentRotation, Rotation targetRotation, float hSpeed,
            float vSpeed, boolean legitimize, float minRotationDiff, String minRotationDiffResetTiming) {
        Vector2f diffs = angleDifferences(targetRotation, currentRotation);
        float yawDiff = diffs.x;
        float pitchDiff = diffs.y;

        float rotationDifference = (float) Math.hypot(yawDiff, pitchDiff);

        boolean isShortStopActive = System.currentTimeMillis() < shortStopEndTime;

        if (isShortStopActive || (activeSettings != null && activeSettings.shouldPerformShortStop())) {
            if (!isShortStopActive && activeSettings != null) {
                int duration = ThreadLocalRandom.current().nextInt(activeSettings.shortStopDurationMin.getValue(),
                        activeSettings.shortStopDurationMax.getValue() + 1);
                shortStopEndTime = System.currentTimeMillis() + (duration * 50L); // Approximate ticks to ms
            }

            if (activeSettings != null)
                activeSettings.resetSimulateShortStopData();

            float yawSlowdown = (float) ThreadLocalRandom.current().nextDouble(0.0, 0.1);
            float pitchSlowdown = (float) ThreadLocalRandom.current().nextDouble(0.0, 0.1);

            yawDiff = getFixedAngleDelta(mc.gameSettings.mouseSensitivity)
                    * Math.round(yawDiff * yawSlowdown / getFixedAngleDelta(mc.gameSettings.mouseSensitivity));
            pitchDiff = getFixedAngleDelta(mc.gameSettings.mouseSensitivity)
                    * Math.round(pitchDiff * pitchSlowdown / getFixedAngleDelta(mc.gameSettings.mouseSensitivity));
        }

        float straightLineYaw;
        float straightLinePitch;

        if (rotationDifference == 0.0f) {
            straightLineYaw = 0;
            straightLinePitch = 0;
        } else {
            float baseYawSpeed = Math.abs(yawDiff / rotationDifference) * hSpeed;
            float basePitchSpeed = Math.abs(pitchDiff / rotationDifference) * vSpeed;

            if (legitimize) {
                baseYawSpeed *= ThreadLocalRandom.current().nextDouble(0.9, 1.1);
                basePitchSpeed *= ThreadLocalRandom.current().nextDouble(0.9, 1.1);
            }

            straightLineYaw = MathHelper.clamp_float(yawDiff, -baseYawSpeed, baseYawSpeed);
            straightLinePitch = MathHelper.clamp_float(pitchDiff, -basePitchSpeed, basePitchSpeed);
        }

        if (rotationDifference > 0F) {
            float yawJitter = (float) (ThreadLocalRandom.current().nextDouble(-0.03, 0.03) * straightLineYaw);
            float pitchJitter = (float) (ThreadLocalRandom.current().nextDouble(-0.02, 0.02) * straightLinePitch);

            straightLineYaw += yawJitter;
            straightLinePitch += pitchJitter;
        }

        float gcd = getFixedAngleDelta(mc.gameSettings.mouseSensitivity);
        float minYaw = (float) (ThreadLocalRandom.current().nextDouble(Math.min(minRotationDiff, gcd),
                minRotationDiff));
        minYaw = gcd * Math.round(minYaw / gcd);

        float minPitch = (float) (ThreadLocalRandom.current().nextDouble(Math.min(minRotationDiff, gcd),
                minRotationDiff));
        minPitch = gcd * Math.round(minPitch / gcd);

        // Apply slowdown logic inline
        straightLineYaw = applySlowDown(straightLineYaw, minYaw, minRotationDiffResetTiming, true, legitimize);
        straightLinePitch = applySlowDown(straightLinePitch, minPitch, minRotationDiffResetTiming, false, legitimize);

        return currentRotation.plus(new Rotation(straightLineYaw, straightLinePitch));
    }

    private static float applySlowDown(float diff, float min, String timing, boolean yaw, boolean applyRealism) {
        if (diff == 0f)
            return diff;

        Vector2f lastTickDiffs = angleDifferences(serverRotation, lastRotations.get(1));
        float lastTick1 = yaw ? lastTickDiffs.x : lastTickDiffs.y;

        float diffAbs = Math.abs(diff);
        boolean isSlowingDown = diffAbs <= Math.abs(lastTick1);

        float gcd = getFixedAngleDelta(mc.gameSettings.mouseSensitivity);
        float diffAbsGCD = gcd * Math.round(diffAbs / gcd);

        if (diffAbsGCD <= min && (timing.equals("Always") || (timing.equals("OnSlowDown") && isSlowingDown)
                || (timing.equals("OnStart") && lastTick1 == 0F))) {
            return 0f;
        }

        if (!applyRealism)
            return diff;

        double rangeMin;
        double rangeMax;

        if (lastTick1 == 0f) {
            float inc = 0.2f * MathHelper.clamp_float(diffAbs / 50f, 0f, 1f);
            rangeMin = 0.1 + inc;
            rangeMax = 0.5 + inc;
        } else {
            rangeMin = 0.3;
            rangeMax = 0.7;
        }

        double randomFactor = ThreadLocalRandom.current().nextDouble(rangeMin, rangeMax);
        float newDiff = (float) (lastTick1 + (diff - lastTick1) * randomFactor);

        float newDiffGCD = gcd * Math.round(newDiff / gcd);

        if (Math.abs(newDiffGCD) <= min && isSlowingDown) {
            return diff;
        } else {
            return newDiff;
        }
    }

    public static float angleDifference(float a, float b) {
        return MathHelper.wrapAngleTo180_float(a - b);
    }

    public static Vector2f angleDifferences(Rotation target, Rotation current) {
        return new Vector2f(angleDifference(target.getYaw(), current.getYaw()), target.getPitch() - current.getPitch());
    }

    public static Vec3 getVectorForRotation(float yaw, float pitch) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    public static Vec3 getVectorForRotation(Rotation rotation) {
        return getVectorForRotation(rotation.getYaw(), rotation.getPitch());
    }

    public static boolean isVisible(Vec3 vec3) {
        return mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionEyes(1.0f), vec3) == null;
    }

    public static void setTargetRotation(Rotation rotation, RotationSettings options) {
        if (Float.isNaN(rotation.getYaw()) || Float.isNaN(rotation.getPitch()) || rotation.getPitch() > 90
                || rotation.getPitch() < -90) {
            return;
        }

        if (!options.prioritizeRequest.getValue() && activeSettings != null
                && activeSettings.prioritizeRequest.getValue()) {
            return;
        }

        if (!options.applyServerSide.getValue()) {
            if (currentRotation != null) {
                mc.thePlayer.rotationYaw = currentRotation.getYaw();
                mc.thePlayer.rotationPitch = currentRotation.getPitch();
            }
            resetRotation();
        }

        targetRotation = rotation;
        resetTicks = (!options.applyServerSide.getValue() || options.resetTicks.getValue() <= 0) ? 1
                : options.resetTicks.getValue();
        activeSettings = options;

        if (options.immediate) {
            update();
        }
    }

    public static void resetRotation() {
        resetTicks = 0;
        if (currentRotation != null) {
            if (mc.thePlayer != null) {
                mc.thePlayer.rotationYaw = currentRotation.getYaw()
                        + angleDifference(mc.thePlayer.rotationYaw, currentRotation.getYaw());
                syncRotations();
            }
        }
        targetRotation = null;
        currentRotation = null;
        activeSettings = null;
    }

    public static float getFixedAngleDelta(float sensitivity) {
        return (float) (Math.pow(sensitivity * 0.6f + 0.2f, 3) * 1.2f);
    }

    public static float getFixedSensitivityAngle(float targetAngle, float startAngle, float gcd) {
        return startAngle + Math.round((targetAngle - startAngle) / gcd) * gcd;
    }

    public static MovingObjectPosition performRaytrace(BlockPos blockPos, Rotation rotation) {
        if (mc.theWorld == null || mc.thePlayer == null)
            return null;
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        double reach = mc.playerController.getBlockReachDistance();
        Vec3 vec = eyes.addVector(getVectorForRotation(rotation).xCoord * reach,
                getVectorForRotation(rotation).yCoord * reach, getVectorForRotation(rotation).zCoord * reach);
        return mc.theWorld.rayTraceBlocks(eyes, vec, false, false, true);
    }

    public static void syncRotations() {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null)
            return;

        player.prevRotationYaw = player.rotationYaw;
        player.prevRotationPitch = player.rotationPitch;
        player.renderArmYaw = player.rotationYaw;
        player.renderArmPitch = player.rotationPitch;
        player.prevRenderArmYaw = player.rotationYaw;
        player.prevRenderArmPitch = player.rotationPitch;
    }

    public static void update() {
        if (activeSettings == null || mc.thePlayer == null)
            return;

        Rotation playerRotation = new Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);

        // Check if inventory is open (simplified)
        if (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.GuiChat)) {
            // Assuming inventory open if screen is not null/chat.
            // FDPClient checks serverOpenContainer/Inventory which are more specific.
            // For now, we skip rotation if any GUI is open to be safe, or we can check if
            // it's a container.
        }

        Rotation sRotation = currentRotation != null ? currentRotation : serverRotation;

        if (resetTicks == 0) {
            if (isDifferenceAcceptableForReset(sRotation, playerRotation, activeSettings)) {
                resetRotation();
                return;
            }

            currentRotation = limitAngleChange(sRotation, playerRotation, activeSettings)
                    .fixedSensitivity(mc.gameSettings.mouseSensitivity);
            return;
        }

        if (targetRotation != null) {
            Rotation rotation = limitAngleChange(sRotation, targetRotation, activeSettings);
            if (!activeSettings.applyServerSide.getValue()) {
                rotation.toPlayer(mc.thePlayer);
            } else {
                currentRotation = rotation.fixedSensitivity(mc.gameSettings.mouseSensitivity);
            }
        }

        if (resetTicks > 0) {
            resetTicks--;
        }
    }

    private static boolean isDifferenceAcceptableForReset(Rotation curr, Rotation target, RotationSettings options) {
        if (!options.applyServerSide.getValue())
            return true;

        if (rotationDifference(target, curr) > options.angleResetDifference.getValue())
            return false;

        Vector2f diffs = angleDifferences(target, curr);
        diffs.set(Math.abs(diffs.x), Math.abs(diffs.y));

        Vector2f lastTickDiffs = angleDifferences(curr, lastRotations.get(1));
        lastTickDiffs.set(Math.abs(lastTickDiffs.x), Math.abs(lastTickDiffs.y));

        return (diffs.x <= lastTickDiffs.x && diffs.y <= lastTickDiffs.y) || !options.legitimize.getValue();
    }

    // Event Handlers

    public static void onUpdateEvent() {
        if (activeSettings != null && activeSettings.immediate) {
            activeSettings.immediate = false;
            return;
        }
        update();
    }

    public static void onStrafe(StrafeEvent event) {
        if (activeSettings == null || !activeSettings.strafe.getValue())
            return;

        if (currentRotation != null) {
            currentRotation.applyStrafeToPlayer(event, activeSettings.strict.getValue());
            event.setCanceled(true);
        }
    }

    public static void setRenderYaw(float yaw) {
        renderYaw = yaw;
    }

    public static void setRenderPitch(float pitch) {
        renderPitch = pitch;
    }

    public static void onPacketSend(C03PacketPlayer packet) {
        // The mixin already creates packets with the correct rotation values from
        // currentRotation
        // This method is called to track/update server rotation state
        if (activeSettings == null || !activeSettings.applyServerSide.getValue() || currentRotation == null)
            return;

        // Update server rotation to match what was sent
        setServerRotation(currentRotation);
    }
}
