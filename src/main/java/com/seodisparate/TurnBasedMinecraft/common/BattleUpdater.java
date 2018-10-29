package com.seodisparate.TurnBasedMinecraft.common;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class BattleUpdater implements Runnable
{
    private BattleManager manager;
    private AtomicBoolean isRunning;
    
    private class UpdateRunnable implements Runnable
    {
        private Battle battle;
        private AtomicBoolean finished = new AtomicBoolean(false);
        private AtomicBoolean battleFinished = new AtomicBoolean(false);
        
        public UpdateRunnable(Battle battle)
        {
            this.battle = battle;
        }
        
        public boolean isFinished()
        {
            return finished.get();
        }
        
        public boolean isBattleFinished()
        {
            return battleFinished.get();
        }

        @Override
        public void run()
        {
            battleFinished.set(battle.update());
            finished.set(true);
        }
    }
    
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
                    UpdateRunnable updateRunnable = new UpdateRunnable(entry.getValue());
                    Thread updateThread = new Thread(updateRunnable);
                    updateThread.start();
                    try { updateThread.join(2000); } catch(InterruptedException e){ /* exception ignored */ }
                    if(!updateRunnable.isFinished())
                    {
                        TurnBasedMinecraftMod.logger.warn("Battle (" + entry.getValue().getId() + "; " + entry.getValue().debugLog + ") update hanged for 2 seconds!");
                        try { updateThread.join(2000); } catch(InterruptedException e){ /* exception ignored */ }
                        if(!updateRunnable.isFinished())
                        {
                            TurnBasedMinecraftMod.logger.warn("Battle (" + entry.getValue().getId() + "; " + entry.getValue().debugLog + ") update hanged for 4 seconds!");
                            try { updateThread.join(2000); } catch(InterruptedException e){ /* exception ignored */ }
                            if(!updateRunnable.isFinished())
                            {
                                TurnBasedMinecraftMod.logger.error("Battle (" + entry.getValue().getId() + "; " + entry.getValue().debugLog + ") update timed out (6 seconds)!");
                                updateThread.interrupt();
                                try { updateThread.join(2000); } catch(InterruptedException e){ /* exception ignored */ }
                                if(!updateRunnable.isFinished())
                                {
                                    // TODO this is an ugly fix to a still-not-found freeze bug in Battle.update()
                                    TurnBasedMinecraftMod.logger.error("Battle update will not stop, forcing it to stop (8 seconds)!");
                                    updateThread.stop();
                                }
                            }
                        }
                    }
                    if(updateRunnable.isFinished() && updateRunnable.isBattleFinished())
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