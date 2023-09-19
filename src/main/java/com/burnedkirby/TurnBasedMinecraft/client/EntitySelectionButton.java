package com.burnedkirby.TurnBasedMinecraft.client;

import com.burnedkirby.TurnBasedMinecraft.common.CommonProxy;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EntitySelectionButton extends AbstractButton {
    TBMButtonPress onPress;
    private int entityID;
    private boolean isSideA;

    public EntitySelectionButton(int x, int y, int widthIn, int heightIn, String buttonText, int entityID, boolean isSideA, TBMButtonPress onPress) {
        super(x, y, widthIn, heightIn, Component.literal(buttonText));
        this.onPress = onPress;
        this.entityID = entityID;
        this.isSideA = isSideA;
    }

    public EntitySelectionButton(int x, int y, int widthIn, int heightIn, Component buttonTextComponent, int entityID, boolean isSideA, TBMButtonPress onPress) {
        super(x, y, widthIn, heightIn, buttonTextComponent);
        this.onPress = onPress;
        this.entityID = entityID;
        this.isSideA = isSideA;
    }

    public int getID() {
        return entityID;
    }

    public boolean getIsSideA() {
        return isSideA;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        Entity e = Minecraft.getInstance().level.getEntity(entityID);
        if (e != null && e instanceof LivingEntity && ((LivingEntity) e).isAlive()) {
            int health = (int) (((LivingEntity) e).getHealth() + 0.5f);
            int xpos = getX();
            int xoffset;
            if (isSideA) {
                xpos += getWidth() + 4;
                xoffset = 4;
            } else {
                xpos -= 6;
                xoffset = -4;
            }
            if (health > 200) {
                guiGraphics.fill(xpos, getY() + getHeight() * 4 / 5, xpos + 2, getY() + getHeight(), 0xFFFF0000);
                guiGraphics.fill(xpos, getY() + getHeight() * 3 / 5, xpos + 2, getY() + getHeight() * 4 / 5, 0xFFFFFF00);
                guiGraphics.fill(xpos, getY() + getHeight() * 2 / 5, xpos + 2, getY() + getHeight() * 3 / 5, 0xFF00FF00);
                guiGraphics.fill(xpos, getY() + getHeight() / 5, xpos + 2, getY() + getHeight() * 2 / 5, 0xFF00FFFF);
                guiGraphics.fill(xpos, getY(), xpos + 2, getY() + getHeight() / 5, 0xFF0000FF);
                int healthHeight = ((health - 200) * getHeight() / 100);
                guiGraphics.fill(xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFFFFFFFF);
            } else if (health > 100) {
                guiGraphics.fill(xpos, getY() + getHeight() * 4 / 5, xpos + 2, getY() + getHeight(), 0xFFFF0000);
                guiGraphics.fill(xpos, getY() + getHeight() * 3 / 5, xpos + 2, getY() + getHeight() * 4 / 5, 0xFFFFFF00);
                guiGraphics.fill(xpos, getY() + getHeight() * 2 / 5, xpos + 2, getY() + getHeight() * 3 / 5, 0xFF00FF00);
                guiGraphics.fill(xpos, getY() + getHeight() / 5, xpos + 2, getY() + getHeight() * 2 / 5, 0xFF00FFFF);
                int healthHeight = ((health - 100) * getHeight() / 100);
                guiGraphics.fill(xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFF0000FF);
            } else if (health > 50) {
                guiGraphics.fill(xpos, getY() + getHeight() * 4 / 5, xpos + 2, getY() + getHeight(), 0xFFFF0000);
                guiGraphics.fill(xpos, getY() + getHeight() * 3 / 5, xpos + 2, getY() + getHeight() * 4 / 5, 0xFFFFFF00);
                guiGraphics.fill(xpos, getY() + getHeight() * 2 / 5, xpos + 2, getY() + getHeight() * 3 / 5, 0xFF00FF00);
                int healthHeight = ((health - 50) * getHeight() / 50);
                guiGraphics.fill(xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFF00FFFF);
            } else if (health > 20) {
                guiGraphics.fill(xpos, getY() + getHeight() * 4 / 5, xpos + 2, getY() + getHeight(), 0xFFFF0000);
                guiGraphics.fill(xpos, getY() + getHeight() * 3 / 5, xpos + 2, getY() + getHeight() * 4 / 5, 0xFFFFFF00);
                int healthHeight = ((health - 20) * getHeight() / 30);
                guiGraphics.fill(xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFF00FF00);
            } else if (health > 10) {
                guiGraphics.fill(xpos, getY() + getHeight() * 4 / 5, xpos + 2, getY() + getHeight(), 0xFFFF0000);
                int healthHeight = ((health - 10) * getHeight() / 10);
                guiGraphics.fill(xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFFFFFF00);
            } else {
                int healthHeight = (health * getHeight() / 10);
                guiGraphics.fill(xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFFFF0000);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_259858_) {
        p_259858_.add(NarratedElementType.HINT, TurnBasedMinecraftMod.proxy.getEntity(entityID, Minecraft.getInstance().level.dimension()).getName());
    }

    @Override
    public void onPress() {
        onPress.onPress(this);
    }
}
