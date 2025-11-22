package com.bhop4real.proton.client.learning.click;

import java.util.Locale;

/**
 * Represents the persisted outcome of a training run, including human-readable context.
 */
public final class ClickTrainingResult
{
    private final boolean success;
    private final String message;
    private final ClickModel model;
    private final ClickTrainingSummary summary;
    private final int totalSampleSets;
    private final int validSampleSets;
    private final boolean samplesCleared;

    private ClickTrainingResult(boolean success,
                                String message,
                                ClickModel model,
                                ClickTrainingSummary summary,
                                int totalSampleSets,
                                int validSampleSets,
                                boolean samplesCleared)
    {
        this.success = success;
        this.message = message;
        this.model = model;
        this.summary = summary;
        this.totalSampleSets = totalSampleSets;
        this.validSampleSets = validSampleSets;
        this.samplesCleared = samplesCleared;
    }

    public static ClickTrainingResult success(ClickTrainingData data, boolean samplesCleared)
    {
        String message = String.format(Locale.US,
            "processed %d/%d sample sets",
            data.getValidSampleSets(),
            data.getTotalSampleSets());

        return new ClickTrainingResult(
            true,
            message,
            data.getModel(),
            data.getSummary(),
            data.getTotalSampleSets(),
            data.getValidSampleSets(),
            samplesCleared);
    }

    public static ClickTrainingResult failure(String reason, ClickTrainingData data)
    {
        return new ClickTrainingResult(
            false,
            reason,
            data != null ? data.getModel() : null,
            data != null ? data.getSummary() : ClickTrainingSummary.empty(),
            data != null ? data.getTotalSampleSets() : 0,
            data != null ? data.getValidSampleSets() : 0,
            false);
    }

    public boolean isSuccess()
    {
        return success;
    }

    public String getMessage()
    {
        return message;
    }

    public ClickModel getModel()
    {
        return model;
    }

    public ClickTrainingSummary getSummary()
    {
        return summary;
    }

    public int getTotalSampleSets()
    {
        return totalSampleSets;
    }

    public int getValidSampleSets()
    {
        return validSampleSets;
    }

    public boolean didClearSamples()
    {
        return samplesCleared;
    }

    public String formatSuccessDetails()
    {
        if (!success)
        {
            return message;
        }
        if (summary == null || summary.getIntervalCount() == 0)
        {
            return "training completed but no statistical summary is available.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.US,
            "%d/%d sample sets, %d intervals",
            validSampleSets,
            totalSampleSets,
            summary.getIntervalCount()));
        builder.append(String.format(Locale.US,
            " (median %d ms, σ %d ms, p90 %d ms, range %d-%d ms)",
            Math.round(summary.getMedian()),
            Math.round(summary.getStdDeviation()),
            Math.round(summary.getPercentile90()),
            Math.round(summary.getMin()),
            Math.round(summary.getMax())));
        return builder.toString();
    }
}

