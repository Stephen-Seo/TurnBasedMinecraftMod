package com.burnedkirby.TurnBasedMinecraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class ItemSelectionButton implements Renderable, GuiEventListener, NarratableEntry {
    private int x;
    private int y;
    private int width;
    private int height;
    TBMItemButtonPress onPress;
    private int itemStackID;
    private boolean focused;

    public ItemSelectionButton(int x, int y, int widthIn, int heightIn, int itemStackID, TBMItemButtonPress onPress) {
        this.x = x;
        this.y = y;
        this.width = widthIn;
        this.height = heightIn;
        this.onPress = onPress;
        this.itemStackID = itemStackID;
    }

    public int getID() {
        return itemStackID;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float unk) {
        boolean hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        if (hovered) {
            guiGraphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x80FFFFFF);
        } else {
            guiGraphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x20707070);
        }
    }

    public void onPress() {
        onPress.onPress(this);
    }

    @Override
    public void setFocused(boolean b) {
        focused = b;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.FOCUSED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.HINT, "Item " + this.itemStackID);
    }

    @Override
    public boolean mouseClicked(double x, double y, int unknown) {
        if (unknown == 0 && x >= this.x && y >= this.y && x <= (double)(this.x + this.width) && y <= (double)(this.y + this.height)) {
            onPress();
            return true;
        }
        return false;
    }
}
