package com.bhop4real.proton.client.gui.util;

import com.bhop4real.proton.client.util.animation.Animation;
import com.bhop4real.proton.client.util.animation.Easing;

/**
 * Utility class for common GUI animations
 */
public final class GuiAnimation
{
    private final Animation animation;
    private double startValue;
    private double targetValue;
    
    public GuiAnimation(Easing easing, long duration)
    {
        this.animation = new Animation(easing, duration);
        this.startValue = 0.0;
        this.targetValue = 0.0;
    }
    
    /**
     * Animates to a target value
     */
    public void animateTo(double target)
    {
        // Check if target changed (with small epsilon for floating point comparison)
        if (Math.abs(this.targetValue - target) > 0.001)
        {
            // Target changed - capture current animation value
            double currentValue = getValue();
            this.startValue = currentValue;
            this.targetValue = target;
            
            // Update the underlying animation
            // Set the current value first, then set start and destination
            this.animation.setValue(currentValue);
            this.animation.setDestinationValue(target);
            // Reset timeline (this sets startTime to now and startValue = current value)
            // Note: reset() sets startValue = value, so we set value first
            this.animation.reset();
        }
        
        // Update animation each frame (this interpolates from current to target)
        this.animation.run(this.targetValue);
    }
    
    /**
     * Gets the current animated value
     */
    public double getValue()
    {
        return this.animation.getValue();
    }
    
    /**
     * Gets the current progress (0.0 to 1.0)
     */
    public double getProgress()
    {
        if (Math.abs(this.targetValue - this.startValue) < 0.001)
        {
            return 1.0;
        }
        return (getValue() - this.startValue) / (this.targetValue - this.startValue);
    }
    
    /**
     * Checks if the animation is finished
     */
    public boolean isFinished()
    {
        return this.animation.isFinished();
    }
    
    /**
     * Resets the animation
     */
    public void reset()
    {
        this.animation.reset();
    }
    
    /**
     * Sets the start value
     */
    public void setStartValue(double value)
    {
        this.startValue = value;
        this.animation.setStartValue(value);
    }
    
    /**
     * Sets the animation duration
     */
    public void setDuration(long duration)
    {
        this.animation.setDuration(duration);
    }
    
    /**
     * Creates a fade animation (0.0 to 1.0)
     */
    public static GuiAnimation createFade(long duration)
    {
        return new GuiAnimation(Easing.EASE_OUT_QUINT, duration);
    }
    
    /**
     * Creates a slide animation
     */
    public static GuiAnimation createSlide(long duration)
    {
        return new GuiAnimation(Easing.EASE_OUT_QUINT, duration);
    }
}

