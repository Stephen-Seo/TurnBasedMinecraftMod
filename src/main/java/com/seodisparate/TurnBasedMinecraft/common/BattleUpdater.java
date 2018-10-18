package com.seodisparate.TurnBasedMinecraft.common;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class BattleUpdater implements Runnable
{
    private BattleManager manager;
    private AtomicBoolean isRunning;
    
    public BattleUpdater(BattleManager manager)
    {
        this.manager = manager;
        isRunning = new AtomicBoolean(true);
    }

    @Override
    public void run()
    {
        while(isRunning.get())
        {
            synchronized(manager.battleMap)
            {
                for(Iterator<Map.Entry<Integer, Battle>> iter = manager.battleMap.entrySet().iterator(); iter.hasNext();)
                {
                    Map.Entry<Integer, Battle> entry = iter.next();
                    if(entry.getValue().update())
                    {
                        iter.remove();
                    }
                }
            }
            manager.updateRecentlyLeftBattle();
            try { Thread.sleep(250); } catch (Throwable t) { /* ignored */ }
        }
    }
    
    public void setIsRunning(boolean isRunning)
    {
        this.isRunning.set(isRunning);
    }
}