package com.seodisparate.TurnBasedMinecraft.common;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

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
    private Map<Integer, EntityPlayer> players;
    
    private Instant lastUpdated;
    private State state;
    private int playerCount;
    private int undecidedCount;
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
        players = new HashMap<Integer, EntityPlayer>();
        playerCount = 0;
        if(sideA != null)
        {
            for(Entity e : sideA)
            {
                this.sideA.put(e.getEntityId(), new Combatant(e));
                if(e instanceof EntityPlayer)
                {
                    ++playerCount;
                    players.put(e.getEntityId(), (EntityPlayer)e);
                }
            }
        }
        if(sideB != null)
        {
            for(Entity e : sideB)
            {
                this.sideB.put(e.getEntityId(), new Combatant(e));
                if(e instanceof EntityPlayer)
                {
                    ++playerCount;
                    players.put(e.getEntityId(), (EntityPlayer)e);
                }
            }
        }
        
        lastUpdated = null;
        state = State.DECISION;
        undecidedCount = playerCount;
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
        sideA.put(e.getEntityId(), new Combatant(e));
        if(e instanceof EntityPlayer)
        {
            ++playerCount;
            players.put(e.getEntityId(), (EntityPlayer)e);
            if(state == State.DECISION)
            {
                ++undecidedCount;
            }
        }
    }
    
    public void addCombatantToSideB(Entity e)
    {
        sideB.put(e.getEntityId(), new Combatant(e));
        if(e instanceof EntityPlayer)
        {
            ++playerCount;
            players.put(e.getEntityId(), (EntityPlayer)e);
            if(state == State.DECISION)
            {
                ++undecidedCount;
            }
        }
    }
    
    public void clearCombatants()
    {
        sideA.clear();
        sideB.clear();
        players.clear();
        playerCount = 0;
        undecidedCount = 0;
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
    
    public void setDecision(int entityID, Decision decision)
    {
        if(state != State.DECISION)
        {
            return;
        }
        Combatant combatant = sideA.get(entityID);
        if(combatant == null)
        {
            combatant = sideB.get(entityID);
            if(combatant == null)
            {
                return;
            }
        }
        
        if(combatant.entity instanceof EntityPlayer)
        {
            combatant.decision = decision;
            --undecidedCount;
        }
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
        for(EntityPlayer p : players.values())
        {
            PacketHandler.INSTANCE.sendTo(infoPacket, (EntityPlayerMP)p);
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
            if(timer.isNegative() || timer.isZero() || undecidedCount <= 0)
            {
                state = State.ATTACK;
                timer = TurnBasedMinecraftMod.BattleDecisionTime;
                update(Duration.ZERO);
            }
            break;
        case ATTACK:
            // TODO
            break;
        case HEALTH_CHECK:
            // TODO
            break;
        }
    }
}
