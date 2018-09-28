package com.seodisparate.TurnBasedMinecraft.client;

import org.apache.logging.log4j.Logger;

import com.seodisparate.TurnBasedMinecraft.common.Battle;
import com.seodisparate.TurnBasedMinecraft.common.CommonProxy;
import com.seodisparate.TurnBasedMinecraft.common.Config;
import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;

public class ClientProxy extends CommonProxy
{
    private BattleGui battleGui;
    private BattleMusic battleMusic;
    private Logger logger;
    private Config config;
    private int battleMusicCount;
    private int sillyMusicCount;
    private Battle localBattle;
    
    @Override
    public void initialize()
    {
        battleGui = new BattleGui();
        battleMusic = null; // will be initialized in postInit()
        battleMusicCount = 0;
        sillyMusicCount = 0;
        localBattle = null;
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
    public void battleStarted()
    {
        setBattleGuiAsGui();
    }

    @Override
    public void battleEnded()
    {
        localBattle = null;
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Minecraft.getMinecraft().displayGuiScreen(null);
            Minecraft.getMinecraft().setIngameFocus();
        });
        stopMusic(true);
        battleMusicCount = 0;
        sillyMusicCount = 0;
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
        battleMusic.playBattle(Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC));
    }

    @Override
    public void playSillyMusic()
    {
        battleMusic.playSilly(Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MUSIC));
    }

    @Override
    public void stopMusic(boolean resumeMCSounds)
    {
        battleMusic.stopMusic(resumeMCSounds);
    }

    /**
     * Sets what music to play based on type and loaded Config
     */
    @Override
    public void typeEnteredBattle(String type)
    {
        if(localBattle == null)
        {
            return;
        }
        if(type == null || type.isEmpty() || config.isBattleMusicType(type))
        {
            ++battleMusicCount;
        }
        else if(config.isSillyMusicType(type))
        {
            ++sillyMusicCount;
        }
        else
        {
            ++battleMusicCount;
        }
        checkBattleTypes();
    }

    @Override
    public void typeLeftBattle(String type)
    {
        if(localBattle == null)
        {
            return;
        }
        if(type == null || type.isEmpty() || config.isBattleMusicType(type))
        {
            --battleMusicCount;
        }
        else if(config.isSillyMusicType(type))
        {
            --sillyMusicCount;
        }
        else
        {
            --battleMusicCount;
        }
        checkBattleTypes();
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
    
    private void checkBattleTypes()
    {
        float percentage = 0.0f;
        if(sillyMusicCount == 0 && battleMusicCount == 0)
        {
            percentage = 0.0f;
        }
        else if(battleMusicCount == 0)
        {
            percentage = 100.0f;
        }
        else
        {
            percentage = 100.0f * (float)sillyMusicCount / (float)(sillyMusicCount + battleMusicCount);
        }
        
        if(percentage >= (float)TurnBasedMinecraftMod.getConfig().getSillyMusicThreshold())
        {
            if(battleMusic.isPlaying())
            {
                if(!battleMusic.isPlayingSilly() && battleMusic.hasSillyMusic())
                {
                    stopMusic(false);
                    playSillyMusic();
                }
            }
            else if(battleMusic.hasSillyMusic())
            {
                playSillyMusic();
            }
        }
        else
        {
            if(battleMusic.isPlaying())
            {
                if(battleMusic.isPlayingSilly() && battleMusic.hasBattleMusic())
                {
                    stopMusic(false);
                    playBattleMusic();
                }
            }
            else if(battleMusic.hasBattleMusic())
            {
                playBattleMusic();
            }
        }
    }

    @Override
    public Battle getLocalBattle()
    {
        return localBattle;
    }

    @Override
    public void createLocalBattle(int id)
    {
        localBattle = new Battle(id, null, null, false);
    }
}
