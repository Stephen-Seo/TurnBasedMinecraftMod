package com.burnedkirby.TurnBasedMinecraft.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.Logger;

import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketGeneralMessage;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;

public class BattleManager
{
    private int IDCounter = 0;
    protected Map<Integer, Battle> battleMap;
    private Logger logger;
    private Map<Integer, Combatant> recentlyLeftBattle;
    private BattleUpdater battleUpdater;
    
    public BattleManager(Logger logger)
    {
        this.logger = logger;
        battleMap = new HashMap<Integer, Battle>();
        recentlyLeftBattle = new HashMap<Integer, Combatant>();
        battleUpdater = new BattleUpdater(this);
        MinecraftForge.EVENT_BUS.register(battleUpdater);
    }
    
    /**
     * Either creates a new Battle, adds a combatant to an existing Battle, or does
     * nothing, depending on if a player is involved and/or an entity is currently
     * in battle.
     * 
     * @param event
     * @return True if event should be canceled
     */
    public boolean checkAttack(final LivingAttackEvent event)
    {
        Config config = TurnBasedMinecraftMod.proxy.getConfig();
        String receiverClassName = event.getEntity().getClass().getName();
        String receiverCustomName;

        try {
            receiverCustomName = event.getEntity().getCustomName().getUnformattedComponentText();
        } catch (NullPointerException e) {
            receiverCustomName = null;
        }
        String attackerClassName;
        try {
            attackerClassName = event.getSource().getTrueSource().getClass().getName();
        } catch (NullPointerException e) {
            attackerClassName = null;
        }
        String attackerCustomName;
        try {
            attackerCustomName = event.getSource().getTrueSource().getCustomName().getUnformattedComponentText();
        } catch (NullPointerException e) {
            attackerCustomName = null;
        }

        // verify that both entities are EntityPlayer and not in creative or has a corresponding EntityInfo
        if(!((event.getEntity() instanceof PlayerEntity && !((PlayerEntity)event.getEntity()).isCreative())
                || (config.getEntityInfoReference(receiverClassName) != null || config.getCustomEntityInfoReference(receiverCustomName) != null))
            || !((event.getSource().getTrueSource() instanceof PlayerEntity && !((PlayerEntity)event.getSource().getTrueSource()).isCreative())
                || (config.getEntityInfoReference(attackerClassName) != null || config.getCustomEntityInfoReference(attackerCustomName) != null)))
        {
            logger.debug("BattleManager: Failed first check, attacker is \"" + attackerClassName + "\", defender is \"" + receiverClassName + "\"");
            return false;
        }
        
        // check if ignore battle in config
        EntityInfo entityInfo = config.getCustomEntityInfoReference(receiverCustomName);
        if(entityInfo == null)
        {
            entityInfo = config.getMatchingEntityInfo(event.getEntity());
        }

        if(entityInfo != null && (config.isIgnoreBattleType(entityInfo.category) || entityInfo.ignoreBattle))
        {
            // attacked entity ignores battle
            synchronized(battleMap)
            {
                for(Battle b : battleMap.values())
                {
                    if(b.hasCombatant(event.getSource().getTrueSource().getEntityId()))
                    {
                        logger.debug("Attack Canceled: attacked ignores battle but attacker in battle");
                        return true;
                    }
                }
            }
            logger.debug("Attack Not Canceled: attacked ignores battle");
            return false;
        }

        entityInfo = config.getCustomEntityInfoReference(attackerCustomName);
        if(entityInfo == null)
        {
            entityInfo = config.getMatchingEntityInfo(event.getSource().getTrueSource());
        }

        if(entityInfo != null && (config.isIgnoreBattleType(entityInfo.category) || entityInfo.ignoreBattle))
        {
            // attacker entity ignores battle
            synchronized(battleMap)
            {
                for(Battle b : battleMap.values())
                {
                    if(b.hasCombatant(event.getEntity().getEntityId()))
                    {
                        logger.debug("Attack Canceled: attacker ignores battle but attacked in battle");
                        return true;
                    }
                }
            }
            logger.debug("Attack Not Canceled: attacker ignores battle");
            return false;
        }
        
        // check if one is in battle
        Entity inBattle = null;
        Entity notInBattle = null;
        Battle battle = null;
        
        synchronized(battleMap)
        {
            for(Battle b : battleMap.values())
            {
                if(b.hasCombatant(event.getSource().getTrueSource().getEntityId()))
                {
                    if(inBattle != null)
                    {
                        // both combatants are in battle
                        logger.debug("Attack Canceled: both are in battle");
                        return true;
                    }
                    else
                    {
                        inBattle = event.getSource().getTrueSource();
                        notInBattle = event.getEntity();
                        battle = b;
                    }
                }
                if(b.hasCombatant(event.getEntity().getEntityId()))
                {
                    if(inBattle != null)
                    {
                        // both combatants are in battle
                        logger.debug("Attack Canceled: both are in battle");
                        return true;
                    }
                    else
                    {
                        inBattle = event.getEntity();
                        notInBattle = event.getSource().getTrueSource();
                        battle = b;
                    }
                }
            }
        }
        
        if(inBattle == null)
        {
            // neither entity is in battle
            if(event.getEntity() instanceof PlayerEntity || event.getSource().getTrueSource() instanceof PlayerEntity)
            {
                // at least one of the entities is a player, create Battle
                Collection<Entity> sideA = new ArrayList<Entity>(1);
                Collection<Entity> sideB = new ArrayList<Entity>(1);
                sideA.add(event.getEntity());
                sideB.add(event.getSource().getTrueSource());
                createBattle(sideA, sideB);
                logger.debug("Attack Not Canceled: new battle created");
            }
            else
            {
                logger.debug("Attack Not Canceled: neither are in battle or players");
            }
            return false;
        }

        // at this point only one entity is in battle, so add entity to other side
        if(battle.getSize() >= config.getMaxInBattle())
        {
            // battle limit reached, cannot add to battle
            return true;
        }
        else if(battle.hasCombatantInSideA(inBattle.getEntityId()))
        {
            battle.addCombatantToSideB(notInBattle);
        }
        else
        {
            battle.addCombatantToSideA(notInBattle);
        }

        logger.debug("Attack Canceled: one is in battle");
        return true;
    }
    
