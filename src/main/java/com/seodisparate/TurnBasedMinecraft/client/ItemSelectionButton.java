package com.seodisparate.TurnBasedMinecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class ItemSelectionButton extends GuiButton
{
    int itemStackID;

    public ItemSelectionButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, int itemStackID)
    {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.itemStackID = itemStackID;
    }

    public ItemSelectionButton(int buttonId, int x, int y, String buttonText, int itemStackID)
    {
        super(buttonId, x, y, buttonText);
        this.itemStackID = itemStackID;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        if(visible)
        {
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            if(hovered)
            {
                drawRect(x, y, x + width, y + height, 0x80FFFFFF);
            }
            else
            {
                drawRect(x, y, x + width, y + height, 0x20707070);
            }
        }
    }
}
