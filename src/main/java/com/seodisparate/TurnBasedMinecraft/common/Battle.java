package com.seodisparate.TurnBasedMinecraft.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleInfo;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleMessage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemLingeringPotion;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSplashPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

public class Battle
{
    private final int id;
    private Map<Integer, Combatant> sideA;
    private Map<Integer, Combatant> sideB;
    private Map<Integer, Combatant> players;
    private PriorityQueue<Combatant> turnOrderQueue;
    
    private State state;
    private AtomicInteger playerCount;
    private AtomicInteger undecidedCount;
    private long lastInstant;
    private long timer;
    
    private boolean isServer;
    private boolean battleEnded;
    
    public enum State
    {
        DECISION(0),
        ACTION(1),
        DECISION_PLAYER_READY(2);
        
        private int value;
        private static Map<Integer, State> map = new HashMap<Integer, State>();
        
        private State(int value)
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
        
        static
        {
            for(State state : State.values())
            {
                map.put(state.value, state);
            }
        }
        
        public static State valueOf(int stateType)
        {
            return map.get(stateType);
        }
    }
    
    public enum Decision
    {
        UNDECIDED(0),
        ATTACK(1),
        DEFEND(2),
        FLEE(3),
        USE_ITEM(4),
        SWITCH_ITEM(5);
        
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

