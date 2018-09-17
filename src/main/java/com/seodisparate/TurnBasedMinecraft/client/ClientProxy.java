package com.seodisparate.TurnBasedMinecraft.client;

import org.apache.logging.log4j.Logger;

import com.seodisparate.TurnBasedMinecraft.common.CommonProxy;
import com.seodisparate.TurnBasedMinecraft.common.Config;
import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.TextComponentString;

public class ClientProxy extends CommonProxy
{
    private BattleGui battleGui;
    private BattleMusic battleMusic;
    private Logger logger;
    private Config config;
    
    public ClientProxy()
    {
        super();
        battleGui = new BattleGui();
        battleMusic = null; // will be initialized in postInit()
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

    @Override
    public void postInit()
    {
        battleMusic = new BattleMusic(logger);
    }

    @Override
    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    @Override
    public void playBattleMusic()
    {
        battleMusic.playBattle();
    }

    @Override
    public void playSillyMusic()
    {
        battleMusic.playSilly();
    }

    @Override
    public void stopMusic()
    {
        battleMusic.stopMusic();
    }

    /**
     * Sets what music to play based on type and loaded Config
     */
    @Override
    public void typeEnteredBattle(String type)
    {
        if(type == null || type.isEmpty() || config.isBattleMusicType(type))
        {
            if(battleMusic.isPlaying())
            {
                if(battleMusic.isPlayingSilly())
                {
                    stopMusic();
                    playBattleMusic();
                }
            }
            else
            {
                playBattleMusic();
            }
        }
        else if(config.isSillyMusicType(type))
        {
            if(battleMusic.isPlaying())
            {
                if(!battleMusic.isPlayingSilly())
                {
                    stopMusic();
                    playSillyMusic();
                }
            }
            else
            {
                playSillyMusic();
            }
        }
    }

    @Override
    public void setConfig(Config config)
    {
        this.config = config;
    }

    @Override
    public void displayString(String message)
    {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(message));
    }

    @Override
    public Entity getEntityByID(int id)
    {
        return Minecraft.getMinecraft().world.getEntityByID(id);
    }
}
