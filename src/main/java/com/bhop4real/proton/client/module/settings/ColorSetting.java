package com.bhop4real.proton.client.module.settings;

public class ColorSetting extends Setting
{
    private int argb;
    
    public ColorSetting(String name, String description, int defaultArgb)
    {
        super(name, description);
        this.argb = defaultArgb;
    }
    
    public int getColor()
    {
        return argb;
    }
    
    public void setColor(int argb)
    {
        this.argb = argb;
    }
}


