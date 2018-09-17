package com.seodisparate.TurnBasedMinecraft.common;

import net.minecraft.entity.Entity;

public class AttackerViaBow
{
    public static long ATTACK_TIMEOUT = 10000000000L;
    
    public Entity entity;
    public long attackTime;
    public int battleID;
    
    public AttackerViaBow()
    {
        entity = null;
        attackTime = 0;
        battleID = -1;
    }
    
    public AttackerViaBow(Entity entity, int battleID)
    {
        this.entity = entity;
        attackTime = System.nanoTime();
        this.battleID = battleID;
    }
}
