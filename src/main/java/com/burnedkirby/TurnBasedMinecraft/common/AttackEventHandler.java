package com.burnedkirby.TurnBasedMinecraft.common;

import java.util.Iterator;

import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketBattleMessage;

import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketEditingMessage;
import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketGeneralMessage;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public class AttackEventHandler
{
    private boolean isAttackerValid(LivingAttackEvent event)
    {
        if(event.getSource().getEntity() == null)
        {
            return false;
        }
        else if(event.getSource().getEntity().equals(TurnBasedMinecraftMod.proxy.getAttackingEntity()))
        {
            return true;
        }
        else
        {
            final long now = System.nanoTime();
            boolean isValid = false;
            synchronized(TurnBasedMinecraftMod.proxy.getAttackerViaBowSet())
            {
                for(Iterator<AttackerViaBow> iter = TurnBasedMinecraftMod.proxy.getAttackerViaBowSet().iterator(); iter.hasNext();)
                {
                    AttackerViaBow attacker = iter.next();
                    if(now - attacker.attackTime >= AttackerViaBow.ATTACK_TIMEOUT)
                    {
                        iter.remove();
                    }
                    else if(event.getSource().getEntity().equals(attacker.entity) && event.getSource().isProjectile())
                    {
                        iter.remove();
                        if(!isValid)
                        {
                            Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(attacker.battleID);
                            if(b != null)
                            {
                                b.sendMessageToAllPlayers(PacketBattleMessage.MessageType.ARROW_HIT, attacker.entity.getId(), event.getEntity().getId(), 0);
                            }
                            isValid = true;
                        }
                    }
                }
            }
            return isValid;
        }
    }
    
    @SubscribeEvent
    public void entityAttacked(LivingAttackEvent event)
    {
        if(event.getEntity().level.isClientSide)
        {
            return;
        }
        CommonProxy proxy = TurnBasedMinecraftMod.proxy;
        Config config = proxy.getConfig();
        BattleManager battleManager = proxy.getBattleManager();
        // handle edit entity, pick entity via attack
        {
            if(event.getSource().getEntity() != null && event.getEntity() != null)
            {
                final EditingInfo editingInfo = proxy.getEditingInfo(event.getSource().getEntity().getId());
                if(editingInfo != null && editingInfo.isPendingEntitySelection)
                {
                    editingInfo.isPendingEntitySelection = false;
                    event.setCanceled(true);
                    if(editingInfo.isEditingCustomName)
                    {
                    	if(!event.getEntity().hasCustomName())
                        {
                            TurnBasedMinecraftMod.logger.error("Cannot edit custom name from entity without custom name");
                            TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)editingInfo.editor), new PacketGeneralMessage("Cannot edit custom name from entity without custom name"));
                            return;
                        }
                        editingInfo.entityInfo = config.getCustomEntityInfo(event.getEntity().getCustomName().getString());
                        if(editingInfo.entityInfo == null)
                        {
                            editingInfo.entityInfo = new EntityInfo();
                            editingInfo.entityInfo.customName = event.getEntity().getCustomName().getString();
                        }
                        TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)editingInfo.editor), new PacketGeneralMessage("Editing custom name \"" + event.getEntity().getCustomName().getString() + "\""));
                        TurnBasedMinecraftMod.logger.info("Begin editing custom \"" + event.getEntity().getCustomName().getString() + "\"");
                        TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)editingInfo.editor), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                    }
                    else
                    {
                        editingInfo.entityInfo = config.getMatchingEntityInfo(event.getEntity());
                        if(editingInfo.entityInfo == null)
                        {
                            editingInfo.entityInfo = new EntityInfo();
                            editingInfo.entityInfo.classType = event.getEntity().getClass();
                        }
                        else
                        {
                            editingInfo.entityInfo = editingInfo.entityInfo.clone();
                        }
                        TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)editingInfo.editor), new PacketGeneralMessage("Editing entity \"" + editingInfo.entityInfo.classType.getName() + "\""));
                        TurnBasedMinecraftMod.logger.info("Begin editing \"" + editingInfo.entityInfo.classType.getName() + "\"");
                        TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)editingInfo.editor), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                    }
                    return;
                }
            }
        }
        if(event.getEntity() != null && event.getSource().getEntity() != null && (battleManager.isRecentlyLeftBattle(event.getEntity().getId()) || battleManager.isRecentlyLeftBattle(event.getSource().getEntity().getId())))
        {
            if(event.getSource().getEntity() instanceof Creeper && TurnBasedMinecraftMod.proxy.getConfig().getCreeperAlwaysAllowDamage()) {
                event.setCanceled(false);
            } else {
//            TurnBasedMinecraftMod.logger.debug("Canceled attack");
                event.setCanceled(true);
            }
            return;
        }
        else if(!isAttackerValid(event)
                && event.getEntity() != null
                && event.getSource().getEntity() != null
                && event.getEntity() != event.getSource().getEntity()
                && !config.getBattleIgnoringPlayers().contains(event.getSource().getEntity().getId())
                && !config.getBattleIgnoringPlayers().contains(event.getEntity().getId())
                && event.getEntity().level.dimension().equals(event.getSource().getEntity().level.dimension())
                && battleManager.checkAttack(event))
        {
//            TurnBasedMinecraftMod.logger.debug("Canceled LivingAttackEvent between " + TurnBasedMinecraftMod.proxy.getAttackingEntity() + " and " + event.getEntity());
            event.setCanceled(true);
        } else {
//            TurnBasedMinecraftMod.logger.debug("Did not cancel attack");
        }

        if(TurnBasedMinecraftMod.proxy.getAttackingDamage() < (int) event.getAmount())
        {
            TurnBasedMinecraftMod.proxy.setAttackingDamage((int) event.getAmount());
        }
    }
    
    @SubscribeEvent
    public void entityTargeted(LivingChangeTargetEvent event)
    {
        Config config = TurnBasedMinecraftMod.proxy.getConfig();
        BattleManager battleManager = TurnBasedMinecraftMod.proxy.getBattleManager();
        if(event.getEntity().level.isClientSide
                || config.isOldBattleBehaviorEnabled()
                || (event.getEntity() != null && battleManager.isRecentlyLeftBattle(event.getEntity().getId()))
                || (event.getNewTarget() != null && battleManager.isRecentlyLeftBattle(event.getNewTarget().getId()))
                || (event.getEntity() != null && event.getNewTarget() != null && Utility.distanceBetweenEntities(event.getEntity(), event.getNewTarget()) > (double)config.getAggroStartBattleDistance()))
        {
            return;
        }
        else if(event.getEntity() != null
                && event.getNewTarget() != null
                && !config.getBattleIgnoringPlayers().contains(event.getEntity().getId())
                && !config.getBattleIgnoringPlayers().contains(event.getNewTarget().getId())
                && event.getEntity().level.dimension().equals(event.getNewTarget().level.dimension()))
        {
            TurnBasedMinecraftMod.proxy.getBattleManager().checkTargeted(event);
        }
    }
}
