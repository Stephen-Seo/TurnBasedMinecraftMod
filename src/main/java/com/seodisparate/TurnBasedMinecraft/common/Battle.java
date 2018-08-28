package com.seodisparate.TurnBasedMinecraft.common;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import net.minecraft.entity.Entity;

public class Battle
{
    private int id;
    private Map<Integer, Entity> sideA;
    private Map<Integer, Entity> sideB;
    
    private Instant lastUpdated;

    public Battle(int id, Collection<Entity> sideA, Collection<Entity> sideB)
    {
        this.id = id;
        this.sideA = new Hashtable<Integer, Entity>();
        this.sideB = new Hashtable<Integer, Entity>();
        for(Entity e : sideA)
        {
            this.sideA.put(e.getEntityId(), e);
        }
        for(Entity e : sideB)
        {
            this.sideB.put(e.getEntityId(), e);
        }
        
        lastUpdated = null;
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
        sideA.put(e.getEntityId(), e);
    }
    
    public void addCombatantToSideB(Entity e)
    {
        sideB.put(e.getEntityId(), e);
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
    
    private void update(Duration dt)
    {
    }
}
