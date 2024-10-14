package com.burnedkirby.TurnBasedMinecraft.common;

import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketGeneralMessage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class DimensionChangedHandler {
    @SubscribeEvent
    public void dimensionChanged(EntityTravelToDimensionEvent event) {
        if(event.getEntity().level().isClientSide) {
            return;
        }
        if(TurnBasedMinecraftMod.proxy.getBattleManager().forceLeaveBattle(new EntityIDDimPair(event.getEntity()))
                && event.getEntity() instanceof ServerPlayer) {
            PacketDistributor.sendToPlayer((ServerPlayer)event.getEntity(), new PacketGeneralMessage("Left battle due to moving to a different dimension"));
        }
    }
}
