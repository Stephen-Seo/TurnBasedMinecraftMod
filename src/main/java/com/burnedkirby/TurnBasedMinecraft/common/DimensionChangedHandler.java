package com.burnedkirby.TurnBasedMinecraft.common;

import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketGeneralMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.network.PacketDistributor;

public class DimensionChangedHandler {
    public static void dimensionChanged(EntityTravelToDimensionEvent event) {
        if(event.getEntity().level().isClientSide) {
            return;
        }
        if(TurnBasedMinecraftMod.proxy.getBattleManager().forceLeaveBattle(new EntityIDDimPair(event.getEntity()))
                && event.getEntity() instanceof ServerPlayer) {
            TurnBasedMinecraftMod.getHandler().send(
                new PacketGeneralMessage("Left battle due to moving to a different dimension"),
                PacketDistributor.PLAYER.with((ServerPlayer)event.getEntity()));
        }
    }
}
