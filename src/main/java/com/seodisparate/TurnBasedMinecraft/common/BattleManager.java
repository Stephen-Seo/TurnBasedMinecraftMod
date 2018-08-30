package com.seodisparate.TurnBasedMinecraft.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleEntered;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

public class BattleManager
{
    private int IDCounter = 0;
    protected Map<Integer, Battle> battleMap;
    private Thread updaterThread;
    
    public BattleManager()
    {
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
        
        if(notInBattle instanceof EntityPlayerMP)
        {
            PacketHandler.INSTANCE.sendTo(new PacketBattleEntered(IDCounter), (EntityPlayerMP)notInBattle);
        }
        
        battle.notifyPlayersBattleInfo();
        return true;
    }
    
    private Battle createBattle(Collection<Entity> sideA, Collection<Entity> sideB)
    {
        while(battleMap.containsKey(IDCounter))
        {
            ++IDCounter;
        }
        Battle newBattle = new Battle(IDCounter, sideA, sideB);
        battleMap.put(IDCounter, newBattle);
        for(Entity e : sideA)
        {
            if(e instanceof EntityPlayerMP)
            {
                PacketHandler.INSTANCE.sendTo(new PacketBattleEntered(IDCounter), (EntityPlayerMP)e);
            }
        }
        for(Entity e : sideB)
        {
            if(e instanceof EntityPlayerMP)
            {
                PacketHandler.INSTANCE.sendTo(new PacketBattleEntered(IDCounter), (EntityPlayerMP)e);
            }
        }
        newBattle.notifyPlayersBattleInfo();
        return newBattle;
    }
    
    public Battle getBattleByID(int id)
    {
        return battleMap.get(id);
    }
}