package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerJoinEventHandler
{
    @SubscribeEvent
    public void entityJoinHandler(EntityJoinWorldEvent event)
    {
        if(event.getWorld().isClientSide)
        {
            return;
        }
        if(event.getEntity() instanceof PlayerEntity && TurnBasedMinecraftMod.proxy.getConfig().getBattleDisabledForAll())
        {
            TurnBasedMinecraftMod.proxy.getConfig().addBattleIgnoringPlayer(event.getEntity().getId());
        }
    }
}
