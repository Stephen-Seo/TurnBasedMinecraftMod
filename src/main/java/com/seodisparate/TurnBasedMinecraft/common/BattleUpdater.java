package com.seodisparate.TurnBasedMinecraft.common;

import java.util.ArrayDeque;
import java.util.Queue;

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
        Queue<Integer> endedQueue = new ArrayDeque<Integer>();
        Integer ended;
        while(isRunning)
        {
            for(Battle e : manager.battleMap.values())
            {
                if(e.update())
                {
                    endedQueue.add(e.getId());
                }
            }
            ended = endedQueue.poll();
            while(ended != null)
            {
                manager.battleMap.remove(ended);
                ended = endedQueue.poll();
            }
            try { Thread.sleep(250); } catch (Exception e) { /* ignored */ }
        }
    }
    
    public void setIsRunning(boolean isRunning)
    {
        this.isRunning = isRunning;
    }
}