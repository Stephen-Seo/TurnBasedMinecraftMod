package com.seodisparate.TurnBasedMinecraft.client;

import net.minecraft.client.gui.GuiButton;

public class EntitySelectionButton extends GuiButton
{
    public int entityID;
    public EntitySelectionButton(int buttonId, int x, int y, String buttonText, int entityID)
    {
        super(buttonId, x, y, buttonText);
        this.entityID = entityID;
    }
    
    public EntitySelectionButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, int entityID)
    {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.entityID = entityID;
    }
}
