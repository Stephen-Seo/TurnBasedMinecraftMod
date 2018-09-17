package com.seodisparate.TurnBasedMinecraft.common;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;

public class EntityInfo
{
    public Class classType;
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
    public String category;
    public int decisionAttack;
    public int decisionDefend;
    public int decisionFlee;
    
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
        
        public PotionEffect getPotionEffect()
        {
            return getPotionEffect(20 * 7, 0);
        }
        
        public PotionEffect getPotionEffect(int duration, int amplifier)
        {
            switch(this)
            {
            case SPEED:
                return new PotionEffect(MobEffects.SPEED, duration, amplifier);
            case SLOW:
                return new PotionEffect(MobEffects.SLOWNESS, duration, amplifier);
            case HASTE:
                return new PotionEffect(MobEffects.HASTE, duration, amplifier);
            case MINING_FATIGUE:
                return new PotionEffect(MobEffects.MINING_FATIGUE, duration, amplifier);
            case STRENGTH:
                return new PotionEffect(MobEffects.STRENGTH, duration, amplifier);
            case JUMP_BOOST:
                return new PotionEffect(MobEffects.JUMP_BOOST, duration, amplifier);
            case NAUSEA:
                return new PotionEffect(MobEffects.NAUSEA, duration, amplifier);
            case REGENERATION:
                return new PotionEffect(MobEffects.REGENERATION, duration, amplifier);
            case RESISTANCE:
                return new PotionEffect(MobEffects.RESISTANCE, duration, amplifier);
            case FIRE_RESISTANCE:
                return new PotionEffect(MobEffects.FIRE_RESISTANCE, duration, amplifier);
            case WATER_BREATHING:
                return new PotionEffect(MobEffects.WATER_BREATHING, duration, amplifier);
            case INVISIBILITY:
                return new PotionEffect(MobEffects.INVISIBILITY, duration, amplifier);
            case BLINDNESS:
                return new PotionEffect(MobEffects.BLINDNESS, duration, amplifier);
            case NIGHT_VISION:
                return new PotionEffect(MobEffects.NIGHT_VISION, duration, amplifier);
            case HUNGER:
                return new PotionEffect(MobEffects.HUNGER, duration, amplifier);
            case WEAKNESS:
                return new PotionEffect(MobEffects.WEAKNESS, duration, amplifier);
            case POISON:
                return new PotionEffect(MobEffects.POISON, duration, amplifier);
            case WITHER:
                return new PotionEffect(MobEffects.WITHER, duration, amplifier);
            case ABSORPTION:
                return new PotionEffect(MobEffects.ABSORPTION, duration, amplifier);
            case SATURATION:
                return new PotionEffect(MobEffects.SATURATION, duration, amplifier);
            case GLOWING:
                return new PotionEffect(MobEffects.GLOWING, duration, amplifier);
            case LEVITATION:
                return new PotionEffect(MobEffects.LEVITATION, duration, amplifier);
            case LUCK:
                return new PotionEffect(MobEffects.LUCK, duration, amplifier);
            case UNLUCK:
                return new PotionEffect(MobEffects.UNLUCK, duration, amplifier);
            case FIRE:
                // FIRE is not a PotionEffect and must be applied directly to the Entity
                return null;
            default:
                return null;
            }
        }
        
        public void applyEffectToEntity(EntityLivingBase entity)
        {
            applyEffectToEntity(entity, 20 * 7, 0);
        }
        
        public void applyEffectToEntity(EntityLivingBase entity, int duration, int amplifier)
        {
            if(this == FIRE)
            {
                entity.setFire(duration / 20);
                return;
            }
            else if(this != UNKNOWN)
            {
                entity.addPotionEffect(getPotionEffect(duration, amplifier));
            }
        }
        
        public String getAffectedString()
        {
            switch(this)
            {
            case SPEED:
                return "made faster";
            case SLOW:
                return "made slower";
            case HASTE:
                return "made hastier";
            case MINING_FATIGUE:
                return "fatigued";
            case STRENGTH:
                return "strengthened";
            case JUMP_BOOST:
                return "jump boosted";
            case NAUSEA:
                return "made nauseous";
            case REGENERATION:
                return "given regeneration";
            case RESISTANCE:
                return "given resistance";
            case FIRE_RESISTANCE:
                return "given fire resistance";
            case WATER_BREATHING:
                return "made able to breathe underwater";
            case INVISIBILITY:
                return "given invisibility";
            case BLINDNESS:
                return "made blind";
            case NIGHT_VISION:
                return "given night vision";
            case HUNGER:
                return "made hungry";
            case WEAKNESS:
                return "made weak";
            case POISON:
                return "poisoned";
            case WITHER:
                return "withered";
            case ABSORPTION:
                return "given absorption";
            case SATURATION:
                return "given saturation";
            case GLOWING:
                return "made to glow";
            case LEVITATION:
                return "made to levitate";
            case LUCK:
                return "given luck";
            case UNLUCK:
                return "made unlucky";
            case FIRE:
                return "set on fire";
            default:
                return "given unknown";
            }
        }
    }
    
    public EntityInfo()
    {
        classType = null;
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
        category = "unknown";
        decisionAttack = 70;
        decisionDefend = 20;
        decisionFlee = 10;
    }
    
    public EntityInfo clone()
    {
        EntityInfo newEntityInfo = new EntityInfo();
        newEntityInfo.classType = classType;
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
