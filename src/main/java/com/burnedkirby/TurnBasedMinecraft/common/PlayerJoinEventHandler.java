package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerJoinEventHandler
{
    @SubscribeEvent
    public void entityJoinHandler(EntityJoinLevelEvent event)
    {
        if(event.getLevel().isClientSide)
        {
            return;
        }
        if(event.getEntity() instanceof Player && TurnBasedMinecraftMod.proxy.getConfig().getBattleDisabledForAll())
        {
            TurnBasedMinecraftMod.proxy.getConfig().addBattleIgnoringPlayer(event.getEntity().getId());
        }
    }
}
