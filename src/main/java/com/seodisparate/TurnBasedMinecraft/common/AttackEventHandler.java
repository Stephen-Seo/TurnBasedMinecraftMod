package com.seodisparate.TurnBasedMinecraft.common;

import java.util.ArrayDeque;
import java.util.Queue;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleMessage;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AttackEventHandler
{
    private boolean isAttackerValid(LivingAttackEvent event)
    {
        if(event.getSource().getTrueSource() == null)
        {
            return false;
        }
        else if(event.getSource().getTrueSource().equals(TurnBasedMinecraftMod.attackingEntity))
        {
            return true;
        }
        else
        {
            Queue<AttackerViaBow> removeQueue = new ArrayDeque<AttackerViaBow>();
            final long now = System.nanoTime();
            boolean isValid = false;
            synchronized(TurnBasedMinecraftMod.attackerViaBow)
            {
                for(AttackerViaBow attacker : TurnBasedMinecraftMod.attackerViaBow)
                {
                    if(now - attacker.attackTime >= AttackerViaBow.ATTACK_TIMEOUT)
                    {
                        removeQueue.add(attacker);
                    }
                    else if(event.getSource().getTrueSource().equals(attacker.entity) && event.getSource().isProjectile())
                    {
                        removeQueue.add(attacker);
                        if(!isValid)
                        {
                            Battle b = TurnBasedMinecraftMod.battleManager.getBattleByID(attacker.battleID);
                            if(b != null)
                            {
                                b.sendMessageToAllPlayers(PacketBattleMessage.MessageType.ARROW_HIT, attacker.entity.getEntityId(), event.getEntity().getEntityId(), 0);
                            }
                            isValid = true;
                        }
                    }
                }
                for(AttackerViaBow next = removeQueue.poll(); next != null; next = removeQueue.poll())
                {
                    TurnBasedMinecraftMod.attackerViaBow.remove(next);
                }
            }
            return isValid;
        }
    }
    
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
        
        if(!isAttackerValid(event) && event.getEntity() != null && event.getSource().getTrueSource() != null && TurnBasedMinecraftMod.battleManager.checkAttack(event))
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
