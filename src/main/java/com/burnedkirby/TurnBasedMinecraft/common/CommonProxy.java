package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class CommonProxy
{
    private Set<AttackerViaBow> attackerViaBow = null;
    private BattleManager battleManager = null;
    private Entity attackingEntity = null;
    private int attackingDamage = 0;
    private Config config = null;
    protected Logger logger = null;
    private Map<Integer, EditingInfo> editingPlayers;
    
    public final void initialize()
    {
        attackerViaBow = new HashSet<AttackerViaBow>();
        editingPlayers = new Hashtable<Integer, EditingInfo>();
        initializeClient();
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
        logger.debug("postInit proxy for com_burnedkirby_turnbasedminecraft");
    }
    
    protected void postInitClient() {}

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

    public void displayComponent(Component textComponent) {}
    
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

    protected final EditingInfo setEditingPlayer(Player player)
    {
        return editingPlayers.put(player.getId(), new EditingInfo(player));
    }

    protected final EditingInfo removeEditingInfo(int id)
    {
        return editingPlayers.remove(id);
    }

    public Entity getEntity(int id, ResourceKey<Level> dim) {
        return ServerLifecycleHooks.getCurrentServer().getLevel(dim).getEntity(id);
    }

    public <MSG> void handlePacket(MSG msg, Supplier<NetworkEvent.Context> ctx) {}
}
