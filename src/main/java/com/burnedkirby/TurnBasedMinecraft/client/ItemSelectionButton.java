package com.burnedkirby.TurnBasedMinecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ItemSelectionButton extends Button {
    private int itemStackID;

    public ItemSelectionButton(int x, int y, int widthIn, int heightIn, String buttonText, int itemStackID, Button.OnPress onPress) {
        super(x, y, widthIn, heightIn, Component.literal(buttonText), onPress);
        this.itemStackID = itemStackID;
    }

    public int getID() {
        return itemStackID;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            boolean hovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + getHeight();
            if (hovered) {
                fill(poseStack, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x80FFFFFF);
            } else {
                fill(poseStack, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x20707070);
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