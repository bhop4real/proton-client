package com.bhop4real.proton.client.learning.click;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple in-game click recorder that samples the Minecraft client tick to capture press/hold timings.
 */
public class ClickSampleRecorder
{
    private final List<Long> intervals = new ArrayList<>();
    private final List<Long> holdDurations = new ArrayList<>();

    private boolean recording;
    private boolean lastButtonState;
    private boolean skipNextPress;

    private long lastReleaseMs = -1L;
    private long currentPressMs = -1L;

    public synchronized boolean start()
    {
        resetBuffers();
        recording = true;
        return true;
    }

    public synchronized void stop()
    {
        recording = false;
        currentPressMs = -1L;
    }

    public synchronized void shutdown()
    {
        stop();
        resetBuffers();
    }

    public synchronized boolean isRecording()
    {
        return recording;
    }

    public synchronized int capturedClicks()
    {
        return holdDurations.size();
    }

    public synchronized List<Long> getIntervalsSnapshot()
    {
        return Collections.unmodifiableList(new ArrayList<>(intervals));
    }

    public synchronized List<Long> getHoldDurationsSnapshot()
    {
        return Collections.unmodifiableList(new ArrayList<>(holdDurations));
    }

    public synchronized ClickSample buildSampleAndReset()
    {
        ClickSample sample = holdDurations.isEmpty()
            ? null
            : ClickSample.create(new ArrayList<>(intervals), new ArrayList<>(holdDurations));
        resetBuffers();
        return sample;
    }

    public synchronized void reset()
    {
        resetBuffers();
    }

    public synchronized void tick(boolean buttonDown, long nowMs)
    {
        if (!recording)
        {
            lastButtonState = buttonDown;
            return;
        }

        if (buttonDown && !lastButtonState)
        {
            handlePress(nowMs);
        }
        else if (!buttonDown && lastButtonState)
        {
            handleRelease(nowMs);
        }

        lastButtonState = buttonDown;
    }

    private void handlePress(long nowMs)
    {
        if (skipNextPress)
        {
            skipNextPress = false;
            currentPressMs = nowMs;
            lastReleaseMs = nowMs;
            return;
        }

        if (lastReleaseMs > 0)
        {
            long interval = nowMs - lastReleaseMs;
            if (interval >= 10L && interval <= 2000L)
            {
                intervals.add(interval);
            }
        }

        currentPressMs = nowMs;
    }

    private void handleRelease(long nowMs)
    {
        if (currentPressMs > 0)
        {
            long hold = nowMs - currentPressMs;
            if (hold >= 5L && hold <= 2000L)
            {
                holdDurations.add(hold);
            }
        }

        currentPressMs = -1L;
        lastReleaseMs = nowMs;
    }

    private void resetBuffers()
    {
        intervals.clear();
        holdDurations.clear();
        lastReleaseMs = -1L;
        currentPressMs = -1L;
        lastButtonState = false;
        skipNextPress = true;
    }
}

