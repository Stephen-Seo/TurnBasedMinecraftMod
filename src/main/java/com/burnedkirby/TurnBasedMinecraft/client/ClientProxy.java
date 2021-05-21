package com.burnedkirby.TurnBasedMinecraft.client;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.CommonProxy;

import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import java.util.UUID;

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
        logger.debug("Init client");
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
        if(Minecraft.getInstance().screen != battleGui)
        {
            battleGui.turnEnd();
            Minecraft.getInstance().setScreen(battleGui);
        }
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
        Minecraft.getInstance().setScreen(null);
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
        GameSettings gs = Minecraft.getInstance().options;
        battleMusic.playBattle(gs.getSoundSourceVolume(SoundCategory.MUSIC) * gs.getSoundSourceVolume(SoundCategory.MASTER));
    }

    @Override
    public void playSillyMusic()
    {
        GameSettings gs = Minecraft.getInstance().options;
        battleMusic.playSilly(gs.getSoundSourceVolume(SoundCategory.MUSIC) * gs.getSoundSourceVolume(SoundCategory.MASTER));
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
        StringTextComponent prefix = new StringTextComponent("TBM: ");
        // func_240718_a_ is set color
        // func_240713_a_ is set bold
        prefix.withStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
        StringTextComponent text = new StringTextComponent(message);
        prefix.getSiblings().add(text);
        text.withStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));
        // UUID is required by sendMessage, but appears to be unused, so just give dummy UUID
        Minecraft.getInstance().player.sendMessage(prefix, UUID.randomUUID());
    }

    @Override
    public void displayTextComponent(ITextComponent text)
    {
        // UUID is required by sendMessage, but appears to be unused, so just give dummy UUID
        Minecraft.getInstance().player.sendMessage(text, UUID.randomUUID());
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
        localBattle = new Battle(null, id, null, null, false, Minecraft.getInstance().level.dimension());
    }

    @Override
    public Entity getEntity(int id, RegistryKey<World> dim) {
        return Minecraft.getInstance().level.getEntity(id);
    }
}
