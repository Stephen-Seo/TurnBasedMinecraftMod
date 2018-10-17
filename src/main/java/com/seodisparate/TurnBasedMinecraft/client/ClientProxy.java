package com.seodisparate.TurnBasedMinecraft.client;

import com.seodisparate.TurnBasedMinecraft.common.Battle;
import com.seodisparate.TurnBasedMinecraft.common.CommonProxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;

public class ClientProxy extends CommonProxy
{
    private BattleGui battleGui = null;
    private BattleMusic battleMusic = null;
    private int battleMusicCount = 0;
    private int sillyMusicCount = 0;
    private Battle localBattle = null;
    
    @Override
    protected void initializeClient()
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
        battleGui.setTimeRemaining(timeRemaining);
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
    protected void postInitClient()
    {
        battleMusic = new BattleMusic(getLogger());
    }

    @Override
    public void playBattleMusic()
    {
        GameSettings gs = Minecraft.getMinecraft().gameSettings;
        battleMusic.playBattle(gs.getSoundLevel(SoundCategory.MUSIC) * gs.getSoundLevel(SoundCategory.MASTER));
    }

    @Override
    public void playSillyMusic()
    {
        GameSettings gs = Minecraft.getMinecraft().gameSettings;
        battleMusic.playSilly(gs.getSoundLevel(SoundCategory.MUSIC) * gs.getSoundLevel(SoundCategory.MASTER));
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
        if(type == null || type.isEmpty() || getConfig().isBattleMusicType(type))
        {
            ++battleMusicCount;
        }
        else if(getConfig().isSillyMusicType(type))
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
        if(type == null || type.isEmpty() || getConfig().isBattleMusicType(type))
        {
            --battleMusicCount;
        }
        else if(getConfig().isSillyMusicType(type))
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
        
        if(percentage >= (float)getConfig().getSillyMusicThreshold())
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
        localBattle = new Battle(null, id, null, null, false);
    }
}
