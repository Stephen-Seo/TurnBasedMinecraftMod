package com.seodisparate.TurnBasedMinecraft.common;

import java.util.ArrayList;
import java.util.List;

public class EntityInfo
{
    public Class classType;
    public List<Class> conflictingTypes;
    public boolean ignoreBattle;
    public int attackPower;
    public int attackProbability;
    public int attackVariance;
    public Effect attackEffect;
    public int attackEffectProbability;
    public int defenseDamage;
    public int defenseDamageProbability;
    public int evasion;
    public int speed;
    public Category category;
    public int decisionAttack;
    public int decisionDefend;
    public int decisionFlee;
    
    public enum Category
    {
        MONSTER,
        PASSIVE,
        ANIMAL,
        BOSS,
        UNKNOWN;
        
        public static Category fromString(String c)
        {
            c = c.toLowerCase();
            if(c.equals("monster"))
            {
                return MONSTER;
            }
            else if(c.equals("passive"))
            {
                return PASSIVE;
            }
            else if(c.equals("animal"))
            {
                return ANIMAL;
            }
            else if(c.equals("boss"))
            {
                return BOSS;
            }
            else
            {
                return UNKNOWN;
            }
        }
    }
    
    public enum Effect
    {
        SPEED,
        SLOW,
        HASTE,
        MINING_FATIGUE,
        STRENGTH,
        JUMP_BOOST,
        NAUSEA,
        REGENERATION,
        RESISTANCE,
        FIRE_RESISTANCE,
        WATER_BREATHING,
        INVISIBILITY,
        BLINDNESS,
        NIGHT_VISION,
        HUNGER,
        WEAKNESS,
        POISON,
        WITHER,
        ABSORPTION,
        SATURATION,
        GLOWING,
        LEVITATION,
        LUCK,
        UNLUCK,
        FIRE,
        UNKNOWN;
        
        public static Effect fromString(String c)
        {
            c = c.toLowerCase();
            if(c.equals("speed"))
            {
                return SPEED;
            }
            else if(c.equals("slow"))
            {
                return SLOW;
            }
            else if(c.equals("haste"))
            {
                return HASTE;
            }
            else if(c.equals("mining_fatigue") || c.equals("fatigue"))
            {
                return MINING_FATIGUE;
            }
            else if(c.equals("strength"))
            {
                return STRENGTH;
            }
            else if(c.equals("jump_boost"))
            {
                return JUMP_BOOST;
            }
            else if(c.equals("nausea"))
            {
                return NAUSEA;
            }
            else if(c.equals("regeneration"))
            {
                return REGENERATION;
            }
            else if(c.equals("resistance"))
            {
                return RESISTANCE;
            }
            else if(c.equals("fire_resistance"))
            {
                return FIRE_RESISTANCE;
            }
            else if(c.equals("water_breathing"))
            {
                return WATER_BREATHING;
            }
            else if(c.equals("invisibility"))
            {
                return INVISIBILITY;
            }
            else if(c.equals("blindness") || c.equals("blind"))
            {
                return BLINDNESS;
            }
            else if(c.equals("night_vision"))
            {
                return NIGHT_VISION;
            }
            else if(c.equals("hunger"))
            {
                return HUNGER;
            }
            else if(c.equals("weakness"))
            {
                return WEAKNESS;
            }
            else if(c.equals("poison"))
            {
                return POISON;
            }
            else if(c.equals("wither"))
            {
                return WITHER;
            }
            else if(c.equals("absorption"))
            {
                return ABSORPTION;
            }
            else if(c.equals("saturation"))
            {
                return SATURATION;
            }
            else if(c.equals("glowing"))
            {
                return GLOWING;
            }
            else if(c.equals("levitation"))
            {
                return LEVITATION;
            }
            else if(c.equals("luck"))
            {
                return LUCK;
            }
            else if(c.equals("unluck"))
            {
                return UNLUCK;
            }
            else if(c.equals("fire"))
            {
                return FIRE;
            }
            else
            {
                return UNKNOWN;
            }
        }
    }
    
    public EntityInfo()
    {
        classType = null;
        conflictingTypes = new ArrayList<Class>();
        ignoreBattle = false;
        attackPower = 0;
        attackProbability = 70;
        attackVariance = 0;
        attackEffect = Effect.UNKNOWN;
        attackEffectProbability = 50;
        defenseDamage = 0;
        defenseDamageProbability = 0;
        evasion = 15;
        speed = 50;
        category = Category.UNKNOWN;
        decisionAttack = 70;
        decisionDefend = 20;
        decisionFlee = 10;
    }
    
    public EntityInfo clone()
    {
        EntityInfo newEntityInfo = new EntityInfo();
        newEntityInfo.classType = classType;
        newEntityInfo.conflictingTypes = new ArrayList<Class>();
        for(Class c : conflictingTypes)
        {
            newEntityInfo.conflictingTypes.add(c);
        }
        newEntityInfo.ignoreBattle = ignoreBattle;
        newEntityInfo.attackPower = attackPower;
        newEntityInfo.attackProbability = attackProbability;
        newEntityInfo.attackVariance = attackVariance;
        newEntityInfo.attackEffect = attackEffect;
        newEntityInfo.attackEffectProbability = attackEffectProbability;
        newEntityInfo.defenseDamage = defenseDamage;
        newEntityInfo.defenseDamageProbability = defenseDamageProbability;
        newEntityInfo.evasion = evasion;
        newEntityInfo.speed = speed;
        newEntityInfo.category = category;
        newEntityInfo.decisionAttack = decisionAttack;
        newEntityInfo.decisionDefend = decisionDefend;
        newEntityInfo.decisionFlee = decisionFlee;
        return newEntityInfo;
    }
}
