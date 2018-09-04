package com.seodisparate.TurnBasedMinecraft.common;

import java.util.Comparator;

import com.seodisparate.TurnBasedMinecraft.TurnBasedMinecraftMod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;

public class Combatant
{
    public Entity entity;
    public Battle.Decision decision;
    public int itemToUse;
    public EntityInfo entityInfo;
    public boolean recalcSpeedOnCompare;
    public int targetEntityID;
    
    public Combatant()
    {
        decision = Battle.Decision.UNDECIDED;
        recalcSpeedOnCompare = false;
    }
    
    public Combatant(Entity e, EntityInfo entityInfo)
    {
        entity = e;
        decision = Battle.Decision.UNDECIDED;
        this.entityInfo = entityInfo;
        recalcSpeedOnCompare = false;
    }
    
    /**
     * Provided in reverse order of speed because PriorityQueue has least first.
     */
    public static class CombatantComparator implements Comparator<Combatant>
    {
        @Override
        public int compare(Combatant c0, Combatant c1)
        {
            if(c0.entity instanceof EntityPlayer && c0.recalcSpeedOnCompare)
            {
                EntityLivingBase c0Entity = (EntityLivingBase)c0.entity;
                boolean isHaste = false;
                boolean isSlow = false;
                for(PotionEffect e : c0Entity.getActivePotionEffects())
                {
                    if(e.getEffectName().equals(MobEffects.HASTE.getName()))
                    {
                        isHaste = true;
                    }
                    else if(e.getEffectName().equals(MobEffects.SLOWNESS.getName()))
                    {
                        isSlow = true;
                    }
                }
                if(c0.entityInfo == null)
                {
                    c0.entityInfo = new EntityInfo();
                }
                if(isHaste && !isSlow)
                {
                    c0.entityInfo.speed = TurnBasedMinecraftMod.config.getPlayerHasteSpeed();
                }
                else if(isSlow && !isHaste)
                {
                    c0.entityInfo.speed = TurnBasedMinecraftMod.config.getPlayerSlowSpeed();
                }
                else
                {
                    c0.entityInfo.speed = TurnBasedMinecraftMod.config.getPlayerSpeed();
                }
            }
            
            if(c1.entity instanceof EntityPlayer && c1.recalcSpeedOnCompare)
            {
                EntityLivingBase c1Entity = (EntityLivingBase)c1.entity;
                boolean isHaste = false;
                boolean isSlow = false;
                for(PotionEffect e : c1Entity.getActivePotionEffects())
                {
                    if(e.getEffectName().equals(MobEffects.HASTE.getName()))
                    {
                        isHaste = true;
                    }
                    else if(e.getEffectName().equals(MobEffects.SLOWNESS.getName()))
                    {
                        isSlow = true;
                    }
                }
                if(c1.entityInfo == null)
                {
                    c1.entityInfo = new EntityInfo();
                }
                if(isHaste && !isSlow)
                {
                    c1.entityInfo.speed = TurnBasedMinecraftMod.config.getPlayerHasteSpeed();
                }
                else if(isSlow && !isHaste)
                {
                    c1.entityInfo.speed = TurnBasedMinecraftMod.config.getPlayerSlowSpeed();
                }
                else
                {
                    c1.entityInfo.speed = TurnBasedMinecraftMod.config.getPlayerSpeed();
                }
            }
            
            if(c0.entityInfo.speed > c1.entityInfo.speed)
            {
                return -1;
            }
            else if(c0.entityInfo.speed < c1.entityInfo.speed)
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
    }
}
