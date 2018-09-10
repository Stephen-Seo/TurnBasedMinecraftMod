package com.seodisparate.TurnBasedMinecraft.client;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.seodisparate.TurnBasedMinecraft.TurnBasedMinecraftMod;
import com.seodisparate.TurnBasedMinecraft.common.Battle;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class BattleGui extends GuiScreen
{
    public AtomicInteger timeRemaining;
    public long lastInstant;
    public long elapsedTime;
    
    public BattleGui()
    {
        timeRemaining = new AtomicInteger((int)TurnBasedMinecraftMod.BattleDecisionTime.getSeconds());
        lastInstant = System.nanoTime();
        elapsedTime = 0;
    }
    
    public void turnBegin()
    {
        TurnBasedMinecraftMod.currentBattle.setState(Battle.State.ACTION);
        // TODO reset gui since decisions ended
    }
    
    public void turnEnd()
    {
        TurnBasedMinecraftMod.currentBattle.setState(Battle.State.DECISION);
        timeRemaining.set((int)TurnBasedMinecraftMod.BattleDecisionTime.getSeconds());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        // TODO Auto-generated method stub
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        if(TurnBasedMinecraftMod.currentBattle.getState() == Battle.State.DECISION && timeRemaining.get() > 0)
        {
            long nextInstant = System.nanoTime();
            elapsedTime += nextInstant - lastInstant;
            lastInstant = nextInstant;
            while(elapsedTime > 1000000000)
            {
                elapsedTime -= 1000000000;
                timeRemaining.decrementAndGet();
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        // TODO Auto-generated method stub
        super.actionPerformed(button);
    }

    @Override
    public void initGui()
    {
        // TODO Auto-generated method stub
        super.initGui();
    }

    @Override
    public void onGuiClosed()
    {
        // TODO Auto-generated method stub
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
