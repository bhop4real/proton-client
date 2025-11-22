package com.bhop4real.proton.client.module.settings;

public class IntSetting extends Setting
{
    private int value;
    private int min;
    private int max;
    
    public IntSetting(String name, String description, int defaultValue, int min, int max)
    {
        super(name, description);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
    }
    
    public int getValue()
    {
        return value;
    }
    
    public void setValue(int value)
    {
        if (value < min) value = min;
        if (value > max) value = max;
        this.value = value;
    }
    
    public int getMin()
    {
        return min;
    }
    
    public int getMax()
    {
        return max;
    }
}

