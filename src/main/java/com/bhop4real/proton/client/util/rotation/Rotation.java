package com.bhop4real.proton.client.util.rotation;

import com.bhop4real.proton.client.util.MinecraftInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import javax.vecmath.Vector2f;

public class Rotation implements MinecraftInstance {
    private float yaw;
    private float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public Rotation abs() {
        return new Rotation(Math.abs(yaw), Math.abs(pitch));
    }

    public Rotation minus(Rotation other) {
        return new Rotation(yaw - other.getYaw(), pitch - other.getPitch());
    }

    public Rotation plus(Rotation other) {
        return new Rotation(yaw + other.getYaw(), pitch + other.getPitch());
    }

    public Rotation times(float value) {
        return new Rotation(yaw * value, pitch * value);
    }

    public Rotation div(float value) {
        return new Rotation(yaw / value, pitch / value);
    }

    public static Rotation of(Vector2f vec) {
        return new Rotation(vec.x, vec.y);
    }

    public Rotation plusDiff(Rotation other) {
        return this.plus(of(RotationUtils.angleDifferences(other, this)));
    }

    public void toPlayer(EntityPlayer player) {
        toPlayer(player, true, true);
    }

    public void toPlayer(EntityPlayer player, boolean changeYaw, boolean changePitch) {
        if (Float.isNaN(yaw) || Float.isNaN(pitch) || pitch > 90 || pitch < -90)
            return;

        fixedSensitivity(mc.gameSettings.mouseSensitivity);

        if (changeYaw)
            player.rotationYaw = yaw;
        if (changePitch)
            player.rotationPitch = pitch;
    }

    public Rotation fixedSensitivity(float sensitivity) {
        float gcd = RotationUtils.getFixedAngleDelta(sensitivity);

        yaw = RotationUtils.getFixedSensitivityAngle(yaw, RotationUtils.serverRotation.getYaw(), gcd);
        pitch = RotationUtils.getFixedSensitivityAngle(pitch, RotationUtils.serverRotation.getPitch(), gcd);

        return this.withLimitedPitch(90f);
    }

    public Rotation withLimitedPitch(float value) {
        pitch = MathHelper.clamp_float(pitch, -value, value);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Rotation rotation = (Rotation) obj;
        return Float.compare(rotation.yaw, yaw) == 0 && Float.compare(rotation.pitch, pitch) == 0;
    }

    public void applyStrafeToPlayer(com.bhop4real.proton.client.event.StrafeEvent event, boolean strict) {
        int dif = (int) ((MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - yaw - 23.5f - 135) + 180) / 45);
        float yaw = this.yaw;
        float strafe = event.getStrafe();
        float forward = event.getForward();
        float friction = event.getFriction();
        float calcForward = 0f;
        float calcStrafe = 0f;

        switch (dif) {
            case 0:
                calcForward = forward;
                calcStrafe = strafe;
                break;
            case 1:
                calcForward += forward;
                calcStrafe -= forward;
                calcForward += strafe;
                calcStrafe += strafe;
                break;
            case 2:
                calcForward = strafe;
                calcStrafe = -forward;
                break;
            case 3:
                calcForward -= forward;
                calcStrafe -= forward;
                calcForward += strafe;
                calcStrafe -= strafe;
                break;
            case 4:
                calcForward = -forward;
                calcStrafe = -strafe;
                break;
            case 5:
                calcForward -= forward;
                calcStrafe += forward;
                calcForward -= strafe;
                calcStrafe -= strafe;
                break;
            case 6:
                calcForward = -strafe;
                calcStrafe = forward;
                break;
            case 7:
                calcForward += forward;
                calcStrafe += forward;
                calcForward -= strafe;
                calcStrafe += strafe;
                break;
        }

        if (calcForward > 1f || calcForward < 0.9f && calcForward > 0.3f || calcForward < -1f
                || calcForward > -0.9f && calcForward < -0.3f) {
            calcForward *= 0.5f;
        }

        if (calcStrafe > 1f || calcStrafe < 0.9f && calcStrafe > 0.3f || calcStrafe < -1f
                || calcStrafe > -0.9f && calcStrafe < -0.3f) {
            calcStrafe *= 0.5f;
        }

        float d = calcStrafe * calcStrafe + calcForward * calcForward;

        if (d >= 1.0E-4f) {
            d = MathHelper.sqrt_float(d);
            if (d < 1.0f)
                d = 1.0f;
            d = friction / d;
            calcStrafe *= d;
            calcForward *= d;
            float yawSin = MathHelper.sin((float) (yaw * Math.PI / 180.0));
            float yawCos = MathHelper.cos((float) (yaw * Math.PI / 180.0));
            mc.thePlayer.motionX += (calcStrafe * yawCos - calcForward * yawSin);
            mc.thePlayer.motionZ += (calcForward * yawCos + calcStrafe * yawSin);
        }
    }

    public static final Rotation ZERO = new Rotation(0f, 0f);
}
