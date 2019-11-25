package com.burnedkirby.TurnBasedMinecraft.client;

import net.minecraft.client.gui.widget.button.Button;

public class ItemSelectionButton extends Button
{
    private int itemStackID;

    public ItemSelectionButton(int x, int y, int widthIn, int heightIn, String buttonText, int itemStackID, Button.IPressable onPress) {
        super(x, y, widthIn, heightIn, buttonText, onPress);
        this.itemStackID = itemStackID;
    }
    
    public int getID() {
    	return itemStackID;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        if(visible) {
            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            if(hovered) {
                fill(x, y, x + width, y + height, 0x80FFFFFF);
            } else {
                fill(x, y, x + width, y + height, 0x20707070);
            }
        }
    }
}
