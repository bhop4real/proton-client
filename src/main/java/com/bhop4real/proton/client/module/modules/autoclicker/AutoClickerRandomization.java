package com.bhop4real.proton.client.module.modules.autoclicker;

import com.bhop4real.proton.client.util.RandomUtil;

/**
 * Encapsulates the delay randomisation profiles for the auto clicker.
 * Keeps internal state for the adaptive profiles so the main module stays lean.
 */
public final class AutoClickerRandomization
{
    public enum Profile
    {
        BASIC("Basic"),
        OPEN("Open");

        private final String label;

        Profile(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }

        public static Profile fromLabel(String value)
        {
            if (value == null)
            {
                return BASIC;
            }
            for (Profile profile : values())
            {
                if (profile.label.equalsIgnoreCase(value))
                {
                    return profile;
                }
            }
            return BASIC;
        }
    }

    public enum Mode
    {
        CLASSIC("Classic"),
        DELAY("Delay");

        private final String label;

        Mode(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return label;
        }

        public static Mode fromLabel(String value)
        {
            if (value == null)
            {
                return CLASSIC;
            }
            for (Mode mode : values())
            {
                if (mode.label.equalsIgnoreCase(value))
                {
                    return mode;
                }
            }
            return CLASSIC;
        }
    }

    public static final class Config
    {
        private final int minCps;
        private final int maxCps;
        private final int minDelayMs;
        private final int maxDelayMs;
        private final int offsetMinMs;
        private final int offsetMaxMs;

        public Config(int minCps, int maxCps, int minDelayMs, int maxDelayMs, int offsetMinMs, int offsetMaxMs)
        {
            this.minCps = minCps;
            this.maxCps = maxCps;
            this.minDelayMs = minDelayMs;
            this.maxDelayMs = maxDelayMs;
            this.offsetMinMs = offsetMinMs;
            this.offsetMaxMs = offsetMaxMs;
        }

        public int minCps()
        {
            return minCps;
        }

        public int maxCps()
        {
            return maxCps;
        }

        public int minDelayMs()
        {
            return minDelayMs;
        }

        public int maxDelayMs()
        {
            return maxDelayMs;
        }

        public int offsetMinMs()
        {
            return offsetMinMs;
        }

        public int offsetMaxMs()
        {
            return offsetMaxMs;
        }
    }

    private Profile lastProfile;
    private Mode lastMode;

    private long lastBasicDelay = -1;
    private long basicTargetDelay = -1;
    private int basicTargetHold = 0;
    private int basicClicksAtTarget = 0;
    private double basicDrift = 0.0;
    private double basicMomentum = 0.0;

    public long nextDelay(Profile profile, Mode mode, Config config)
    {
        if (config == null)
        {
            throw new IllegalArgumentException("config cannot be null");
        }

        if (profile == null)
        {
            profile = Profile.BASIC;
        }

        if (mode == null)
        {
            mode = Mode.CLASSIC;
        }

        ensureContext(profile, mode);

        long delay;
        switch (profile)
        {
            case BASIC:
            default:
                delay = computeBasic(mode, config);
                break;
            case OPEN:
                // OPEN mode is handled by the script engine in AutoClicker
                // This should not be reached, but provide fallback
                delay = computeBasic(mode, config);
                break;
        }

        return Math.max(1L, delay);
    }

    public void resetAll()
    {
        lastProfile = null;
        lastMode = null;
        resetBasicInternal();
    }

    private void ensureContext(Profile profile, Mode mode)
    {
        if (profile != lastProfile || mode != lastMode)
        {
            if (profile != Profile.BASIC)
            {
                resetBasicInternal();
            }

            lastProfile = profile;
            lastMode = mode;
        }
    }

    private long computeBasic(Mode mode, Config config)
    {
        long[] bounds = calculateDelayBounds(mode, config);
        long minDelay = bounds[0];
        long maxDelay = bounds[1];

        if (maxDelay <= minDelay)
        {
            long value = Math.max(1L, minDelay);
            lastBasicDelay = value;
            basicTargetDelay = value;
            basicTargetHold = RandomUtil.nextInt(3, 7);
            basicClicksAtTarget = 0;
            basicDrift = 0.0;
            basicMomentum = 0.0;
            return value;
        }

        boolean resetAnchor = basicTargetDelay < minDelay || basicTargetDelay > maxDelay ||
                              basicTargetHold <= 0 || basicClicksAtTarget >= basicTargetHold;

        if (!resetAnchor && RandomUtil.nextDouble() < 0.18)
        {
            resetAnchor = true;
        }

        if (resetAnchor)
        {
            long baseline = sampleHumanBaseline(mode, config, minDelay, maxDelay);
            double spread = Math.min(14.0, (maxDelay - minDelay) * 0.28);
            double gaussian = RandomUtil.nextGaussian() * spread;
            int skew = RandomUtil.nextInt(-9, 10);
            basicTargetDelay = clamp(Math.round(baseline + gaussian + skew), minDelay, maxDelay);
            basicTargetHold = RandomUtil.nextInt(3, 8);
            basicClicksAtTarget = 0;
            basicMomentum = clamp(RandomUtil.nextGaussian() * 1.6, -4.5, 4.5);
        }

        basicMomentum = clamp(basicMomentum * 0.62 + RandomUtil.nextGaussian() * 1.25, -5.0, 5.0);
        basicDrift = clamp(basicDrift * 0.74 + basicMomentum, -24.0, 24.0);

        double slowDrift = basicDrift;
        double fastNoise = RandomUtil.nextGaussian() * Math.min(11.0, (maxDelay - minDelay) * 0.32);
        int microStep = RandomUtil.nextInt(-8, 9);
        long candidate = clamp(Math.round(basicTargetDelay + slowDrift + fastNoise + microStep), minDelay, maxDelay);

        if (RandomUtil.nextDouble() < 0.22)
        {
            int nudge = RandomUtil.nextInt(6, 19);
            candidate = clamp(candidate + (RandomUtil.nextBoolean() ? nudge : -nudge), minDelay, maxDelay);
        }

        if (RandomUtil.nextDouble() < 0.09)
        {
            candidate = clamp(candidate + RandomUtil.nextInt(18, 70), minDelay, maxDelay);
        }

        if (RandomUtil.nextDouble() < 0.04)
        {
            candidate = clamp(candidate + RandomUtil.nextInt(70, 161), minDelay, maxDelay);
            basicTargetHold = RandomUtil.nextInt(2, 5);
            basicClicksAtTarget = 0;
        }

        if (RandomUtil.nextDouble() < 0.14)
        {
            candidate = clamp(candidate - RandomUtil.nextInt(6, 15), minDelay, maxDelay);
        }

        long smoothed = (lastBasicDelay <= 0)
                ? candidate
                : clamp(Math.round(lastBasicDelay * 0.38 + candidate * 0.62), minDelay, maxDelay);

        lastBasicDelay = smoothed;
        basicClicksAtTarget++;

        return Math.max(1L, smoothed);
    }

