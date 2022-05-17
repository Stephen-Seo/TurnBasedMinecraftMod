package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraft.world.entity.player.Player;
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
        if(event.getEntity() instanceof Player && TurnBasedMinecraftMod.proxy.getConfig().getBattleDisabledForAll())
        {
            TurnBasedMinecraftMod.proxy.getConfig().addBattleIgnoringPlayer(event.getEntity().getId());
        }
    }
}
