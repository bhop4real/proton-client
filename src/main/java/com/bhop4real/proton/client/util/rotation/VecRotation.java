package com.bhop4real.proton.client.util.rotation;

import net.minecraft.util.Vec3;

public class VecRotation {
    private final Vec3 vec;
    private final Rotation rotation;

    public VecRotation(Vec3 vec, Rotation rotation) {
        this.vec = vec;
        this.rotation = rotation;
    }

    public Vec3 getVec() {
        return vec;
    }

    public Rotation getRotation() {
        return rotation;
    }
}
