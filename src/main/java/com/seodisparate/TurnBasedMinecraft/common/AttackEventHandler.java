package com.seodisparate.TurnBasedMinecraft.common;

import java.util.Iterator;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleMessage;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketEditingMessage;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketGeneralMessage;
import net.minecraft.entity.player.EntityPlayerMP;
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
                    else if(event.getSource().getTrueSource().equals(attacker.entity) && event.getSource().isProjectile())
                    {
                        iter.remove();
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
        CommonProxy proxy = TurnBasedMinecraftMod.proxy;
        Config config = proxy.getConfig();
        BattleManager battleManager = proxy.getBattleManager();
        // handle edit entity, pick entity via attack
        {
            EditingInfo editingInfo = null;
            if(event.getSource().getTrueSource() != null && event.getEntity() != null)
            {
                editingInfo = proxy.getEditingInfo(event.getSource().getTrueSource().getEntityId());
                if(editingInfo != null && editingInfo.isPendingEntitySelection)
                {
                    editingInfo.isPendingEntitySelection = false;
                    event.setCanceled(true);
                    if(editingInfo.isEditingCustomName)
                    {
                        if(event.getEntity().getCustomNameTag().isEmpty())
                        {
                            TurnBasedMinecraftMod.logger.error("Cannot edit custom name from entity without custom name");
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Cannot edit custom name from entity without custom name"), (EntityPlayerMP) editingInfo.editor);
                            return;
                        }
                        editingInfo.entityInfo = config.getCustomEntityInfo(event.getEntity().getCustomNameTag());
                        if(editingInfo.entityInfo == null)
                        {
                            editingInfo.entityInfo = new EntityInfo();
                            editingInfo.entityInfo.customName = event.getEntity().getCustomNameTag();
                        }
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Editing custom name \"" + event.getEntity().getCustomNameTag() + "\""), (EntityPlayerMP) editingInfo.editor);
                        TurnBasedMinecraftMod.logger.info("Begin editing custom \"" + event.getEntity().getCustomNameTag() + "\"");
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP) editingInfo.editor);
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
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Editing entity \"" + editingInfo.entityInfo.classType.getName() + "\""), (EntityPlayerMP) editingInfo.editor);
                        TurnBasedMinecraftMod.logger.info("Begin editing \"" + editingInfo.entityInfo.classType.getName() + "\"");
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP) editingInfo.editor);
                    }
                    return;
                }
            }
        }
        if(event.getEntity() != null && event.getSource().getTrueSource() != null && (battleManager.isRecentlyLeftBattle(event.getEntity().getEntityId()) || battleManager.isRecentlyLeftBattle(event.getSource().getTrueSource().getEntityId())))
        {
            event.setCanceled(true);
            return;
        }
        else if(!isAttackerValid(event)
                && event.getEntity() != null
                && event.getSource().getTrueSource() != null
                && event.getEntity() != event.getSource().getTrueSource()
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
                || (event.getTarget() != null && battleManager.isRecentlyLeftBattle(event.getTarget().getEntityId()))
                || (event.getEntity() != null && event.getTarget() != null && Utility.distanceBetweenEntities(event.getEntity(), event.getTarget()) > (double)config.getAggroStartBattleDistance()))
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
