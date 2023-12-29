package com.burnedkirby.TurnBasedMinecraft.common;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;

public class HurtEventHandler {
    @SubscribeEvent
    public void handleHurtEvent(LivingHurtEvent event) {
        CommonProxy proxy = TurnBasedMinecraftMod.proxy;
        if (event.getEntity().level().isClientSide || proxy.getBattleManager() == null) {
            return;
        } else if (proxy.getConfig().getIgnoreHurtDamageSources().contains(event.getSource().getMsgId()) && proxy.getBattleManager().isInBattle(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
