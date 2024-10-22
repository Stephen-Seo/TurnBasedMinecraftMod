package com.burnedkirby.TurnBasedMinecraft.common;


import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public class BattleUpdater
{
    private BattleManager manager;
    private AtomicBoolean isRunning;
    private int tick;
    private final int tickLimit = 3;

    public BattleUpdater(BattleManager manager)
    {
        this.manager = manager;
        isRunning = new AtomicBoolean(true);
        tick = 0;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning.set(isRunning);
    }

    @SubscribeEvent
    public void update(TickEvent.ServerTickEvent tickEvent) {
        if(tickEvent.phase != TickEvent.Phase.START && isRunning.get() && ++tick > tickLimit && tickEvent.haveTime()) {
            tick = 0;
            manager.battleMap.entrySet().removeIf(entry -> entry.getValue().update());
            manager.updateRecentlyLeftBattle();
        }
    }
}
