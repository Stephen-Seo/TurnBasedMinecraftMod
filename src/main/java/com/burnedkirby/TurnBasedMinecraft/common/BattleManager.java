package com.burnedkirby.TurnBasedMinecraft.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.Logger;

import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketGeneralMessage;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;

public class BattleManager
{
    private int IDCounter = 0;
    protected Map<Integer, Battle> battleMap;
    private Logger logger;
    private Map<Integer, Combatant> recentlyLeftBattle;
    private BattleUpdater battleUpdater;
    private Map<EntityIDDimPair, Integer> entityToBattleMap;
    private static Collection<ItemGroup> otherFoodItemGroups = new ArrayList<>();
    
    public BattleManager(Logger logger)
    {
        this.logger = logger;
        battleMap = new HashMap<Integer, Battle>();
        recentlyLeftBattle = new HashMap<Integer, Combatant>();
        battleUpdater = new BattleUpdater(this);
        entityToBattleMap = new HashMap<EntityIDDimPair, Integer>();
        MinecraftForge.EVENT_BUS.register(battleUpdater);
    }
    
    /**
     * Either creates a new Battle, adds a combatant to an existing Battle, or does
     * nothing, depending on if a player is involved and/or an entity is currently
     * in battle.
     * 
     * @param event
     * @return True if event should be canceled
     */
    public boolean checkAttack(final LivingAttackEvent event)
    {
        Config config = TurnBasedMinecraftMod.proxy.getConfig();
        String receiverClassName = event.getEntity().getClass().getName();
        String receiverCustomName;

        try {
            receiverCustomName = event.getEntity().getCustomName().getString();
        } catch (NullPointerException e) {
            receiverCustomName = null;
        }
        if (receiverCustomName == null) {
            // If entity does not have a "custom name", see if it is a CustomNPC and get its name.
            receiverCustomName = OtherModHandler.getCustomNPCName(event.getEntity());
        }

        String attackerClassName;
        try {
            attackerClassName = event.getSource().getEntity().getClass().getName();
        } catch (NullPointerException e) {
            attackerClassName = null;
        }
        String attackerCustomName;
        try {
            attackerCustomName = event.getSource().getEntity().getCustomName().getString();
        } catch (NullPointerException e) {
            attackerCustomName = null;
        }
        if (attackerCustomName == null) {
            // If entity does not have a "custom name", see if it is a CustomNPC and get its name.
            attackerCustomName = OtherModHandler.getCustomNPCName(event.getSource().getEntity());
        }

        // verify that both entities are EntityPlayer and not in creative or has a corresponding EntityInfo
        if(!((event.getEntity() instanceof PlayerEntity && !((PlayerEntity)event.getEntity()).isCreative())
                || (config.getEntityInfoReference(receiverClassName) != null || config.getCustomEntityInfoReference(receiverCustomName) != null))
            || !((event.getSource().getEntity() instanceof PlayerEntity && !((PlayerEntity)event.getSource().getEntity()).isCreative())
                || (config.getEntityInfoReference(attackerClassName) != null || config.getCustomEntityInfoReference(attackerCustomName) != null)))
        {
//            logger.debug("BattleManager: Failed first check, attacker is \"" + attackerClassName + "\", defender is \"" + receiverClassName + "\"");
            return false;
        }
        
        // check if ignore battle in config
        EntityInfo entityInfo = config.getCustomEntityInfoReference(receiverCustomName);
        if(entityInfo == null)
        {
            entityInfo = config.getMatchingEntityInfo(event.getEntity());
        }

        if(entityInfo != null && (config.isIgnoreBattleType(entityInfo.category) || entityInfo.ignoreBattle))
        {
            // attacked entity ignores battle
            Battle battle = battleMap.get(entityToBattleMap.get(new EntityIDDimPair(event.getSource().getEntity())));
            if(battle != null && battle.hasCombatant(event.getSource().getEntity().getId())) {
                logger.debug("Attack Canceled: attacked ignores battle but attacker in battle");
                return true;
            } else {
                logger.debug("Attack Not Canceled: attacked ignores battle");
                return false;
            }
        }

        entityInfo = config.getCustomEntityInfoReference(attackerCustomName);
        if(entityInfo == null)
        {
            entityInfo = config.getMatchingEntityInfo(event.getSource().getEntity());
        }

        if(entityInfo != null && (config.isIgnoreBattleType(entityInfo.category) || entityInfo.ignoreBattle))
        {
            // attacker entity ignores battle
            Battle battle = battleMap.get(entityToBattleMap.get(new EntityIDDimPair(event.getEntity())));
            if(battle != null && battle.hasCombatant(event.getEntity().getId())) {
                logger.debug("Attack Canceled: attacker ignores battle but attacked in battle");
                return true;
            } else {
                logger.debug("Attack Not Canceled: attacker ignores battle");
                return false;
            }
        }
        
        // check if one is in battle
        Battle attackerBattle = battleMap.get(entityToBattleMap.get(new EntityIDDimPair(event.getSource().getEntity())));
        if(attackerBattle != null && !attackerBattle.hasCombatant(event.getSource().getEntity().getId())) {
            attackerBattle = null;
        }
        Battle defenderBattle = battleMap.get(entityToBattleMap.get(new EntityIDDimPair(event.getEntity())));
        if(defenderBattle != null && !defenderBattle.hasCombatant(event.getEntity().getId())) {
            defenderBattle = null;
        }

        if(attackerBattle != null && defenderBattle != null) {
            // both in battle, attack canceled
            return true;
        } else if(attackerBattle == null && defenderBattle == null) {
            // neither entity is in battle
            if(event.getEntity() instanceof PlayerEntity || event.getSource().getEntity() instanceof PlayerEntity)
            {
                // at least one of the entities is a player, create Battle
                Collection<Entity> sideA = new ArrayList<Entity>(1);
                Collection<Entity> sideB = new ArrayList<Entity>(1);
                sideA.add(event.getEntity());
                sideB.add(event.getSource().getEntity());
                createBattle(sideA, sideB, event.getEntity().level.dimension());
                logger.debug("Attack Not Canceled: new battle created");
            }
            else
            {
                logger.debug("Attack Not Canceled: neither are in battle or players");
            }
            return false;
        } else {
            // at this point only one entity is in battle, so add entity to other side
            if(attackerBattle != null) {
                if (attackerBattle.getSize() >= config.getMaxInBattle()) {
                    // battle limit reached, cannot add to battle
                    return true;
                } else if (attackerBattle.hasCombatantInSideA(event.getSource().getEntity().getId())) {
                    attackerBattle.addCombatantToSideB(event.getEntity());
                } else {
                    attackerBattle.addCombatantToSideA(event.getEntity());
                }
                entityToBattleMap.put(new EntityIDDimPair(event.getEntity()), attackerBattle.getId());
            } else {
                if (defenderBattle.getSize() >= config.getMaxInBattle()) {
                    // battle limit reached, cannot add to battle
                    return true;
                } else if (defenderBattle.hasCombatantInSideA(event.getEntity().getId())) {
                    defenderBattle.addCombatantToSideB(event.getSource().getEntity());
                } else {
                    defenderBattle.addCombatantToSideA(event.getSource().getEntity());
                }
                entityToBattleMap.put(new EntityIDDimPair(event.getSource().getEntity()), defenderBattle.getId());
            }
        }

        logger.debug("Attack Canceled: one is in battle");
        return true;
    }
    
