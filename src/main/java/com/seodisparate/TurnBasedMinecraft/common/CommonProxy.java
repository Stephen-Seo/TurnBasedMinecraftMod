package com.seodisparate.TurnBasedMinecraft.common;

import org.apache.logging.log4j.Logger;

public class CommonProxy
{
    public void setBattleGuiTime(int timeRemaining) {}
    
    public void setBattleGuiBattleChanged() {}
    
    public void setBattleGuiAsGui() {}
    
    public void battleGuiTurnBegin() {}
    
    public void battleGuiTurnEnd() {}
    
    public void battleEnded() {}
    
    public void postInit() {}
    
    public void setLogger(Logger logger) {}
    
    public void playBattleMusic() {}
    
    public void playSillyMusic() {}
    
    public void stopMusic() {}
    
    public void typeEnteredBattle(String type) {}
    
    public void setConfig(Config config) {}
}
