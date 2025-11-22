package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.DoubleSetting;
import com.bhop4real.proton.client.module.settings.SeparatorSetting;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;

import java.util.List;

/**
 * Reach module - performs extended ray tracing to allow hitting entities farther away.
 * If Backtrack is enabled, it also considers recent past positions for intersections.
 */
public class Reach extends Module
{
	public static Reach get()
	{
		com.bhop4real.proton.client.ProtonClient client = com.bhop4real.proton.client.ProtonClient.getInstance();
		if (client == null || client.getModuleManager() == null) return null;
		Module m = client.getModuleManager().getModuleByName("Reach");
		return m instanceof Reach ? (Reach) m : null;
	}
	
	private final DoubleSetting minReachBlocks;
	private final DoubleSetting maxReachBlocks;
	private final BooleanSetting weaponOnly;
	private final BooleanSetting movingOnly;
	private final BooleanSetting sprintOnly;
	private final BooleanSetting hitThroughBlocks;
	
	private final BooleanSetting useBacktrackPositions;
	private final DoubleSetting hitboxExpandCm;
	private final BooleanSetting onlyWhenClicking;
	
    public Reach()
    {
		super("Reach", new String[]{"range"}, "Extends your reach distance", Module.Category.COMBAT, 0);
		
		addSetting(new SeparatorSetting("Behavior"));
		minReachBlocks = new DoubleSetting("Min Reach", "Minimum extended reach", 3.10D, 3.00D, 6.00D, 0.01D);
		maxReachBlocks = new DoubleSetting("Max Reach", "Maximum extended reach", 3.30D, 3.00D, 6.00D, 0.01D);
		weaponOnly = new BooleanSetting("Weapon Only", "Only when holding a weapon/item", false);
		movingOnly = new BooleanSetting("Moving Only", "Only when moving", false);
		sprintOnly = new BooleanSetting("Sprint Only", "Only when sprinting", false);
		hitThroughBlocks = new BooleanSetting("Hit Through Blocks", "Allow reach through blocks", false);
		onlyWhenClicking = new BooleanSetting("Only When Clicking", "Only extend reach when attack held", true);
		addSetting(minReachBlocks);
		addSetting(maxReachBlocks);
		addSetting(weaponOnly);
		addSetting(movingOnly);
		addSetting(sprintOnly);
		addSetting(hitThroughBlocks);
		addSetting(onlyWhenClicking);
		
		addSetting(new SeparatorSetting("Precision"));
		hitboxExpandCm = new DoubleSetting("Hitbox Expand (cm)", "Expand target AABB for precision", 2.0D, 0.0D, 20.0D, 0.5D);
		addSetting(hitboxExpandCm);
		
		addSetting(new SeparatorSetting("Backtrack"));
		useBacktrackPositions = new BooleanSetting("Use Backtrack", "Also intersect recent past positions", true);
		addSetting(useBacktrackPositions);
    }
    
    @Override
    public void onEnable()
    {
		// no-op
    }
    
    @Override
    public void onDisable()
    {
		// no-op
    }
	
	@Override
	public void onUpdate()
	{
		applyReachIfEligible();
	}
	
	@Override
	public void onRender3D(float partialTicks)
	{
		// Also apply during render so our override persists after vanilla getMouseOver
		applyReachIfEligible();
	}
	
	public void applyReachIfEligible()
	{
		if (mc.thePlayer == null || mc.theWorld == null) return;
		if (mc.currentScreen != null) return;
		if (onlyWhenClicking.getValue() && !isAttackHeld()) return;
		
		// keep min <= max
		if (minReachBlocks.getValue() > maxReachBlocks.getValue())
		{
			minReachBlocks.setValue(maxReachBlocks.getValue());
		}
		
		// basic constraints
		if (weaponOnly.getValue() && mc.thePlayer.getHeldItem() == null) return;
		if (movingOnly.getValue() && mc.thePlayer.moveForward == 0.0F && mc.thePlayer.moveStrafing == 0.0F) return;
		if (sprintOnly.getValue() && !mc.thePlayer.isSprinting()) return;
		
		// respect block obstruction if configured
		if (!hitThroughBlocks.getValue() && mc.objectMouseOver != null)
		{
			if (mc.objectMouseOver.getBlockPos() != null)
			{
				Block block = mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();
				if (block != null && block.getMaterial() != null && !block.getMaterial().isReplaceable())
				{
					return;
				}
			}
		}
		
		// random reach between [min, max]
		double reach = randomBetween(minReachBlocks.getValue(), maxReachBlocks.getValue());
		Object[] pick = pickEntity(reach, 0.0D, null);
		if (pick == null) return;
		
		Entity hit = (Entity) pick[0];
		Vec3 hitVec = (Vec3) pick[1];
		mc.objectMouseOver = new MovingObjectPosition(hit, hitVec);
		mc.pointedEntity = hit;
	}
	
	private boolean isAttackHeld()
	{
		return mc.gameSettings.keyBindAttack.isKeyDown() || Mouse.isButtonDown(0);
	}
	
