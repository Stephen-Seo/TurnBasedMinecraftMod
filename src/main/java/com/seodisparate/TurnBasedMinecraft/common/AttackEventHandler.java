package com.seodisparate.TurnBasedMinecraft.common;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AttackEventHandler
{
    @SubscribeEvent
    public void entityAttacked(LivingAttackEvent event)
    {
        if(event.getEntity().world.isRemote)
        {
            return;
        }
        else if(TurnBasedMinecraftMod.battleManager == null)
        {
            TurnBasedMinecraftMod.battleManager = new BattleManager(TurnBasedMinecraftMod.logger);
        }
        
        if(!(event.getSource().getTrueSource() == null || event.getSource().getTrueSource().equals(TurnBasedMinecraftMod.attackingEntity)) && TurnBasedMinecraftMod.battleManager.checkAttack(event))
        {
//            TurnBasedMinecraftMod.logger.debug("Canceled LivingAttackEvent between " + TurnBasedMinecraftMod.attackingEntity + " and " + event.getEntity());
            event.setCanceled(true);
        }
        else
        {
//            TurnBasedMinecraftMod.logger.debug("Did not cancel attack");
        }
        if(TurnBasedMinecraftMod.attackingDamage < (int) event.getAmount())
        {
            TurnBasedMinecraftMod.attackingDamage = (int) event.getAmount();
        }
    }
}
