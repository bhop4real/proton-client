package com.bhop4real.proton.client.module.settings;

public abstract class Setting
{
    private String name;
    private String description;
    private java.util.function.Supplier<Boolean> visibilitySupplier;
    
    public Setting(String name, String description)
    {
        this.name = name;
        this.description = description;
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public void setVisibilitySupplier(java.util.function.Supplier<Boolean> supplier)
    {
        this.visibilitySupplier = supplier;
    }
    
    public boolean isVisible()
    {
        return visibilitySupplier == null || Boolean.TRUE.equals(visibilitySupplier.get());
    }
}

