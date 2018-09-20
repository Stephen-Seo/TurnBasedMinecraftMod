package com.seodisparate.TurnBasedMinecraft.common;

import java.util.ArrayDeque;
import java.util.Queue;
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
        Queue<Integer> endedQueue = new ArrayDeque<Integer>();
        Integer ended;
        while(isRunning.get())
        {
            for(Battle e : manager.battleMap.values())
            {
                if(e.update())
                {
                    endedQueue.add(e.getId());
                }
            }
            for(ended = endedQueue.poll(); ended != null; ended = endedQueue.poll())
            {
                manager.battleMap.remove(ended);
            }
            try { Thread.sleep(250); } catch (Exception e) { /* ignored */ }
        }
    }
    
    public void setIsRunning(boolean isRunning)
    {
        this.isRunning.set(isRunning);
    }
}