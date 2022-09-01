package com.burnedkirby.TurnBasedMinecraft.common;

import java.lang.reflect.Field;
import java.util.*;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.Entity;

public class CommonProxy
{
    private Set<AttackerViaBow> attackerViaBow = null;
    private BattleManager battleManager = null;
    private Entity attackingEntity = null;
    private int attackingDamage = 0;
    private Config config = null;
    protected Logger logger = null;
    private Map<Integer, EditingInfo> editingPlayers;

    private OtherModHandler otherModHandler;
    
    public final void initialize()
    {
        attackerViaBow = new HashSet<AttackerViaBow>();
        editingPlayers = new Hashtable<Integer, EditingInfo>();
        initializeClient();
        otherModHandler = new OtherModHandler();
        logger.debug("Init proxy for com_burnedkirby_turnbasedminecraft");
    }
    
    protected void initializeClient() {}
    
    public final boolean initializeBattleManager()
    {
        if(battleManager == null)
        {
            battleManager = new BattleManager(TurnBasedMinecraftMod.logger);
            return true;
        }
        return false;
    }
    
    public final boolean cleanupBattleManager ()
    {
        if(battleManager != null)
        {
            battleManager.cleanup();
            battleManager = null;
            return true;
        }
        return false;
    }
    
    public void setBattleGuiTime(int timeRemaining) {}
    
    public void setBattleGuiBattleChanged() {}
    
    public void setBattleGuiAsGui() {}
    
    public void battleGuiTurnBegin() {}
    
    public void battleGuiTurnEnd() {}
    
    public void battleStarted() {}
    
    public void battleEnded() {}
    
    public final void postInit()
    {
        config = new Config(logger);
        postInitClient();
        pamsFoodIntegrationLoading();
        logger.debug("postInit proxy for com_burnedkirby_turnbasedminecraft");

        otherModHandler.postInit();
    }
    
    protected void postInitClient() {}

    private final void pamsFoodIntegrationLoading() {
        // TODO: generalize other mod's food loading via config with a list of mod ids

        // pamhc2foodcore
        {
            ModList modList = ModList.get();
            Optional<? extends ModContainer> pamsFoodCoreContainer = modList.getModContainerById("pamhc2foodcore");
            if (pamsFoodCoreContainer.isPresent()) {
                Object pamsFoodCore = pamsFoodCoreContainer.get().getMod();
                try {
                    Field itemGroupField = pamsFoodCore.getClass().getField("ITEM_GROUP");
                    ItemGroup foodItemGroup = (ItemGroup) itemGroupField.get(null);
                    if(foodItemGroup != null) {
                        BattleManager.addOtherModItemGroup(foodItemGroup);
                    } else {
                        throw new NullPointerException();
                    }
                } catch (Exception e) {
                    TurnBasedMinecraftMod.logger.info("Failed to get pamhc2foodcore ITEM_GROUP");
                }
            }
        }

        // pamhc2crops
        {
            ModList modList = ModList.get();
            Optional<? extends ModContainer> pamsFoodCoreContainer = modList.getModContainerById("pamhc2crops");
            if (pamsFoodCoreContainer.isPresent()) {
                Object pamsFoodCore = pamsFoodCoreContainer.get().getMod();
                try {
                    Field itemGroupField = pamsFoodCore.getClass().getField("ITEM_GROUP");
                    ItemGroup foodItemGroup = (ItemGroup) itemGroupField.get(null);
                    if(foodItemGroup != null) {
                        BattleManager.addOtherModItemGroup(foodItemGroup);
                    } else {
                        throw new NullPointerException();
                    }
                } catch (Exception e) {
                    TurnBasedMinecraftMod.logger.info("Failed to get pamhc2crops ITEM_GROUP");
                }
            }
        }

        // pamhc2trees
        {
            ModList modList = ModList.get();
            Optional<? extends ModContainer> pamsFoodCoreContainer = modList.getModContainerById("pamhc2trees");
            if (pamsFoodCoreContainer.isPresent()) {
                Object pamsFoodCore = pamsFoodCoreContainer.get().getMod();
                try {
                    Field itemGroupField = pamsFoodCore.getClass().getField("ITEM_GROUP");
                    ItemGroup foodItemGroup = (ItemGroup) itemGroupField.get(null);
                    if(foodItemGroup != null) {
                        BattleManager.addOtherModItemGroup(foodItemGroup);
                    } else {
                        throw new NullPointerException();
                    }
                } catch (Exception e) {
                    TurnBasedMinecraftMod.logger.info("Failed to get pamhc2trees ITEM_GROUP");
                }
            }
        }

        // pamhc2foodextended
        {
            ModList modList = ModList.get();
            Optional<? extends ModContainer> pamsFoodCoreContainer = modList.getModContainerById("pamhc2foodextended");
            if (pamsFoodCoreContainer.isPresent()) {
                Object pamsFoodCore = pamsFoodCoreContainer.get().getMod();
                try {
                    Field itemGroupField = pamsFoodCore.getClass().getField("ITEM_GROUP");
                    ItemGroup foodItemGroup = (ItemGroup) itemGroupField.get(null);
                    if(foodItemGroup != null) {
                        BattleManager.addOtherModItemGroup(foodItemGroup);
                    } else {
                        throw new NullPointerException();
                    }
                } catch (Exception e) {
                    TurnBasedMinecraftMod.logger.info("Failed to get pamhc2foodextended ITEM_GROUP");
                }
            }
        }
    }
    
    public final void setLogger(Logger logger)
    {
        this.logger = logger;
    }
    
    public void playBattleMusic() {}
    
    public void playSillyMusic() {}
    
    public void stopMusic(boolean resumeMCSounds) {}
    
    public void typeEnteredBattle(String type) {}
    
    public void typeLeftBattle(String type) {}
    
    public void displayString(String message) {}

    public void displayTextComponent(ITextComponent textComponent) {}
    
    public final boolean isServerRunning()
    {
        return battleManager != null;
    }
    
    public Battle getLocalBattle()
    {
        return null;
    }
    
    public void createLocalBattle(int id) {}
    
    public final Set<AttackerViaBow> getAttackerViaBowSet()
    {
        return attackerViaBow;
    }
    
    public final BattleManager getBattleManager()
    {
        return battleManager;
    }
    
    protected final void setAttackingEntity(Entity entity)
    {
        attackingEntity = entity;
    }
    
    protected final Entity getAttackingEntity()
    {
        return attackingEntity;
    }
    
    protected final void setAttackingDamage(int damage)
    {
        attackingDamage = damage;
    }
    
    protected final int getAttackingDamage()
    {
        return attackingDamage;
    }
    
    protected final Logger getLogger()
    {
        return logger;
    }
    
    public final Config getConfig()
    {
        return config;
    }

    protected final EditingInfo getEditingInfo(int id)
    {
        return editingPlayers.get(id);
    }

    protected final EditingInfo setEditingPlayer(PlayerEntity player)
    {
        return editingPlayers.put(player.getId(), new EditingInfo(player));
    }

    protected final EditingInfo removeEditingInfo(int id)
    {
        return editingPlayers.remove(id);
    }

    public Entity getEntity(int id, RegistryKey<World> dim) {
        return ServerLifecycleHooks.getCurrentServer().getLevel(dim).getEntity(id);
    }
}
