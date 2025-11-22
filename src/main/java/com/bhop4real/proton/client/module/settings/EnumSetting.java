package com.bhop4real.proton.client.module.settings;

import java.util.Arrays;
import java.util.List;

public class EnumSetting extends Setting
{
    private String value;
    private List<String> modes;
    
    public EnumSetting(String name, String description, String defaultValue, String... modes)
    {
        super(name, description);
        this.modes = Arrays.asList(modes);
        this.value = defaultValue != null && modes.length > 0 ? defaultValue : (modes.length > 0 ? modes[0] : "");
    }
    
    public String getValue()
    {
        return value;
    }
    
    public void setValue(String value)
    {
        if (modes.contains(value))
        {
            this.value = value;
        }
    }
    
    public List<String> getModes()
    {
        return modes;
    }
    
    public void cycle()
    {
        if (modes.isEmpty()) return;
        int currentIndex = modes.indexOf(value);
        int nextIndex = (currentIndex + 1) % modes.size();
        this.value = modes.get(nextIndex);
    }
}

