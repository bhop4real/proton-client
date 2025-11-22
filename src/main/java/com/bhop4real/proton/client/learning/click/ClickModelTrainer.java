package com.bhop4real.proton.client.learning.click;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Trains a click model using simple kernel density estimation over recorded intervals.
 */
public class ClickModelTrainer
{
    public ClickTrainingData train(List<ClickSample> samples)
    {
        if (samples == null || samples.isEmpty())
        {
            return ClickTrainingData.empty();
        }

        List<Double> intervals = new ArrayList<>();
        int validSampleSets = 0;
        for (ClickSample sample : samples)
        {
            if (sample == null)
            {
                continue;
            }
            int before = intervals.size();
            for (Long interval : sample.getIntervals())
            {
                if (interval != null && interval > 0)
                {
                    intervals.add(interval.doubleValue());
                }
            }
            if (intervals.size() > before)
            {
                validSampleSets++;
            }
        }

        int n = intervals.size();
        if (n == 0)
        {
            return ClickTrainingData.empty(samples.size(), validSampleSets);
        }

        ClickTrainingSummary summary = ClickTrainingSummary.fromIntervals(intervals);
        List<Double> kernels = buildExpandedKernelSet(intervals, summary);
        double bandwidth = estimateBandwidth(summary, kernels.size());

        ClickModel model = new ClickModel(
            System.currentTimeMillis(),
            kernels.size(),
            kernels,
            summary.getMean(),
            summary.getStdDeviation(),
            bandwidth);

        return new ClickTrainingData(model, summary, samples.size(), validSampleSets);
    }

    private double estimateBandwidth(ClickTrainingSummary summary, int n)
    {
        if (n <= 1)
        {
            return Math.max(10.0, summary.getStdDeviation());
        }

        double factor = Math.pow(n, -0.2); // n^(-1/5)
        double spread = summary.getStdDeviation();
        double iqrAdjusted = summary.getInterquartileRange() > 0
            ? summary.getInterquartileRange() / 1.34
            : spread;
        if (!Double.isNaN(iqrAdjusted) && iqrAdjusted > 0)
        {
            spread = Math.min(spread, iqrAdjusted);
        }

        double bandwidth = 1.06 * spread * factor;
        if (Double.isNaN(bandwidth) || Double.isInfinite(bandwidth) || bandwidth <= 0)
        {
            bandwidth = Math.max(12.0, summary.getStdDeviation() * 0.75);
        }

        return Math.max(8.0, bandwidth);
    }

    private List<Double> buildExpandedKernelSet(List<Double> intervals, ClickTrainingSummary summary)
    {
        List<Double> kernels = new ArrayList<>(intervals.size() * 3);
        double std = Math.max(4.0, summary.getStdDeviation());
        double iqr = Math.max(2.0, summary.getInterquartileRange());
        double baseSpread = Math.max(4.0, Math.min(std, iqr / 1.34));
        Random random = new Random(Double.doubleToLongBits(summary.getMean()) ^ System.nanoTime());

        for (double interval : intervals)
        {
            double clamped = Math.max(15.0, interval);
            kernels.add(clamped);

            double localSpread = Math.max(3.5, baseSpread * 0.65);
            int extraPoints = summary.getIntervalCount() >= 150 ? 1 : 2;
            for (int i = 0; i < extraPoints; i++)
            {
                double jitter = random.nextGaussian() * localSpread;
                double candidate = Math.max(12.0, clamped + jitter);
                if (random.nextDouble() < 0.12)
                {
                    candidate += Math.abs(random.nextGaussian()) * localSpread * 0.45;
                }
                kernels.add(candidate);
            }
        }

        return kernels;
    }
}

