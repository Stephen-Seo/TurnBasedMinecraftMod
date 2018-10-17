package com.seodisparate.TurnBasedMinecraft.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PlayerJoinEventHandler
{
    @SubscribeEvent
    public void entityJoinHandler(EntityJoinWorldEvent event)
    {
        if(event.getEntity().world.isRemote)
        {
            return;
        }
        if(event.getEntity() instanceof EntityPlayer && TurnBasedMinecraftMod.proxy.getConfig().getBattleDisabledForAll())
        {
            TurnBasedMinecraftMod.proxy.getConfig().addBattleIgnoringPlayer(event.getEntity().getEntityId());
        }
    }
}