    private long sampleCpsDelay(Config config, boolean gaussian)
    {
        double minCps = Math.min(config.minCps(), config.maxCps());
        double maxCps = Math.max(config.minCps(), config.maxCps());

        if (maxCps <= 0.0)
        {
            maxCps = 1.0;
        }

        if (minCps <= 0.0)
        {
            minCps = 1.0;
        }

        double cpsValue;

        if (gaussian)
        {
            double mid = (minCps + maxCps) * 0.5;
            double spread = (maxCps - minCps) * 0.35;
            double sample = mid + RandomUtil.nextGaussian() * spread;
            cpsValue = Math.max(minCps, Math.min(maxCps, sample));
        }
        else
        {
            double randA = RandomUtil.nextDouble();
            double randB = RandomUtil.nextDouble();
            double normalized = (randA + randB) * 0.5;
            cpsValue = minCps + (maxCps - minCps) * normalized;
        }

        cpsValue = Math.max(0.1, cpsValue);
        return Math.max(1L, Math.round(1000.0 / cpsValue));
    }

    private long sampleFixedDelay(Config config, boolean gaussian)
    {
        int minDelay = Math.min(config.minDelayMs(), config.maxDelayMs());
        int maxDelay = Math.max(config.minDelayMs(), config.maxDelayMs());

        if (maxDelay <= 0)
        {
            maxDelay = 1;
        }
        if (minDelay < 0)
        {
            minDelay = 0;
        }

        if (minDelay >= maxDelay)
        {
            return Math.max(1L, minDelay);
        }

        if (gaussian)
        {
            double mid = (minDelay + maxDelay) * 0.5;
            double spread = (maxDelay - minDelay) * 0.3;
            long candidate = Math.round(mid + RandomUtil.nextGaussian() * spread);
            if (candidate < minDelay || candidate > maxDelay)
            {
                candidate = RandomUtil.nextInt(minDelay, maxDelay + 1);
            }
            return Math.max(1L, candidate);
        }

        return Math.max(1L, RandomUtil.nextInt(minDelay, maxDelay + 1));
    }

    private long[] calculateDelayBounds(Mode mode, Config config)
    {
        if (mode == Mode.CLASSIC)
        {
            double minCps = Math.min(config.minCps(), config.maxCps());
            double maxCps = Math.max(config.minCps(), config.maxCps());

            if (maxCps <= 0.0)
            {
                maxCps = 1.0;
            }

            if (minCps <= 0.0)
            {
                minCps = 1.0;
            }

            long minDelay = Math.max(1L, Math.round(1000.0 / Math.max(maxCps, 0.1)));
            long maxDelay = Math.max(minDelay, Math.round(1000.0 / Math.max(minCps, 0.1)));
            return new long[]{minDelay, maxDelay};
        }
        else
        {
            int minDelay = Math.min(config.minDelayMs(), config.maxDelayMs());
            int maxDelay = Math.max(config.minDelayMs(), config.maxDelayMs());

            if (minDelay < 0)
            {
                minDelay = 0;
            }

            if (maxDelay <= 0)
            {
                maxDelay = 1;
            }

            if (minDelay >= maxDelay)
            {
                long value = Math.max(1L, minDelay);
                return new long[]{value, value};
            }

            return new long[]{minDelay, maxDelay};
        }
    }

    private long sampleHumanBaseline(Mode mode, Config config, long minDelay, long maxDelay)
    {
        long baseline = mode == Mode.CLASSIC
                ? sampleCpsDelay(config, true)
                : sampleFixedDelay(config, true);
        return clamp(baseline, minDelay, maxDelay);
    }

    private void resetBasicInternal()
    {
        lastBasicDelay = -1;
        basicTargetDelay = -1;
        basicTargetHold = 0;
        basicClicksAtTarget = 0;
        basicDrift = 0.0;
        basicMomentum = 0.0;
    }

    private long clamp(long value, long min, long max)
    {
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }

    private double clamp(double value, double min, double max)
    {
        if (value < min)
        {
            return min;
        }
        if (value > max)
        {
            return max;
        }
        return value;
    }
}

