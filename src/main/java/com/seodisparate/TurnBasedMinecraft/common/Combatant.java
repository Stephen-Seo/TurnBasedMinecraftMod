package com.seodisparate.TurnBasedMinecraft.common;

import net.minecraft.entity.Entity;

public class Combatant
{
    public Entity entity;
    public Battle.Decision decision;
    public int itemToUse;
    public float speed;
    
    public Combatant()
    {
        decision = Battle.Decision.UNDECIDED;
        speed = 0.5f;
    }
    
    public Combatant(Entity e)
    {
        entity = e;
        decision = Battle.Decision.UNDECIDED;
        speed = 0.5f;
    }
    
    public Combatant(Entity e, float speed)
    {
        entity = e;
        decision = Battle.Decision.UNDECIDED;
        this.speed = speed;
    }
}