    public void checkTargeted(LivingSetAttackTargetEvent event)
    {
        String targetedCustomName;
        try {
            targetedCustomName = event.getTarget().getCustomName().getString();
        } catch (NullPointerException e) {
            targetedCustomName = null;
        }
        if (targetedCustomName == null) {
            // If entity does not have a "custom name", see if it is a CustomNPC and get its name.
            targetedCustomName = OtherModHandler.getCustomNPCName(event.getEntity());
        }
        String attackerCustomName;
        try {
            attackerCustomName = event.getEntity().getCustomName().getString();
        } catch (NullPointerException e) {
            attackerCustomName = null;
        }
        if (attackerCustomName == null) {
            // If entity does not have a "custom name", see if it is a CustomNPC and get its name.
            attackerCustomName = OtherModHandler.getCustomNPCName(event.getEntity());
        }

        EntityInfo attackerInfo = TurnBasedMinecraftMod.proxy.getConfig().getCustomEntityInfoReference(attackerCustomName);
        if(attackerInfo == null)
        {
            attackerInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(event.getEntity());
        }

        EntityInfo targetedInfo;
        if(event.getTarget() instanceof PlayerEntity)
        {
            targetedInfo = null;
        }
        else
        {
            targetedInfo = TurnBasedMinecraftMod.proxy.getConfig().getCustomEntityInfoReference(targetedCustomName);
            if(targetedInfo == null)
            {
                targetedInfo = TurnBasedMinecraftMod.proxy.getConfig().getMatchingEntityInfo(event.getTarget());
            }
        }
        if((event.getTarget() instanceof PlayerEntity && ((PlayerEntity)event.getTarget()).isCreative())
                || attackerInfo == null
                || attackerInfo.ignoreBattle
                || TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType(attackerInfo.category)
                || (targetedInfo != null
                    && (targetedInfo.ignoreBattle
                        || TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType(targetedInfo.category))))
        {
            return;
        }

        // check if one is in battle
        Battle attackerBattle = battleMap.get(entityToBattleMap.get(new EntityIDDimPair(event.getEntity())));
        if(attackerBattle != null && !attackerBattle.hasCombatant(event.getEntity().getId())) {
            attackerBattle = null;
        }
        Battle defenderBattle = battleMap.get(entityToBattleMap.get(new EntityIDDimPair(event.getTarget())));
        if(defenderBattle != null && !defenderBattle.hasCombatant(event.getTarget().getId())) {
            defenderBattle = null;
        }

        if(attackerBattle != null && defenderBattle != null) {
            return;
        } else if(attackerBattle == null && defenderBattle == null) {
            // neither in battle
            if(event.getEntity() instanceof PlayerEntity || event.getTarget() instanceof PlayerEntity)
            {
                // at least one is a player, create battle
                Collection<Entity> sideA = new ArrayList<Entity>(1);
                Collection<Entity> sideB = new ArrayList<Entity>(1);
                sideA.add(event.getEntity());
                sideB.add(event.getTarget());
                createBattle(sideA, sideB, event.getEntity().level.dimension());
                logger.debug("neither in battle, at least one is player, creating new battle");
            }
        } else {
            // add entity to battle
            if(attackerBattle != null) {
                if (attackerBattle.getSize() >= TurnBasedMinecraftMod.proxy.getConfig().getMaxInBattle()) {
                    // battle max reached, cannot add to battle
                    return;
                } else if (attackerBattle.hasCombatantInSideA(event.getEntity().getId())) {
                    attackerBattle.addCombatantToSideB(event.getTarget());
                } else {
                    attackerBattle.addCombatantToSideA(event.getTarget());
                }
                entityToBattleMap.put(new EntityIDDimPair(event.getTarget()), attackerBattle.getId());
            } else {
                if (defenderBattle.getSize() >= TurnBasedMinecraftMod.proxy.getConfig().getMaxInBattle()) {
                    // battle max reached, cannot add to battle
                    return;
                } else if (defenderBattle.hasCombatantInSideA(event.getTarget().getId())) {
                    defenderBattle.addCombatantToSideB(event.getEntity());
                } else {
                    defenderBattle.addCombatantToSideA(event.getEntity());
                }
                entityToBattleMap.put(new EntityIDDimPair(event.getEntity()), defenderBattle.getId());
            }
        }
    }
    