    public void checkTargeted(LivingSetAttackTargetEvent event)
    {
        String targetedCustomName;
        try {
            targetedCustomName = event.getTarget().getCustomName().getUnformattedComponentText();
        } catch (NullPointerException e) {
            targetedCustomName = null;
        }
        String attackerCustomName;
        try {
            attackerCustomName = event.getEntity().getCustomName().getUnformattedComponentText();
        } catch (NullPointerException e) {
            attackerCustomName = null;
        }

        EntityInfo attackerInfo = TurnBasedMinecraftMod.proxy.getConfig().getCustomEntityInfoReference(attackerCustomName);
        if(attackerInfo == null)
        {
            attackerInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(event.getEntity());
        }

        EntityInfo targetedInfo;
        if(event.getTarget() instanceof PlayerEntity)
        {
            targetedInfo = null;
        }
        else
        {
            targetedInfo = TurnBasedMinecraftMod.proxy.getConfig().getCustomEntityInfoReference(targetedCustomName);
            if(targetedInfo == null)
            {
                targetedInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(event.getTarget());
            }
        }
        if((event.getTarget() instanceof PlayerEntity && ((PlayerEntity)event.getTarget()).isCreative())
                || attackerInfo == null
                || attackerInfo.ignoreBattle
                || TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType(attackerInfo.category)
                || (targetedInfo != null
                    && (targetedInfo.ignoreBattle
                        || TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType(targetedInfo.category))))
        {
            return;
        }
        
        Entity inBattle = null;
        Entity notInBattle = null;
        Battle battle = null;
        
        synchronized(battleMap)
        {
            for(Battle b : battleMap.values())
            {
                if(b.hasCombatant(event.getEntity().getEntityId()))
                {
                    if(inBattle != null)
                    {
                        // both entities already in battle
                        return;
                    }
                    else
                    {
                        inBattle = event.getEntity();
                        notInBattle = event.getTarget();
                        battle = b;
                    }
                }
                if(b.hasCombatant(event.getTarget().getEntityId()))
                {
                    if(inBattle != null)
                    {
                        // both entities already in battle
                        return;
                    }
                    else
                    {
                        inBattle = event.getTarget();
                        notInBattle = event.getEntity();
                        battle = b;
                    }
                }
            }
        }
        
        if(battle == null)
        {
            // neither in battle
            if(event.getEntity() instanceof PlayerEntity || event.getTarget() instanceof PlayerEntity)
            {
                // at least one is a player, create battle
                Collection<Entity> sideA = new ArrayList<Entity>(1);
                Collection<Entity> sideB = new ArrayList<Entity>(1);
                sideA.add(event.getEntity());
                sideB.add(event.getTarget());
                createBattle(sideA, sideB);
            }
        }
        else
        {
            // add entity to battle
            if(battle.getSize() >= TurnBasedMinecraftMod.proxy.getConfig().getMaxInBattle())
            {
                // battle max reached, cannot add to battle
                return;
            }
            else if(battle.hasCombatantInSideA(inBattle.getEntityId()))
            {
                battle.addCombatantToSideB(notInBattle);
            }
            else
            {
                battle.addCombatantToSideA(notInBattle);
            }
        }
    }
    
