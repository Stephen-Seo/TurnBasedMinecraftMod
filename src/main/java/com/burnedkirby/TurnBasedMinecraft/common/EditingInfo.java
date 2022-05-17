package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraft.world.entity.player.Player;

public class EditingInfo
{
    public Player editor;
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

    public EditingInfo(Player player)
    {
        editor = player;
        entityInfo = null;
        isPendingEntitySelection = true;
        isEditingCustomName = false;
    }
}
