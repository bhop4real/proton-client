package com.bhop4real.proton.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

public class Reach {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static Object[] getEntity(double range, double expand, float[] rotations) {
        if (mc.thePlayer == null || mc.theWorld == null) return null;

        MovingObjectPosition hitResult = RotationUtils.rayCastStrict(
                rotations != null ? rotations[0] : mc.thePlayer.rotationYaw,
                rotations != null ? rotations[1] : mc.thePlayer.rotationPitch,
                (float) range
        );

        if (hitResult != null && hitResult.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            Entity hitEntity = hitResult.entityHit;
            if (hitEntity instanceof EntityLivingBase) {
                return new Object[]{hitEntity, hitResult.hitVec};
            }
        }

        return null;
    }
}
