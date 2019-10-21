package com.seodisparate.TurnBasedMinecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public class EntitySelectionButton extends Button
{
    private int entityID;
    private boolean isSideA;
    
    public EntitySelectionButton(int x, int y, int widthIn, int heightIn, String buttonText, int entityID, boolean isSideA, Button.IPressable onPress) {
        super(x, y, widthIn, heightIn, buttonText, onPress);
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
    public void render(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
        Entity e = Minecraft.getInstance().world.getEntityByID(entityID);
        if(e != null && e instanceof LivingEntity && ((LivingEntity)e).isAlive()) {
            int health = (int)(((LivingEntity)e).getHealth() + 0.5f);
            int xpos = x;
            int xoffset;
            if(isSideA) {
                xpos += width + 4;
                xoffset = 4;
            } else {
                xpos -= 6;
                xoffset = -4;
            }
            if(health > 200) {
                fill(xpos, y + height * 4 / 5, xpos + 2, y + height        , 0xFFFF0000);
                fill(xpos, y + height * 3 / 5, xpos + 2, y + height * 4 / 5, 0xFFFFFF00);
                fill(xpos, y + height * 2 / 5, xpos + 2, y + height * 3 / 5, 0xFF00FF00);
                fill(xpos, y + height     / 5, xpos + 2, y + height * 2 / 5, 0xFF00FFFF);
                fill(xpos, y                 , xpos + 2, y + height     / 5, 0xFF0000FF);
                int healthHeight = ((health - 200) * height / 100);
                fill(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFFFFFFFF);
            } else if(health > 100) {
                fill(xpos, y + height * 4 / 5, xpos + 2, y + height        , 0xFFFF0000);
                fill(xpos, y + height * 3 / 5, xpos + 2, y + height * 4 / 5, 0xFFFFFF00);
                fill(xpos, y + height * 2 / 5, xpos + 2, y + height * 3 / 5, 0xFF00FF00);
                fill(xpos, y + height     / 5, xpos + 2, y + height * 2 / 5, 0xFF00FFFF);
                int healthHeight = ((health - 100) * height / 100);
                fill(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFF0000FF);
            } else if(health > 50) {
                fill(xpos, y + height * 4 / 5, xpos + 2, y + height        , 0xFFFF0000);
                fill(xpos, y + height * 3 / 5, xpos + 2, y + height * 4 / 5, 0xFFFFFF00);
                fill(xpos, y + height * 2 / 5, xpos + 2, y + height * 3 / 5, 0xFF00FF00);
                int healthHeight = ((health - 50) * height / 50);
                fill(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFF00FFFF);
            } else if(health > 20) {
                fill(xpos, y + height * 4 / 5, xpos + 2, y + height        , 0xFFFF0000);
                fill(xpos, y + height * 3 / 5, xpos + 2, y + height * 4 / 5, 0xFFFFFF00);
                int healthHeight = ((health - 20) * height / 30);
                fill(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFF00FF00);
            } else if(health > 10) {
                fill(xpos, y + height * 4 / 5, xpos + 2, y + height, 0xFFFF0000);
                int healthHeight = ((health - 10) * height / 10);
                fill(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFFFFFF00);
            } else {
                int healthHeight = (health * height / 10);
                fill(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFFFF0000);
            }
        }
    }
}
