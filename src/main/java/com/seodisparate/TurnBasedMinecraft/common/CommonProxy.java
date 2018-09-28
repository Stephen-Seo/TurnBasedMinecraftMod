package com.seodisparate.TurnBasedMinecraft.common;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class CommonProxy
{
    private Set<AttackerViaBow> attackerViaBow = null;
    private BattleManager battleManager = null;
    private Entity attackingEntity = null;
    private int attackingDamage = 0;
    private Config config = null;
    private Logger logger = null;
    
    public void initialize()
    {
        attackerViaBow = new HashSet<AttackerViaBow>();
    }
    
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
    
    public void postInit()
    {
        config = new Config(logger);
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
    
    public Entity getEntityByID(int id)
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getEntityByID(id);
    }
    
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
}
