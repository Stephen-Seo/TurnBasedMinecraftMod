package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class HurtEventHandler {
    @SubscribeEvent
    public void handleHurtEvent(LivingHurtEvent event) {
        CommonProxy proxy = TurnBasedMinecraftMod.proxy;
        if (event.getEntity().level.isClientSide || proxy.getBattleManager() == null) {
            return;
        } else if (proxy.getConfig().getIgnoreHurtDamageSources().contains(event.getSource().msgId) && proxy.getBattleManager().isInBattle(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
