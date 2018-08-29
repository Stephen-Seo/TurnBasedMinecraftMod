package com.seodisparate.TurnBasedMinecraft.common;

public class BattleUpdater implements Runnable
{
    private BattleManager manager;
    private boolean isRunning;
    
    public BattleUpdater(BattleManager manager)
    {
        this.manager = manager;
        isRunning = true;
    }

    @Override
    public void run()
    {
        while(isRunning)
        {
            for(Battle e : manager.battleMap.values())
            {
                e.update();
            }
            try { Thread.sleep(250); } catch (Exception e) { /* ignored */ }
        }
    }
    
    public void setIsRunning(boolean isRunning)
    {
        this.isRunning = isRunning;
    }
}