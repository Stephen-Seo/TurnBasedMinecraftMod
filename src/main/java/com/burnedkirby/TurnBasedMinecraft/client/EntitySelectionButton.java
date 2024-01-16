package com.burnedkirby.TurnBasedMinecraft.client;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EntitySelectionButton implements Renderable, GuiEventListener, NarratableEntry {
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean focused;
    private Button nestedButton;
    TBMEntityButtonPress onPress;
    private int entityID;
    private boolean isSideA;

    public EntitySelectionButton(int x, int y, int widthIn, int heightIn, String buttonText, int entityID, boolean isSideA, TBMEntityButtonPress onPress) {
        this.x = x;
        this.y = y;
        this.width = widthIn;
        this.height = heightIn;
        this.onPress = onPress;
        this.entityID = entityID;
        this.isSideA = isSideA;
        this.nestedButton = Button.builder(Component.literal(buttonText), (unused) -> {}).pos(x, y).size(widthIn, heightIn).build();
    }

    public EntitySelectionButton(int x, int y, int widthIn, int heightIn, Component buttonTextComponent, int entityID, boolean isSideA, TBMEntityButtonPress onPress) {
        this.x = x;
        this.y = y;
        this.width = widthIn;
        this.height = heightIn;
        this.onPress = onPress;
        this.entityID = entityID;
        this.isSideA = isSideA;
        this.nestedButton = Button.builder(buttonTextComponent, (unused) -> {}).pos(x, y).size(widthIn, heightIn).build();
    }

    public int getID() {
        return entityID;
    }

    public boolean getIsSideA() {
        return isSideA;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.nestedButton.render(guiGraphics, mouseX, mouseY, partialTicks);
        Entity e = Minecraft.getInstance().level.getEntity(entityID);
        if (e != null && e instanceof LivingEntity && ((LivingEntity) e).isAlive()) {
            int health = (int) (((LivingEntity) e).getHealth() + 0.5f);
            int xpos = this.x;
            int xoffset;
            if (isSideA) {
                xpos += this.width + 4;
                xoffset = 4;
            } else {
                xpos -= 6;
                xoffset = -4;
            }
            if (health > 200) {
                guiGraphics.fill(xpos, this.y + this.height * 4 / 5, xpos + 2, this.y + this.height, 0xFFFF0000);
                guiGraphics.fill(xpos, this.y + this.height * 3 / 5, xpos + 2, this.y + this.height * 4 / 5, 0xFFFFFF00);
                guiGraphics.fill(xpos, this.y + this.height * 2 / 5, xpos + 2, this.y + this.height * 3 / 5, 0xFF00FF00);
                guiGraphics.fill(xpos, this.y + this.height / 5, xpos + 2, this.y + this.height * 2 / 5, 0xFF00FFFF);
                guiGraphics.fill(xpos, this.y, xpos + 2, this.y + this.height / 5, 0xFF0000FF);
                int healthHeight = ((health - 200) * this.height / 100);
                guiGraphics.fill(xpos + xoffset, this.y + this.height - healthHeight, xpos + xoffset + 2, this.y + this.height, 0xFFFFFFFF);
            } else if (health > 100) {
                guiGraphics.fill(xpos, this.y + this.height * 4 / 5, xpos + 2, this.y + this.height, 0xFFFF0000);
                guiGraphics.fill(xpos, this.y + this.height * 3 / 5, xpos + 2, this.y + this.height * 4 / 5, 0xFFFFFF00);
                guiGraphics.fill(xpos, this.y + this.height * 2 / 5, xpos + 2, this.y + this.height * 3 / 5, 0xFF00FF00);
                guiGraphics.fill(xpos, this.y + this.height / 5, xpos + 2, this.y + this.height * 2 / 5, 0xFF00FFFF);
                int healthHeight = ((health - 100) * this.height / 100);
                guiGraphics.fill(xpos + xoffset, this.y + this.height - healthHeight, xpos + xoffset + 2, this.y + this.height, 0xFF0000FF);
            } else if (health > 50) {
                guiGraphics.fill(xpos, this.y + this.height * 4 / 5, xpos + 2, this.y + this.height, 0xFFFF0000);
                guiGraphics.fill(xpos, this.y + this.height * 3 / 5, xpos + 2, this.y + this.height * 4 / 5, 0xFFFFFF00);
                guiGraphics.fill(xpos, this.y + this.height * 2 / 5, xpos + 2, this.y + this.height * 3 / 5, 0xFF00FF00);
                int healthHeight = ((health - 50) * this.height / 50);
                guiGraphics.fill(xpos + xoffset, this.y + this.height - healthHeight, xpos + xoffset + 2, this.y + this.height, 0xFF00FFFF);
            } else if (health > 20) {
                guiGraphics.fill(xpos, this.y + this.height * 4 / 5, xpos + 2, this.y + this.height, 0xFFFF0000);
                guiGraphics.fill(xpos, this.y + this.height * 3 / 5, xpos + 2, this.y + this.height * 4 / 5, 0xFFFFFF00);
                int healthHeight = ((health - 20) * this.height / 30);
                guiGraphics.fill(xpos + xoffset, this.y + this.height - healthHeight, xpos + xoffset + 2, this.y + this.height, 0xFF00FF00);
            } else if (health > 10) {
                guiGraphics.fill(xpos, this.y + this.height * 4 / 5, xpos + 2, this.y + this.height, 0xFFFF0000);
                int healthHeight = ((health - 10) * this.height / 10);
                guiGraphics.fill(xpos + xoffset, this.y + this.height - healthHeight, xpos + xoffset + 2, this.y + this.height, 0xFFFFFF00);
            } else {
                int healthHeight = (health * this.height / 10);
                guiGraphics.fill(xpos + xoffset, this.y + this.height - healthHeight, xpos + xoffset + 2, this.y + this.height, 0xFFFF0000);
            }
        }
    }

    public void onPress() {
        onPress.onPress(this);
    }

    @Override
    public void setFocused(boolean b) {
        this.focused = b;
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
        narrationElementOutput.add(NarratedElementType.HINT, TurnBasedMinecraftMod.proxy.getEntity(entityID, Minecraft.getInstance().level.dimension()).getName());
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
