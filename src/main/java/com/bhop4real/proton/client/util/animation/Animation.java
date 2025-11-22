package com.bhop4real.proton.client.util.animation;

/**
 * Time-based animation helper supporting easing curves and destination changes.
 */
public class Animation
{
    private Easing easing;
    private long duration;
    private long millis;
    private long startTime;

    private double startValue;
    private double destinationValue;
    private double value;
    private boolean finished;

    public Animation(final Easing easing, final long duration)
    {
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.duration = Math.max(1L, duration);
    }

    /**
     * Updates the animation using the supplied destination value.
     */
    public void run(final double destinationValue)
    {
        this.millis = System.currentTimeMillis();
        if (this.destinationValue != destinationValue)
        {
            this.destinationValue = destinationValue;
            this.reset();
        }
        else
        {
            this.finished = this.millis - this.duration > this.startTime;
            if (this.finished)
            {
                this.value = destinationValue;
                return;
            }
        }

        final double result = this.easing.getFunction().applyAsDouble(this.getProgress());
        if (this.value > destinationValue)
        {
            this.value = this.startValue - (this.startValue - destinationValue) * result;
        }
        else
        {
            this.value = this.startValue + (destinationValue - this.startValue) * result;
        }
    }

    /**
     * Returns the percentage progress of the animation (0..1).
     */
    public double getProgress()
    {
        double progress = (double) (System.currentTimeMillis() - this.startTime) / (double) this.duration;
        if (progress < 0.0D) progress = 0.0D;
        if (progress > 1.0D) progress = 1.0D;
        return progress;
    }

    /**
     * Resets the animation timeline while keeping the current value.
     */
    public void reset()
    {
        this.startTime = System.currentTimeMillis();
        this.startValue = value;
        this.finished = false;
    }

    public Easing getEasing()
    {
        return easing;
    }

    public void setEasing(Easing easing)
    {
        this.easing = easing;
    }

    public long getDuration()
    {
        return duration;
    }

    public void setDuration(long duration)
    {
        this.duration = Math.max(1L, duration);
    }

    public double getStartValue()
    {
        return startValue;
    }

    public void setStartValue(double startValue)
    {
        this.startValue = startValue;
    }

    public double getDestinationValue()
    {
        return destinationValue;
    }

    public void setDestinationValue(double destinationValue)
    {
        this.destinationValue = destinationValue;
    }

    public double getValue()
    {
        return value;
    }

    public void setValue(double value)
    {
        this.value = value;
    }

    public boolean isFinished()
    {
        return finished;
    }
}

