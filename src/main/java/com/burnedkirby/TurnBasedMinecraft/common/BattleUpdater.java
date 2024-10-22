package com.burnedkirby.TurnBasedMinecraft.common;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
    public void update(ServerTickEvent.Post tickEvent) {
        //if(tickEvent.phase != TickEvent.Phase.START && isRunning.get() && ++tick > tickLimit) {
        if(isRunning.get() && ++tick > tickLimit && tickEvent.hasTime()) {
            tick = 0;
            manager.battleMap.entrySet().removeIf(entry -> entry.getValue().update());
            manager.updateRecentlyLeftBattle();
        }
    }
}