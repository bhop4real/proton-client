package com.bhop4real.proton.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * Utility class for picking aim points on entities.
 * Used by AimAssist and other modules.
 */
public final class PointPickingUtil
{
    private static final Minecraft mc = Minecraft.getMinecraft();

    private PointPickingUtil() {} // Prevent instantiation

    /**
     * Gets the aim point based on point picking mode
     */
    public static Vec3 getAimPoint(EntityLivingBase target, String mode, int smartSwitchDistance)
    {
        return getAimPoint(target, mode, smartSwitchDistance, false);
    }

    /**
     * Gets the aim point based on point picking mode
     * @param hitThroughWalls If true, allows picking points through walls
     */
    public static Vec3 getAimPoint(EntityLivingBase target, String mode, int smartSwitchDistance, boolean hitThroughWalls)
    {
        Vec3 basePoint = null;

        if ("Head".equalsIgnoreCase(mode))
        {
            basePoint = getHeadPoint(target);
        }
        else if ("Front".equalsIgnoreCase(mode))
        {
            basePoint = getFrontPoint(target, hitThroughWalls);
        }
        else if ("BestHitVec".equalsIgnoreCase(mode))
        {
            basePoint = getBestHitVec(target, hitThroughWalls);
        }
        else if ("ClosestHitVec".equalsIgnoreCase(mode))
        {
            basePoint = getClosestHitVec(target, hitThroughWalls);
        }
        else if ("Smart".equalsIgnoreCase(mode))
        {
            double distance = mc.thePlayer.getDistanceToEntity(target);
            if (distance < smartSwitchDistance)
            {
                basePoint = getFrontPoint(target, hitThroughWalls);
            }
            else
            {
                basePoint = getBestHitVec(target, hitThroughWalls);
            }
        }

        return basePoint;
    }

    /**
     * Gets the head point of an entity
     */
    public static Vec3 getHeadPoint(EntityLivingBase entity)
    {
        double x = entity.posX;
        double y = entity.posY + entity.getEyeHeight() * 0.92D;
        double z = entity.posZ;
        return new Vec3(x, y, z);
    }

    /**
     * Gets the front point (direction-based with pitch 0, or closest reachable point)
     */
    public static Vec3 getFrontPoint(EntityLivingBase entity)
    {
        return getFrontPoint(entity, false);
    }

    /**
     * Gets the front point (direction-based with pitch 0, or closest reachable point)
     * @param hitThroughWalls If true, allows picking points through walls
     */
    public static Vec3 getFrontPoint(EntityLivingBase entity, boolean hitThroughWalls)
    {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        AxisAlignedBB box = entity.getEntityBoundingBox();
        if (box == null)
        {
            return getHeadPoint(entity);
        }

        // Calculate direction to entity center
        double centerX = (box.minX + box.maxX) * 0.5D;
        double centerZ = (box.minZ + box.maxZ) * 0.5D;

        double dx = centerX - eyes.xCoord;
        double dz = centerZ - eyes.zCoord;

        // Normalize horizontal direction
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDist < 1.0E-6D)
        {
            return getHeadPoint(entity);
        }

        // Normalize direction vector
        double dirX = dx / horizontalDist;
        double dirZ = dz / horizontalDist;

        // Try pitch 0 first (horizontal aim) - extend the horizontal line to intersect entity
        // Find where the horizontal line from eyes intersects the entity's bounding box
        // We need to find the point on the line (eyes + t * direction) that's closest to entity center
        // and check if it's within the entity's Y bounds

        // Calculate intersection with entity's XZ bounds
        double tMin = Double.MAX_VALUE;
        double tMax = -Double.MAX_VALUE;

        // Check X boundaries
        if (Math.abs(dirX) > 1.0E-6D)
        {
            double t1 = (box.minX - eyes.xCoord) / dirX;
            double t2 = (box.maxX - eyes.xCoord) / dirX;
            tMin = Math.min(tMin, Math.min(t1, t2));
            tMax = Math.max(tMax, Math.max(t1, t2));
        }

        // Check Z boundaries
        if (Math.abs(dirZ) > 1.0E-6D)
        {
            double t1 = (box.minZ - eyes.zCoord) / dirZ;
            double t2 = (box.maxZ - eyes.zCoord) / dirZ;
            tMin = Math.min(tMin, Math.min(t1, t2));
            tMax = Math.max(tMax, Math.max(t1, t2));
        }

        // Use the closest intersection point (smallest positive t)
        double t = Math.max(0.0, tMin);
        double targetX = eyes.xCoord + dirX * t;
        double targetY = eyes.yCoord; // Pitch 0 = same Y as eyes
        double targetZ = eyes.zCoord + dirZ * t;

        Vec3 pitchZeroPoint = new Vec3(targetX, targetY, targetZ);

        // Check if pitch 0 point can reach the entity (ray trace check)
        boolean canReach = hitThroughWalls;
        if (!canReach)
        {
            MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(eyes, pitchZeroPoint, false, true, false);
            canReach = (hit == null || hit.typeOfHit == MovingObjectPosition.MovingObjectType.MISS);
        }
        
