package com.bhop4real.proton.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.Random;

public class RotationUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static Random random = new Random();
    public static float renderPitch;
    public static float prevRenderPitch;
    public static float renderYaw;
    public static float prevRenderYaw;
    
    // Cryptix rotation system
    public static float currentYaw = 0.0f;
    private static float currentPitch = 0.0f;

    public static void setRenderYaw(float yaw) {
        mc.thePlayer.rotationYawHead = yaw;
    }

    public static float[] getRotations(BlockPos blockPos, final float n, final float n2) {
        final float[] array = getRotations(blockPos);
        return fixRotation(array[0], array[1], n, n2);
    }

    public static float[] getRotations(final BlockPos blockPos) {
        final double n = blockPos.getX() + 0.45 - mc.thePlayer.posX;
        final double n2 = blockPos.getY() + 0.45 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        final double n3 = blockPos.getZ() + 0.45 - mc.thePlayer.posZ;
        return new float[] { mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float((float)(Math.atan2(n3, n) * 57.295780181884766) - 90.0f - mc.thePlayer.rotationYaw), clampTo90(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float((float)(-(Math.atan2(n2, MathHelper.sqrt_double(n * n + n3 * n3)) * 57.295780181884766)) - mc.thePlayer.rotationPitch)) };
    }

    public static float[] getRotations(Entity entity, final float n, final float n2) {
        final float[] array = getRotations(entity);
        if (array == null) {
            return new float[] { mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch };
        }
        return fixRotation(array[0], array[1], n, n2);
    }

    public static float[] getRotations(final Entity entity) {
        if (entity == null || mc.thePlayer == null) {
            return null;
        }
        final double n = entity.posX - mc.thePlayer.posX;
        final double n2 = entity.posZ - mc.thePlayer.posZ;
        double n3;
        if (entity instanceof EntityLivingBase) {
            final EntityLivingBase entityLivingBase = (EntityLivingBase) entity;
            n3 = entityLivingBase.posY + entityLivingBase.getEyeHeight() * 0.9 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        } else {
            n3 = (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        }
        return new float[]{mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float((float) (Math.atan2(n2, n) * 57.295780181884766) - 90.0f - mc.thePlayer.rotationYaw), clampTo90(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float((float) (-(Math.atan2(n3, MathHelper.sqrt_double(n * n + n2 * n2)) * 57.295780181884766)) - mc.thePlayer.rotationPitch) + 3.0f)};
    }

    public static float clampTo90(final float n) {
        return MathHelper.clamp_float(n, -90.0f, 90.0f);
    }

    public static float[] fixRotation(float n, float n2, final float n3, final float n4) {
        float n5 = n - n3;
        final float abs = Math.abs(n5);
        final float n7 = n2 - n4;
        final float n8 = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        final double n9 = n8 * n8 * n8 * 1.2;
        final float n10 = (float) (Math.round((double) n5 / n9) * n9);
        final float n11 = (float) (Math.round((double) n7 / n9) * n9);
        n = n3 + n10;
        n2 = n4 + n11;
        if (abs >= 1.0f) {
        } else if (abs <= 0.04) {
            n += ((abs > 0.0f) ? 0.01 : -0.01);
        }
        return new float[]{n, clampTo90(n2)};
    }

    public static float angle(final double n, final double n2) {
        return (float) (Math.atan2(n - mc.thePlayer.posX, n2 - mc.thePlayer.posZ) * 57.295780181884766 * -1.0);
    }

    public static MovingObjectPosition rayCast(final Vec3 from, final double distance, final float yaw, final float pitch) {
        final float n4 = -yaw * 0.017453292f;
        final float n5 = -pitch * 0.017453292f;
        final float cos = MathHelper.cos(n4 - 3.1415927f);
        final float sin = MathHelper.sin(n4 - 3.1415927f);
        final float n6 = -MathHelper.cos(n5);
        final Vec3 vec3 = new Vec3(sin * n6, MathHelper.sin(n5), cos * n6);
        return mc.theWorld.rayTraceBlocks(from, from.addVector(vec3.xCoord * distance, vec3.yCoord * distance, vec3.zCoord * distance), false, false, false);
    }

    public static MovingObjectPosition rayCast(final double distance, final float yaw, final float pitch) {
        final Vec3 getPositionEyes = mc.thePlayer.getPositionEyes(1.0f);
        return rayCast(getPositionEyes, distance, yaw, pitch);
    }

    public static MovingObjectPosition rayCastStrict(final float yaw, final float pitch, final float range) {
        final float partialTicks = 1.0f; // Simplified
        final Entity entity = mc.thePlayer;
        MovingObjectPosition objectMouseOver;

        if (entity != null && mc.theWorld != null) {
            final double d0 = mc.playerController.getBlockReachDistance();
            objectMouseOver = entity.rayTrace(d0, partialTicks);
            double d1 = d0;
            final Vec3 vec3 = entity.getPositionEyes(partialTicks);
            final boolean flag = d0 > (double) range;

            if (objectMouseOver != null) {
                d1 = objectMouseOver.hitVec.distanceTo(vec3);
            }

            final Vec3 vec31 = getVectorForRotation(pitch, yaw);
            final Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
            Entity pointedEntity = null;
            Vec3 vec33 = null;
            final float f = 1.0F;
            final java.util.List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f, f, f), null);
            double d2 = d1;

            for (final Entity entity1 : list) {
                final float f1 = entity1.getCollisionBorderSize();
                final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                        d2 = 0.0D;
                    }
                } else if (movingobjectposition != null) {
                    final double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                    if (d3 < d2 || d2 == 0.0D) {
                        pointedEntity = entity1;
                        vec33 = movingobjectposition.hitVec;
                        d2 = d3;
                    }
                }
            }

            if (pointedEntity != null && flag && vec3.distanceTo(vec33) > range) {
                return new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, null, new BlockPos(vec33));
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                return new MovingObjectPosition(pointedEntity, vec33);
            }
        }

        return new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, new net.minecraft.util.Vec3(0, 0, 0), null, BlockPos.ORIGIN);
    }

    public static boolean isMouseOver(final float yaw, final float pitch, final Entity target, final float range) {
        final float partialTicks = 1.0f;
        final Entity entity = mc.thePlayer;
        MovingObjectPosition objectMouseOver;
        Entity mcPointedEntity = null;

        if (entity != null && mc.theWorld != null) {
            final double d0 = mc.playerController.getBlockReachDistance();
            objectMouseOver = entity.rayTrace(d0, partialTicks);
            double d1 = d0;
            final Vec3 vec3 = entity.getPositionEyes(partialTicks);
            final boolean flag = d0 > (double) range;

            if (objectMouseOver != null) {
                d1 = objectMouseOver.hitVec.distanceTo(vec3);
            }

            final Vec3 vec31 = getVectorForRotation(pitch, yaw);
            final Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
            Vec3 vec33 = null;
            final float f = 1.0F;
            final java.util.List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f, f, f), null);
            double d2 = d1;

            for (final Entity entity1 : list) {
                if (entity1 == target) {
                    final float f1 = entity1.getCollisionBorderSize();
                    final AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
                    final MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                    if (axisalignedbb.isVecInside(vec3)) {
                        if (d2 >= 0.0D) {
                            mcPointedEntity = entity1;
                            vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
                            d2 = 0.0D;
                        }
                    } else if (movingobjectposition != null) {
                        final double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                        if (d3 < d2 || d2 == 0.0D) {
                            mcPointedEntity = entity1;
                            vec33 = movingobjectposition.hitVec;
                            d2 = d3;
                        }
                    }
                }
            }

            return mcPointedEntity == target && (!flag || vec3.distanceTo(vec33) <= range);
        }

        return false;
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }

    public static com.bhop4real.proton.client.util.Vec3 getNearestPoint(final AxisAlignedBB from, final com.bhop4real.proton.client.util.Vec3 to) {
        double pointX, pointY, pointZ;
        if (to.x() >= from.maxX) {
            pointX = from.maxX;
        } else pointX = Math.max(to.x(), from.minX);
        if (to.y() >= from.maxY) {
            pointY = from.maxY;
        } else pointY = Math.max(to.y(), from.minY);
        if (to.z() >= from.maxZ) {
            pointZ = from.maxZ;
        } else pointZ = Math.max(to.z(), from.minZ);

        return new com.bhop4real.proton.client.util.Vec3(pointX, pointY, pointZ);
    }

    public static float normalize(float yaw) {
        return normalize(yaw, -180, 180);
    }

    public static float normalize(float yaw, float min, float max) {
        yaw %= 360.0F;
        if (yaw >= max) {
            yaw -= 360.0F;
        }
        if (yaw < min) {
            yaw += 360.0F;
        }
        return yaw;
    }

    public static double distanceFromYaw(final Entity entity, final boolean b) {
        return Math.abs(MathHelper.wrapAngleTo180_double(angle(entity.posX, entity.posZ) - (b ? RotationUtils.renderYaw : mc.thePlayer.rotationYaw)));
    }

    public static com.bhop4real.proton.client.util.Vec3 getEyePos() {
        if (mc.thePlayer == null) return com.bhop4real.proton.client.util.Vec3.ZERO;
        return new com.bhop4real.proton.client.util.Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
    }

    public static double randomizeDouble(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    public static int randomizeInt(double min, double max) {
        return (int) randomizeDouble(min, max);
    }
    
    // Cryptix rotation methods
    public static float[] getRotations(EntityLivingBase e, String rotationMode) {
        if (rotationMode != null && (rotationMode.equalsIgnoreCase("BlocksMC") || 
            rotationMode.equalsIgnoreCase("Vulcan"))) {

            double x = e.posX + (e.posX - e.lastTickPosX) - mc.thePlayer.posX;
            double y = e.posY - 3.4 + e.getEyeHeight() - mc.thePlayer.posY + mc.thePlayer.getEyeHeight();
            double z = e.posZ + (e.posZ - e.lastTickPosZ) - mc.thePlayer.posZ;
            double dist = Math.sqrt(Math.pow(x, 2) + Math.pow(z, 2));

            float targetYaw = (float) Math.toDegrees(-Math.atan(x / z));
            float targetPitch = (float) -Math.toDegrees(Math.atan(y / dist));

            if (rotationMode.equalsIgnoreCase("Vulcan")) {
                if (mc.thePlayer.posY < e.posY) targetPitch = 0.05F;
                if (e.posY + 1 > mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) {
                    targetPitch = (float) -Math.toDegrees(Math.atan(
                        (e.posY - 3.3 + e.getEyeHeight() - mc.thePlayer.posY + 1) / dist));
                }
            }

            if (x < 0 && z < 0) {
                targetYaw = (float) (90 + Math.toDegrees(Math.atan(z / x)));
            } else if (x > 0 && z < 0) {
                targetYaw = (float) (-90 + Math.toDegrees(Math.atan(z / x)));
            }

            if (currentYaw == 0) currentYaw = mc.thePlayer.rotationYawHead;

            float[] gcd = applyGCD(new float[] {targetYaw, targetPitch}, new float[] {currentYaw, currentPitch});
            targetYaw = gcd[0];
            targetPitch = gcd[1];
            currentYaw = wrapAngleTo180(targetYaw);

            float pitchDiff = wrapAngleTo180(currentPitch - targetPitch);
            float min = -random.nextFloat() * 10;
            float max = random.nextFloat() * 5;

            float randomFloat = min + random.nextFloat() * (max - min);
            if(Math.abs(pitchDiff) > 7.5) {
                currentPitch = clampTo90(targetPitch -= randomFloat);
            }

            return new float[] {wrapAngleTo180(currentYaw), clampTo90(currentPitch)};
        } else {
            return getRotationFromPosition(e.posX, e.posZ, e.posY + e.getEyeHeight() / 1.6);
        }
    }
    
    public static float[] applyGCD(float[] rotations, float[] prevRots) {
        float f = (float) (mc.gameSettings.mouseSensitivity * 0.6F + 0.2F);
        float multiplier = f * f * f * 1.2f;
        float yaw = prevRots[0] + (float) (Math.round((rotations[0] - prevRots[0]) / multiplier) * multiplier);
        float pitch = prevRots[1] + (float) (Math.round((rotations[1] - prevRots[1]) / multiplier) * multiplier);

        return new float[]{yaw, MathHelper.clamp_float(pitch, -90, 90)};
    }
    
    public static float getGCD() {
        float sensitivity = mc.gameSettings.mouseSensitivity;
        float f = sensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 1.2F;
        return gcd;
    }
    
    public static float[] getRotationsBlock(final BlockPos pos) {
        return getRotationFromPosition(pos.getX() + 0.5, pos.getZ() + 0.5, pos.getY() - 0.25);
    }
    
    public static float[] getRotationFromPosition(double x, double z, double y) { 
        return new float[]{(float) (Math.atan2(z - mc.thePlayer.posZ, x - mc.thePlayer.posX) * 180.0D / Math.PI - 90.0F), 
                          (float)-(Math.atan2(y - mc.thePlayer.posY - 1.2, MathHelper.sqrt_double((x - mc.thePlayer.posX) * (x - mc.thePlayer.posX) + (z - mc.thePlayer.posZ) * (z - mc.thePlayer.posZ))) * 180.0D / Math.PI)};
    }

    public static float smoothYaw(float targetYaw) {
        float playerYaw = mc.thePlayer.rotationYaw;
        if (currentYaw == 0.0f) {
            currentYaw = playerYaw;
        }
        float deltaYaw = wrapAngleTo180(rotDistance(currentYaw, targetYaw));
        currentYaw = wrapAngleTo180((float)(currentYaw + deltaYaw));
        return currentYaw;
    }

    public static float smoothPitch(float targetPitch) {
        float playerPitch = mc.thePlayer.rotationPitch;
        if (currentPitch == 0.0f) {
            currentPitch = playerPitch;
        }
        float deltaPitch = rotDistance(currentPitch, targetPitch);
        float threshold = (float)(8.0);
        if ((Math.abs(deltaPitch) < threshold)) {
            return (float) (clampTo90(currentPitch));
        }

        currentPitch += deltaPitch;
        return currentPitch;
    }

    public static float rotDistance(float src, float target) {
        float difference = wrapAngleTo180(target - src);
        return difference;
    }

    public static float wrapAngleTo180(float angle) {
        angle %= 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }
    
    public static float[] rotateToVec3(Vec3 targetVec) {
        Vec3 playerPos = mc.thePlayer.getPositionVector();

        double deltaX = targetVec.xCoord - playerPos.xCoord;
        double deltaY = targetVec.yCoord - playerPos.yCoord;
        double deltaZ = targetVec.zCoord - playerPos.zCoord;

        double yaw = Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI) - 90.0;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double pitch = -Math.atan2(deltaY, horizontalDistance / 2) * (180.0 / Math.PI);

        return new float[] {(float) yaw, (float) pitch};
    }
    
    public static float getMovementYaw() {
        float yaw = 0.0f;
        double moveForward = mc.thePlayer.moveForward;
        double moveStrafe = mc.thePlayer.moveStrafing;
        if (moveForward == 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 90.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -90.0f;
            }
        }
        else if (moveForward > 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 135.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -135.0f;
            }
        }
        else if (moveForward < 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 0.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 45.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -45.0f;
            }
        }
        return (MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) + yaw % 360 + 360) % 360;
    }
    
    public static void resetCurrentRotations() {
        currentYaw = 0.0f;
        currentPitch = 0.0f;
    }
    
    public static float getCurrentYaw() {
        return currentYaw;
    }
    
    public static float getCurrentPitch() {
        return currentPitch;
    }
}
