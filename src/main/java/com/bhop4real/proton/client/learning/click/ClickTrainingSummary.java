package com.bhop4real.proton.client.learning.click;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Statistical summary produced during click model training.
 */
public final class ClickTrainingSummary
{
    private static final ClickTrainingSummary EMPTY = new ClickTrainingSummary(
        0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

    private final int intervalCount;
    private final double mean;
    private final double stdDeviation;
    private final double median;
    private final double medianAbsoluteDeviation;
    private final double percentile90;
    private final double min;
    private final double max;
    private final double interquartileRange;

    private ClickTrainingSummary(int intervalCount,
                                 double mean,
                                 double stdDeviation,
                                 double median,
                                 double medianAbsoluteDeviation,
                                 double percentile90,
                                 double min,
                                 double max,
                                 double interquartileRange)
    {
        this.intervalCount = intervalCount;
        this.mean = mean;
        this.stdDeviation = stdDeviation;
        this.median = median;
        this.medianAbsoluteDeviation = medianAbsoluteDeviation;
        this.percentile90 = percentile90;
        this.min = min;
        this.max = max;
        this.interquartileRange = interquartileRange;
    }

    public static ClickTrainingSummary empty()
    {
        return EMPTY;
    }

    public static ClickTrainingSummary fromIntervals(List<Double> intervals)
    {
        if (intervals == null || intervals.isEmpty())
        {
            return empty();
        }

        List<Double> sorted = new ArrayList<>(intervals);
        Collections.sort(sorted);
        int n = sorted.size();

        double sum = 0.0;
        for (double value : sorted)
        {
            sum += value;
        }
        double mean = sum / n;

        double varianceAccumulator = 0.0;
        for (double value : sorted)
        {
            double delta = value - mean;
            varianceAccumulator += delta * delta;
        }
        double variance = varianceAccumulator / Math.max(1, n - 1);
        double stdDev = Math.sqrt(Math.max(variance, 1e-6));

        double median = quantile(sorted, 0.5);
        double q1 = quantile(sorted, 0.25);
        double q3 = quantile(sorted, 0.75);
        double iqr = Math.max(0.0, q3 - q1);
        double percentile90 = quantile(sorted, 0.90);

        double mad;
        {
            List<Double> deviations = new ArrayList<>(n);
            for (double value : sorted)
            {
                deviations.add(Math.abs(value - median));
            }
            Collections.sort(deviations);
            mad = quantile(deviations, 0.5);
        }

        double min = sorted.get(0);
        double max = sorted.get(sorted.size() - 1);

        return new ClickTrainingSummary(
            n,
            mean,
            stdDev,
            median,
            mad,
            percentile90,
            min,
            max,
            iqr);
    }

    private static double quantile(List<Double> sorted, double probability)
    {
        if (sorted.isEmpty())
        {
            return 0.0;
        }
        if (sorted.size() == 1)
        {
            return sorted.get(0);
        }

        double clamped = Math.max(0.0, Math.min(1.0, probability));
        double position = clamped * (sorted.size() - 1);
        int lowerIndex = (int) Math.floor(position);
        int upperIndex = Math.min(sorted.size() - 1, lowerIndex + 1);
        double weight = position - lowerIndex;
        double lowerValue = sorted.get(lowerIndex);
        double upperValue = sorted.get(upperIndex);
        return lowerValue + (upperValue - lowerValue) * weight;
    }

    public int getIntervalCount()
    {
        return intervalCount;
    }

    public double getMean()
    {
        return mean;
    }

    public double getStdDeviation()
    {
        return stdDeviation;
    }

    public double getMedian()
    {
        return median;
    }

    public double getMedianAbsoluteDeviation()
    {
        return medianAbsoluteDeviation;
    }

    public double getPercentile90()
    {
        return percentile90;
    }

    public double getMin()
    {
        return min;
    }

    public double getMax()
    {
        return max;
    }

    public double getInterquartileRange()
    {
        return interquartileRange;
    }

    public String toChatSummary()
    {
        if (intervalCount == 0)
        {
            return "no intervals";
        }

        return String.format(Locale.US,
            "%d intervals (median %d ms, σ %d ms, p90 %d ms, range %d-%d ms)",
            intervalCount,
            Math.round(median),
            Math.round(stdDeviation),
            Math.round(percentile90),
            Math.round(min),
            Math.round(max));
    }

    public String toLogSummary()
    {
        if (intervalCount == 0)
        {
            return "no intervals";
        }

        return String.format(Locale.US,
            "%d intervals | mean=%.2f ms | median=%.2f ms | σ=%.2f ms | MAD=%.2f ms | p90=%.2f ms | range=%.2f-%.2f ms | IQR=%.2f ms",
            intervalCount,
            mean,
            median,
            stdDeviation,
            medianAbsoluteDeviation,
            percentile90,
            min,
            max,
            interquartileRange);
    }
}

