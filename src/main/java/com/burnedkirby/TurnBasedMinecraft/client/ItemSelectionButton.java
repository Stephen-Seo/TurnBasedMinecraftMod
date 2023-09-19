package com.burnedkirby.TurnBasedMinecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ItemSelectionButton extends AbstractButton {

    TBMButtonPress onPress;
    private int itemStackID;

    public ItemSelectionButton(int x, int y, int widthIn, int heightIn, String buttonText, int itemStackID, TBMButtonPress onPress) {
        super(x, y, widthIn, heightIn, Component.literal(buttonText));
        this.onPress = onPress;
        this.itemStackID = itemStackID;
    }

    public int getID() {
        return itemStackID;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float unk) {
        if (visible) {
            boolean hovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + getHeight();
            if (hovered) {
                guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x80FFFFFF);
            } else {
                guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x20707070);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_259858_) {
        p_259858_.add(NarratedElementType.HINT, "Item " + this.itemStackID);
    }

    @Override
    public void onPress() {
        onPress.onPress(this);
    }
}