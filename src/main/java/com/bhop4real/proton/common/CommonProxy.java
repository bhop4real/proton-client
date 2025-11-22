package com.bhop4real.proton.common;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Common proxy for server-side initialization.
 * Client-specific initialization should be handled in ClientProxy.
 */
public class CommonProxy
{
    public void preInit(FMLPreInitializationEvent event)
    {
        // Server-side initialization
    }
    
    public void init(FMLInitializationEvent event)
    {
        // Server-side initialization
    }
}