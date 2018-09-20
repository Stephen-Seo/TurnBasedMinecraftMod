package com.seodisparate.TurnBasedMinecraft.common;

import org.apache.logging.log4j.Logger;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class CommonProxy
{
    public void initialize() {}
    
    public boolean initializeBattleManager()
    {
        if(TurnBasedMinecraftMod.battleManager == null)
        {
            TurnBasedMinecraftMod.battleManager = new BattleManager(TurnBasedMinecraftMod.logger);
            return true;
        }
        return false;
    }
    
    public boolean cleanupBattleManager ()
    {
        if(TurnBasedMinecraftMod.battleManager != null)
        {
            TurnBasedMinecraftMod.battleManager.cleanup();
            TurnBasedMinecraftMod.battleManager = null;
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
    
    public void postInit() {}
    
    public void setLogger(Logger logger) {}
    
    public void playBattleMusic() {}
    
    public void playSillyMusic() {}
    
    public void stopMusic() {}
    
    public void typeEnteredBattle(String type) {}
    
    public void setConfig(Config config) {}
    
    public void displayString(String message) {}
    
    public Entity getEntityByID(int id)
    {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getEntityWorld().getEntityByID(id);
    }
}
