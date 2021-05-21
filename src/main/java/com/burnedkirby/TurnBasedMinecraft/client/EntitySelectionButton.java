package com.burnedkirby.TurnBasedMinecraft.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.StringTextComponent;

public class EntitySelectionButton extends Button {
    private int entityID;
    private boolean isSideA;

    public EntitySelectionButton(int x, int y, int widthIn, int heightIn, String buttonText, int entityID, boolean isSideA, Button.IPressable onPress) {
        super(x, y, widthIn, heightIn, new StringTextComponent(buttonText), onPress);
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
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.renderButton(matrixStack, mouseX, mouseY, partialTicks);
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
                fill(matrixStack, xpos, getY() + getHeight() * 4 / 5, xpos + 2, getY() + getHeight(), 0xFFFF0000);
                fill(matrixStack, xpos, getY() + getHeight() * 3 / 5, xpos + 2, getY() + getHeight() * 4 / 5, 0xFFFFFF00);
                fill(matrixStack, xpos, getY() + getHeight() * 2 / 5, xpos + 2, getY() + getHeight() * 3 / 5, 0xFF00FF00);
                fill(matrixStack, xpos, getY() + getHeight() / 5, xpos + 2, getY() + getHeight() * 2 / 5, 0xFF00FFFF);
                fill(matrixStack, xpos, getY(), xpos + 2, getY() + getHeight() / 5, 0xFF0000FF);
                int healthHeight = ((health - 200) * getHeight() / 100);
                fill(matrixStack, xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFFFFFFFF);
            } else if (health > 100) {
                fill(matrixStack, xpos, getY() + getHeight() * 4 / 5, xpos + 2, getY() + getHeight(), 0xFFFF0000);
                fill(matrixStack, xpos, getY() + getHeight() * 3 / 5, xpos + 2, getY() + getHeight() * 4 / 5, 0xFFFFFF00);
                fill(matrixStack, xpos, getY() + getHeight() * 2 / 5, xpos + 2, getY() + getHeight() * 3 / 5, 0xFF00FF00);
                fill(matrixStack, xpos, getY() + getHeight() / 5, xpos + 2, getY() + getHeight() * 2 / 5, 0xFF00FFFF);
                int healthHeight = ((health - 100) * getHeight() / 100);
                fill(matrixStack, xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFF0000FF);
            } else if (health > 50) {
                fill(matrixStack, xpos, getY() + getHeight() * 4 / 5, xpos + 2, getY() + getHeight(), 0xFFFF0000);
                fill(matrixStack, xpos, getY() + getHeight() * 3 / 5, xpos + 2, getY() + getHeight() * 4 / 5, 0xFFFFFF00);
                fill(matrixStack, xpos, getY() + getHeight() * 2 / 5, xpos + 2, getY() + getHeight() * 3 / 5, 0xFF00FF00);
                int healthHeight = ((health - 50) * getHeight() / 50);
                fill(matrixStack, xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFF00FFFF);
            } else if (health > 20) {
                fill(matrixStack, xpos, getY() + getHeight() * 4 / 5, xpos + 2, getY() + getHeight(), 0xFFFF0000);
                fill(matrixStack, xpos, getY() + getHeight() * 3 / 5, xpos + 2, getY() + getHeight() * 4 / 5, 0xFFFFFF00);
                int healthHeight = ((health - 20) * getHeight() / 30);
                fill(matrixStack, xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFF00FF00);
            } else if (health > 10) {
                fill(matrixStack, xpos, getY() + getHeight() * 4 / 5, xpos + 2, getY() + getHeight(), 0xFFFF0000);
                int healthHeight = ((health - 10) * getHeight() / 10);
                fill(matrixStack, xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFFFFFF00);
            } else {
                int healthHeight = (health * getHeight() / 10);
                fill(matrixStack, xpos + xoffset, getY() + getHeight() - healthHeight, xpos + xoffset + 2, getY() + getHeight(), 0xFFFF0000);
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
