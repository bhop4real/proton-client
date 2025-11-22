package com.bhop4real.proton.client.command;

import com.bhop4real.proton.client.ProtonClient;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class CommandHandler
{
    private static final String PREFIX = ".";
    private static CommandHandler instance;
    
    public CommandHandler()
    {
        instance = this;
    }
    
    /**
     * Process a command message - simple module toggle only
     */
    public static void processCommand(String message)
    {
        if (instance == null)
        {
            return;
        }
        
        if (!message.startsWith(PREFIX))
        {
            return;
        }
        
        // Remove prefix and get module name
        String moduleName = message.substring(PREFIX.length()).trim();
        
        if (moduleName.isEmpty())
        {
            sendMessage(EnumChatFormatting.RED + "Usage: .<module_name>");
            return;
        }
        
        // Try to toggle module by name
        if (ProtonClient.getInstance().getModuleManager() != null)
        {
            com.bhop4real.proton.client.module.Module module = ProtonClient.getInstance().getModuleManager().getModuleByName(moduleName);
            if (module != null)
            {
                module.toggle();
                String status = module.isEnabled() ? EnumChatFormatting.GREEN + "enabled" : EnumChatFormatting.RED + "disabled";
                sendMessage(EnumChatFormatting.GREEN + module.getName() + " is now " + status);
            }
            else
            {
                sendMessage(EnumChatFormatting.RED + "Module not found: " + moduleName);
            }
        }
    }
    
    public static void sendMessage(String message)
    {
        if (Minecraft.getMinecraft().thePlayer != null)
        {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
