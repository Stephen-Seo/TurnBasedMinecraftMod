package com.seodisparate.TurnBasedMinecraft.common;

public class BattleUpdater implements Runnable
{
    BattleManager manager;
    
    public BattleUpdater(BattleManager manager)
    {
        this.manager = manager;
    }

    @Override
    public void run()
    {
        for(Battle e : manager.battleMap.values())
        {
            e.update();
        }
        try { Thread.sleep(250); } catch (Exception e) { /* ignored */ }
    }
}