package com.burnedkirby.TurnBasedMinecraft.common;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketBattleInfo;
import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketBattleMessage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.DamageSource;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.PacketDistributor;

public class Battle
{
    private final int id;
    private Map<Integer, Combatant> sideA;
    private Map<Integer, Combatant> sideB;
    private Map<Integer, Combatant> players;
    private PriorityQueue<Combatant> turnOrderQueue;
    private Queue<Combatant> sideAEntryQueue;
    private Queue<Combatant> sideBEntryQueue;
    
    private State state;
    private AtomicInteger playerCount;
    private AtomicInteger undecidedCount;
    private long lastInstant;
    private long timer;
    
    private boolean isServer;
    private boolean battleEnded;
    
    private BattleManager battleManager;

    private Random random;
    
    public String debugLog; // TODO remove after freeze bug has been found

    private DimensionType dimension;
    
    public enum State
    {
        DECISION(0),
        ACTION(1),
        DECISION_PLAYER_READY(2);
        
        private int value;
        private static Map<Integer, State> map = new HashMap<Integer, State>();
        
        State(int value)
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
        
        Decision(int value)
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

    public Battle(BattleManager battleManager, int id, Collection<Entity> sideA, Collection<Entity> sideB, boolean isServer, DimensionType dimension)
    {
        this.battleManager = battleManager;
        this.isServer = isServer;
        this.id = id;
        this.sideA = new ConcurrentHashMap<Integer, Combatant>();
        this.sideB = new ConcurrentHashMap<Integer, Combatant>();
        players = new ConcurrentHashMap<Integer, Combatant>();
        turnOrderQueue = new PriorityQueue<Combatant>(new Combatant.CombatantComparator());
        sideAEntryQueue = new ArrayDeque<Combatant>();
        sideBEntryQueue = new ArrayDeque<Combatant>();
        playerCount = new AtomicInteger(0);
        undecidedCount = new AtomicInteger(0);
        random = new Random();
        this.dimension = dimension;
        if(sideA != null)
        {
            for(Entity e : sideA)
            {
                EntityInfo entityInfo;
                try {
                    entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getCustomEntityInfoReference(e.getCustomName().getUnformattedComponentText());
                } catch(NullPointerException exception) {
                    entityInfo = null;
                }
                if(entityInfo == null)
                {
                    entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(e);
                }

                if(entityInfo == null && !(e instanceof PlayerEntity) && TurnBasedMinecraftMod.proxy.isServerRunning())
                {
                    continue;
                }
                Combatant newCombatant = new Combatant(e, entityInfo);
                newCombatant.isSideA = true;
                newCombatant.battleID = getId();
                this.sideA.put(e.getEntityId(), newCombatant);
                if(e instanceof PlayerEntity)
                {
                    newCombatant.recalcSpeedOnCompare = true;
                    playerCount.incrementAndGet();
                    players.put(e.getEntityId(), newCombatant);
                }
                if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
                {
                    newCombatant.x = e.posX;
                    newCombatant.z = e.posZ;
                    newCombatant.yaw = e.rotationYaw;
                    newCombatant.pitch = e.rotationPitch;
                }
            }
        }
        if(sideB != null)
        {
            for(Entity e : sideB)
            {
                EntityInfo entityInfo;
                try {
                    entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getCustomEntityInfoReference(e.getCustomName().getUnformattedComponentText());
                } catch(NullPointerException exception) {
                    entityInfo = null;
                }
                if(entityInfo == null)
                {
                    entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(e);
                }

                if(entityInfo == null && !(e instanceof PlayerEntity) && TurnBasedMinecraftMod.proxy.isServerRunning())
                {
                    continue;
                }
                Combatant newCombatant = new Combatant(e, entityInfo);
                newCombatant.isSideA = false;
                newCombatant.battleID = getId();
                this.sideB.put(e.getEntityId(), newCombatant);
                if(e instanceof PlayerEntity)
                {
                    newCombatant.recalcSpeedOnCompare = true;
                    playerCount.incrementAndGet();
                    players.put(e.getEntityId(), newCombatant);
                }
                if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
                {
                    newCombatant.x = e.posX;
                    newCombatant.z = e.posZ;
                    newCombatant.yaw = e.rotationYaw;
                    newCombatant.pitch = e.rotationPitch;
                }
            }
        }
        
        if(isServer)
        {
            for(Combatant c : this.sideA.values())
            {
                if(c.entityInfo != null)
                {
                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id, c.entityInfo.category);
                }
                else if(c.entity instanceof PlayerEntity)
                {
                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id, "player");
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
                else if(c.entity instanceof PlayerEntity)
                {
                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id, "player");
                }
                else
                {
                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id);
                }
            }
        }
        
        lastInstant = System.nanoTime();
        state = State.DECISION;
        undecidedCount.set(playerCount.get());
        timer = TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos();
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
        synchronized(sideAEntryQueue)
        {
            for(Combatant c : sideAEntryQueue)
            {
                if(c.entity.getEntityId() == entityID)
                {
                    return true;
                }
            }
        }
        synchronized(sideBEntryQueue)
        {
            for(Combatant c : sideBEntryQueue)
            {
                if(c.entity.getEntityId() == entityID)
                {
                    return true;
                }
            }
        }
        return sideA.containsKey(entityID) || sideB.containsKey(entityID);
    }
    
    public boolean hasCombatantInSideA(int entityID)
    {
        synchronized(sideAEntryQueue)
        {
            for(Combatant c : sideAEntryQueue)
            {
                if(c.entity.getEntityId() == entityID)
                {
                    return true;
                }
            }
        }
        return sideA.containsKey(entityID);
    }
    
    public void addCombatantToSideA(Entity e)
    {
        EntityInfo entityInfo;
        try {
            entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getCustomEntityInfoReference(e.getCustomName().getUnformattedComponentText());
        } catch(NullPointerException exception) {
            entityInfo = null;
        }
        if(entityInfo == null)
        {
            entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(e);
        }

        if(entityInfo == null && !(e instanceof PlayerEntity) && TurnBasedMinecraftMod.proxy.isServerRunning())
        {
            return;
        }
        Combatant newCombatant = new Combatant(e, entityInfo);
        newCombatant.isSideA = true;
        newCombatant.battleID = getId();
        if(isServer)
        {
            synchronized(sideAEntryQueue)
            {
                sideAEntryQueue.add(newCombatant);
            }
        }
        else
        {
            sideA.put(e.getEntityId(), newCombatant);
        }
        if(e instanceof PlayerEntity)
        {
            newCombatant.recalcSpeedOnCompare = true;
            playerCount.incrementAndGet();
            players.put(e.getEntityId(), newCombatant);
            if(state == State.DECISION)
            {
                undecidedCount.incrementAndGet();
            }
        }
        if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
        {
            newCombatant.x = e.posX;
            newCombatant.z = e.posZ;
            newCombatant.yaw = e.rotationYaw;
            newCombatant.pitch = e.rotationPitch;
        }
        if(isServer)
        {
            if(newCombatant.entityInfo != null)
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id, newCombatant.entityInfo.category);
            }
            else if(newCombatant.entity instanceof PlayerEntity)
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id, "player");
            }
            else
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id);
            }
        }
        notifyPlayersBattleInfo();
    }
    
    public void addCombatantToSideB(Entity e)
    {
        EntityInfo entityInfo;
        try {
            entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getCustomEntityInfoReference(e.getCustomName().getUnformattedComponentText());
        } catch(NullPointerException exception) {
            entityInfo = null;
        }
        if(entityInfo == null)
        {
            entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(e);
        }

        if(entityInfo == null && !(e instanceof PlayerEntity) && TurnBasedMinecraftMod.proxy.isServerRunning())
        {
            return;
        }
        Combatant newCombatant = new Combatant(e, entityInfo);
        newCombatant.isSideA = false;
        newCombatant.battleID = getId();
        if(isServer)
        {
            synchronized(sideBEntryQueue)
            {
                sideBEntryQueue.add(newCombatant);
            }
        }
        else
        {
            sideB.put(e.getEntityId(), newCombatant);
        }
        if(e instanceof PlayerEntity)
        {
            newCombatant.recalcSpeedOnCompare = true;
            playerCount.incrementAndGet();
            players.put(e.getEntityId(), newCombatant);
            if(state == State.DECISION)
            {
                undecidedCount.incrementAndGet();
            }
        }
        if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
        {
            newCombatant.x = e.posX;
            newCombatant.z = e.posZ;
            newCombatant.yaw = e.rotationYaw;
            newCombatant.pitch = e.rotationPitch;
        }
        if(isServer)
        {
            if(newCombatant.entityInfo != null)
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id, newCombatant.entityInfo.category);
            }
            else if(newCombatant.entity instanceof PlayerEntity)
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id, "player");
            }
            else
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id);
            }
        }
        notifyPlayersBattleInfo();
    }
    
    public void clearCombatants()
    {
        sideA.clear();
        sideB.clear();
        sideAEntryQueue.clear();
        sideBEntryQueue.clear();
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
        int size = sideA.size() + sideB.size();
        synchronized(sideAEntryQueue)
        {
            size += sideAEntryQueue.size();
        }
        synchronized(sideBEntryQueue)
        {
            size += sideBEntryQueue.size();
        }
        return size;
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
            TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)p.entity), infoPacket);
        }
    }
    
    protected void sendMessageToAllPlayers(PacketBattleMessage.MessageType type, int from, int to, int amount)
    {
        sendMessageToAllPlayers(type, from, to, amount, new String());
    }
    
    protected void sendMessageToAllPlayers(PacketBattleMessage.MessageType type, int from, int to, int amount, String custom)
    {
        if(!isServer)
        {
            return;
        }
        PacketBattleMessage packet = new PacketBattleMessage(type, from, to, dimension, amount, custom);
        for(Combatant p : players.values())
        {
            if(p.entity.isAlive())
            {
                TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) p.entity), packet);
            }
        }
    }
    
    /**
     * @return true if at least one combatant was removed
     */
    private boolean healthCheck()
    {
        boolean didRemove = false;
        for(Iterator<Map.Entry<Integer, Combatant>> iter = sideA.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry<Integer, Combatant> entry = iter.next();
            if(!entry.getValue().entity.isAlive())
            {
                iter.remove();
                players.remove(entry.getKey());
                removeCombatantPostRemove(entry.getValue());
                didRemove = true;
                String category = null;
                if(entry.getValue().entityInfo != null)
                {
                    category = entry.getValue().entityInfo.category;
                }
                else if(entry.getValue().entity instanceof PlayerEntity)
                {
                    category = "player";
                }
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.DIED, entry.getValue().entity.getEntityId(), 0, 0, category);
            }
        }
        for(Iterator<Map.Entry<Integer, Combatant>> iter = sideB.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry<Integer, Combatant> entry = iter.next();
            if(!entry.getValue().entity.isAlive())
            {
                iter.remove();
                players.remove(entry.getKey());
                removeCombatantPostRemove(entry.getValue());
                didRemove = true;
                String category = null;
                if(entry.getValue().entityInfo != null)
                {
                    category = entry.getValue().entityInfo.category;
                }
                else if(entry.getValue().entity instanceof PlayerEntity)
                {
                    category = "player";
                }
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.DIED, entry.getValue().entity.getEntityId(), 0, 0, category);
            }
        }
        if(players.isEmpty() || sideA.isEmpty() || sideB.isEmpty())
        {
            battleEnded = true;
        }
        else if(didRemove)
        {
            resetUndecidedCount();
        }
        
        return didRemove;
    }
    
    /**
     * @return true if at least one combatant was removed
     */
    private boolean isCreativeCheck()
    {
        boolean didRemove = false;
        for(Iterator<Map.Entry<Integer, Combatant>> iter = players.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry<Integer, Combatant> entry = iter.next();
            if(entry.getValue().entity != null && ((PlayerEntity)entry.getValue().entity).isCreative())
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.BECAME_CREATIVE, entry.getValue().entity.getEntityId(), 0, 0);
                iter.remove();
                sideA.remove(entry.getKey());
                sideB.remove(entry.getKey());
                playerCount.decrementAndGet();
                removeCombatantPostRemove(entry.getValue());
                didRemove = true;
            }
        }
        if(didRemove)
        {
            resetUndecidedCount();
        }
        
        return didRemove;
    }
    
    private void resetUndecidedCount()
    {
        if(state == State.DECISION)
        {
            undecidedCount.set(0);
            for(Combatant p : players.values())
            {
                if(p.decision == Decision.UNDECIDED)
                {
                    undecidedCount.incrementAndGet();
                }
            }
        }
    }
    
    private void enforceFreezePositions()
    {
        for(Combatant c : sideA.values())
        {
            c.entity.setPositionAndRotation(c.x, c.entity.posY, c.z, c.yaw, c.pitch);
        }
        for(Combatant c : sideB.values())
        {
            c.entity.setPositionAndRotation(c.x, c.entity.posY, c.z, c.yaw, c.pitch);
        }
    }
    
    private void removeCombatant(Combatant c)
    {
        sideA.remove(c.entity.getEntityId());
        sideB.remove(c.entity.getEntityId());
        if(players.remove(c.entity.getEntityId()) != null)
        {
            playerCount.decrementAndGet();
        }
        removeCombatantPostRemove(c);
    }
    
    private void removeCombatantPostRemove(Combatant c)
    {
        if(c.entity instanceof PlayerEntity)
        {
            TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) c.entity), new PacketBattleMessage(PacketBattleMessage.MessageType.ENDED, 0, 0, dimension, 0));
        }
        battleManager.addRecentlyLeftBattle(c);
    }

    public void forceRemoveCombatant(EntityIDDimPair e) {
        sideA.remove(e.id);
        sideB.remove(e.id);
        if(players.remove(e.id) != null) {
            playerCount.decrementAndGet();
        }
        for(Iterator<Combatant> iter = sideAEntryQueue.iterator(); iter.hasNext();) {
            Combatant c = iter.next();
            if(c.entity.getEntityId() == e.id) {
                iter.remove();
                break;
            }
        }
        for(Iterator<Combatant> iter = sideBEntryQueue.iterator(); iter.hasNext();) {
            Combatant c = iter.next();
            if(c.entity.getEntityId() == e.id) {
                iter.remove();
                break;
            }
        }
    }

    private void setDecisionState()
    {
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
    }

    /**
     * @return True if battle has ended
     */
    public boolean update()
    {
        if(!isServer)
        {
            return false;
        }
        else if(battleEnded)
        {
            Collection<Combatant> combatants = new ArrayList<Combatant>();
            combatants.addAll(sideA.values());
            combatants.addAll(sideB.values());
            for(Combatant c : combatants)
            {
                removeCombatant(c);
            }
            return true;
        }
        long nextInstant = System.nanoTime();
        long dt = nextInstant - lastInstant;
        lastInstant = nextInstant;
        try
        {
            return update(dt);
        } catch (Throwable t)
        {
            TurnBasedMinecraftMod.logger.error("Update: ", t);
            setDecisionState();
            boolean changed = false;
            if(healthCheck())
            {
                changed = true;
            }
            if(isCreativeCheck())
            {
                changed = true;
            }
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.TURN_END, 0, 0, 1);
            if(changed)
            {
                notifyPlayersBattleInfo();
            }
            return battleEnded;
        }
    }
    
    private boolean update(final long dt)
    {
        if(battleEnded)
        {
            Collection<Combatant> combatants = new ArrayList<Combatant>();
            combatants.addAll(sideA.values());
            combatants.addAll(sideB.values());
            for(Combatant c : combatants)
            {
                removeCombatant(c);
            }
            return true;
        }
        boolean combatantsChanged = false;
        synchronized(sideAEntryQueue)
        {
            for(Combatant c = sideAEntryQueue.poll(); c != null; c = sideAEntryQueue.poll())
            {
                sideA.put(c.entity.getEntityId(), c);
                combatantsChanged = true;
            }
        }
        synchronized(sideBEntryQueue)
        {
            for(Combatant c = sideBEntryQueue.poll(); c != null; c = sideBEntryQueue.poll())
            {
                sideB.put(c.entity.getEntityId(), c);
                combatantsChanged = true;
            }
        }
        if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
        {
            enforceFreezePositions();
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
                    if(!(c.entity instanceof PlayerEntity) && c.decision == Decision.UNDECIDED && c.entityInfo != null)
                    {
                        int percentage = random.nextInt(100);
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
                    if(!(c.entity instanceof PlayerEntity) && c.decision == Decision.UNDECIDED && c.entityInfo != null)
                    {
                        int percentage = random.nextInt(100);
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
                timer = TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos();
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
                return update(0);
            }
            else
            {
                if(healthCheck())
                {
                    combatantsChanged = true;
                }
                if(isCreativeCheck())
                {
                    combatantsChanged = true;
                }
            }
            break;
        case ACTION:
            {
                for(Combatant next = turnOrderQueue.poll(); next != null; next = turnOrderQueue.poll())
                {
                    if(!next.entity.isAlive())
                    {
                        continue;
                    }

                    debugLog = next.entity.getDisplayName().getUnformattedComponentText();

                    next.remainingDefenses = 0;

                    Decision decision = next.decision;
                    next.decision = Decision.UNDECIDED;
                    
                    switch(decision)
                    {
                    case UNDECIDED:
                        debugLog += " undecided";
                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.DID_NOTHING, next.entity.getEntityId(), 0, 0);
                        break;
                    case ATTACK:
                        debugLog += " attack";
                        Combatant target = null;
                        if(next.entity instanceof PlayerEntity)
                        {
                            debugLog += " as player";
                            target = sideA.get(next.targetEntityID);
                            if(target == null)
                            {
                                target = sideB.get(next.targetEntityID);
                            }
                            if(target == null || !target.entity.isAlive() || target == next)
                            {
                                continue;
                            }
                            ItemStack heldItemStack = ((PlayerEntity)next.entity).getHeldItemMainhand();
                            if(heldItemStack.getItem() instanceof BowItem)
                            {
                                debugLog += " with bow";
                                if(Utility.doesPlayerHaveArrows((PlayerEntity)next.entity))
                                {
                                    final Entity nextEntity = next.entity;
                                    final Entity targetEntity = target.entity;
                                    final float yawDirection = Utility.yawDirection(next.entity.posX, next.entity.posZ, target.entity.posX, target.entity.posZ);
                                    final float pitchDirection = Utility.pitchDirection(next.entity.posX, next.entity.posY, next.entity.posZ, target.entity.posX, target.entity.posY, target.entity.posZ);
                                    final int randomTimeLeft = random.nextInt(heldItemStack.getItem().getUseDuration(heldItemStack) / 3);
                                    if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
                                    {
                                        next.yaw = yawDirection;
                                        next.pitch = pitchDirection;
                                    }
                                    // have player look at attack target
                                    ((ServerPlayerEntity)nextEntity).connection.setPlayerLocation(nextEntity.posX, nextEntity.posY, nextEntity.posZ, yawDirection, pitchDirection);
                                    BowItem itemBow = (BowItem)heldItemStack.getItem();
                                    synchronized(TurnBasedMinecraftMod.proxy.getAttackerViaBowSet())
                                    {
                                        TurnBasedMinecraftMod.proxy.getAttackerViaBowSet().add(new AttackerViaBow(nextEntity, getId()));
                                    }
                                    itemBow.onPlayerStoppedUsing(((PlayerEntity)nextEntity).getHeldItemMainhand(), nextEntity.getEntityWorld(), (LivingEntity) nextEntity, randomTimeLeft);
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.FIRED_ARROW, nextEntity.getEntityId(), targetEntity.getEntityId(), 0);
                                }
                                else
                                {
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.BOW_NO_AMMO, next.entity.getEntityId(), 0, 0);
                                }
                                continue;
                            }
                            debugLog += " without bow";
                            int hitChance = TurnBasedMinecraftMod.proxy.getConfig().getPlayerAttackProbability();
                            if(target.entity instanceof PlayerEntity)
                            {
                                hitChance = hitChance * (100 - TurnBasedMinecraftMod.proxy.getConfig().getPlayerEvasion()) / 100;
                            }
                            else
                            {
                                hitChance = hitChance * (100 - target.entityInfo.evasion) / 100;
                            }
                            if(hitChance < TurnBasedMinecraftMod.proxy.getConfig().getMinimumHitPercentage())
                            {
                                hitChance = TurnBasedMinecraftMod.proxy.getConfig().getMinimumHitPercentage();
                            }
                            if(random.nextInt(100) < hitChance)
                            {
                                if(target.remainingDefenses <= 0)
                                {
                                    debugLog += " hit success";
                                    // attack
                                    final Entity nextEntity = next.entity;
                                    final Entity targetEntity = target.entity;
                                    final EntityInfo targetEntityInfo = target.entityInfo;
                                    final float yawDirection = Utility.yawDirection(next.entity.posX, next.entity.posZ, target.entity.posX, target.entity.posZ);
                                    final float pitchDirection = Utility.pitchDirection(next.entity.posX, next.entity.posY, next.entity.posZ, target.entity.posX, target.entity.posY, target.entity.posZ);
                                    final boolean defenseDamageTriggered;
                                    if(!(targetEntity instanceof PlayerEntity) && targetEntityInfo.defenseDamage > 0 && targetEntityInfo.defenseDamageProbability > 0)
                                    {
                                        if(random.nextInt(100) < targetEntityInfo.defenseDamageProbability)
                                        {
                                            defenseDamageTriggered = true;
                                        }
                                        else
                                        {
                                            defenseDamageTriggered = false;
                                        }
                                    }
                                    else
                                    {
                                        defenseDamageTriggered = false;
                                    }
                                    if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
                                    {
                                        next.yaw = yawDirection;
                                        next.pitch = pitchDirection;
                                    }
                                    // have player look at attack target
                                    ((ServerPlayerEntity)nextEntity).connection.setPlayerLocation(nextEntity.posX, nextEntity.posY, nextEntity.posZ, yawDirection, pitchDirection);
                                    TurnBasedMinecraftMod.proxy.setAttackingEntity(nextEntity);
                                    TurnBasedMinecraftMod.proxy.setAttackingDamage(0);
                                    ((PlayerEntity)nextEntity).attackTargetEntityWithCurrentItem(targetEntity);
                                    TurnBasedMinecraftMod.proxy.setAttackingEntity(null);
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.ATTACK, nextEntity.getEntityId(), targetEntity.getEntityId(), TurnBasedMinecraftMod.proxy.getAttackingDamage());
                                    if(defenseDamageTriggered)
                                    {
                                        // defense damage
                                        DamageSource defenseDamageSource = DamageSource.causeMobDamage((LivingEntity) targetEntity);
                                        TurnBasedMinecraftMod.proxy.setAttackingEntity(targetEntity);
                                        nextEntity.attackEntityFrom(defenseDamageSource, targetEntityInfo.defenseDamage);
                                        TurnBasedMinecraftMod.proxy.setAttackingEntity(null);
                                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFENSE_DAMAGE, targetEntity.getEntityId(), nextEntity.getEntityId(), targetEntityInfo.defenseDamage);
                                    }
                                }
                                else
                                {
                                    debugLog += " hit blocked";
                                    // blocked
                                    --target.remainingDefenses;
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFEND, target.entity.getEntityId(), next.entity.getEntityId(), 0);
                                }
                            }
                            else
                            {
                                debugLog += " hit missed";
                                // miss
                                sendMessageToAllPlayers(PacketBattleMessage.MessageType.MISS, next.entity.getEntityId(), target.entity.getEntityId(), 0);
                            }
                        }
                        else
                        {
                            debugLog += " as mob";
                            LivingEntity attackTarget = ((MobEntity)next.entity).getAttackTarget();
                            if(attackTarget != null && hasCombatant(attackTarget.getEntityId()))
                            {
                                debugLog += " to targeted";
                                target = getCombatantByID(attackTarget.getEntityId());
                            }
                            else
                            {
                                debugLog += " to random other side";
                                if(next.isSideA)
                                {
                                    if(sideB.size() > 0)
                                    {
                                        int randomTargetIndex = random.nextInt(sideB.size());
                                        for(Combatant c : sideB.values())
                                        {
                                            if(randomTargetIndex-- == 0)
                                            {
                                                target = c;
                                                break;
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    if(sideA.size() > 0)
                                    {
                                        int randomTargetIndex = random.nextInt(sideA.size());
                                        for(Combatant c : sideA.values())
                                        {
                                            if(randomTargetIndex-- == 0)
                                            {
                                                target = c;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            if(target == null || !target.entity.isAlive() || target == next)
                            {
                                continue;
                            }
                            int hitChance = next.entityInfo.attackProbability;
                            if(target.entity instanceof PlayerEntity)
                            {
                                hitChance = hitChance * (100 - TurnBasedMinecraftMod.proxy.getConfig().getPlayerEvasion()) / 100;
                            }
                            else
                            {
                                hitChance = hitChance * (100 - target.entityInfo.evasion) / 100;
                            }
                            if(hitChance < TurnBasedMinecraftMod.proxy.getConfig().getMinimumHitPercentage())
                            {
                                hitChance = TurnBasedMinecraftMod.proxy.getConfig().getMinimumHitPercentage();
                            }
                            if(random.nextInt(100) < hitChance)
                            {
                                if(target.remainingDefenses <= 0)
                                {
                                    debugLog += " hit success";
                                    DamageSource damageSource = DamageSource.causeMobDamage((LivingEntity)next.entity);
                                    int damageAmount = next.entityInfo.attackPower;
                                    if(next.entityInfo.attackVariance > 0)
                                    {
                                        damageAmount += random.nextInt(next.entityInfo.attackVariance * 2 + 1) - next.entityInfo.attackVariance;
                                    }
                                    if(damageAmount < 0)
                                    {
                                        damageAmount = 0;
                                    }
                                    // attack
                                    final Entity nextEntity = next.entity;
                                    final EntityInfo nextEntityInfo = next.entityInfo;
                                    final Entity targetEntity = target.entity;
                                    final EntityInfo targetEntityInfo = target.entityInfo;
                                    final int finalDamageAmount = damageAmount;
                                    final boolean defenseDamageTriggered;
                                    final boolean attackEffectTriggered;

                                    if(!(targetEntity instanceof PlayerEntity) && targetEntityInfo.defenseDamage > 0 && targetEntityInfo.defenseDamageProbability > 0)
                                    {
                                        if(random.nextInt(100) < targetEntityInfo.defenseDamageProbability)
                                        {
                                            defenseDamageTriggered = true;
                                        }
                                        else
                                        {
                                            defenseDamageTriggered = false;
                                        }
                                    }
                                    else
                                    {
                                        defenseDamageTriggered = false;
                                    }

                                    if(nextEntityInfo.attackEffect != EntityInfo.Effect.UNKNOWN && nextEntityInfo.attackEffectProbability > 0)
                                    {
                                        if(random.nextInt(100) < nextEntityInfo.attackEffectProbability)
                                        {
                                            attackEffectTriggered = true;
                                        }
                                        else
                                        {
                                            attackEffectTriggered = false;
                                        }
                                    }
                                    else
                                    {
                                        attackEffectTriggered = false;
                                    }

                                    TurnBasedMinecraftMod.proxy.setAttackingEntity(nextEntity);
                                    targetEntity.attackEntityFrom(damageSource, finalDamageAmount);
                                    TurnBasedMinecraftMod.proxy.setAttackingEntity(null);
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.ATTACK, nextEntity.getEntityId(), targetEntity.getEntityId(), finalDamageAmount);
                                    if(defenseDamageTriggered)
                                    {
                                        // defense damage
                                        DamageSource defenseDamageSource = DamageSource.causeMobDamage((LivingEntity)targetEntity);
                                        TurnBasedMinecraftMod.proxy.setAttackingEntity(targetEntity);
                                        nextEntity.attackEntityFrom(defenseDamageSource, targetEntityInfo.defenseDamage);
                                        TurnBasedMinecraftMod.proxy.setAttackingEntity(null);
                                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFENSE_DAMAGE, targetEntity.getEntityId(), nextEntity.getEntityId(), targetEntityInfo.defenseDamage);
                                    }
                                    // attack effect
                                    if(attackEffectTriggered)
                                    {
                                        nextEntityInfo.attackEffect.applyEffectToEntity((LivingEntity)targetEntity);
                                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.WAS_AFFECTED, nextEntity.getEntityId(), targetEntity.getEntityId(), 0, nextEntityInfo.attackEffect.getAffectedString());
                                    }
                                }
                                else
                                {
                                    debugLog += " hit blocked";
                                    // blocked
                                    --target.remainingDefenses;
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFEND, target.entity.getEntityId(), next.entity.getEntityId(), 0);
                                }
                            }
                            else
                            {
                                debugLog += " hit missed";
                                // miss
                                sendMessageToAllPlayers(PacketBattleMessage.MessageType.MISS, next.entity.getEntityId(), target.entity.getEntityId(), 0);
                            }
                        }
                        break;
                    case DEFEND:
                        debugLog += " defend";
                        next.remainingDefenses = TurnBasedMinecraftMod.proxy.getConfig().getDefenseDuration();
                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFENDING, next.entity.getEntityId(), 0, 0);
                        break;
                    case FLEE:
                        debugLog += " flee";
                        int fastestEnemySpeed = 0;
                        if(next.isSideA)
                        {
                            for(Combatant c : sideB.values())
                            {
                                if(c.entity instanceof PlayerEntity)
                                {
                                    if(TurnBasedMinecraftMod.proxy.getConfig().getPlayerSpeed() > fastestEnemySpeed)
                                    {
                                        fastestEnemySpeed = TurnBasedMinecraftMod.proxy.getConfig().getPlayerSpeed();
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
                                if(c.entity instanceof PlayerEntity)
                                {
                                    if(TurnBasedMinecraftMod.proxy.getConfig().getPlayerSpeed() > fastestEnemySpeed)
                                    {
                                        fastestEnemySpeed = TurnBasedMinecraftMod.proxy.getConfig().getPlayerSpeed();
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
                        if(next.entity instanceof PlayerEntity)
                        {
                            if(fastestEnemySpeed >= TurnBasedMinecraftMod.proxy.getConfig().getPlayerSpeed())
                            {
                                fleeProbability = TurnBasedMinecraftMod.proxy.getConfig().getFleeBadProbability();
                            }
                            else
                            {
                                fleeProbability = TurnBasedMinecraftMod.proxy.getConfig().getFleeGoodProbability();
                            }
                        }
                        else
                        {
                            if(fastestEnemySpeed >= next.entityInfo.speed)
                            {
                                fleeProbability = TurnBasedMinecraftMod.proxy.getConfig().getFleeBadProbability();
                            }
                            else
                            {
                                fleeProbability = TurnBasedMinecraftMod.proxy.getConfig().getFleeGoodProbability();
                            }
                        }
                        if(random.nextInt(100) < fleeProbability)
                        {
                            debugLog += " success";
                            // flee success
                            combatantsChanged = true;
                            String fleeingCategory = new String();
                            if(next.entityInfo != null)
                            {
                                fleeingCategory = next.entityInfo.category;
                            }
                            else if(next.entity instanceof PlayerEntity)
                            {
                                fleeingCategory = "player";
                            }
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.FLEE, next.entity.getEntityId(), 0, 1, fleeingCategory);
                            removeCombatant(next);
                        }
                        else
                        {
                            debugLog += " fail";
                            // flee fail
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.FLEE, next.entity.getEntityId(), 0, 0);
                        }
                        break;
                    case USE_ITEM:
                        debugLog += " use item";
                        if(next.itemToUse < 0 || next.itemToUse > 8)
                        {
                            debugLog += " invalid";
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_INVALID.getValue());
                            break;
                        }
                        ItemStack targetItemStack = ((PlayerEntity)next.entity).inventory.getStackInSlot(next.itemToUse);
                        Item targetItem = targetItemStack.getItem();
                        if(targetItem == null)
                        {
                            debugLog += " null";
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_NOTHING.getValue());
                            break;
                        }
                        if(targetItem.getGroup() == ItemGroup.FOOD)
                        {
                            debugLog += " food";
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_FOOD.getValue(), targetItemStack.getDisplayName().getFormattedText());
                            final Entity nextEntity = next.entity;
                            final int nextItemToUse = next.itemToUse;
                            ((PlayerEntity)nextEntity).inventory.setInventorySlotContents(nextItemToUse, targetItem.onItemUseFinish(targetItemStack, nextEntity.world, (LivingEntity)nextEntity));
                        }
                        else if(targetItem instanceof PotionItem)
                        {
                            debugLog += " potion";
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_POTION.getValue(), targetItemStack.getDisplayName().getFormattedText());
                            final Entity nextEntity = next.entity;
                            final int nextItemToUse = next.itemToUse;
                            ((PlayerEntity)nextEntity).inventory.setInventorySlotContents(nextItemToUse, targetItem.onItemUseFinish(targetItemStack, nextEntity.world, (LivingEntity)nextEntity));
                        }
                        else
                        {
                            debugLog += " non-consumable";
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_INVALID.getValue(), targetItemStack.getDisplayName().getFormattedText());
                        }
                        break;
                    case SWITCH_ITEM:
                        debugLog += " switch item";
                        if(next.itemToUse < 0 || next.itemToUse > 8)
                        {
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.SWITCHED_ITEM, next.entity.getEntityId(), 0, 0);
                            break;
                        }
                        final Entity nextEntity = next.entity;
                        final int nextItemToUse = next.itemToUse;
                        ((PlayerEntity)nextEntity).inventory.currentItem = nextItemToUse;
                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.SWITCHED_ITEM, next.entity.getEntityId(), 0, 1);
                        break;
                    }
                }
                debugLog = "Actions almost end";
                setDecisionState();
                if(healthCheck())
                {
                    combatantsChanged = true;
                }
                if(isCreativeCheck())
                {
                    combatantsChanged = true;
                }
                debugLog += ", adding task";
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.TURN_END, 0, 0, 0);
                debugLog = "Actions end";
                break;
            } // case ACTION
        default:
            state = State.DECISION;
            break;
        } // switch(state)
        debugLog = "Update almost end";
        if(combatantsChanged)
        {
            notifyPlayersBattleInfo();
        }
        if(battleEnded)
        {
            Collection<Combatant> combatants = new ArrayList<Combatant>();
            combatants.addAll(sideA.values());
            combatants.addAll(sideB.values());
            for(Combatant c : combatants)
            {
                removeCombatant(c);
            }
        }
        debugLog = "Update end";
        return battleEnded;
    } // update(final long dt)
}