	/**
	 * Ray-pick an entity up to reach, optionally consulting backtrack history.
	 * Returns {Entity, Vec3} or null.
	 */
	private Object[] pickEntity(double reach, double expand, float[] rotations)
	{
		Entity viewer = mc.getRenderViewEntity();
		if (viewer == null) return null;
		
		mc.mcProfiler.startSection("reach-pick");
		
		Vec3 eyePos = viewer.getPositionEyes(1.0F);
		Vec3 look;
		if (rotations != null)
		{
			// yaw, pitch order in reference code: rotations[1]=yaw, rotations[0]=pitch
			look = getVectorForRotation(rotations[1], rotations[0]);
		}
		else
		{
			look = viewer.getLook(1.0F);
		}
		
		Vec3 rayEnd = eyePos.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
		double bestDist = reach;
		Entity bestEntity = null;
		Vec3 bestHitVec = null;
		
		// cm -> blocks
		double expandBlocks = Math.max(0.0D, hitboxExpandCm.getValue()) / 100.0D;
		
		List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(
				viewer,
				viewer.getEntityBoundingBox()
						.addCoord(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach)
						.expand(1.0D, 1.0D, 1.0D)
		);
		
		for (Entity entity : list)
		{
			if (!entity.canBeCollidedWith()) continue;
			if (entity == viewer) continue;
			
			// current position AABB
			AxisAlignedBB bb = entity.getEntityBoundingBox();
			if (bb == null) continue;
			// include entity collision border size
			double border = 0.0D;
			try
			{
				border = entity.getCollisionBorderSize();
			}
			catch (Throwable ignored) {}
			AxisAlignedBB grown = bb.expand(expandBlocks + border, expandBlocks + border, expandBlocks + border);
			MovingObjectPosition intercept = grown.calculateIntercept(eyePos, rayEnd);
			if (grown.isVecInside(eyePos))
			{
				// starting inside the box
				bestEntity = entity;
				bestHitVec = intercept == null ? eyePos : intercept.hitVec;
				bestDist = 0.0D;
			}
			else if (intercept != null)
			{
				double dist = eyePos.distanceTo(intercept.hitVec);
				if (dist < bestDist || bestEntity == null)
				{
					bestDist = dist;
					bestEntity = entity;
					bestHitVec = intercept.hitVec;
				}
			}
			
			// Backtrack intersection (if enabled)
			if (useBacktrackPositions.getValue())
			{
				Backtrack bt = Backtrack.get();
				if (bt != null && bt.isEnabled())
				{
					MovingObjectPosition past = raycastBacktrack(bt, entity, eyePos, rayEnd, expandBlocks);
					if (past != null && past.hitVec != null)
					{
						double dist = eyePos.distanceTo(past.hitVec);
						if (dist < bestDist || bestEntity == null)
						{
							bestDist = dist;
							bestEntity = entity;
							bestHitVec = past.hitVec;
						}
					}
				}
			}
		}
		
		// If walls are not allowed, ensure no block obstructs between eye and entity hitVec
		if (bestEntity != null && bestHitVec != null && !hitThroughBlocks.getValue())
		{
			MovingObjectPosition blockHit = mc.theWorld.rayTraceBlocks(eyePos, bestHitVec, false, true, false);
			if (blockHit != null && blockHit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
			{
				// blocked by a solid block
				bestEntity = null;
				bestHitVec = null;
			}
		}
		
		mc.mcProfiler.endSection();
		if (bestEntity != null && bestHitVec != null)
		{
			return new Object[]{bestEntity, bestHitVec};
		}
		return null;
	}
	
	// Utility: replicate RotationUtils.getVectorForRotation without external dep
	private Vec3 getVectorForRotation(float yaw, float pitch)
	{
		float f = (float) Math.cos(-yaw * 0.017453292F - Math.PI);
		float f1 = (float) Math.sin(-yaw * 0.017453292F - Math.PI);
		float f2 = (float) -Math.cos(-pitch * 0.017453292F);
		float f3 = (float) Math.sin(-pitch * 0.017453292F);
		return new Vec3(f1 * f2, f3, f * f2);
	}
	
	/**
	 * Fallback: Intersect backtrack snapshots using Backtrack#getHistoryFor(...) to avoid
	 * depending on Backtrack#raycastHistory which may not exist in some source sets.
	 */
	private MovingObjectPosition raycastBacktrack(Backtrack bt, Entity entity, Vec3 eyePos, Vec3 rayEnd, double expand)
	{
		if (bt == null || entity == null) return null;
		
		java.util.Deque<Backtrack.Snapshot> history = bt.getHistoryFor(entity);
		if (history == null || history.isEmpty()) return null;
		
		long now = System.currentTimeMillis();
		// Try to read window setting if present via reflection; otherwise default to 150ms
		long windowLimitMs = 150L;
		try
		{
			java.lang.reflect.Field f = bt.getClass().getDeclaredField("windowMs");
			f.setAccessible(true);
			Object setting = f.get(bt);
			if (setting instanceof com.bhop4real.proton.client.module.settings.IntSetting)
			{
				int v = Math.max(50, ((com.bhop4real.proton.client.module.settings.IntSetting) setting).getValue());
				windowLimitMs = v;
			}
		}
		catch (Throwable ignored) {}
		
		MovingObjectPosition best = null;
		double bestDist = Double.MAX_VALUE;
		
		for (Backtrack.Snapshot s : history)
		{
			if (s == null) continue;
			if (now - s.timestampMs > windowLimitMs) continue;
			AxisAlignedBB past = s.toAabb().expand(expand, expand, expand);
			MovingObjectPosition mop = past.calculateIntercept(eyePos, rayEnd);
			if (mop == null || mop.hitVec == null) continue;
			double dist = eyePos.distanceTo(mop.hitVec);
			if (dist < bestDist)
			{
				bestDist = dist;
				best = mop;
			}
		}
		
		return best;
	}
	
	private double randomBetween(double min, double max)
	{
		if (max <= min) return min;
		return min + (max - min) * Math.random();
	}
}

