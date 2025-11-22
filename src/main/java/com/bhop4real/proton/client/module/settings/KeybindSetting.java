package com.bhop4real.proton.client.module.settings;

public class KeybindSetting extends Setting
{
    private int keyCode;
    
    public KeybindSetting(String name, String description, int defaultKeyCode)
    {
        super(name, description);
        this.keyCode = defaultKeyCode;
    }
    
    public int getKeyCode()
    {
        return keyCode;
    }
    
    public void setKeyCode(int keyCode)
    {
        this.keyCode = keyCode;
    }
    
    public void clear()
    {
        this.keyCode = 0;
    }
}

