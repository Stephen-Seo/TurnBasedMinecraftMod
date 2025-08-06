package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class HurtEventHandler {
    public static boolean handleHurtEvent(LivingHurtEvent event) {
        boolean ret = false;
        CommonProxy proxy = TurnBasedMinecraftMod.proxy;
        if (event.getEntity().level().isClientSide || proxy.getBattleManager() == null) {
            return ret;
        } else if (proxy.getConfig().getIgnoreHurtDamageSources().contains(event.getSource().getMsgId()) && proxy.getBattleManager().isInBattle(event.getEntity())) {
            ret = true;
        }

        return ret;
    }
}
