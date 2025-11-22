package com.bhop4real.proton.client.util.math;

import net.minecraft.util.MathHelper;

/**
 * Mathematical utility functions for common operations
 */
public final class MathUtil
{
    private MathUtil() {} // Prevent instantiation
    
    /**
     * Rounds a double value to a specified number of decimal places
     * @param value The value to round
     * @param places The number of decimal places
     * @return The rounded value
     * @throws IllegalArgumentException if places is negative
     */
    public static double round(double value, int places)
    {
        if (places < 0)
        {
            throw new IllegalArgumentException("Places must be non-negative");
        }
        
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
    
    /**
     * Clamps a float value between min and max
     */
    public static float clamp(float val, float min, float max)
    {
        return MathHelper.clamp_float(val, min, max);
    }
    
    /**
     * Clamps a double value between min and max
     */
    public static double clamp(double val, double min, double max)
    {
        return val < min ? min : (val > max ? max : val);
    }
    
    /**
     * Clamps an int value between min and max
     */
    public static int clamp(int val, int min, int max)
    {
        return val < min ? min : (val > max ? max : val);
    }
    
    /**
     * Linear interpolation between two values
     * @param start The start value
     * @param end The end value
     * @param delta The interpolation factor (0.0 to 1.0)
     * @return The interpolated value
     */
    public static double lerp(double start, double end, double delta)
    {
        return start + (end - start) * delta;
    }
    
    /**
     * Linear interpolation between two float values
     */
    public static float lerp(float start, float end, float delta)
    {
        return start + (end - start) * delta;
    }
    
    /**
     * Maps a value from one range to another
     * @param value The value to map
     * @param inMin Input range minimum
     * @param inMax Input range maximum
     * @param outMin Output range minimum
     * @param outMax Output range maximum
     * @return The mapped value
     */
    public static double map(double value, double inMin, double inMax, double outMin, double outMax)
    {
        return outMin + (outMax - outMin) * ((value - inMin) / (inMax - inMin));
    }
    
    /**
     * Calculates the distance between two 2D points
     */
    public static double distance(double x1, double y1, double x2, double y2)
    {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculates the distance between two 3D points
     */
    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}

