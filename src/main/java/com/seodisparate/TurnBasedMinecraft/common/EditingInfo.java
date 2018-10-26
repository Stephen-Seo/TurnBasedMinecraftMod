package com.seodisparate.TurnBasedMinecraft.common;

import net.minecraft.entity.player.EntityPlayer;

public class EditingInfo
{
    public EntityPlayer editor;
    public EntityInfo entityInfo;
    public boolean isPendingEntitySelection;
    public boolean isEditingCustomName;

    public EditingInfo()
    {
        editor = null;
        entityInfo = null;
        isPendingEntitySelection = true;
        isEditingCustomName = false;
    }

    public EditingInfo(EntityPlayer player)
    {
        editor = player;
        entityInfo = null;
        isPendingEntitySelection = true;
        isEditingCustomName = false;
    }
}
