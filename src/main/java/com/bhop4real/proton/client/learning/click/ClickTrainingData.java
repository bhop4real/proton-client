package com.bhop4real.proton.client.learning.click;

/**
 * Raw outcome of the trainer before persistence.
 */
public final class ClickTrainingData
{
    private static final ClickTrainingData EMPTY = new ClickTrainingData(
        null,
        ClickTrainingSummary.empty(),
        0,
        0);

    private final ClickModel model;
    private final ClickTrainingSummary summary;
    private final int totalSampleSets;
    private final int validSampleSets;

    public ClickTrainingData(ClickModel model,
                             ClickTrainingSummary summary,
                             int totalSampleSets,
                             int validSampleSets)
    {
        this.model = model;
        this.summary = summary == null ? ClickTrainingSummary.empty() : summary;
        this.totalSampleSets = Math.max(0, totalSampleSets);
        this.validSampleSets = Math.max(0, validSampleSets);
    }

    public static ClickTrainingData empty(int totalSampleSets, int validSampleSets)
    {
        return new ClickTrainingData(null, ClickTrainingSummary.empty(), totalSampleSets, validSampleSets);
    }

    public static ClickTrainingData empty()
    {
        return EMPTY;
    }

    public boolean hasModel()
    {
        return model != null && model.isReady();
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
}

