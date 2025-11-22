package com.bhop4real.proton.client.learning.click;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Represents a trained click model capable of sampling realistic inter-click delays.
 */
public class ClickModel
{
    private final long trainedAt;
    private final int sampleCount;
    private final List<Double> kernels;
    private final double mean;
    private final double stdDeviation;
    private final double bandwidth;
    private final Random random = new Random();

    public ClickModel(long trainedAt,
                      int sampleCount,
                      List<Double> kernels,
                      double mean,
                      double stdDeviation,
                      double bandwidth)
    {
        this.trainedAt = trainedAt;
        this.sampleCount = sampleCount;
        this.kernels = kernels != null
            ? Collections.unmodifiableList(new ArrayList<>(kernels))
            : Collections.emptyList();
        this.mean = mean;
        this.stdDeviation = stdDeviation;
        this.bandwidth = bandwidth;
    }

    public boolean isReady()
    {
        return !kernels.isEmpty();
    }

    public long getTrainedAt()
    {
        return trainedAt;
    }

    public int getSampleCount()
    {
        return sampleCount;
    }

    public double getMean()
    {
        return mean;
    }

    public double getStdDeviation()
    {
        return stdDeviation;
    }

    public double getBandwidth()
    {
        return bandwidth;
    }

    public List<Double> getKernels()
    {
        return kernels;
    }

    public long sampleDelay()
    {
        if (kernels.isEmpty())
        {
            return 120L;
        }

        double base = kernels.get(random.nextInt(kernels.size()));
        double spread = bandwidth > 0 ? bandwidth : Math.max(8.0, stdDeviation * 0.4);
        double noise = random.nextGaussian() * spread;

        // occasional micro-bursts
        if (random.nextDouble() < 0.08)
        {
            noise -= Math.abs(random.nextGaussian()) * spread * 0.5;
        }

        double candidate = Math.max(30.0, base + noise);
        if (random.nextDouble() < 0.02)
        {
            // simulate minor fatigue / hesitation
            candidate += Math.abs(random.nextGaussian()) * spread * 1.4;
        }

        return Math.round(candidate);
    }

}

