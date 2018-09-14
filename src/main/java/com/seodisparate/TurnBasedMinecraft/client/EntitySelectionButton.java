package com.seodisparate.TurnBasedMinecraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class EntitySelectionButton extends GuiButton
{
    public int entityID;
    private boolean isSideA;
    public EntitySelectionButton(int buttonId, int x, int y, String buttonText, int entityID, boolean isSideA)
    {
        super(buttonId, x, y, buttonText);
        this.entityID = entityID;
        this.isSideA = isSideA;
    }
    
    public EntitySelectionButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, int entityID, boolean isSideA)
    {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.entityID = entityID;
        this.isSideA = isSideA;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        super.drawButton(mc, mouseX, mouseY, partialTicks);
        Entity e = Minecraft.getMinecraft().world.getEntityByID(entityID);
        if(e != null && e instanceof EntityLivingBase && ((EntityLivingBase)e).isEntityAlive())
        {
            int health = (int)(((EntityLivingBase)e).getHealth() + 0.5f);
            int xpos = x;
            int xoffset;
            if(isSideA)
            {
                xpos += width + 4;
                xoffset = 4;
            }
            else
            {
                xpos -= 6;
                xoffset = -4;
            }
            if(health > 200)
            {
                drawRect(xpos, y + height * 4 / 5, xpos + 2, y + height        , 0xFFFF0000);
                drawRect(xpos, y + height * 3 / 5, xpos + 2, y + height * 4 / 5, 0xFFFFFF00);
                drawRect(xpos, y + height * 2 / 5, xpos + 2, y + height * 3 / 5, 0xFF00FF00);
                drawRect(xpos, y + height     / 5, xpos + 2, y + height * 2 / 5, 0xFF00FFFF);
                drawRect(xpos, y                 , xpos + 2, y + height     / 5, 0xFF0000FF);
                int healthHeight = ((health - 200) * height / 100);
                drawRect(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFFFFFFFF);
            }
            else if(health > 100)
            {
                drawRect(xpos, y + height * 4 / 5, xpos + 2, y + height        , 0xFFFF0000);
                drawRect(xpos, y + height * 3 / 5, xpos + 2, y + height * 4 / 5, 0xFFFFFF00);
                drawRect(xpos, y + height * 2 / 5, xpos + 2, y + height * 3 / 5, 0xFF00FF00);
                drawRect(xpos, y + height     / 5, xpos + 2, y + height * 2 / 5, 0xFF00FFFF);
                int healthHeight = ((health - 100) * height / 100);
                drawRect(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFF0000FF);
            }
            else if(health > 50)
            {
                drawRect(xpos, y + height * 4 / 5, xpos + 2, y + height        , 0xFFFF0000);
                drawRect(xpos, y + height * 3 / 5, xpos + 2, y + height * 4 / 5, 0xFFFFFF00);
                drawRect(xpos, y + height * 2 / 5, xpos + 2, y + height * 3 / 5, 0xFF00FF00);
                int healthHeight = ((health - 50) * height / 50);
                drawRect(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFF00FFFF);
            }
            else if(health > 20)
            {
                drawRect(xpos, y + height * 4 / 5, xpos + 2, y + height        , 0xFFFF0000);
                drawRect(xpos, y + height * 3 / 5, xpos + 2, y + height * 4 / 5, 0xFFFFFF00);
                int healthHeight = ((health - 20) * height / 30);
                drawRect(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFF00FF00);
            }
            else if(health > 10)
            {
                drawRect(xpos, y + height * 4 / 5, xpos + 2, y + height, 0xFFFF0000);
                int healthHeight = ((health - 10) * height / 10);
                drawRect(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFFFFFF00);
            }
            else
            {
                int healthHeight = (health * height / 10);
                drawRect(xpos + xoffset, y + height - healthHeight, xpos + xoffset + 2, y + height, 0xFFFF0000);
            }
        }
    }
}
