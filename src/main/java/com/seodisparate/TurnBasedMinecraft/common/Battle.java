package com.seodisparate.TurnBasedMinecraft.common;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.seodisparate.TurnBasedMinecraft.TurnBasedMinecraftMod;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleInfo;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class Battle
{
    private final int id;
    private Map<Integer, Combatant> sideA;
    private Map<Integer, Combatant> sideB;
    private Map<Integer, Combatant> players;
    private PriorityQueue<Combatant> turnOrderQueue;
    
    private Instant lastUpdated;
    private State state;
    private AtomicInteger playerCount;
    private AtomicInteger undecidedCount;
    private Duration timer;
    
    public enum State
    {
        DECISION,
        ATTACK,
        HEALTH_CHECK
    }
    
    public enum Decision
    {
        UNDECIDED(0),
        ATTACK(1),
        DEFEND(2),
        FLEE(3),
        USE_ITEM(4);
        
        private int value;
        private static Map<Integer, Decision> map = new HashMap<Integer, Decision>();
        
        private Decision(int value)
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
        
        static
        {
            for(Decision decision : Decision.values())
            {
                map.put(decision.value, decision);
            }
        }
        
        public static Decision valueOf(int decisionType)
        {
            return map.get(decisionType);
        }
    }

    public Battle(int id, Collection<Entity> sideA, Collection<Entity> sideB)
    {
        this.id = id;
        this.sideA = new Hashtable<Integer, Combatant>();
        this.sideB = new Hashtable<Integer, Combatant>();
        players = new Hashtable<Integer, Combatant>();
        turnOrderQueue = new PriorityQueue<Combatant>(new Combatant.CombatantComparator());
        playerCount = new AtomicInteger(0);
        undecidedCount = new AtomicInteger(0);
        if(sideA != null)
        {
            for(Entity e : sideA)
            {
                EntityInfo entityInfo = TurnBasedMinecraftMod.config.getMatchingEntityInfo(e);
                if(entityInfo == null && !(e instanceof EntityPlayer))
                {
                    continue;
                }
                Combatant newCombatant = new Combatant(e, entityInfo);
                this.sideA.put(e.getEntityId(), newCombatant);
                if(e instanceof EntityPlayer)
                {
                    newCombatant.recalcSpeedOnCompare = true;
                    playerCount.incrementAndGet();
                    players.put(e.getEntityId(), newCombatant);
                }
            }
        }
        if(sideB != null)
        {
            for(Entity e : sideB)
            {
                EntityInfo entityInfo = TurnBasedMinecraftMod.config.getMatchingEntityInfo(e);
                if(entityInfo == null && !(e instanceof EntityPlayer))
                {
                    continue;
                }
                Combatant newCombatant = new Combatant(e, entityInfo);
                this.sideB.put(e.getEntityId(), newCombatant);
                if(e instanceof EntityPlayer)
                {
                    newCombatant.recalcSpeedOnCompare = true;
                    playerCount.incrementAndGet();
                    players.put(e.getEntityId(), newCombatant);
                }
            }
        }
        
        lastUpdated = null;
        state = State.DECISION;
        undecidedCount.set(playerCount.get());
        timer = TurnBasedMinecraftMod.BattleDecisionTime;
    }

    public int getId()
    {
        return id;
    }
    
    public boolean hasCombatant(int entityID)
    {
        return sideA.containsKey(entityID) || sideB.containsKey(entityID);
    }
    
    public boolean hasCombatantInSideA(int entityID)
    {
        return sideA.containsKey(entityID);
    }
    
    public void addCombatantToSideA(Entity e)
    {
        EntityInfo entityInfo = TurnBasedMinecraftMod.config.getMatchingEntityInfo(e);
        if(entityInfo == null && !(e instanceof EntityPlayer))
        {
            return;
        }
        Combatant newCombatant = new Combatant(e, entityInfo);
        sideA.put(e.getEntityId(), newCombatant);
        if(e instanceof EntityPlayer)
        {
            newCombatant.recalcSpeedOnCompare = true;
            playerCount.incrementAndGet();
            players.put(e.getEntityId(), newCombatant);
            if(state == State.DECISION)
            {
                undecidedCount.incrementAndGet();
            }
        }
    }
    
    public void addCombatantToSideB(Entity e)
    {
        EntityInfo entityInfo = TurnBasedMinecraftMod.config.getMatchingEntityInfo(e);
        if(entityInfo == null && !(e instanceof EntityPlayer))
        {
            return;
        }
        Combatant newCombatant = new Combatant(e, entityInfo);
        sideB.put(e.getEntityId(), newCombatant);
        if(e instanceof EntityPlayer)
        {
            newCombatant.recalcSpeedOnCompare = true;
            playerCount.incrementAndGet();
            players.put(e.getEntityId(), newCombatant);
            if(state == State.DECISION)
            {
                undecidedCount.incrementAndGet();
            }
        }
    }
    
    public void clearCombatants()
    {
        sideA.clear();
        sideB.clear();
        players.clear();
        playerCount.set(0);
        undecidedCount.set(0);
    }
    
    public Collection<Combatant> getSideA()
    {
        return sideA.values();
    }
    
    public Collection<Combatant> getSideB()
    {
        return sideB.values();
    }
    
    public Collection<Integer> getSideAIDs()
    {
        Collection<Integer> sideAIDs = new ArrayList<Integer>(sideA.size());
        for(Combatant combatant : sideA.values())
        {
            sideAIDs.add(combatant.entity.getEntityId());
        }
        return sideAIDs;
    }
    
    public Collection<Integer> getSideBIDs()
    {
        Collection<Integer> sideBIDs = new ArrayList<Integer>(sideB.size());
        for(Combatant combatant : sideB.values())
        {
            sideBIDs.add(combatant.entity.getEntityId());
        }
        return sideBIDs;
    }
    
    public Combatant getCombatantByID(int entityID)
    {
        Combatant combatant = sideA.get(entityID);
        if(combatant == null)
        {
            combatant = sideB.get(entityID);
        }
        return combatant;
    }
    
    public void setDecision(int entityID, Decision decision, int targetEntityID)
    {
        if(state != State.DECISION)
        {
            return;
        }
        Combatant combatant = players.get(entityID);
        if(combatant == null)
        {
            return;
        }
        combatant.decision = decision;
        combatant.targetEntityID = targetEntityID;
        undecidedCount.decrementAndGet();
    }
    
    public State getState()
    {
        return state;
    }
    
    public void notifyPlayersBattleInfo()
    {
        if(TurnBasedMinecraftMod.getBattleManager() == null)
        {
            return;
        }
        PacketBattleInfo infoPacket = new PacketBattleInfo(getSideAIDs(), getSideBIDs());
        for(Combatant p : players.values())
        {
            PacketHandler.INSTANCE.sendTo(infoPacket, (EntityPlayerMP)p.entity);
        }
    }
    
    public void update()
    {
        if(lastUpdated == null)
        {
            lastUpdated = Instant.now();
            update(Duration.ZERO);
        }
        else
        {
            Instant now = Instant.now();
            update(Duration.between(lastUpdated, now));
            lastUpdated = now;
        }
    }
    
    private void update(final Duration dt)
    {
        switch(state)
        {
        case DECISION:
            timer = timer.minus(dt);
            if(timer.isNegative() || timer.isZero() || undecidedCount.get() <= 0)
            {
                state = State.ATTACK;
                timer = TurnBasedMinecraftMod.BattleDecisionTime;
                turnOrderQueue.clear();
                for(Combatant c : sideA.values())
                {
                    turnOrderQueue.add(c);
                }
                for(Combatant c : sideB.values())
                {
                    turnOrderQueue.add(c);
                }
                update(Duration.ZERO);
            }
            break;
        case ATTACK:
            Combatant next = turnOrderQueue.poll();
            while(next != null)
            {
                // TODO attack per entity here
                next = turnOrderQueue.poll();
            }
            break;
        case HEALTH_CHECK:
            // TODO
            break;
        }
    }
}
