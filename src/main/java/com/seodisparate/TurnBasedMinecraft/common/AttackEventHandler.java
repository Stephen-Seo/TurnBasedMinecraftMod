package com.seodisparate.TurnBasedMinecraft.common;

import java.util.ArrayDeque;
import java.util.Queue;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleMessage;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AttackEventHandler
{
    private boolean isAttackerValid(LivingAttackEvent event)
    {
        if(event.getSource().getTrueSource() == null)
        {
            return false;
        }
        else if(event.getSource().getTrueSource().equals(TurnBasedMinecraftMod.proxy.getAttackingEntity()))
        {
            return true;
        }
        else
        {
            Queue<AttackerViaBow> removeQueue = new ArrayDeque<AttackerViaBow>();
            final long now = System.nanoTime();
            boolean isValid = false;
            synchronized(TurnBasedMinecraftMod.proxy.getAttackerViaBowSet())
            {
                for(AttackerViaBow attacker : TurnBasedMinecraftMod.proxy.getAttackerViaBowSet())
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
                            Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(attacker.battleID);
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
                    TurnBasedMinecraftMod.proxy.getAttackerViaBowSet().remove(next);
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
        Config config = TurnBasedMinecraftMod.proxy.getConfig();
        BattleManager battleManager = TurnBasedMinecraftMod.proxy.getBattleManager();
        if((event.getEntity() != null && battleManager.isRecentlyLeftBattle(event.getEntity().getEntityId()))
                || (event.getSource().getTrueSource() != null && battleManager.isRecentlyLeftBattle(event.getSource().getTrueSource().getEntityId())))
        {
            event.setCanceled(true);
            return;
        }
        else if(!isAttackerValid(event)
                && event.getEntity() != null
                && event.getSource().getTrueSource() != null
                && !config.getBattleIgnoringPlayers().contains(event.getSource().getTrueSource().getEntityId())
                && !config.getBattleIgnoringPlayers().contains(event.getEntity().getEntityId())
                && battleManager.checkAttack(event))
        {
//            TurnBasedMinecraftMod.logger.debug("Canceled LivingAttackEvent between " + TurnBasedMinecraftMod.commonProxy.getAttackingEntity() + " and " + event.getEntity());
            event.setCanceled(true);
        }
        else
        {
//            TurnBasedMinecraftMod.logger.debug("Did not cancel attack");
        }
        if(TurnBasedMinecraftMod.proxy.getAttackingDamage() < (int) event.getAmount())
        {
            TurnBasedMinecraftMod.proxy.setAttackingDamage((int) event.getAmount());
        }
    }
    
    @SubscribeEvent
    public void entityTargeted(LivingSetAttackTargetEvent event)
    {
        Config config = TurnBasedMinecraftMod.proxy.getConfig();
        BattleManager battleManager = TurnBasedMinecraftMod.proxy.getBattleManager();
        if(event.getEntity().world.isRemote
                || config.isOldBattleBehaviorEnabled()
                || (event.getEntity() != null && battleManager.isRecentlyLeftBattle(event.getEntity().getEntityId()))
                || (event.getTarget() != null && battleManager.isRecentlyLeftBattle(event.getTarget().getEntityId())))
        {
            return;
        }
        else if(event.getEntity() != null
                && event.getTarget() != null
                && !config.getBattleIgnoringPlayers().contains(event.getEntity().getEntityId())
                && !config.getBattleIgnoringPlayers().contains(event.getTarget().getEntityId()))
        {
            TurnBasedMinecraftMod.proxy.getBattleManager().checkTargeted(event);
        }
    }
}
