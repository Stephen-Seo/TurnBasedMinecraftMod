package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;

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
    public String customName;
    
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
        HEALTH_BOOST,
        ABSORPTION,
        SATURATION,
        GLOWING,
        LEVITATION,
        LUCK,
        UNLUCK,
        SLOW_FALLING,
        CONDUIT_POWER,
        DOLPHINS_GRACE,
        BAD_OMEN,
        FIRE,
        UNKNOWN;
        
        public static Effect fromString(String c)
        {
            c = c.toLowerCase();
            if(c.equals("speed")) {
                return SPEED;
            } else if(c.equals("slow")) {
                return SLOW;
            } else if(c.equals("haste")) {
                return HASTE;
            } else if(c.equals("mining_fatigue") || c.equals("fatigue")) {
                return MINING_FATIGUE;
            } else if(c.equals("strength")) {
                return STRENGTH;
            } else if(c.equals("jump_boost")) {
                return JUMP_BOOST;
            } else if(c.equals("nausea")) {
                return NAUSEA;
            } else if(c.equals("regeneration")) {
                return REGENERATION;
            } else if(c.equals("resistance")) {
                return RESISTANCE;
            } else if(c.equals("fire_resistance")) {
                return FIRE_RESISTANCE;
            } else if(c.equals("water_breathing")) {
                return WATER_BREATHING;
            } else if(c.equals("invisibility")) {
                return INVISIBILITY;
            } else if(c.equals("blindness") || c.equals("blind")) {
                return BLINDNESS;
            } else if(c.equals("night_vision")) {
                return NIGHT_VISION;
            } else if(c.equals("hunger")) {
                return HUNGER;
            } else if(c.equals("weakness")) {
                return WEAKNESS;
            } else if(c.equals("poison")) {
                return POISON;
            } else if(c.equals("wither")) {
                return WITHER;
            } else if(c.equals("health_boost")) {
                return HEALTH_BOOST;
            } else if(c.equals("absorption")) {
                return ABSORPTION;
            } else if(c.equals("saturation")) {
                return SATURATION;
            } else if(c.equals("glowing")) {
                return GLOWING;
            } else if(c.equals("levitation")) {
                return LEVITATION;
            } else if(c.equals("luck")) {
                return LUCK;
            } else if(c.equals("unluck")) {
                return UNLUCK;
            } else if(c.equals("slow_falling")) {
                return SLOW_FALLING;
            } else if(c.equals("conduit_power")) {
                return CONDUIT_POWER;
            } else if(c.equals("dolphins_grace")) {
                return DOLPHINS_GRACE;
            } else if(c.equals("bad_omen")) {
                return BAD_OMEN;
            } else if(c.equals("fire")) {
                return FIRE;
            } else {
                return UNKNOWN;
            }
        }

        public String toString()
        {
            switch(this)
            {
            case SPEED:
                return "speed";
            case SLOW:
                return "slow";
            case HASTE:
                return "haste";
            case MINING_FATIGUE:
                return "mining_fatigue";
            case STRENGTH:
                return "strength";
            case JUMP_BOOST:
                return "jump_boost";
            case NAUSEA:
                return "nausea";
            case REGENERATION:
                return "regeneration";
            case RESISTANCE:
                return "resistance";
            case FIRE_RESISTANCE:
                return "fire_resistance";
            case WATER_BREATHING:
                return "water_breathing";
            case INVISIBILITY:
                return "invisibility";
            case BLINDNESS:
                return "blindness";
            case NIGHT_VISION:
                return "night_vision";
            case HUNGER:
                return "hunger";
            case WEAKNESS:
                return "weakness";
            case POISON:
                return "poison";
            case WITHER:
                return "wither";
            case HEALTH_BOOST:
                return "health_boost";
            case ABSORPTION:
                return "absorption";
            case SATURATION:
                return "saturation";
            case GLOWING:
                return "glowing";
            case LEVITATION:
                return "levitation";
            case LUCK:
                return "luck";
            case UNLUCK:
                return "unluck";
            case SLOW_FALLING:
                return "slow_falling";
            case CONDUIT_POWER:
                return "conduit_power";
            case DOLPHINS_GRACE:
                return "dolphins_grace";
            case BAD_OMEN:
                return "bad_omen";
            case FIRE:
                return "fire";
            default:
                return "unknown";
            }
        }
        
        public EffectInstance getPotionEffect()
        {
            return getPotionEffect(20 * 7, 0);
        }
        
        public EffectInstance getPotionEffect(int duration, int amplifier) {
            switch(this) {
            case SPEED:
                return new EffectInstance(Effects.SPEED, duration, amplifier);
            case SLOW:
                return new EffectInstance(Effects.SLOWNESS, duration, amplifier);
            case HASTE:
                return new EffectInstance(Effects.HASTE, duration, amplifier);
            case MINING_FATIGUE:
                return new EffectInstance(Effects.MINING_FATIGUE, duration, amplifier);
            case STRENGTH:
                return new EffectInstance(Effects.STRENGTH, duration, amplifier);
            case JUMP_BOOST:
                return new EffectInstance(Effects.JUMP_BOOST, duration, amplifier);
            case NAUSEA:
                return new EffectInstance(Effects.NAUSEA, duration, amplifier);
            case REGENERATION:
                return new EffectInstance(Effects.REGENERATION, duration, amplifier);
            case RESISTANCE:
                return new EffectInstance(Effects.RESISTANCE, duration, amplifier);
            case FIRE_RESISTANCE:
                return new EffectInstance(Effects.FIRE_RESISTANCE, duration, amplifier);
            case WATER_BREATHING:
                return new EffectInstance(Effects.WATER_BREATHING, duration, amplifier);
            case INVISIBILITY:
                return new EffectInstance(Effects.INVISIBILITY, duration, amplifier);
            case BLINDNESS:
                return new EffectInstance(Effects.BLINDNESS, duration, amplifier);
            case NIGHT_VISION:
                return new EffectInstance(Effects.NIGHT_VISION, duration, amplifier);
            case HUNGER:
                return new EffectInstance(Effects.HUNGER, duration, amplifier);
            case WEAKNESS:
                return new EffectInstance(Effects.WEAKNESS, duration, amplifier);
            case POISON:
                return new EffectInstance(Effects.POISON, duration, amplifier);
            case WITHER:
                return new EffectInstance(Effects.WITHER, duration, amplifier);
            case HEALTH_BOOST:
                return new EffectInstance(Effects.HEALTH_BOOST, duration, amplifier);
            case ABSORPTION:
                return new EffectInstance(Effects.ABSORPTION, duration, amplifier);
            case SATURATION:
                return new EffectInstance(Effects.SATURATION, duration, amplifier);
            case GLOWING:
                return new EffectInstance(Effects.GLOWING, duration, amplifier);
            case LEVITATION:
                return new EffectInstance(Effects.LEVITATION, duration, amplifier);
            case LUCK:
                return new EffectInstance(Effects.LUCK, duration, amplifier);
            case UNLUCK:
                return new EffectInstance(Effects.UNLUCK, duration, amplifier);
            case SLOW_FALLING:
                return new EffectInstance(Effects.SLOW_FALLING, duration, amplifier);
            case CONDUIT_POWER:
                return new EffectInstance(Effects.CONDUIT_POWER, duration, amplifier);
            case DOLPHINS_GRACE:
                return new EffectInstance(Effects.DOLPHINS_GRACE, duration, amplifier);
            case BAD_OMEN:
                return new EffectInstance(Effects.BAD_OMEN, duration, amplifier);
            case FIRE:
                // FIRE is not a PotionEffect and must be applied directly to the Entity
                return null;
            default:
                return null;
            }
        }
        
        public void applyEffectToEntity(LivingEntity entity)
        {
            applyEffectToEntity(entity, 20 * 12, 0);
        }
        
        public void applyEffectToEntity(LivingEntity entity, int duration, int amplifier)
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
            case HEALTH_BOOST:
                return "given more health";
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
            case SLOW_FALLING:
                return "falls slower";
            case CONDUIT_POWER:
                return "made able to live underwater";
            case BAD_OMEN:
                return "feels a bad omen";
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
        customName = new String();
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
        newEntityInfo.customName = new String(customName);
        return newEntityInfo;
    }
}
