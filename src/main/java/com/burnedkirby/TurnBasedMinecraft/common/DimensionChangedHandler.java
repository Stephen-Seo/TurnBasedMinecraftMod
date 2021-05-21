package com.burnedkirby.TurnBasedMinecraft.common;

import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketGeneralMessage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;

public class DimensionChangedHandler {
    @SubscribeEvent
    public void dimensionChanged(EntityTravelToDimensionEvent event) {
        if(event.getEntity().level.isClientSide) {
            return;
        }
        if(TurnBasedMinecraftMod.proxy.getBattleManager().forceLeaveBattle(new EntityIDDimPair(event.getEntity()))
                && event.getEntity() instanceof ServerPlayerEntity) {
            TurnBasedMinecraftMod.getHandler().send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)event.getEntity()),
                    new PacketGeneralMessage("Left battle due to moving to a different dimension"));
        }
    }
}
