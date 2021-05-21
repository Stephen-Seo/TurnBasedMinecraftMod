package com.burnedkirby.TurnBasedMinecraft.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

public class ItemSelectionButton extends Button {
    private int itemStackID;

    public ItemSelectionButton(int x, int y, int widthIn, int heightIn, String buttonText, int itemStackID, Button.IPressable onPress) {
        super(x, y, widthIn, heightIn, new StringTextComponent(buttonText), onPress);
        this.itemStackID = itemStackID;
    }

    public int getID() {
        return itemStackID;
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            boolean hovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + getHeight();
            if (hovered) {
                fill(matrixStack, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x80FFFFFF);
            } else {
                fill(matrixStack, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x20707070);
            }
        }
    }

    private int getX() {
        return x;
    }

    private int getY() {
        return y;
    }
}