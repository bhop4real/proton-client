package com.bhop4real.proton.client.module.settings;

public class StringSetting extends Setting
{
    private String value;
    
    public StringSetting(String name, String description, String defaultValue)
    {
        super(name, description);
        this.value = defaultValue != null ? defaultValue : "";
    }
    
    public String getValue()
    {
        return value;
    }
    
    public void setValue(String value)
    {
        this.value = value != null ? value : "";
    }
}