    private Battle createBattle(Collection<Entity> sideA, Collection<Entity> sideB)
    {
        Battle newBattle = null;
        synchronized(battleMap)
        {
            while(battleMap.containsKey(IDCounter))
            {
                ++IDCounter;
            }
            newBattle = new Battle(this, IDCounter, sideA, sideB, true);
            battleMap.put(IDCounter, newBattle);
        }
        newBattle.notifyPlayersBattleInfo();
        return newBattle;
    }
    
    public Battle getBattleByID(int id)
    {
        synchronized(battleMap)
        {
            return battleMap.get(id);
        }
    }
    
    public void cleanup()
    {
        battleUpdater.setRunning(false);
        MinecraftForge.EVENT_BUS.unregister(battleUpdater);
        synchronized(battleMap)
        {
            battleMap.clear();
        }
        battleUpdater = null;
    }
    
    protected void addRecentlyLeftBattle(Combatant c)
    {
        c.time = System.nanoTime();
        Config config = TurnBasedMinecraftMod.proxy.getConfig();
        if(c.entity instanceof ServerPlayerEntity) {
            TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity) c.entity), new PacketGeneralMessage("You just left battle! " + config.getLeaveBattleCooldownSeconds() + " seconds until you can attack/be-attacked again!"));
        }
        synchronized(recentlyLeftBattle) {
            recentlyLeftBattle.put(c.entity.getEntityId(), c);
        }
    }
    
    protected void updateRecentlyLeftBattle()
    {
        long current = System.nanoTime();
        synchronized(recentlyLeftBattle)
        {
            for(Iterator<Map.Entry<Integer, Combatant>> iter = recentlyLeftBattle.entrySet().iterator(); iter.hasNext();)
            {
                Map.Entry<Integer, Combatant> entry = iter.next();
                if(current - entry.getValue().time > TurnBasedMinecraftMod.proxy.getConfig().getLeaveBattleCooldownNanos())
                {
                    iter.remove();
                    if(entry.getValue().entity instanceof ServerPlayerEntity)
                    {
                        TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)entry.getValue().entity), new PacketGeneralMessage("Timer ended, you can now attack/be-attacked again."));
                    }
                }
            }
        }
    }
    
    public boolean isRecentlyLeftBattle(int entityID)
    {
        synchronized(recentlyLeftBattle)
        {
            return recentlyLeftBattle.containsKey(entityID);
        }
    }
}