    private Battle createBattle(Collection<Entity> sideA, Collection<Entity> sideB, RegistryKey<World> dimension)
    {
        Battle newBattle = null;
        while(battleMap.containsKey(IDCounter))
        {
            ++IDCounter;
        }
        newBattle = new Battle(this, IDCounter, sideA, sideB, true, dimension);
        battleMap.put(IDCounter, newBattle);
        for(Entity e : sideA) {
            entityToBattleMap.put(new EntityIDDimPair(e), newBattle.getId());
        }
        for(Entity e : sideB) {
            entityToBattleMap.put(new EntityIDDimPair(e), newBattle.getId());
        }
        newBattle.notifyPlayersBattleInfo();
        return newBattle;
    }
    
    public Battle getBattleByID(int id)
    {
        return battleMap.get(id);
    }
    
    public void cleanup()
    {
        battleUpdater.setRunning(false);
        MinecraftForge.EVENT_BUS.unregister(battleUpdater);
        battleMap.clear();
        battleUpdater = null;
    }
    
    protected void addRecentlyLeftBattle(Combatant c)
    {
        c.time = System.nanoTime();
        Config config = TurnBasedMinecraftMod.proxy.getConfig();
        if(c.entity instanceof ServerPlayerEntity) {
            TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity) c.entity), new PacketGeneralMessage("You just left battle! " + config.getLeaveBattleCooldownSeconds() + " seconds until you can attack/be-attacked again!"));
        }
        recentlyLeftBattle.put(c.entity.getId(), c);
        entityToBattleMap.remove(new EntityIDDimPair(c.entity));
    }
    
    protected void updateRecentlyLeftBattle()
    {
        long current = System.nanoTime();
        for(Iterator<Map.Entry<Integer, Combatant>> iter = recentlyLeftBattle.entrySet().iterator(); iter.hasNext();)
        {
            Map.Entry<Integer, Combatant> entry = iter.next();
            if(entry.getValue().entity instanceof CreeperEntity && TurnBasedMinecraftMod.proxy.getConfig().getCreeperStopExplodeOnLeaveBattle()) {
                ((CreeperEntity)entry.getValue().entity).setSwellDir(-10);
            }
            if(current - entry.getValue().time > TurnBasedMinecraftMod.proxy.getConfig().getLeaveBattleCooldownNanos())
            {
                iter.remove();
                if(entry.getValue().entity instanceof ServerPlayerEntity)
                {
                    TurnBasedMinecraftMod.getHandler().send(PacketDistributor.PLAYER.with(()->(ServerPlayerEntity)entry.getValue().entity), new PacketGeneralMessage("Timer ended, you can now attack/be-attacked again."));
                }
            }
        }
    }
    
    public boolean isRecentlyLeftBattle(int entityID)
    {
        return recentlyLeftBattle.containsKey(entityID);
    }

    public boolean forceLeaveBattle(EntityIDDimPair entityInfo) {
        boolean result = false;
        Integer battleID = entityToBattleMap.get(entityInfo);
        if(battleID != null) {
            Battle battle = battleMap.get(battleID);
            if (battle != null && battle.hasCombatant(entityInfo.id)) {
                battle.forceRemoveCombatant(entityInfo);
                result = true;
            }
            entityToBattleMap.remove(entityInfo);
        }
        return result;
    }

    public static void addOtherModItemGroup(ItemGroup itemGroup) {
        otherFoodItemGroups.add(itemGroup);
    }

    public static Collection<ItemGroup> getOtherFoodItemGroups() {
        return otherFoodItemGroups;
    }
}
