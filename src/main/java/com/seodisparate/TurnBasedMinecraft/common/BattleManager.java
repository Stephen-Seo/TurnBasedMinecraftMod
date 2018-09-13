package com.seodisparate.TurnBasedMinecraft.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

public class BattleManager
{
    private int IDCounter = 0;
    protected Map<Integer, Battle> battleMap;
    private Thread updaterThread;
    private Logger logger;
    
    public BattleManager(Logger logger)
    {
        this.logger = logger;
        battleMap = new Hashtable<Integer, Battle>();
        updaterThread = new Thread(new BattleUpdater(this));
        updaterThread.start();
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
        if(!((event.getEntity() instanceof EntityPlayer && !((EntityPlayer)event.getEntity()).isCreative()) || TurnBasedMinecraftMod.config.getEntityInfoReference(event.getEntity().getClass().getName()) != null)
            || !((event.getSource().getTrueSource() instanceof EntityPlayer && !((EntityPlayer)event.getSource().getTrueSource()).isCreative()) || TurnBasedMinecraftMod.config.getEntityInfoReference(event.getSource().getTrueSource().getClass().getName()) != null))
        {
            return false;
        }
        
        // check if ignore battle in config
        EntityInfo entityInfo = TurnBasedMinecraftMod.config.getMatchingEntityInfo(event.getEntity());
        if(entityInfo != null && (TurnBasedMinecraftMod.config.isIgnoreBattleType(entityInfo.category) || entityInfo.ignoreBattle))
        {
            // attacked entity ignores battle
            for(Battle b : battleMap.values())
            {
                if(b.hasCombatant(event.getSource().getTrueSource().getEntityId()))
                {
//                    logger.debug("Attack Canceled: attacked ignores battle but attacker in battle");
                    return true;
                }
            }
//            logger.debug("Attack Not Canceled: attacked ignores battle");
            return false;
        }
        entityInfo = TurnBasedMinecraftMod.config.getMatchingEntityInfo(event.getSource().getTrueSource());
        if(entityInfo != null && (TurnBasedMinecraftMod.config.isIgnoreBattleType(entityInfo.category) || entityInfo.ignoreBattle))
        {
            // attacker entity ignores battle
            for(Battle b : battleMap.values())
            {
                if(b.hasCombatant(event.getEntity().getEntityId()))
                {
//                    logger.debug("Attack Canceled: attacker ignores battle but attacked in battle");
                    return true;
                }
            }
//            logger.debug("Attack Not Canceled: attacker ignores battle");
            return false;
        }
        
        // check if one is in battle
        Entity inBattle = null;
        Entity notInBattle = null;
        Battle battle = null;
        
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
        if(battle.hasCombatantInSideA(inBattle.getEntityId()))
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
    
    private Battle createBattle(Collection<Entity> sideA, Collection<Entity> sideB)
    {
        while(battleMap.containsKey(IDCounter))
        {
            ++IDCounter;
        }
        Battle newBattle = new Battle(IDCounter, sideA, sideB, true);
        battleMap.put(IDCounter, newBattle);
        newBattle.notifyPlayersBattleInfo();
        return newBattle;
    }
    
    public Battle getBattleByID(int id)
    {
        return battleMap.get(id);
    }
}