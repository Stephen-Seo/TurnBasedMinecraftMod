package com.seodisparate.TurnBasedMinecraft.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketGeneralMessage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;

public class BattleManager
{
    private int IDCounter = 0;
    protected Map<Integer, Battle> battleMap;
    private Thread updaterThread;
    private BattleUpdater battleUpdater;
    private Logger logger;
    private Map<Integer, Combatant> recentlyLeftBattle;
    
    public BattleManager(Logger logger)
    {
        this.logger = logger;
        battleMap = new HashMap<Integer, Battle>();
        battleUpdater = new BattleUpdater(this);
        updaterThread = new Thread(battleUpdater);
        updaterThread.start();
        recentlyLeftBattle = new HashMap<Integer, Combatant>();
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
        // verify that both entities are EntityPlayer and not in creative or has a corresponding EntityInfo
        if(!((event.getEntity() instanceof EntityPlayer && !((EntityPlayer)event.getEntity()).isCreative()) || TurnBasedMinecraftMod.proxy.getConfig().getEntityInfoReference(event.getEntity().getClass().getName()) != null)
            || !((event.getSource().getTrueSource() instanceof EntityPlayer && !((EntityPlayer)event.getSource().getTrueSource()).isCreative()) || TurnBasedMinecraftMod.proxy.getConfig().getEntityInfoReference(event.getSource().getTrueSource().getClass().getName()) != null))
        {
            return false;
        }
        
        // check if ignore battle in config
        EntityInfo entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(event.getEntity());
        if(entityInfo != null && (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType(entityInfo.category) || entityInfo.ignoreBattle))
        {
            // attacked entity ignores battle
            synchronized(battleMap)
            {
                for(Battle b : battleMap.values())
                {
                    if(b.hasCombatant(event.getSource().getTrueSource().getEntityId()))
                    {
    //                    logger.debug("Attack Canceled: attacked ignores battle but attacker in battle");
                        return true;
                    }
                }
            }
//            logger.debug("Attack Not Canceled: attacked ignores battle");
            return false;
        }
        entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(event.getSource().getTrueSource());
        if(entityInfo != null && (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType(entityInfo.category) || entityInfo.ignoreBattle))
        {
            // attacker entity ignores battle
            synchronized(battleMap)
            {
                for(Battle b : battleMap.values())
                {
                    if(b.hasCombatant(event.getEntity().getEntityId()))
                    {
    //                    logger.debug("Attack Canceled: attacker ignores battle but attacked in battle");
                        return true;
                    }
                }
            }
//            logger.debug("Attack Not Canceled: attacker ignores battle");
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
    //                    logger.debug("Attack Canceled: both are in battle");
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
    //                    logger.debug("Attack Canceled: both are in battle");
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
            if(event.getEntity() instanceof EntityPlayer || event.getSource().getTrueSource() instanceof EntityPlayer)
            {
                // at least one of the entities is a player, create Battle
                Collection<Entity> sideA = new ArrayList<Entity>(1);
                Collection<Entity> sideB = new ArrayList<Entity>(1);
                sideA.add(event.getEntity());
                sideB.add(event.getSource().getTrueSource());
                createBattle(sideA, sideB);
//                logger.debug("Attack Not Canceled: new battle created");
            }
            else
            {
//                logger.debug("Attack Not Canceled: neither are in battle or players");
            }
            return false;
        }

        // at this point only one entity is in battle, so add entity to other side
        if(battle.getSize() >= TurnBasedMinecraftMod.proxy.getConfig().getMaxInBattle())
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

//        logger.debug("Attack Canceled: one is in battle");
        return true;
    }
    
    public void checkTargeted(LivingSetAttackTargetEvent event)
    {
        EntityInfo attackerInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(event.getEntity());
        EntityInfo targetedInfo = event.getTarget() instanceof EntityPlayer ? null : TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(event.getTarget());
        if((event.getTarget() instanceof EntityPlayer && ((EntityPlayer)event.getTarget()).isCreative())
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
            if(event.getEntity() instanceof EntityPlayer || event.getTarget() instanceof EntityPlayer)
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
        battleUpdater.setIsRunning(false);
        battleUpdater = null;
        updaterThread = null;
        synchronized(battleMap)
        {
            battleMap.clear();
        }
    }
    
    protected void addRecentlyLeftBattle(Combatant c)
    {
        c.time = System.nanoTime();
        Config config = TurnBasedMinecraftMod.proxy.getConfig();
        if(c.entity instanceof EntityPlayerMP)
        {
            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("You just left battle! " + config.getLeaveBattleCooldownSeconds() + " seconds until you can attack/be-attacked again!"), (EntityPlayerMP)c.entity);
        }
        synchronized(recentlyLeftBattle)
        {
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
                    if(entry.getValue().entity instanceof EntityPlayerMP)
                    {
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Timer ended, you can now attack/be-attacked again."), (EntityPlayerMP)entry.getValue().entity);
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