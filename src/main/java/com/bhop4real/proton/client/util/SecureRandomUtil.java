package com.bhop4real.proton.client.util;

import java.security.SecureRandom;

/**
 * Utility for secure random number generation
 * Uses SecureRandom for cryptographically strong randomness
 */
public final class SecureRandomUtil
{
    private static final SecureRandom secureRandom = new SecureRandom();
    
    private SecureRandomUtil() {} // Prevent instantiation
    
    /**
     * Gets a secure random integer between min (inclusive) and max (exclusive)
     */
    public static int nextInt(int min, int max)
    {
        if (min >= max)
        {
            return min;
        }
        return secureRandom.nextInt(max - min) + min;
    }
    
    /**
     * Gets a secure random double between 0.0 (inclusive) and 1.0 (exclusive)
     */
    public static double nextDouble()
    {
        return secureRandom.nextDouble();
    }
    
    /**
     * Gets a secure random double between min (inclusive) and max (exclusive)
     */
    public static double nextDouble(double min, double max)
    {
        if (min >= max)
        {
            return min;
        }
        return secureRandom.nextDouble() * (max - min) + min;
    }
    
    /**
     * Gets a secure random long
     */
    public static long nextLong()
    {
        return secureRandom.nextLong();
    }
    
    /**
     * Gets a secure random boolean
     */
    public static boolean nextBoolean()
    {
        return secureRandom.nextBoolean();
    }

    /**
     * Gets a secure random Gaussian (normal distribution, mean 0, std dev 1)
     */
    public static double nextGaussian()
    {
        return secureRandom.nextGaussian();
    }
}

