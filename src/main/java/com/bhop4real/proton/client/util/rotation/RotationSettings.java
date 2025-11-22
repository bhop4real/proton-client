package com.bhop4real.proton.client.util.rotation;

import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.DoubleSetting;
import com.bhop4real.proton.client.module.settings.IntSetting;
import com.bhop4real.proton.client.module.settings.StringSetting;

import java.util.concurrent.ThreadLocalRandom;

public class RotationSettings {

    public BooleanSetting applyServerSide = new BooleanSetting("Apply Server Side", "Applies rotation server-side only",
            true);
    public BooleanSetting prioritizeRequest = new BooleanSetting("Prioritize Request", "Prioritizes rotation requests",
            false);
    public BooleanSetting strafe = new BooleanSetting("Strafe", "Enables strafe fix", false);
    public BooleanSetting strict = new BooleanSetting("Strict", "Enables strict strafe fix", false);
    public BooleanSetting instant = new BooleanSetting("Instant", "Instantly rotates to target", false);
    public BooleanSetting legitimize = new BooleanSetting("Legitimize", "Adds randomization to rotation", false);

    public DoubleSetting minRotationDifference = new DoubleSetting("Min Rotation Diff", "Minimum rotation difference",
            1.0, 0.0, 180.0, 0.1);
    public DoubleSetting angleResetDifference = new DoubleSetting("Angle Reset Diff",
            "Angle difference to reset rotation", 1.0, 0.0, 180.0, 0.1);
    public IntSetting resetTicks = new IntSetting("Reset Ticks", "Ticks to reset rotation", 1, 0, 20);

    public DoubleSetting horizontalSpeed = new DoubleSetting("Horizontal Speed", "Horizontal rotation speed", 180.0,
            0.0, 180.0, 1.0);
    public DoubleSetting verticalSpeed = new DoubleSetting("Vertical Speed", "Vertical rotation speed", 180.0, 0.0,
            180.0, 1.0);

    public DoubleSetting randomHorizontalSpeed = new DoubleSetting("Random H Speed", "Random horizontal speed", 0.0,
            0.0, 180.0, 1.0);
    public DoubleSetting randomVerticalSpeed = new DoubleSetting("Random V Speed", "Random vertical speed", 0.0, 0.0,
            180.0, 1.0);

    public IntSetting shortStopDurationMin = new IntSetting("Short Stop Min", "Minimum short stop duration", 1, 0, 20);
    public IntSetting shortStopDurationMax = new IntSetting("Short Stop Max", "Maximum short stop duration", 1, 0, 20);

    public StringSetting minRotationDifferenceResetTiming = new StringSetting("Reset Timing", "When to reset rotation",
            "Always");

    public boolean immediate = false;

    // Simulation fields
    public float rotDiffBuildUp = 0f;
    public int maxThresholdReachAttempts = 0;
    public DoubleSetting rotationDiffBuildUpToStop = new DoubleSetting("RotationDiffBuildUpToStop",
            "Rotation diff build up to stop", 180.0, 50.0, 720.0, 1.0);
    public IntSetting maxThresholdAttemptsToStop = new IntSetting("MaxThresholdAttemptsToStop",
            "Max threshold attempts to stop", 1, 0, 5);
    public BooleanSetting simulateShortStop = new BooleanSetting("SimulateShortStop", "Simulate short stop", false);

    public float getHorizontalSpeed() {
        return (float) (horizontalSpeed.getValue() + Math.random() * randomHorizontalSpeed.getValue());
    }

    public float getVerticalSpeed() {
        return (float) (verticalSpeed.getValue() + Math.random() * randomVerticalSpeed.getValue());
    }

    public boolean shouldPerformShortStop() {
        if (Math.abs(rotDiffBuildUp) < rotationDiffBuildUpToStop.getValue() || !simulateShortStop.getValue())
            return false;

        if (maxThresholdReachAttempts < maxThresholdAttemptsToStop.getValue()) {
            maxThresholdReachAttempts++;
            return false;
        }

        return true;
    }

    public void resetSimulateShortStopData() {
        rotDiffBuildUp = 0f;
        maxThresholdReachAttempts = 0;
    }
}
