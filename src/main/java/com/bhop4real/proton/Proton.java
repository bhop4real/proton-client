package com.bhop4real.proton;

import com.bhop4real.proton.common.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Proton.MODID, version = Proton.VERSION, clientSideOnly = true)
public class Proton
{
    public static final String MODID = "proton";
    public static final String VERSION = "1.0";
    public static final Logger logger = LogManager.getLogger(MODID);
    
    @SidedProxy(
        clientSide = "com.bhop4real.proton.client.ClientProxy",
        serverSide = "com.bhop4real.proton.common.CommonProxy"
    )
    public static CommonProxy proxy;
    
    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger.info("Proton mod is loading...");
        proxy.preInit(event);
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.init(event);
        logger.info("Proton mod initialized successfully!");
    }
    
    @EventHandler
    public void onServerStopping(FMLServerStoppingEvent event)
    {
        // This fires when the client/server is stopping
        if (proxy instanceof com.bhop4real.proton.client.ClientProxy)
        {
            ((com.bhop4real.proton.client.ClientProxy) proxy).onShutdown();
        }
    }
}
