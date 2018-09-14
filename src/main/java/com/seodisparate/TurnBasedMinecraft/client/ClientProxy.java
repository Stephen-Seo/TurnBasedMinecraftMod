package com.seodisparate.TurnBasedMinecraft.client;

import com.seodisparate.TurnBasedMinecraft.common.CommonProxy;
import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.client.Minecraft;

public class ClientProxy extends CommonProxy
{
    private BattleGui battleGui;
    
    public ClientProxy()
    {
        super();
        battleGui = new BattleGui();
    }
    
    @Override
    public void setBattleGuiTime(int timeRemaining)
    {
        battleGui.timeRemaining.set(timeRemaining);
    }

    @Override
    public void setBattleGuiBattleChanged()
    {
        battleGui.battleChanged();
    }

    @Override
    public void setBattleGuiAsGui()
    {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if(Minecraft.getMinecraft().currentScreen != battleGui)
            {
                battleGui.turnEnd();
                Minecraft.getMinecraft().displayGuiScreen(battleGui);
            }
        });
    }

    @Override
    public void battleGuiTurnBegin()
    {
        battleGui.turnBegin();
    }

    @Override
    public void battleGuiTurnEnd()
    {
        battleGui.turnEnd();
    }

    @Override
    public void battleEnded()
    {
        TurnBasedMinecraftMod.currentBattle = null;
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Minecraft.getMinecraft().displayGuiScreen(null);
            Minecraft.getMinecraft().setIngameFocus();
        });
    }
}
