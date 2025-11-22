package com.bhop4real.proton.client.gui;

import com.bhop4real.proton.client.command.CommandHandler;
import com.bhop4real.proton.client.util.font.FontUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiCommand extends GuiScreen
{
    private GuiTextField commandField;
    private String lastCommand = "";
    
    @Override
    public void initGui()
    {
        super.initGui();
        
        // Create command input field at the center of the screen
        int width = 400;
        int height = 20;
        int x = (this.width - width) / 2;
        int y = this.height / 2;
        
        this.commandField = new GuiTextField(0, this.fontRendererObj, x, y, width, height);
        this.commandField.setMaxStringLength(256);
        this.commandField.setFocused(true);
        this.commandField.setText(this.lastCommand);
        
        Keyboard.enableRepeatEvents(true);
    }
    
    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        this.lastCommand = this.commandField.getText();
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            this.mc.displayGuiScreen(null);
            return;
        }
        
        if (this.commandField.isFocused())
        {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)
            {
                String command = this.commandField.getText().trim();
                if (!command.isEmpty())
                {
                    // Process the command (add . prefix if not present)
                    String commandToProcess = command.startsWith(".") ? command : "." + command;
                    CommandHandler.processCommand(commandToProcess);
                    
                    // Clear the field for next command
                    this.commandField.setText("");
                }
                return;
            }
            
            this.commandField.textboxKeyTyped(typedChar, keyCode);
        }
        
        super.keyTyped(typedChar, keyCode);
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.commandField.mouseClicked(mouseX, mouseY, mouseButton);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        // Draw dark background
        this.drawDefaultBackground();
        
        // Draw title
        String title = EnumChatFormatting.GREEN + "Proton Commands";
        int titleWidth = FontUtil.getStringWidth(title);
        FontUtil.drawString(title, (this.width - titleWidth) / 2, this.height / 2 - 40, 0xFFFFFF, true);
        
        // Draw command input field
        this.commandField.drawTextBox();
        
        // Draw hint text below input
        String hint = EnumChatFormatting.GRAY + "Type module name and press Enter (e.g., .sprint)";
        int hintWidth = FontUtil.getStringWidth(hint);
        FontUtil.drawString(hint, (this.width - hintWidth) / 2, this.height / 2 + 25, 0xFFFFFF, true);
        
        // Draw some example commands
        String examples = EnumChatFormatting.DARK_GRAY + "Example: .sprint (toggles sprint module)";
        int examplesWidth = FontUtil.getStringWidth(examples);
        FontUtil.drawString(examples, (this.width - examplesWidth) / 2, this.height / 2 + 40, 0xFFFFFF, true);
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    @Override
    public void updateScreen()
    {
        super.updateScreen();
        this.commandField.updateCursorCounter();
    }
}

