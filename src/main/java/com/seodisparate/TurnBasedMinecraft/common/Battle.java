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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;

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
        ACTION,
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
                newCombatant.isSideA = true;
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
                newCombatant.isSideA = false;
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
        newCombatant.isSideA = true;
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
        newCombatant.isSideA = false;
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
    
    public void setDecision(int entityID, Decision decision, int targetIDOrItemID)
    {
        if(state != State.DECISION)
        {
            return;
        }
        Combatant combatant = players.get(entityID);
        if(combatant == null || combatant.decision != Decision.UNDECIDED)
        {
            return;
        }
        combatant.decision = decision;
        if(decision == Decision.ATTACK)
        {
            combatant.targetEntityID = targetIDOrItemID;
        }
        else if(decision == Decision.USE_ITEM)
        {
            combatant.itemToUse = targetIDOrItemID;
        }
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
                state = State.ACTION;
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
                // TODO assign decisions to non-players
            }
            break;
        case ACTION:
        {
            Combatant next = turnOrderQueue.poll();
            while(next != null)
            {
                if(!next.entity.isEntityAlive())
                {
                    next = turnOrderQueue.poll();
                    continue;
                }
                switch(next.decision)
                {
                case UNDECIDED:
                    next = turnOrderQueue.poll();
                    continue;
                case ATTACK:
                    Combatant target = null;
                    if(next.entity instanceof EntityPlayer)
                    {
                        if(next.isSideA)
                        {
                            target = sideB.get(next.targetEntityID);
                        }
                        else
                        {
                            target = sideA.get(next.targetEntityID);
                        }
                        if(target == null || !target.entity.isEntityAlive())
                        {
                            next = turnOrderQueue.poll();
                            continue;
                        }
                        int hitChance = TurnBasedMinecraftMod.config.getPlayerAttackProbability();
                        if(target.entity instanceof EntityPlayer)
                        {
                            hitChance -= TurnBasedMinecraftMod.config.getPlayerEvasion();
                        }
                        else
                        {
                            hitChance -= target.entityInfo.evasion;
                        }
                        if((int)(Math.random() * 100) < hitChance)
                        {
                            if(target.remainingDefenses <= 0)
                            {
                                // attack
                                // TODO damage via bow and arrow
                                TurnBasedMinecraftMod.attackingEntity = next.entity;
                                ((EntityPlayer)next.entity).attackTargetEntityWithCurrentItem(target.entity);
                                TurnBasedMinecraftMod.attackingEntity = null;
                                if(!(target.entity instanceof EntityPlayer) && target.entityInfo.defenseDamage > 0)
                                {
                                    if((int)(Math.random() * 100) < target.entityInfo.defenseDamageProbability)
                                    {
                                        // defense damage
                                        DamageSource defenseDamageSource = DamageSource.causeMobDamage((EntityLivingBase)target.entity);
                                        TurnBasedMinecraftMod.attackingEntity = target.entity;
                                        next.entity.attackEntityFrom(defenseDamageSource, target.entityInfo.defenseDamage);
                                        TurnBasedMinecraftMod.attackingEntity = null;
                                    }
                                }
                            }
                            else
                            {
                                // blocked
                                --target.remainingDefenses;
                            }
                        }
                        else
                        {
                            // miss
                        }
                    }
                    else
                    {
                        if(next.isSideA)
                        {
                            int randomTargetIndex = (int)(Math.random() * sideB.size());
                            for(Combatant c : sideB.values())
                            {
                                if(randomTargetIndex-- == 0)
                                {
                                    target = c;
                                    break;
                                }
                            }
                        }
                        else
                        {
                            int randomTargetIndex = (int)(Math.random() * sideA.size());
                            for(Combatant c : sideA.values())
                            {
                                if(randomTargetIndex-- == 0)
                                {
                                    target = c;
                                    break;
                                }
                            }
                        }
                        if(target == null || !target.entity.isEntityAlive())
                        {
                            next = turnOrderQueue.poll();
                            continue;
                        }
                        int hitChance = next.entityInfo.attackProbability;
                        if(target.entity instanceof EntityPlayer)
                        {
                            hitChance -= TurnBasedMinecraftMod.config.getPlayerEvasion();
                        }
                        else
                        {
                            hitChance -= target.entityInfo.evasion;
                        }
                        if((int)(Math.random() * 100) < hitChance)
                        {
                            if(target.remainingDefenses <= 0)
                            {
                                DamageSource damageSource = DamageSource.causeMobDamage((EntityLivingBase)next.entity);
                                int damageAmount = next.entityInfo.attackPower;
                                if(next.entityInfo.attackVariance > 0)
                                {
                                    damageAmount += (int)(Math.random() * (next.entityInfo.attackVariance * 2 + 1)) - next.entityInfo.attackVariance;
                                }
                                // attack
                                TurnBasedMinecraftMod.attackingEntity = next.entity;
                                target.entity.attackEntityFrom(damageSource, next.entityInfo.attackPower);
                                TurnBasedMinecraftMod.attackingEntity = null;
                                if(!(target.entity instanceof EntityPlayer) && target.entityInfo.defenseDamage > 0)
                                {
                                    if((int)(Math.random() * 100) < target.entityInfo.defenseDamageProbability)
                                    {
                                        // defense damage
                                        DamageSource defenseDamageSource = DamageSource.causeMobDamage((EntityLivingBase)target.entity);
                                        TurnBasedMinecraftMod.attackingEntity = target.entity;
                                        next.entity.attackEntityFrom(defenseDamageSource, target.entityInfo.defenseDamage);
                                        TurnBasedMinecraftMod.attackingEntity = null;
                                    }
                                }
                            }
                            else
                            {
                                // blocked
                                --target.remainingDefenses;
                            }
                        }
                        else
                        {
                            // miss
                        }
                    }
                    break;
                case DEFEND:
                    next.remainingDefenses = TurnBasedMinecraftMod.config.getDefenseDuration();
                    break;
                case FLEE:
                    int fastestEnemySpeed = 0;
                    if(next.isSideA)
                    {
                        for(Combatant c : sideB.values())
                        {
                            if(c.entity instanceof EntityPlayer)
                            {
                                if(TurnBasedMinecraftMod.config.getPlayerSpeed() > fastestEnemySpeed)
                                {
                                    fastestEnemySpeed = TurnBasedMinecraftMod.config.getPlayerSpeed();
                                }
                            }
                            else
                            {
                                if(c.entityInfo.speed > fastestEnemySpeed)
                                {
                                    fastestEnemySpeed = c.entityInfo.speed;
                                }
                            }
                        }
                    }
                    else
                    {
                        for(Combatant c : sideA.values())
                        {
                            if(c.entity instanceof EntityPlayer)
                            {
                                if(TurnBasedMinecraftMod.config.getPlayerSpeed() > fastestEnemySpeed)
                                {
                                    fastestEnemySpeed = TurnBasedMinecraftMod.config.getPlayerSpeed();
                                }
                            }
                            else
                            {
                                if(c.entityInfo.speed > fastestEnemySpeed)
                                {
                                    fastestEnemySpeed = c.entityInfo.speed;
                                }
                            }
                        }
                    }
                    int fleeProbability = 0;
                    if(next.entity instanceof EntityPlayer)
                    {
                        if(fastestEnemySpeed >= TurnBasedMinecraftMod.config.getPlayerSpeed())
                        {
                            fleeProbability = TurnBasedMinecraftMod.config.getFleeBadProbability();
                        }
                        else
                        {
                            fleeProbability = TurnBasedMinecraftMod.config.getFleeGoodProbability();
                        }
                    }
                    else
                    {
                        if(fastestEnemySpeed >= next.entityInfo.speed)
                        {
                            fleeProbability = TurnBasedMinecraftMod.config.getFleeBadProbability();
                        }
                        else
                        {
                            fleeProbability = TurnBasedMinecraftMod.config.getFleeGoodProbability();
                        }
                    }
                    if((int)(Math.random() * 100) < fleeProbability)
                    {
                        // flee success
                        if(next.isSideA)
                        {
                            sideA.remove(next.entity.getEntityId());
                        }
                        else
                        {
                            sideB.remove(next.entity.getEntityId());
                        }
                        if(next.entity instanceof EntityPlayer)
                        {
                            players.remove(next.entity.getEntityId());
                            playerCount.decrementAndGet();
                            // TODO notify player exited battle
                        }
                    }
                    break;
                case USE_ITEM:
                    break;
                }
                next = turnOrderQueue.poll();
            }
            for(Combatant c : sideA.values())
            {
                c.decision = Decision.UNDECIDED;
            }
            for(Combatant c : sideB.values())
            {
                c.decision = Decision.UNDECIDED;
            }
            state = State.HEALTH_CHECK;
            update(Duration.ZERO);
            break;
        }
        case HEALTH_CHECK:
            // TODO
            break;
        }
    }
}
