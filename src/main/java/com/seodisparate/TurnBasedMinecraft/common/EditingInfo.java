package com.seodisparate.TurnBasedMinecraft.common;

import net.minecraft.entity.player.PlayerEntity;

public class EditingInfo
{
    public PlayerEntity editor;
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

    public EditingInfo(PlayerEntity player)
    {
        editor = player;
        entityInfo = null;
        isPendingEntitySelection = true;
        isEditingCustomName = false;
    }
}
