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
    public void func_230430_a_(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // field_230694_p_ is probably isVisible
        if (field_230694_p_) {
            boolean hovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + getHeight();
            if (hovered) {
                fill(matrixStack, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x80FFFFFF);
            } else {
                fill(matrixStack, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x20707070);
            }
        }
    }

    private int getX() {
        return field_230690_l_;
    }

    private int getY() {
        return field_230691_m_;
    }

    private int getWidth() {
        return field_230688_j_;
    }

    private int getHeight() {
        return field_230689_k_;

    }

    private void fill(MatrixStack matrixStack, int x, int y, int width, int height, int color) {
        func_238467_a_(matrixStack, x, y, width, height, color);
    }
}
