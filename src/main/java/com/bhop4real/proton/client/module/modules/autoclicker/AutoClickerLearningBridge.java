package com.bhop4real.proton.client.module.modules.autoclicker;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared observation buffer + suggestion mailbox for the AutoClicker module.
 * Modules or future ML pipelines can ingest the captured click statistics and
 * provide optional delay overrides without tight coupling.
 */
public final class AutoClickerLearningBridge
{
    private static final int MAX_OBSERVATIONS = 512;
    private static final AutoClickerLearningBridge INSTANCE = new AutoClickerLearningBridge();

    private final Deque<Observation> buffer = new ArrayDeque<>();
    private final AtomicReference<Long> pendingDelayOverride = new AtomicReference<>();

    private AutoClickerLearningBridge() {}

    public static AutoClickerLearningBridge get()
    {
        return INSTANCE;
    }

    public synchronized void recordObservation(Observation observation)
    {
        if (observation == null)
        {
            return;
        }

        buffer.addLast(observation);
        while (buffer.size() > MAX_OBSERVATIONS)
        {
            buffer.removeFirst();
        }
    }

    public synchronized Observation[] snapshot()
    {
        return buffer.toArray(new Observation[0]);
    }

    public synchronized void reset()
    {
        buffer.clear();
        pendingDelayOverride.set(null);
    }

    public void submitDelayOverride(long delayMs)
    {
        pendingDelayOverride.set(delayMs);
    }

    public Long pollDelayOverride()
    {
        return pendingDelayOverride.getAndSet(null);
    }

    public static final class Observation
    {
        public final long timestamp;
        public final long interClickDelayMs;
        public final long scheduledDelayMs;
        public final String cpsMode;
        public final String randomizationProfile;
        public final int minCps;
        public final int maxCps;
        public final int minDelayMs;
        public final int maxDelayMs;
        public final int offsetMinMs;
        public final int offsetMaxMs;
        public final boolean requireHolding;
        public final int ticksButtonHeld;
        public final boolean buttonCurrentlyDown;
        public final boolean breakBlocks;
        public final boolean hoveredBlock;
        public final boolean attackKeyHeld;
        public final boolean overrideApplied;

        private Observation(long timestamp,
                            long interClickDelayMs,
                            long scheduledDelayMs,
                            String cpsMode,
                            String randomizationProfile,
                            int minCps,
                            int maxCps,
                            int minDelayMs,
                            int maxDelayMs,
                            int offsetMinMs,
                            int offsetMaxMs,
                            boolean requireHolding,
                            int ticksButtonHeld,
                            boolean buttonCurrentlyDown,
                            boolean breakBlocks,
                            boolean hoveredBlock,
                            boolean attackKeyHeld,
                            boolean overrideApplied)
        {
            this.timestamp = timestamp;
            this.interClickDelayMs = interClickDelayMs;
            this.scheduledDelayMs = scheduledDelayMs;
            this.cpsMode = cpsMode;
            this.randomizationProfile = randomizationProfile;
            this.minCps = minCps;
            this.maxCps = maxCps;
            this.minDelayMs = minDelayMs;
            this.maxDelayMs = maxDelayMs;
            this.offsetMinMs = offsetMinMs;
            this.offsetMaxMs = offsetMaxMs;
            this.requireHolding = requireHolding;
            this.ticksButtonHeld = ticksButtonHeld;
            this.buttonCurrentlyDown = buttonCurrentlyDown;
            this.breakBlocks = breakBlocks;
            this.hoveredBlock = hoveredBlock;
            this.attackKeyHeld = attackKeyHeld;
            this.overrideApplied = overrideApplied;
        }

        public static Observation create(long interClickDelayMs,
                                         long scheduledDelayMs,
                                         String cpsMode,
                                         String randomizationProfile,
                                         int minCps,
                                         int maxCps,
                                         int minDelayMs,
                                         int maxDelayMs,
                                         int offsetMinMs,
                                         int offsetMaxMs,
                                         boolean requireHolding,
                                         int ticksButtonHeld,
                                         boolean buttonCurrentlyDown,
                                         boolean breakBlocks,
                                         boolean hoveredBlock,
                                         boolean attackKeyHeld,
                                         boolean overrideApplied)
        {
            return new Observation(System.currentTimeMillis(),
                interClickDelayMs,
                scheduledDelayMs,
                cpsMode,
                randomizationProfile,
                minCps,
                maxCps,
                minDelayMs,
                maxDelayMs,
                offsetMinMs,
                offsetMaxMs,
                requireHolding,
                ticksButtonHeld,
                buttonCurrentlyDown,
                breakBlocks,
                hoveredBlock,
                attackKeyHeld,
                overrideApplied);
        }
    }
}

