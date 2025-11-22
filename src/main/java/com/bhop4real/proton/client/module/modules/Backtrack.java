package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.IntSetting;
import com.bhop4real.proton.client.module.settings.SeparatorSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backtrack module - records recent positions of entities to allow lag-compensated hit checks.
 */
public class Backtrack extends Module
{
	// Snapshot of an entity's bounding box at a point in time
	public static final class Snapshot
	{
		public final long timestampMs;
		public final double minX, minY, minZ, maxX, maxY, maxZ;
		
		public Snapshot(long timestampMs, AxisAlignedBB bb)
		{
			this.timestampMs = timestampMs;
			this.minX = bb.minX;
			this.minY = bb.minY;
			this.minZ = bb.minZ;
			this.maxX = bb.maxX;
			this.maxY = bb.maxY;
			this.maxZ = bb.maxZ;
		}
		
		public AxisAlignedBB toAabb()
		{
			return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
		}
	}
	
	private static Backtrack instance;
	
	private final IntSetting windowMs;
	private final IntSetting maxSnapshotsPerEntity;
	
	// entityId -> deque of snapshots (newest at tail)
	private final Map<Integer, Deque<Snapshot>> historyByEntityId = new ConcurrentHashMap<>();
	
    public Backtrack()
    {
		super("Backtrack", new String[]{"bt", "back"}, "Allows hitting entities in recent past positions", Module.Category.COMBAT, 0);
		instance = this;
		
		addSetting(new SeparatorSetting("Recording"));
		windowMs = new IntSetting("Window (ms)", "How far back to keep snapshots", 150, 50, 500);
		maxSnapshotsPerEntity = new IntSetting("Max Snapshots", "Safety cap per entity", 12, 4, 64);
		addSetting(windowMs);
		addSetting(maxSnapshotsPerEntity);
    }
    
    @Override
    public void onEnable()
    {
		historyByEntityId.clear();
    }
    
    @Override
    public void onDisable()
    {
		historyByEntityId.clear();
    }
	
	@Override
	public void onUpdate()
	{
		if (mc.theWorld == null || mc.thePlayer == null) return;
		
		long now = System.currentTimeMillis();
		int window = Math.max(50, windowMs.getValue());
		
		// Record current snapshots
		for (Entity entity : mc.theWorld.loadedEntityList)
		{
            if (!(entity instanceof EntityLivingBase)) continue;
			if (entity == mc.thePlayer) continue;
			AxisAlignedBB bb = entity.getEntityBoundingBox();
			if (bb == null) continue;
			
			Deque<Snapshot> deque = historyByEntityId.computeIfAbsent(entity.getEntityId(), id -> new ArrayDeque<>());
			deque.addLast(new Snapshot(now, bb));
			
			// Trim by size
			while (deque.size() > Math.max(4, maxSnapshotsPerEntity.getValue()))
			{
				deque.pollFirst();
			}
			
			// Trim by time window
			while (!deque.isEmpty())
			{
				Snapshot head = deque.peekFirst();
				if (head == null || now - head.timestampMs <= window) break;
				deque.pollFirst();
			}
		}
		
		// Cleanup entities that no longer exist or are out of window entirely
		historyByEntityId.entrySet().removeIf(entry -> {
			Deque<Snapshot> dq = entry.getValue();
			if (dq == null || dq.isEmpty()) return true;
			Snapshot last = dq.peekLast();
			return last == null || now - last.timestampMs > Math.max(50, windowMs.getValue()) + 50;
		});
	}
	
	public static Backtrack get()
	{
		return instance;
	}
	
	public Deque<Snapshot> getHistoryFor(Entity entity)
	{
		if (entity == null) return null;
		return historyByEntityId.get(entity.getEntityId());
	}
}

