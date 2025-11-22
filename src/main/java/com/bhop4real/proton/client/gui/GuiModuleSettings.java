package com.bhop4real.proton.client.gui;

import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.EnumSetting;
import com.bhop4real.proton.client.module.settings.IntSetting;
import com.bhop4real.proton.client.module.settings.DoubleSetting;
import com.bhop4real.proton.client.module.settings.Setting;
import com.bhop4real.proton.client.module.settings.StringSetting;
import com.bhop4real.proton.client.util.font.FontUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class GuiModuleSettings extends GuiScreen
{
    private Module module;
    private GuiScreen parent;
    private int scroll;
    
    private IntSetting draggingSetting = null;
    private DoubleSetting draggingDoubleSetting = null;
    private EnumSetting dropdownSetting = null;
    private StringSetting editingStringSetting = null;
    private GuiTextField textField = null;
    
    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 250;
    private static final int ITEM_HEIGHT = 25;
    private static final int HEADER_HEIGHT = 30;
    private static final int SLIDER_WIDTH = 120;
    private static final int TEXT_FIELD_WIDTH = 200;
    
    private int guiLeft;
    private int guiTop;
    
    public GuiModuleSettings(Module module, GuiScreen parent)
    {
        this.module = module;
        this.parent = parent;
    }
    
    @Override
    public void initGui()
    {
        super.initGui();
        updateGuiPositions();
        Keyboard.enableRepeatEvents(true);
        
        // Initialize text field for StringSetting
        if (textField == null)
        {
            textField = new GuiTextField(0, mc.fontRendererObj, 0, 0, TEXT_FIELD_WIDTH, 16);
            textField.setMaxStringLength(256);
            textField.setEnableBackgroundDrawing(true);
        }
    }
    
    private void updateGuiPositions()
    {
        ScaledResolution sr = new ScaledResolution(mc);
        this.guiLeft = (sr.getScaledWidth() - GUI_WIDTH) / 2;
        this.guiTop = (sr.getScaledHeight() - GUI_HEIGHT) / 2;
    }
    
    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        draggingSetting = null;
        dropdownSetting = null;
        if (editingStringSetting != null && textField != null)
        {
            editingStringSetting.setValue(textField.getText());
            editingStringSetting = null;
        }
    }
    
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            if (editingStringSetting != null)
            {
                if (textField != null)
                {
                    editingStringSetting.setValue(textField.getText());
                }
                editingStringSetting = null;
            }
            else if (dropdownSetting != null)
            {
                dropdownSetting = null;
            }
            else
            {
                mc.displayGuiScreen(parent);
            }
            return;
        }
        
        if (editingStringSetting != null && textField != null)
        {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)
            {
                editingStringSetting.setValue(textField.getText());
                editingStringSetting = null;
                return;
            }
            textField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        
        super.keyTyped(typedChar, keyCode);
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        updateGuiPositions();
        
        if (mouseX >= guiLeft && mouseX <= guiLeft + GUI_WIDTH &&
            mouseY >= guiTop + HEADER_HEIGHT && mouseY <= guiTop + GUI_HEIGHT)
        {
            int clickY = mouseY - (guiTop + HEADER_HEIGHT) - scroll;
            int settingIndex = clickY / ITEM_HEIGHT;
            
            java.util.List<Setting> settings = module.getSettings();
            if (settingIndex >= 0 && settingIndex < settings.size())
            {
                Setting setting = settings.get(settingIndex);
                
                if (setting instanceof BooleanSetting)
                {
                    BooleanSetting boolSetting = (BooleanSetting) setting;
                    boolSetting.toggle();
                }
                else if (setting instanceof IntSetting)
                {
                    IntSetting intSetting = (IntSetting) setting;
                    int sliderX = guiLeft + GUI_WIDTH - SLIDER_WIDTH - 10;
                    int sliderY = guiTop + HEADER_HEIGHT + scroll + settingIndex * ITEM_HEIGHT + 8;
                    
                    // Check if clicking on slider
                    if (mouseX >= sliderX && mouseX <= sliderX + SLIDER_WIDTH && 
                        mouseY >= sliderY && mouseY <= sliderY + 10)
                    {
                        draggingSetting = intSetting;
                        updateSliderValue(intSetting, mouseX, sliderX);
                    }
                }
                else if (setting instanceof DoubleSetting)
                {
                    DoubleSetting doubleSetting = (DoubleSetting) setting;
                    int sliderX = guiLeft + GUI_WIDTH - SLIDER_WIDTH - 10;
                    int sliderY = guiTop + HEADER_HEIGHT + scroll + settingIndex * ITEM_HEIGHT + 8;
                    
                    if (mouseX >= sliderX && mouseX <= sliderX + SLIDER_WIDTH &&
                        mouseY >= sliderY && mouseY <= sliderY + 10)
                    {
                        draggingDoubleSetting = doubleSetting;
                        updateSliderValue(doubleSetting, mouseX, sliderX);
                    }
                }
                else if (setting instanceof EnumSetting)
                {
                    EnumSetting enumSetting = (EnumSetting) setting;
                    
                    // Toggle dropdown or cycle if clicking on value
                    int valueX = guiLeft + GUI_WIDTH - 100;
                    if (mouseX >= valueX && mouseX <= guiLeft + GUI_WIDTH - 10)
                    {
                        if (dropdownSetting == enumSetting)
                        {
                            // Clicking dropdown item
                            int dropdownY = guiTop + HEADER_HEIGHT + scroll + (settingIndex + 1) * ITEM_HEIGHT;
                            java.util.List<String> modes = enumSetting.getModes();
                            for (int i = 0; i < modes.size(); i++)
                            {
                                if (mouseY >= dropdownY + i * ITEM_HEIGHT && mouseY < dropdownY + (i + 1) * ITEM_HEIGHT)
                                {
                                    enumSetting.setValue(modes.get(i));
                                    dropdownSetting = null;
                                    break;
                                }
                            }
                        }
                        else
                        {
                            dropdownSetting = enumSetting;
                        }
                    }
                    else
                    {
                        enumSetting.cycle();
                    }
                }
                else if (setting instanceof StringSetting)
                {
                    StringSetting stringSetting = (StringSetting) setting;
                    
                    // Start editing if clicking on the setting
                    int textFieldX = guiLeft + GUI_WIDTH - TEXT_FIELD_WIDTH - 10;
                    int textFieldY = guiTop + HEADER_HEIGHT + scroll + settingIndex * ITEM_HEIGHT + 4;
                    
                    if (editingStringSetting != stringSetting)
                    {
                        editingStringSetting = stringSetting;
                        if (textField != null)
                        {
                            textField.setText(stringSetting.getValue());
                            textField.setFocused(true);
                            textField.xPosition = textFieldX;
                            textField.yPosition = textFieldY;
                        }
                    }
                    else if (textField != null)
                    {
                        // Click on text field area
                        textField.xPosition = textFieldX;
                        textField.yPosition = textFieldY;
                        textField.mouseClicked(mouseX, mouseY, mouseButton);
                    }
                }
            }
        }
        else
        {
            // Click outside - close dropdown or finish editing string
            if (editingStringSetting != null)
            {
                if (textField != null)
                {
                    editingStringSetting.setValue(textField.getText());
                }
                editingStringSetting = null;
            }
            dropdownSetting = null;
        }
    }
    
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick)
    {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        
        if ((draggingSetting != null || draggingDoubleSetting != null) && clickedMouseButton == 0)
        {
            java.util.List<Setting> settings = module.getSettings();
            int settingIndex = draggingSetting != null ? settings.indexOf(draggingSetting) : settings.indexOf(draggingDoubleSetting);
            if (settingIndex >= 0)
            {
                int sliderX = guiLeft + GUI_WIDTH - SLIDER_WIDTH - 10;
                if (draggingSetting != null)
                {
                    updateSliderValue(draggingSetting, mouseX, sliderX);
                }
                else if (draggingDoubleSetting != null)
                {
                    updateSliderValue(draggingDoubleSetting, mouseX, sliderX);
                }
            }
        }
    }
    
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
        draggingSetting = null;
        draggingDoubleSetting = null;
    }
    
    private void updateSliderValue(IntSetting setting, int mouseX, int sliderX)
    {
        float ratio = (float)(mouseX - sliderX) / (float)SLIDER_WIDTH;
        ratio = Math.max(0.0F, Math.min(1.0F, ratio));
        
        int range = setting.getMax() - setting.getMin();
        int value = setting.getMin() + (int)(range * ratio);
        setting.setValue(value);
    }
    
    private void updateSliderValue(DoubleSetting setting, int mouseX, int sliderX)
    {
        float ratio = (float)(mouseX - sliderX) / (float)SLIDER_WIDTH;
        ratio = Math.max(0.0F, Math.min(1.0F, ratio));
        
        double range = setting.getMax() - setting.getMin();
        double value = setting.getMin() + (range * ratio);
        setting.setValue(value);
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        updateGuiPositions();
        
        // Draw dark background
        this.drawDefaultBackground();
        
        // Draw main GUI background (unified with main click GUI)
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xDD101010);
        drawRect(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + HEADER_HEIGHT, 0xFF333333);
        
        // Draw header
        String title = EnumChatFormatting.GREEN + "Settings: " + EnumChatFormatting.WHITE + module.getName();
        FontUtil.drawString(title, guiLeft + 10, guiTop + 10, 0xFFFFFF, true);
        
        String backHint = EnumChatFormatting.GRAY + "Press ESC to go back";
        FontUtil.drawString(backHint, guiLeft + GUI_WIDTH - FontUtil.getStringWidth(backHint) - 10, guiTop + 10, 0xFFFFFF, true);
        
        // Draw settings list
        java.util.List<Setting> settings = module.getSettings();
        int settingY = guiTop + HEADER_HEIGHT + scroll;
        
        for (int i = 0; i < settings.size(); i++)
        {
            Setting setting = settings.get(i);
            
            if (settingY + ITEM_HEIGHT >= guiTop + HEADER_HEIGHT && settingY <= guiTop + GUI_HEIGHT)
            {
                // Draw setting background
                boolean hovered = mouseX >= guiLeft && mouseX <= guiLeft + GUI_WIDTH &&
                                 mouseY >= settingY && mouseY < settingY + ITEM_HEIGHT;
                
                if (hovered)
                {
                    drawRect(guiLeft, settingY, guiLeft + GUI_WIDTH, settingY + ITEM_HEIGHT, 0x80333333);
                }
                
                // Draw setting name
                FontUtil.drawString(setting.getName(), guiLeft + 10, settingY + 8, 0xFFFFFF, true);
                
                // Draw setting control
                if (setting instanceof BooleanSetting)
                {
                    BooleanSetting boolSetting = (BooleanSetting) setting;
                    String valueText = boolSetting.getValue() ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF";
                    int valueX = guiLeft + GUI_WIDTH - FontUtil.getStringWidth(valueText) - 10;
                    FontUtil.drawString(valueText, valueX, settingY + 8, 0xFFFFFF, true);
                }
                else if (setting instanceof IntSetting)
                {
                    IntSetting intSetting = (IntSetting) setting;
                    
                    // Draw slider
                    int sliderX = guiLeft + GUI_WIDTH - SLIDER_WIDTH - 10;
                    int sliderY = settingY + 8;
                    int sliderHeight = 10;

                    int trackLeft = sliderX;
                    int trackTop = sliderY;
                    int trackRight = sliderX + SLIDER_WIDTH;
                    int trackBottom = sliderY + sliderHeight;

                    // Slider track with outline
                    drawRect(trackLeft, trackTop, trackRight, trackBottom, 0xFF151515);
                    drawRect(trackLeft, trackTop, trackRight, trackTop + 1, 0xFF303030);
                    drawRect(trackLeft, trackBottom - 1, trackRight, trackBottom, 0xFF303030);
                    drawRect(trackLeft, trackTop, trackLeft + 1, trackBottom, 0xFF303030);
                    drawRect(trackRight - 1, trackTop, trackRight, trackBottom, 0xFF303030);

                    // Slider fill
                    float ratio = (float)(intSetting.getValue() - intSetting.getMin()) / (float)(intSetting.getMax() - intSetting.getMin());
                    ratio = Math.max(0.0F, Math.min(1.0F, ratio));
                    int innerWidth = SLIDER_WIDTH - 4;
                    int fillWidth = (int)(innerWidth * ratio);
                    int fillLeft = sliderX + 2;
                    int fillRight = fillLeft + Math.max(1, fillWidth);
                    drawRect(fillLeft, sliderY + 2, fillRight, sliderY + sliderHeight - 2, 0xFF34C759);

                    // Slider handle
                    int handleCenter = Math.min(sliderX + SLIDER_WIDTH - 3, Math.max(sliderX + 3, fillRight));
                    int handleLeft = handleCenter - 2;
                    int handleRight = handleCenter + 2;
                    drawRect(handleLeft - 1, sliderY + 1, handleRight + 1, sliderY + sliderHeight - 1, 0xFF2C2C2C);
                    drawRect(handleLeft, sliderY + 2, handleRight, sliderY + sliderHeight - 2, 0xFFFFFFFF);
                    
                    // Value text
                    String valueText = EnumChatFormatting.YELLOW + String.valueOf(intSetting.getValue());
                    int textX = sliderX - FontUtil.getStringWidth(valueText) - 5;
                    FontUtil.drawString(valueText, textX, settingY + 8, 0xFFFFFF, true);
                }
                else if (setting instanceof DoubleSetting)
                {
                    DoubleSetting doubleSetting = (DoubleSetting) setting;
                    
                    int sliderX = guiLeft + GUI_WIDTH - SLIDER_WIDTH - 10;
                    int sliderY = settingY + 8;
                    int sliderHeight = 10;

                    int trackLeft = sliderX;
                    int trackTop = sliderY;
                    int trackRight = sliderX + SLIDER_WIDTH;
                    int trackBottom = sliderY + sliderHeight;

                    drawRect(trackLeft, trackTop, trackRight, trackBottom, 0xFF151515);
                    drawRect(trackLeft, trackTop, trackRight, trackTop + 1, 0xFF303030);
                    drawRect(trackLeft, trackBottom - 1, trackRight, trackBottom, 0xFF303030);
                    drawRect(trackLeft, trackTop, trackLeft + 1, trackBottom, 0xFF303030);
                    drawRect(trackRight - 1, trackTop, trackRight, trackBottom, 0xFF303030);

                    float ratio = (float)((doubleSetting.getValue() - doubleSetting.getMin()) / (doubleSetting.getMax() - doubleSetting.getMin()));
                    ratio = Math.max(0.0F, Math.min(1.0F, ratio));
                    int innerWidth = SLIDER_WIDTH - 4;
                    int fillWidth = (int)(innerWidth * ratio);
                    int fillLeft = sliderX + 2;
                    int fillRight = fillLeft + Math.max(1, fillWidth);
                    drawRect(fillLeft, sliderY + 2, fillRight, sliderY + sliderHeight - 2, 0xFF34C759);

                    int handleCenter = Math.min(sliderX + SLIDER_WIDTH - 3, Math.max(sliderX + 3, fillRight));
                    int handleLeft = handleCenter - 2;
                    int handleRight = handleCenter + 2;
                    drawRect(handleLeft - 1, sliderY + 1, handleRight + 1, sliderY + sliderHeight - 1, 0xFF2C2C2C);
                    drawRect(handleLeft, sliderY + 2, handleRight, sliderY + sliderHeight - 2, 0xFFFFFFFF);
                    
                    String valueText = EnumChatFormatting.YELLOW + String.format(java.util.Locale.US, "%.2f", doubleSetting.getValue());
                    int textX = sliderX - FontUtil.getStringWidth(valueText) - 5;
                    FontUtil.drawString(valueText, textX, settingY + 8, 0xFFFFFF, true);
                }
                else if (setting instanceof EnumSetting)
                {
                    EnumSetting enumSetting = (EnumSetting) setting;
                    
                    // Draw current value
                    String valueText = EnumChatFormatting.AQUA + enumSetting.getValue() + (dropdownSetting == enumSetting ? " ▼" : " ▶");
                    int valueX = guiLeft + GUI_WIDTH - FontUtil.getStringWidth(valueText) - 10;
                    FontUtil.drawString(valueText, valueX, settingY + 8, 0xFFFFFF, true);
                    
                    // Draw dropdown if open
                    if (dropdownSetting == enumSetting)
                    {
                        java.util.List<String> modes = enumSetting.getModes();
                        int dropdownY = settingY + ITEM_HEIGHT;
                        int dropdownHeight = modes.size() * ITEM_HEIGHT;

                        int left = guiLeft + GUI_WIDTH - 100;
                        int right = guiLeft + GUI_WIDTH - 10;
                        int top = dropdownY;
                        int bottom = dropdownY + dropdownHeight;

                        // Dropdown background and outline
                        drawRect(left, top, right, bottom, 0xFF171717);
                        int outlineColor = 0xFF34C759;
                        drawRect(left, top, right, top + 1, outlineColor);
                        drawRect(left, bottom - 1, right, bottom, outlineColor);
                        drawRect(left, top, left + 1, bottom, outlineColor);
                        drawRect(right - 1, top, right, bottom, outlineColor);

                        // Dropdown items
                        for (int j = 0; j < modes.size(); j++)
                        {
                            String mode = modes.get(j);
                            boolean selected = mode.equals(enumSetting.getValue());
                            boolean itemHovered = mouseY >= dropdownY + j * ITEM_HEIGHT && mouseY < dropdownY + (j + 1) * ITEM_HEIGHT;
                            
                            if (itemHovered)
                            {
                                drawRect(left + 1, dropdownY + j * ITEM_HEIGHT + 1,
                                        right - 1, dropdownY + (j + 1) * ITEM_HEIGHT - 1, 0xFF262626);
                            }
                            else if (selected)
                            {
                                drawRect(left + 1, dropdownY + j * ITEM_HEIGHT + 1,
                                        right - 1, dropdownY + (j + 1) * ITEM_HEIGHT - 1, 0xFF1F1F1F);
                            }
                            
                            int textColor = selected ? 0xFFFFFFFF : (itemHovered ? 0xFFD0D0D0 : 0xFFB0B0B0);
                            String displayText = selected ? EnumChatFormatting.GREEN + "● " + mode : mode;
                            FontUtil.drawString(displayText, left + 6,
                                    dropdownY + j * ITEM_HEIGHT + 8, textColor, true);
                        }
                    }
                }
                else if (setting instanceof StringSetting)
                {
                    StringSetting stringSetting = (StringSetting) setting;
                    
                    // Draw value or text field
                    int textFieldX = guiLeft + GUI_WIDTH - TEXT_FIELD_WIDTH - 10;
                    int textFieldY = settingY + 4;
                    
                    if (editingStringSetting == stringSetting && textField != null)
                    {
                        // Draw text field
                        textField.xPosition = textFieldX;
                        textField.yPosition = textFieldY;
                        textField.drawTextBox();
                    }
                    else
                    {
                        // Draw current value
                        String value = stringSetting.getValue();
                        if (value == null || value.isEmpty())
                        {
                            value = EnumChatFormatting.GRAY + "(empty)";
                        }
                        else if (value.length() > 30)
                        {
                            value = value.substring(0, 27) + "...";
                        }
                        int valueX = guiLeft + GUI_WIDTH - FontUtil.getStringWidth(value) - 10;
                        FontUtil.drawString(EnumChatFormatting.YELLOW + value, valueX, settingY + 8, 0xFFFFFF, true);
                    }
                }
            }
            
            settingY += ITEM_HEIGHT;
            
            // Add extra space for dropdown
            if (setting instanceof EnumSetting && dropdownSetting == setting)
            {
                EnumSetting enumSettingForDropdown = (EnumSetting) setting;
                settingY += enumSettingForDropdown.getModes().size() * ITEM_HEIGHT;
            }
        }
        
        // Handle scrolling
        int mouseWheel = Mouse.getDWheel();
        if (mouseWheel != 0)
        {
            scroll -= mouseWheel > 0 ? 20 : -20;
            int maxScroll = Math.max(0, calculateTotalHeight(settings) - (GUI_HEIGHT - HEADER_HEIGHT));
            if (scroll < 0) scroll = 0;
            if (scroll > maxScroll) scroll = maxScroll;
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    private int calculateTotalHeight(java.util.List<Setting> settings)
    {
        int height = 0;
        for (Setting setting : settings)
        {
            height += ITEM_HEIGHT;
            if (setting instanceof EnumSetting && dropdownSetting == setting)
            {
                height += ((EnumSetting) setting).getModes().size() * ITEM_HEIGHT;
            }
        }
        return height;
    }
    
    @Override
    public void updateScreen()
    {
        super.updateScreen();
        if (textField != null && editingStringSetting != null)
        {
            textField.updateCursorCounter();
        }
    }
    
    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