        if (canReach)
        {
            // Pitch 0 can reach - check if it intersects with entity bounding box
            // Check if the point is within entity's XZ bounds and Y bounds
            if (targetX >= box.minX && targetX <= box.maxX &&
                targetZ >= box.minZ && targetZ <= box.maxZ &&
                targetY >= box.minY && targetY <= box.maxY)
            {
                return pitchZeroPoint;
            }
        }

        // Pitch 0 cannot reach - find closest reachable point
        // Sample points along the entity's bounding box at the horizontal direction
        double[] ys = {box.minY, (box.minY + box.maxY) * 0.5D, box.maxY};

        Vec3 bestPoint = null;
        double bestDist = Double.MAX_VALUE;

        for (double y : ys)
        {
            // Calculate point at this Y level in the horizontal direction to entity
            double ratio = (y - eyes.yCoord) / horizontalDist;
            if (Math.abs(ratio) > 10.0) // Sanity check - avoid extreme angles
            {
                continue;
            }

            double testX = eyes.xCoord + dx * ratio;
            double testZ = eyes.zCoord + dz * ratio;
            Vec3 testPoint = new Vec3(testX, y, testZ);

            // Check if this point is reachable (no block obstruction)
            boolean testReachable = hitThroughWalls;
            if (!testReachable)
            {
                MovingObjectPosition testHit = mc.theWorld.rayTraceBlocks(eyes, testPoint, false, true, false);
                testReachable = (testHit == null || testHit.typeOfHit == MovingObjectPosition.MovingObjectType.MISS);
            }
            
            if (testReachable)
            {
                // Check if point is within entity bounds
                if (testX >= box.minX && testX <= box.maxX &&
                    testZ >= box.minZ && testZ <= box.maxZ)
                {
                    // This point is reachable and on the entity
                    double dist = Math.abs(y - eyes.yCoord); // Distance from pitch 0
                    if (dist < bestDist)
                    {
                        bestDist = dist;
                        bestPoint = testPoint;
                    }
                }
            }
        }

