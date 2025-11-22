package com.bhop4real.proton.client.learning.click;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Immutable representation of a collected click sample consisting of inter-click intervals.
 */
public final class ClickSample
{
    private final String id;
    private final long createdAt;
    private final List<Long> intervals;
    private final List<Long> holdDurations;

    public ClickSample(String id, long createdAt, List<Long> intervals, List<Long> holdDurations)
    {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.createdAt = createdAt;
        this.intervals = intervals != null
            ? Collections.unmodifiableList(new ArrayList<>(intervals))
            : Collections.emptyList();
        this.holdDurations = holdDurations != null
            ? Collections.unmodifiableList(new ArrayList<>(holdDurations))
            : Collections.emptyList();
    }

    public static ClickSample create(List<Long> intervals, List<Long> holdDurations)
    {
        return new ClickSample(UUID.randomUUID().toString(), System.currentTimeMillis(), intervals, holdDurations);
    }

    public String getId()
    {
        return id;
    }

    public long getCreatedAt()
    {
        return createdAt;
    }

    public List<Long> getIntervals()
    {
        return intervals;
    }

    public List<Long> getHoldDurations()
    {
        return holdDurations;
    }

    public int size()
    {
        return Math.max(holdDurations.size(), intervals.size());
    }
}