    public Battle(int id, Collection<Entity> sideA, Collection<Entity> sideB, boolean isServer)
    {
        this.isServer = isServer;
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
        
        for(Combatant c : this.sideA.values())
        {
            if(c.entityInfo != null)
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id, c.entityInfo.category);
            }
            else
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id);
            }
        }
        for(Combatant c : this.sideB.values())
        {
            if(c.entityInfo != null)
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id, c.entityInfo.category);
            }
            else
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id);
            }
        }
        
        lastInstant = System.nanoTime();
        state = State.DECISION;
        undecidedCount.set(playerCount.get());
        timer = TurnBasedMinecraftMod.BattleDecisionTime.getSeconds() * 1000000000;
        battleEnded = false;
        
        notifyPlayersBattleInfo();
    }

    public int getId()
    {
        return id;
    }
    
    public Entity getCombatantEntity(int entityID)
    {
        Combatant c = sideA.get(entityID);
        if(c != null)
        {
            return c.entity;
        }
        c = sideB.get(entityID);
        if(c != null)
        {
            return c.entity;
        }
        return null;
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
        if(newCombatant.entityInfo != null)
        {
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id, newCombatant.entityInfo.category);
        }
        else
        {
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id);
        }
        notifyPlayersBattleInfo();
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
        if(newCombatant.entityInfo != null)
        {
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id, newCombatant.entityInfo.category);
        }
        else
        {
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id);
        }
        notifyPlayersBattleInfo();
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
    
    public Set<Map.Entry<Integer, Combatant>> getSideAEntrySet()
    {
        return sideA.entrySet();
    }
    
    public Set<Map.Entry<Integer, Combatant>> getSideBEntrySet()
    {
        return sideB.entrySet();
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
        else if(decision == Decision.USE_ITEM || decision == Decision.SWITCH_ITEM)
        {
            combatant.itemToUse = targetIDOrItemID;
        }
        undecidedCount.decrementAndGet();
    }
    
    public State getState()
    {
        return state;
    }
    
    public void setState(State state)
    {
        this.state = state;
    }
    
    public long getTimerSeconds()
    {
        return timer / 1000000000;
    }
    
    public int getSize()
    {
        return sideA.size() + sideB.size();
    }
    
    protected void notifyPlayersBattleInfo()
    {
        if(!isServer)
        {
            return;
        }
        PacketBattleInfo infoPacket = new PacketBattleInfo(getSideAIDs(), getSideBIDs(), timer);
        for(Combatant p : players.values())
        {
            TurnBasedMinecraftMod.NWINSTANCE.sendTo(infoPacket, (EntityPlayerMP)p.entity);
        }
    }
    
    private void sendMessageToAllPlayers(PacketBattleMessage.MessageType type, int from, int to, int amount)
    {
        if(!isServer)
        {
            return;
        }
        for(Combatant p : players.values())
        {
            if(p.entity.isEntityAlive())
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketBattleMessage(type, from, to, amount), (EntityPlayerMP)p.entity);
            }
        }
    }
    
    private void sendMessageToAllPlayers(PacketBattleMessage.MessageType type, int from, int to, int amount, String custom)
    {
        if(!isServer)
        {
            return;
        }
        for(Combatant p : players.values())
        {
            if(p.entity.isEntityAlive())
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketBattleMessage(type, from, to, amount, custom), (EntityPlayerMP)p.entity);
            }
        }
    }
    
    /**
     * @return true if at least one combatant was removed
     */
    private boolean healthCheck()
    {
        Queue<Integer> removeQueue = new ArrayDeque<Integer>();
        for(Combatant c : sideA.values())
        {
            if(!c.entity.isEntityAlive())
            {
                removeQueue.add(c.entity.getEntityId());
                if(c.entity instanceof EntityPlayer)
                {
                    TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketBattleMessage(PacketBattleMessage.MessageType.ENDED, c.entity.getEntityId(), 0, 0), (EntityPlayerMP)c.entity);
                }
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.DIED, c.entity.getEntityId(), 0, 0);
            }
        }
        for(Combatant c : sideB.values())
        {
            if(!c.entity.isEntityAlive())
            {
                removeQueue.add(c.entity.getEntityId());
                if(c.entity instanceof EntityPlayer)
                {
                    TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketBattleMessage(PacketBattleMessage.MessageType.ENDED, c.entity.getEntityId(), 0, 0), (EntityPlayerMP)c.entity);
                }
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.DIED, c.entity.getEntityId(), 0, 0);
            }
        }
        boolean didRemove = !removeQueue.isEmpty();
        Integer toRemove = removeQueue.poll();
        while(toRemove != null)
        {
            sideA.remove(toRemove);
            sideB.remove(toRemove);
            if(players.remove(toRemove) != null)
            {
                playerCount.decrementAndGet();
            }
            toRemove = removeQueue.poll();
        }
        if(players.isEmpty() || sideA.isEmpty() || sideB.isEmpty())
        {
            battleEnded = true;
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENDED, 0, 0, 0);
        }
        else if(didRemove)
        {
            notifyPlayersBattleInfo();
        }
        
        return didRemove;
    }
    
    private void isCreativeCheck()
    {
        Queue<Integer> removeQueue = new ArrayDeque<Integer>();
        for(Combatant c : players.values())
        {
            if(c.entity != null && ((EntityPlayer)c.entity).isCreative())
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketBattleMessage(PacketBattleMessage.MessageType.ENDED, c.entity.getEntityId(), 0, 0), (EntityPlayerMP)c.entity);
                removeQueue.add(c.entity.getEntityId());
            }
        }
        Integer toRemove = removeQueue.poll();
        while(toRemove != null)
        {
            sideA.remove(toRemove);
            sideB.remove(toRemove);
            players.remove(toRemove);
            playerCount.decrementAndGet();
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.BECAME_CREATIVE, toRemove, 0, 0);
        }
    }
    
    /**
     * @return True if battle has ended
     */
    public boolean update()
    {
        if(battleEnded)
        {
            return true;
        }
        long nextInstant = System.nanoTime();
        long dt = nextInstant - lastInstant;
        lastInstant = nextInstant;
        return update(dt);
    }
    
    private boolean update(final long dt)
    {
        if(battleEnded)
        {
            return true;
        }
        switch(state)
        {
        case DECISION:
            timer -= dt;
            if(timer <= 0 || undecidedCount.get() <= 0)
            {
                for(Combatant c : sideA.values())
                {
                    // picking decision for sideA non-players
                    if(!(c.entity instanceof EntityPlayer) && c.decision == Decision.UNDECIDED && c.entityInfo != null)
                    {
                        int percentage = (int)(Math.random() * 100);
                        if(percentage < c.entityInfo.decisionAttack)
                        {
                            c.decision = Decision.ATTACK;
                        }
                        else if(percentage - c.entityInfo.decisionAttack < c.entityInfo.decisionDefend)
                        {
                            c.decision = Decision.DEFEND;
                        }
                        else if(percentage - c.entityInfo.decisionAttack - c.entityInfo.decisionDefend < c.entityInfo.decisionFlee)
                        {
                            c.decision = Decision.FLEE;
                        }
                    }
                }
                for(Combatant c : sideB.values())
                {
                    if(!(c.entity instanceof EntityPlayer) && c.decision == Decision.UNDECIDED && c.entityInfo != null)
                    {
                        int percentage = (int)(Math.random() * 100);
                        if(percentage < c.entityInfo.decisionAttack)
                        {
                            c.decision = Decision.ATTACK;
                        }
                        else if(percentage - c.entityInfo.decisionAttack < c.entityInfo.decisionDefend)
                        {
                            c.decision = Decision.DEFEND;
                        }
                        else if(percentage - c.entityInfo.decisionAttack - c.entityInfo.decisionDefend < c.entityInfo.decisionFlee)
                        {
                            c.decision = Decision.FLEE;
                        }
                    }
                }
                state = State.ACTION;
                timer = TurnBasedMinecraftMod.BattleDecisionTime.getSeconds() * 1000000000;
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.TURN_BEGIN, 0, 0, 0);
                turnOrderQueue.clear();
                for(Combatant c : sideA.values())
                {
                    turnOrderQueue.add(c);
                }
                for(Combatant c : sideB.values())
                {
                    turnOrderQueue.add(c);
                }
                update(0);
            }
            else
            {
                healthCheck();
				isCreativeCheck();
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
                    
                    next.remainingDefenses = 0;
                    
                    switch(next.decision)
                    {
                    case UNDECIDED:
                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.DID_NOTHING, next.entity.getEntityId(), 0, 0);
                        break;
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
                                break;
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
                            if(hitChance < TurnBasedMinecraftMod.config.getMinimumHitPercentage())
                            {
                                hitChance = TurnBasedMinecraftMod.config.getMinimumHitPercentage();
                            }
                            if((int)(Math.random() * 100) < hitChance)
                            {
                                if(target.remainingDefenses <= 0)
                                {
                                    // attack
                                    // TODO damage via bow and arrow
                                    // have player look at attack target
                                    final Entity nextEntity = next.entity;
                                    final Entity targetEntity = target.entity;
                                    next.entity.getServer().addScheduledTask(() -> {
                                        ((EntityPlayerMP)nextEntity).connection.setPlayerLocation(nextEntity.posX, nextEntity.posY, nextEntity.posZ, Utility.yawDirection(nextEntity.posX, nextEntity.posZ, targetEntity.posX, targetEntity.posZ), Utility.pitchDirection(nextEntity.posX, nextEntity.posY, nextEntity.posZ, targetEntity.posX, targetEntity.posY, targetEntity.posZ));
                                    });
                                    TurnBasedMinecraftMod.attackingEntity = next.entity;
                                    TurnBasedMinecraftMod.attackingDamage = 0;
                                    ((EntityPlayer)next.entity).attackTargetEntityWithCurrentItem(target.entity);
                                    TurnBasedMinecraftMod.attackingEntity = null;
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.ATTACK, next.entity.getEntityId(), target.entity.getEntityId(), TurnBasedMinecraftMod.attackingDamage);
                                    if(!(target.entity instanceof EntityPlayer) && target.entityInfo.defenseDamage > 0)
                                    {
                                        if((int)(Math.random() * 100) < target.entityInfo.defenseDamageProbability)
                                        {
                                            // defense damage
                                            DamageSource defenseDamageSource = DamageSource.causeMobDamage((EntityLivingBase)target.entity);
                                            TurnBasedMinecraftMod.attackingEntity = target.entity;
                                            next.entity.attackEntityFrom(defenseDamageSource, target.entityInfo.defenseDamage);
                                            TurnBasedMinecraftMod.attackingEntity = null;
                                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFENSE_DAMAGE, target.entity.getEntityId(), next.entity.getEntityId(), target.entityInfo.defenseDamage);
                                        }
                                    }
                                }
                                else
                                {
                                    // blocked
                                    --target.remainingDefenses;
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFEND, target.entity.getEntityId(), next.entity.getEntityId(), 0);
                                }
                            }
                            else
                            {
                                // miss
                                sendMessageToAllPlayers(PacketBattleMessage.MessageType.MISS, next.entity.getEntityId(), target.entity.getEntityId(), 0);
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
                            if(hitChance < TurnBasedMinecraftMod.config.getMinimumHitPercentage())
                            {
                                hitChance = TurnBasedMinecraftMod.config.getMinimumHitPercentage();
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
                                    target.entity.attackEntityFrom(damageSource, damageAmount);
                                    TurnBasedMinecraftMod.attackingEntity = null;
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.ATTACK, next.entity.getEntityId(), target.entity.getEntityId(), damageAmount);
                                    if(!(target.entity instanceof EntityPlayer) && target.entityInfo.defenseDamage > 0)
                                    {
                                        if((int)(Math.random() * 100) < target.entityInfo.defenseDamageProbability)
                                        {
                                            // defense damage
                                            DamageSource defenseDamageSource = DamageSource.causeMobDamage((EntityLivingBase)target.entity);
                                            TurnBasedMinecraftMod.attackingEntity = target.entity;
                                            next.entity.attackEntityFrom(defenseDamageSource, target.entityInfo.defenseDamage);
                                            TurnBasedMinecraftMod.attackingEntity = null;
                                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFENSE_DAMAGE, target.entity.getEntityId(), next.entity.getEntityId(), target.entityInfo.defenseDamage);
                                        }
                                    }
                                    // attack effect
                                    if(next.entityInfo.attackEffect != EntityInfo.Effect.UNKNOWN && next.entityInfo.attackEffectProbability > 0)
                                    {
                                        int effectChance = (int)(Math.random() * 100);
                                        if(effectChance < next.entityInfo.attackEffectProbability)
                                        {
                                            next.entityInfo.attackEffect.applyEffectToEntity((EntityLivingBase)target.entity);
                                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.WAS_AFFECTED, next.entity.getEntityId(), target.entity.getEntityId(), 0, next.entityInfo.attackEffect.getAffectedString());
                                        }
                                    }
                                }
                                else
                                {
                                    // blocked
                                    --target.remainingDefenses;
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFEND, target.entity.getEntityId(), next.entity.getEntityId(), 0);
                                }
                            }
                            else
                            {
                                // miss
                                sendMessageToAllPlayers(PacketBattleMessage.MessageType.MISS, next.entity.getEntityId(), target.entity.getEntityId(), 0);
                            }
                        }
                        break;
                    case DEFEND:
                        next.remainingDefenses = TurnBasedMinecraftMod.config.getDefenseDuration();
                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFENDING, next.entity.getEntityId(), 0, 0);
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
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.FLEE, next.entity.getEntityId(), 0, 1);
                            if(next.entity instanceof EntityPlayer)
                            {
                                players.remove(next.entity.getEntityId());
                                playerCount.decrementAndGet();
                                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketBattleMessage(PacketBattleMessage.MessageType.ENDED, 0, 0, 0), (EntityPlayerMP)next.entity);
                            }
                        }
                        else
                        {
                            // flee fail
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.FLEE, next.entity.getEntityId(), 0, 0);
                        }
                        break;
                    case USE_ITEM:
                        if(next.itemToUse < 0 || next.itemToUse > 8)
                        {
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_INVALID.getValue());
                            break;
                        }
                        ItemStack targetItemStack = ((EntityPlayer)next.entity).inventory.getStackInSlot(next.itemToUse);
                        Item targetItem = targetItemStack.getItem();
                        if(targetItem == null)
                        {
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_NOTHING.getValue());
                            break;
                        }
                        if(targetItem instanceof ItemFood)
                        {
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_FOOD.getValue(), targetItemStack.getDisplayName());
                            ((ItemFood)targetItem).onItemUseFinish(targetItemStack, next.entity.world, (EntityLivingBase)next.entity);
                        }
                        else if(targetItem instanceof ItemPotion && !(targetItem instanceof ItemSplashPotion) && !(targetItem instanceof ItemLingeringPotion))
                        {
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_POTION.getValue(), targetItemStack.getDisplayName());
                            ((ItemPotion)targetItem).onItemUseFinish(targetItemStack, next.entity.world, (EntityLivingBase)next.entity);
                            ((EntityPlayer)next.entity).inventory.setInventorySlotContents(next.itemToUse, new ItemStack(Items.GLASS_BOTTLE));
                        }
                        else
                        {
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_INVALID.getValue(), targetItemStack.getDisplayName());
                        }
                        break;
                    case SWITCH_ITEM:
                        if(next.itemToUse < 0 || next.itemToUse > 8)
                        {
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.SWITCHED_ITEM, next.entity.getEntityId(), 0, 0);
                            break;
                        }
                        ((EntityPlayer)next.entity).inventory.currentItem = next.itemToUse;
                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.SWITCHED_ITEM, next.entity.getEntityId(), 0, 1);
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
                state = State.DECISION;
                undecidedCount.set(players.size());
                healthCheck();
				isCreativeCheck();
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.TURN_END, 0, 0, 0);
                break;
            } // case ACTION
        default:
            state = State.DECISION;
            break;
        } // switch(state)
        return battleEnded;
    } // update(final long dt)
}
