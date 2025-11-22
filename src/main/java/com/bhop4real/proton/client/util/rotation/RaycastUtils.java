package com.bhop4real.proton.client.util.rotation;

import com.bhop4real.proton.client.util.MinecraftInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RaycastUtils implements MinecraftInstance {

    public static Entity raycastEntity(double range, float yaw, float pitch, Predicate<Entity> entityFilter) {
        Entity renderViewEntity = mc.getRenderViewEntity();

        if (renderViewEntity == null || mc.theWorld == null)
            return null;

        double blockReachDistance = range;
        Vec3 eyePosition = renderViewEntity.getPositionEyes(1.0f);
        Vec3 entityLook = RotationUtils.getVectorForRotation(yaw, pitch);
        Vec3 vec = eyePosition.addVector(entityLook.xCoord * blockReachDistance, entityLook.yCoord * blockReachDistance,
                entityLook.zCoord * blockReachDistance);

        List<Entity> entityList = mc.theWorld.getEntitiesInAABBexcluding(renderViewEntity,
                renderViewEntity
                        .getEntityBoundingBox().addCoord(entityLook.xCoord * blockReachDistance,
                                entityLook.yCoord * blockReachDistance, entityLook.zCoord * blockReachDistance)
                        .expand(1.0, 1.0, 1.0),
                null);

        Entity pointedEntity = null;

        for (Entity entity : entityList) {
            if (!entityFilter.test(entity))
                continue;
            if (!(entity instanceof EntityLivingBase) && !(entity instanceof EntityLargeFireball))
                continue;
            if (entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator())
                continue;
            if (!entity.canBeCollidedWith())
                continue;

            float collisionBorderSize = entity.getCollisionBorderSize();
            AxisAlignedBB axisAlignedBB = entity.getEntityBoundingBox().expand(collisionBorderSize, collisionBorderSize,
                    collisionBorderSize);
            MovingObjectPosition movingObjectPosition = axisAlignedBB.calculateIntercept(eyePosition, vec);

            if (axisAlignedBB.isVecInside(eyePosition)) {
                if (blockReachDistance >= 0.0) {
                    pointedEntity = entity;
                    blockReachDistance = 0.0;
                }
            } else if (movingObjectPosition != null) {
                double eyeDistance = eyePosition.distanceTo(movingObjectPosition.hitVec);

                if (eyeDistance < blockReachDistance || blockReachDistance == 0.0) {
                    if (entity == renderViewEntity.ridingEntity && !renderViewEntity.canRiderInteract()) {
                        if (blockReachDistance == 0.0)
                            pointedEntity = entity;
                    } else {
                        pointedEntity = entity;
                        blockReachDistance = eyeDistance;
                    }
                }
            }
        }

        return pointedEntity;
    }

    public static Entity raycastEntity(double range, Predicate<Entity> entityFilter) {
        return raycastEntity(range, RotationUtils.serverRotation.getYaw(), RotationUtils.serverRotation.getPitch(),
                entityFilter);
    }

    public static void runWithModifiedRaycastResult(Rotation rotation, double range, double wallRange,
            Consumer<MovingObjectPosition> action) {
        Entity entity = mc.getRenderViewEntity();
        Entity prevPointedEntity = mc.pointedEntity;
        MovingObjectPosition prevObjectMouseOver = mc.objectMouseOver;

        if (entity != null && mc.theWorld != null) {
            mc.pointedEntity = null;

            double buildReach = mc.playerController.getCurrentGameType().isCreative() ? 5.0 : 4.5;

            Vec3 vec3 = entity.getPositionEyes(1.0f);
            Vec3 vec31 = RotationUtils.getVectorForRotation(rotation);
            Vec3 vec32 = vec3.addVector(vec31.xCoord * buildReach, vec31.yCoord * buildReach,
                    vec31.zCoord * buildReach);

            mc.objectMouseOver = mc.theWorld.rayTraceBlocks(vec3, vec32, false, false, true);

            double d1 = buildReach;
            boolean flag = false;

            if (mc.playerController.extendedReach()) {
                d1 = 6.0;
            } else if (buildReach > 3) {
                flag = true;
            }

            if (mc.objectMouseOver != null) {
                d1 = mc.objectMouseOver.hitVec.distanceTo(vec3);
            }

            Entity pointedEntity = null;
            Vec3 vec33 = null;

            List<Entity> list = mc.theWorld.getEntitiesInAABBexcluding(
                    entity, entity.getEntityBoundingBox()
                            .addCoord(vec31.xCoord * d1, vec31.yCoord * d1, vec31.zCoord * d1).expand(1.0, 1.0, 1.0),
                    null);

            double d2 = d1;

            for (Entity entity1 : list) {
                if (entity1 instanceof EntityPlayer && ((EntityPlayer) entity1).isSpectator())
                    continue;
                if (!entity1.canBeCollidedWith())
                    continue;

                float f1 = entity1.getCollisionBorderSize();
                List<AxisAlignedBB> boxes = new ArrayList<>();
                boxes.add(entity1.getEntityBoundingBox().expand(f1, f1, f1));

                for (AxisAlignedBB box : boxes) {
                    MovingObjectPosition intercept = box.calculateIntercept(vec3, vec32);

                    if (box.isVecInside(vec3)) {
                        if (d2 >= 0) {
                            pointedEntity = entity1;
                            vec33 = intercept == null ? vec3 : intercept.hitVec;
                            d2 = 0.0;
                        }
                    } else if (intercept != null) {
                        double d3 = vec3.distanceTo(intercept.hitVec);

                        if (!RotationUtils.isVisible(intercept.hitVec)) {
                            if (d3 <= wallRange) {
                                if (d3 < d2 || d2 == 0.0) {
                                    pointedEntity = entity1;
                                    vec33 = intercept.hitVec;
                                    d2 = d3;
                                }
                            }
                            continue;
                        }

                        if (d3 < d2 || d2 == 0.0) {
                            if (entity1 == entity.ridingEntity && !entity.canRiderInteract()) {
                                if (d2 == 0.0) {
                                    pointedEntity = entity1;
                                    vec33 = intercept.hitVec;
                                }
                            } else {
                                pointedEntity = entity1;
                                vec33 = intercept.hitVec;
                                d2 = d3;
                            }
                        }
                    }
                }
            }

            if (pointedEntity != null && flag && vec3.distanceTo(vec33) > range) {
                pointedEntity = null;
                mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, null,
                        new BlockPos(vec33));
            }

            if (pointedEntity != null && (d2 < d1 || mc.objectMouseOver == null)) {
                mc.objectMouseOver = new MovingObjectPosition(pointedEntity, vec33);

                if (pointedEntity instanceof EntityLivingBase || pointedEntity instanceof EntityItemFrame) {
                    mc.pointedEntity = pointedEntity;
                }
            }

            action.accept(mc.objectMouseOver);

            mc.objectMouseOver = prevObjectMouseOver;
            mc.pointedEntity = prevPointedEntity;
        }
    }
}
