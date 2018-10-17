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
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemLingeringPotion;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSplashPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.common.FMLCommonHandler;

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
        sideAEntryQueue = new ArrayDeque<Combatant>();
        sideBEntryQueue = new ArrayDeque<Combatant>();
        playerCount = new AtomicInteger(0);
        undecidedCount = new AtomicInteger(0);
        if(sideA != null)
        {
            for(Entity e : sideA)
            {
                EntityInfo entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(e);
                if(entityInfo == null && !(e instanceof EntityPlayer) && TurnBasedMinecraftMod.proxy.isServerRunning())
                {
                    continue;
                }
                Combatant newCombatant = new Combatant(e, entityInfo);
                newCombatant.isSideA = true;
                newCombatant.battleID = getId();
                this.sideA.put(e.getEntityId(), newCombatant);
                if(e instanceof EntityPlayer)
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
                EntityInfo entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(e);
                if(entityInfo == null && !(e instanceof EntityPlayer) && TurnBasedMinecraftMod.proxy.isServerRunning())
                {
                    continue;
                }
                Combatant newCombatant = new Combatant(e, entityInfo);
                newCombatant.isSideA = false;
                newCombatant.battleID = getId();
                this.sideB.put(e.getEntityId(), newCombatant);
                if(e instanceof EntityPlayer)
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
        
        for(Combatant c : this.sideA.values())
        {
            if(c.entityInfo != null)
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id, c.entityInfo.category);
            }
            else if(c.entity instanceof EntityPlayer)
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
            else if(c.entity instanceof EntityPlayer)
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id, "player");
            }
            else
            {
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, c.entity.getEntityId(), 0, id);
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
        return sideA.containsKey(entityID) || sideB.containsKey(entityID);
    }
    
    public boolean hasCombatantInSideA(int entityID)
    {
        return sideA.containsKey(entityID);
    }
    
    public void addCombatantToSideA(Entity e)
    {
        EntityInfo entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(e);
        if(entityInfo == null && !(e instanceof EntityPlayer) && TurnBasedMinecraftMod.proxy.isServerRunning())
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
        if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
        {
            newCombatant.x = e.posX;
            newCombatant.z = e.posZ;
            newCombatant.yaw = e.rotationYaw;
            newCombatant.pitch = e.rotationPitch;
        }
        if(newCombatant.entityInfo != null)
        {
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id, newCombatant.entityInfo.category);
        }
        else if(newCombatant.entity instanceof EntityPlayer)
        {
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id, "player");
        }
        else
        {
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id);
        }
        notifyPlayersBattleInfo();
    }
    
    public void addCombatantToSideB(Entity e)
    {
        EntityInfo entityInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(e);
        if(entityInfo == null && !(e instanceof EntityPlayer) && TurnBasedMinecraftMod.proxy.isServerRunning())
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
        if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
        {
            newCombatant.x = e.posX;
            newCombatant.z = e.posZ;
            newCombatant.yaw = e.rotationYaw;
            newCombatant.pitch = e.rotationPitch;
        }
        if(newCombatant.entityInfo != null)
        {
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id, newCombatant.entityInfo.category);
        }
        else if(newCombatant.entity instanceof EntityPlayer)
        {
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENTERED, newCombatant.entity.getEntityId(), 0, id, "player");
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
    
    protected void sendMessageToAllPlayers(PacketBattleMessage.MessageType type, int from, int to, int amount)
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
    
    protected void sendMessageToAllPlayers(PacketBattleMessage.MessageType type, int from, int to, int amount, String custom)
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
                String category = new String();
                if(c.entityInfo != null)
                {
                    category = c.entityInfo.category;
                }
                else if(c.entity instanceof EntityPlayer)
                {
                    category = "player";
                }
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.DIED, c.entity.getEntityId(), 0, 0, category);
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
                String category = new String();
                if(c.entityInfo != null)
                {
                    category = c.entityInfo.category;
                }
                else if(c.entity instanceof EntityPlayer)
                {
                    category = "player";
                }
                sendMessageToAllPlayers(PacketBattleMessage.MessageType.DIED, c.entity.getEntityId(), 0, 0, category);
            }
        }
        boolean didRemove = !removeQueue.isEmpty();
        for(Integer toRemove = removeQueue.poll(); toRemove != null; toRemove = removeQueue.poll())
        {
            sideA.remove(toRemove);
            sideB.remove(toRemove);
            if(players.remove(toRemove) != null)
            {
                playerCount.decrementAndGet();
            }
        }
        if(players.isEmpty() || sideA.isEmpty() || sideB.isEmpty())
        {
            battleEnded = true;
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.ENDED, 0, 0, 0);
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
        Queue<Integer> removeQueue = new ArrayDeque<Integer>();
        for(Combatant c : players.values())
        {
            if(c.entity != null && ((EntityPlayer)c.entity).isCreative())
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketBattleMessage(PacketBattleMessage.MessageType.ENDED, c.entity.getEntityId(), 0, 0), (EntityPlayerMP)c.entity);
                removeQueue.add(c.entity.getEntityId());
            }
        }
        boolean didRemove = false;
        for(Integer toRemove = removeQueue.poll(); toRemove != null; toRemove = removeQueue.poll())
        {
            didRemove = true;
            sideA.remove(toRemove);
            sideB.remove(toRemove);
            players.remove(toRemove);
            playerCount.decrementAndGet();
            sendMessageToAllPlayers(PacketBattleMessage.MessageType.BECAME_CREATIVE, toRemove, 0, 0);
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
                update(0);
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
                    if(!next.entity.isEntityAlive())
                    {
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
                            ItemStack heldItemStack = ((EntityPlayer)next.entity).getHeldItemMainhand();
                            if(heldItemStack.getItem() instanceof ItemBow)
                            {
                                if(Utility.doesPlayerHaveArrows((EntityPlayer)next.entity))
                                {
                                    final Entity nextEntity = next.entity;
                                    final Entity targetEntity = target.entity;
                                    final float yawDirection = Utility.yawDirection(next.entity.posX, next.entity.posZ, target.entity.posX, target.entity.posZ);
                                    final float pitchDirection = Utility.pitchDirection(next.entity.posX, next.entity.posY, next.entity.posZ, target.entity.posX, target.entity.posY, target.entity.posZ);
                                    if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
                                    {
                                        next.yaw = yawDirection;
                                        next.pitch = pitchDirection;
                                    }
                                    next.entity.getServer().addScheduledTask(() -> {
                                        // have player look at attack target
                                        ((EntityPlayerMP)nextEntity).connection.setPlayerLocation(nextEntity.posX, nextEntity.posY, nextEntity.posZ, yawDirection, pitchDirection);
                                        ItemBow itemBow = (ItemBow)heldItemStack.getItem();
                                        synchronized(TurnBasedMinecraftMod.proxy.getAttackerViaBowSet())
                                        {
                                            TurnBasedMinecraftMod.proxy.getAttackerViaBowSet().add(new AttackerViaBow(nextEntity, getId()));
                                        }
                                        itemBow.onPlayerStoppedUsing(((EntityPlayer)nextEntity).getHeldItemMainhand(), nextEntity.getEntityWorld(), (EntityLivingBase)nextEntity, (int)(Math.random() * (itemBow.getMaxItemUseDuration(heldItemStack)) / 3));
                                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.FIRED_ARROW, nextEntity.getEntityId(), targetEntity.getEntityId(), 0);
                                    });
                                }
                                else
                                {
                                    sendMessageToAllPlayers(PacketBattleMessage.MessageType.BOW_NO_AMMO, next.entity.getEntityId(), 0, 0);
                                }
                                continue;
                            }
                            int hitChance = TurnBasedMinecraftMod.proxy.getConfig().getPlayerAttackProbability();
                            if(target.entity instanceof EntityPlayer)
                            {
                                hitChance -= TurnBasedMinecraftMod.proxy.getConfig().getPlayerEvasion();
                            }
                            else
                            {
                                hitChance -= target.entityInfo.evasion;
                            }
                            if(hitChance < TurnBasedMinecraftMod.proxy.getConfig().getMinimumHitPercentage())
                            {
                                hitChance = TurnBasedMinecraftMod.proxy.getConfig().getMinimumHitPercentage();
                            }
                            if((int)(Math.random() * 100) < hitChance)
                            {
                                if(target.remainingDefenses <= 0)
                                {
                                    // attack
                                    final Entity nextEntity = next.entity;
                                    final Entity targetEntity = target.entity;
                                    final EntityInfo targetEntityInfo = target.entityInfo;
                                    final float yawDirection = Utility.yawDirection(next.entity.posX, next.entity.posZ, target.entity.posX, target.entity.posZ);
                                    final float pitchDirection = Utility.pitchDirection(next.entity.posX, next.entity.posY, next.entity.posZ, target.entity.posX, target.entity.posY, target.entity.posZ);
                                    if(TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled())
                                    {
                                        next.yaw = yawDirection;
                                        next.pitch = pitchDirection;
                                    }
                                    next.entity.getServer().addScheduledTask(() -> {
                                        // have player look at attack target
                                        ((EntityPlayerMP)nextEntity).connection.setPlayerLocation(nextEntity.posX, nextEntity.posY, nextEntity.posZ, yawDirection, pitchDirection);
                                        TurnBasedMinecraftMod.proxy.setAttackingEntity(nextEntity);
                                        TurnBasedMinecraftMod.proxy.setAttackingDamage(0);
                                        ((EntityPlayer)nextEntity).attackTargetEntityWithCurrentItem(targetEntity);
                                        TurnBasedMinecraftMod.proxy.setAttackingEntity(null);
                                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.ATTACK, nextEntity.getEntityId(), targetEntity.getEntityId(), TurnBasedMinecraftMod.proxy.getAttackingDamage());
                                        if(!(targetEntity instanceof EntityPlayer) && targetEntityInfo.defenseDamage > 0)
                                        {
                                            if((int)(Math.random() * 100) < targetEntityInfo.defenseDamageProbability)
                                            {
                                                // defense damage
                                                DamageSource defenseDamageSource = DamageSource.causeMobDamage((EntityLivingBase)targetEntity);
                                                TurnBasedMinecraftMod.proxy.setAttackingEntity(targetEntity);
                                                nextEntity.attackEntityFrom(defenseDamageSource, targetEntityInfo.defenseDamage);
                                                TurnBasedMinecraftMod.proxy.setAttackingEntity(null);
                                                sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFENSE_DAMAGE, targetEntity.getEntityId(), nextEntity.getEntityId(), targetEntityInfo.defenseDamage);
                                            }
                                        }
                                    });
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
                            EntityLivingBase attackTarget = ((EntityLiving)next.entity).getAttackTarget();
                            if(attackTarget != null && hasCombatant(attackTarget.getEntityId()))
                            {
                                target = getCombatantByID(attackTarget.getEntityId());
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
                            }
                            if(target == null || !target.entity.isEntityAlive())
                            {
                                continue;
                            }
                            int hitChance = next.entityInfo.attackProbability;
                            if(target.entity instanceof EntityPlayer)
                            {
                                hitChance -= TurnBasedMinecraftMod.proxy.getConfig().getPlayerEvasion();
                            }
                            else
                            {
                                hitChance -= target.entityInfo.evasion;
                            }
                            if(hitChance < TurnBasedMinecraftMod.proxy.getConfig().getMinimumHitPercentage())
                            {
                                hitChance = TurnBasedMinecraftMod.proxy.getConfig().getMinimumHitPercentage();
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
                                    final Entity nextEntity = next.entity;
                                    final EntityInfo nextEntityInfo = next.entityInfo;
                                    final Entity targetEntity = target.entity;
                                    final EntityInfo targetEntityInfo = target.entityInfo;
                                    final int finalDamageAmount = damageAmount;
                                    next.entity.getServer().addScheduledTask(() -> {
                                        TurnBasedMinecraftMod.proxy.setAttackingEntity(nextEntity);
                                        targetEntity.attackEntityFrom(damageSource, finalDamageAmount);
                                        TurnBasedMinecraftMod.proxy.setAttackingEntity(null);
                                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.ATTACK, nextEntity.getEntityId(), targetEntity.getEntityId(), finalDamageAmount);
                                        if(!(targetEntity instanceof EntityPlayer) && targetEntityInfo.defenseDamage > 0)
                                        {
                                            if((int)(Math.random() * 100) < targetEntityInfo.defenseDamageProbability)
                                            {
                                                // defense damage
                                                DamageSource defenseDamageSource = DamageSource.causeMobDamage((EntityLivingBase)targetEntity);
                                                TurnBasedMinecraftMod.proxy.setAttackingEntity(targetEntity);
                                                nextEntity.attackEntityFrom(defenseDamageSource, targetEntityInfo.defenseDamage);
                                                TurnBasedMinecraftMod.proxy.setAttackingEntity(null);
                                                sendMessageToAllPlayers(PacketBattleMessage.MessageType.DEFENSE_DAMAGE, targetEntity.getEntityId(), nextEntity.getEntityId(), targetEntityInfo.defenseDamage);
                                            }
                                        }
                                        // attack effect
                                        if(nextEntityInfo.attackEffect != EntityInfo.Effect.UNKNOWN && nextEntityInfo.attackEffectProbability > 0)
                                        {
                                            int effectChance = (int)(Math.random() * 100);
                                            if(effectChance < nextEntityInfo.attackEffectProbability)
                                            {
                                                nextEntityInfo.attackEffect.applyEffectToEntity((EntityLivingBase)targetEntity);
                                                sendMessageToAllPlayers(PacketBattleMessage.MessageType.WAS_AFFECTED, nextEntity.getEntityId(), targetEntity.getEntityId(), 0, nextEntityInfo.attackEffect.getAffectedString());
                                            }
                                        }
                                    });
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
                        next.remainingDefenses = TurnBasedMinecraftMod.proxy.getConfig().getDefenseDuration();
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
                                if(c.entity instanceof EntityPlayer)
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
                        if(next.entity instanceof EntityPlayer)
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
                            combatantsChanged = true;
                            String fleeingCategory = new String();
                            if(next.entityInfo != null)
                            {
                                fleeingCategory = next.entityInfo.category;
                            }
                            else if(next.entity instanceof EntityPlayer)
                            {
                                fleeingCategory = "player";
                            }
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.FLEE, next.entity.getEntityId(), 0, 1, fleeingCategory);
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
                            final Entity nextEntity = next.entity;
                            next.entity.getServer().addScheduledTask(() -> {
                                ((ItemFood)targetItem).onItemUseFinish(targetItemStack, nextEntity.world, (EntityLivingBase)nextEntity);
                            });
                        }
                        else if(targetItem instanceof ItemPotion && !(targetItem instanceof ItemSplashPotion) && !(targetItem instanceof ItemLingeringPotion))
                        {
                            sendMessageToAllPlayers(PacketBattleMessage.MessageType.USED_ITEM, next.entity.getEntityId(), 0, PacketBattleMessage.UsedItemAction.USED_POTION.getValue(), targetItemStack.getDisplayName());
                            final Entity nextEntity = next.entity;
                            final int nextItemToUse = next.itemToUse;
                            next.entity.getServer().addScheduledTask(() -> {
                                ((ItemPotion)targetItem).onItemUseFinish(targetItemStack, nextEntity.world, (EntityLivingBase)nextEntity);
                                ((EntityPlayer)nextEntity).inventory.setInventorySlotContents(nextItemToUse, new ItemStack(Items.GLASS_BOTTLE));
                            });
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
                        final Entity nextEntity = next.entity;
                        final int nextItemToUse = next.itemToUse;
                        next.entity.getServer().addScheduledTask(() -> {
                            ((EntityPlayer)nextEntity).inventory.currentItem = nextItemToUse;
                        });
                        sendMessageToAllPlayers(PacketBattleMessage.MessageType.SWITCHED_ITEM, next.entity.getEntityId(), 0, 1);
                        break;
                    }
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
                if(healthCheck())
                {
                    combatantsChanged = true;
                }
                if(isCreativeCheck())
                {
                    combatantsChanged = true;
                }
				FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
				    sendMessageToAllPlayers(PacketBattleMessage.MessageType.TURN_END, 0, 0, 0);
				});
                break;
            } // case ACTION
        default:
            state = State.DECISION;
            break;
        } // switch(state)
        if(combatantsChanged)
        {
            notifyPlayersBattleInfo();
        }
        return battleEnded;
    } // update(final long dt)
}