        // If we found a reachable point, use it; otherwise fall back to head
        return bestPoint != null ? bestPoint : getHeadPoint(entity);
    }

    /**
     * Gets the best hit vector (most visible point on entity)
     */
    public static Vec3 getBestHitVec(EntityLivingBase entity)
    {
        return getBestHitVec(entity, false);
    }

    /**
     * Gets the best hit vector (most visible point on entity)
     * @param hitThroughWalls If true, allows picking points through walls
     */
    public static Vec3 getBestHitVec(EntityLivingBase entity, boolean hitThroughWalls)
    {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        AxisAlignedBB box = entity.getEntityBoundingBox();
        if (box == null)
        {
            return getHeadPoint(entity);
        }

        // Sample points on the bounding box
        double[] xs = {box.minX, (box.minX + box.maxX) * 0.5D, box.maxX};
        double[] ys = {box.minY, (box.minY + box.maxY) * 0.5D, box.maxY};
        double[] zs = {box.minZ, (box.minZ + box.maxZ) * 0.5D, box.maxZ};

        Vec3 bestPoint = null;
        double bestScore = Double.MAX_VALUE;

        for (double x : xs)
        {
            for (double y : ys)
            {
                for (double z : zs)
                {
                    Vec3 point = new Vec3(x, y, z);
                    
                    // Prefer points that are visible (no block obstruction)
                    double score = 0.0;
                    boolean isVisible = hitThroughWalls;
                    if (!isVisible)
                    {
                        MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(eyes, point, false, true, false);
                        isVisible = (hit == null || hit.typeOfHit == MovingObjectPosition.MovingObjectType.MISS);
                    }
                    
                    if (isVisible)
                    {
                        // Visible point - prefer closer to center
                        double centerX = (box.minX + box.maxX) * 0.5D;
                        double centerY = (box.minY + box.maxY) * 0.5D;
                        double centerZ = (box.minZ + box.maxZ) * 0.5D;
                        double distFromCenter = Math.sqrt(
                            (x - centerX) * (x - centerX) +
                            (y - centerY) * (y - centerY) +
                            (z - centerZ) * (z - centerZ)
                        );
                        score = distFromCenter;
                    }
                    else
                    {
                        // Obstructed point - penalize (unless hitThroughWalls is enabled)
                        score = hitThroughWalls ? 0.0 : 1000.0;
                    }

                    if (score < bestScore)
                    {
                        bestScore = score;
                        bestPoint = point;
                    }
                }
            }
        }

        return bestPoint != null ? bestPoint : getHeadPoint(entity);
    }

    /**
     * Gets the closest hit vector (closest visible point to player)
     */
    public static Vec3 getClosestHitVec(EntityLivingBase entity)
    {
        return getClosestHitVec(entity, false);
    }

    /**
     * Gets the closest hit vector (closest visible point to player)
     * @param hitThroughWalls If true, allows picking points through walls
     */
    public static Vec3 getClosestHitVec(EntityLivingBase entity, boolean hitThroughWalls)
    {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        AxisAlignedBB box = entity.getEntityBoundingBox();
        if (box == null)
        {
            return getHeadPoint(entity);
        }

        // Sample points on the bounding box
        double[] xs = {box.minX, (box.minX + box.maxX) * 0.5D, box.maxX};
        double[] ys = {box.minY, (box.minY + box.maxY) * 0.5D, box.maxY};
        double[] zs = {box.minZ, (box.minZ + box.maxZ) * 0.5D, box.maxZ};

        Vec3 closestPoint = null;
        double closestDist = Double.MAX_VALUE;

        for (double x : xs)
        {
            for (double y : ys)
            {
                for (double z : zs)
                {
                    Vec3 point = new Vec3(x, y, z);
                    
                    // Only consider visible points (or all points if hitThroughWalls)
                    boolean isVisible = hitThroughWalls;
                    if (!isVisible)
                    {
                        MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(eyes, point, false, true, false);
                        isVisible = (hit == null || hit.typeOfHit == MovingObjectPosition.MovingObjectType.MISS);
                    }
                    
                    if (isVisible)
                    {
                        double dist = eyes.distanceTo(point);
                        if (dist < closestDist)
                        {
                            closestDist = dist;
                            closestPoint = point;
                        }
                    }
                }
            }
        }

        return closestPoint != null ? closestPoint : getHeadPoint(entity);
    }

    /**
     * Gets a reachable point on the entity (for partially exposed targets)
     * Samples the entity's bounding box and returns the best reachable point
     * @param hitThroughWalls If true, allows picking points through walls
     */
    public static Vec3 getReachablePoint(EntityLivingBase entity, boolean hitThroughWalls)
    {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        AxisAlignedBB box = entity.getEntityBoundingBox();
        if (box == null)
        {
            return getHeadPoint(entity);
        }

        // Sample more points for better coverage of partially exposed targets
        double[] xs = {box.minX, box.minX + (box.maxX - box.minX) * 0.25D, (box.minX + box.maxX) * 0.5D, box.minX + (box.maxX - box.minX) * 0.75D, box.maxX};
        double[] ys = {box.minY, box.minY + (box.maxY - box.minY) * 0.25D, (box.minY + box.maxY) * 0.5D, box.minY + (box.maxY - box.minY) * 0.75D, box.maxY};
        double[] zs = {box.minZ, box.minZ + (box.maxZ - box.minZ) * 0.25D, (box.minZ + box.maxZ) * 0.5D, box.minZ + (box.maxZ - box.minZ) * 0.75D, box.maxZ};

        Vec3 bestReachablePoint = null;
        double bestScore = Double.MAX_VALUE;

        for (double x : xs)
        {
            for (double y : ys)
            {
                for (double z : zs)
                {
                    Vec3 point = new Vec3(x, y, z);
                    
                    // Check if point is reachable
                    boolean isReachable = hitThroughWalls;
                    if (!isReachable)
                    {
                        MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(eyes, point, false, true, false);
                        isReachable = (hit == null || hit.typeOfHit == MovingObjectPosition.MovingObjectType.MISS);
                    }
                    
                    if (isReachable)
                    {
                        // Score based on distance from center (prefer center points) and distance from player (prefer closer)
                        double centerX = (box.minX + box.maxX) * 0.5D;
                        double centerY = (box.minY + box.maxY) * 0.5D;
                        double centerZ = (box.minZ + box.maxZ) * 0.5D;
                        double distFromCenter = Math.sqrt(
                            (x - centerX) * (x - centerX) +
                            (y - centerY) * (y - centerY) +
                            (z - centerZ) * (z - centerZ)
                        );
                        double distFromPlayer = eyes.distanceTo(point);
                        
                        // Combine scores (prefer closer to center and closer to player)
                        double score = distFromCenter * 0.3 + distFromPlayer * 0.7;
                        
                        if (score < bestScore)
                        {
                            bestScore = score;
                            bestReachablePoint = point;
                        }
                    }
                }
            }
        }

        // If we found a reachable point, use it; otherwise fall back to head
        return bestReachablePoint != null ? bestReachablePoint : getHeadPoint(entity);
    }

    /**
     * Computes rotation angles to look at a point
     */
    public static float[] computeRotation(Vec3 from, Vec3 to)
    {
        double dx = to.xCoord - from.xCoord;
        double dy = to.yCoord - from.yCoord;
        double dz = to.zCoord - from.zCoord;

        double distanceXZ = Math.sqrt(dx * dx + dz * dz);
        if (distanceXZ < 1.0E-6D && Math.abs(dy) < 1.0E-6D)
        {
            return null;
        }

        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(dy, distanceXZ) * 180.0D / Math.PI));
        yaw = MathHelper.wrapAngleTo180_float(yaw);

        return new float[]{yaw, pitch};
    }
}

