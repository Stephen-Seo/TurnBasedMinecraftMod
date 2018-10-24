package com.seodisparate.TurnBasedMinecraft.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;

import org.apache.logging.log4j.Logger;

import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.client.Minecraft;

public class BattleMusic
{
    private Logger logger;
    private ArrayList<File> battleMusic;
    private ArrayList<File> sillyMusic;
    private boolean initialized;
    private File nextBattle;
    private File nextSilly;
    private Sequencer sequencer;
    private Clip clip;
    private boolean playingIsSilly;
    private boolean isPlaying;
    
    public BattleMusic(Logger logger)
    {
        initialized = false;
        this.logger = logger;
        battleMusic = new ArrayList<File>();
        sillyMusic = new ArrayList<File>();
        isPlaying = false;
        
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
        } catch (Throwable t)
        {
            logger.error("Failed to load midi sequencer");
            return;
        }
        
        try
        {
            clip = AudioSystem.getClip();
        } catch(LineUnavailableException e)
        {
            logger.error("Failed to load clip (for wav)");
            return;
        }
        
        File battleMusicFolder = new File(TurnBasedMinecraftMod.MUSIC_BATTLE);
        File sillyMusicFolder = new File(TurnBasedMinecraftMod.MUSIC_SILLY);
        
        if(!battleMusicFolder.exists())
        {
            if(!battleMusicFolder.mkdirs())
            {
                logger.error("Failed to create " + TurnBasedMinecraftMod.MUSIC_BATTLE);
                return;
            }
        }
        if(!sillyMusicFolder.exists())
        {
            if(!sillyMusicFolder.mkdirs())
            {
                logger.error("Failed to create " + TurnBasedMinecraftMod.MUSIC_SILLY);
                return;
            }
        }
        
        File[] battleFiles = battleMusicFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name)
            {
                int extIndex = name.lastIndexOf(".");
                if(extIndex == -1)
                {
                    return false;
                }
                String ext = name.substring(extIndex + 1).toLowerCase();
                return ext.equals("mid") || ext.equals("wav");
            }
        });
        for(File f : battleFiles)
        {
            battleMusic.add(f);
        }
        
        File[] sillyFiles = sillyMusicFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name)
            {
                int extIndex = name.lastIndexOf(".");
                if(extIndex == -1)
                {
                    return false;
                }
                String ext = name.substring(extIndex + 1).toLowerCase();
                return ext.equals("mid") || ext.equals("wav");
            }
        });
        for(File f : sillyFiles)
        {
            sillyMusic.add(f);
        }
        
        initialized = true;
        
        pickNextBattle();
        pickNextSilly();
    }
    
    private void pickNextBattle()
    {
        if(!initialized || battleMusic.isEmpty())
        {
            nextBattle = null;
        }
        else
        {
            nextBattle = battleMusic.get((int)(Math.random() * battleMusic.size()));
        }
    }
    
    private void pickNextSilly()
    {
        if(!initialized || sillyMusic.isEmpty())
        {
            nextSilly = null;
        }
        else
        {
            nextSilly = sillyMusic.get((int)(Math.random() * sillyMusic.size()));
        }
    }
    
    public void playBattle(float volume)
    {
        if(!initialized || volume <= 0.0f || battleMusic.isEmpty())
        {
            return;
        }
        else if(volume > 1.0f)
        {
            volume = 1.0f;
        }
        play(nextBattle, volume);
        pickNextBattle();
        playingIsSilly = false;
        isPlaying = true;
    }
    
    public void playSilly(float volume)
    {
        if(!initialized || volume <= 0.0f || sillyMusic.isEmpty())
        {
            return;
        }
        else if(volume > 1.0f)
        {
            volume = 1.0f;
        }
        play(nextSilly, volume);
        pickNextSilly();
        playingIsSilly = true;
        isPlaying = true;
    }
    
    private void play(File next, float volume)
    {
        if(initialized && next != null)
        {
            logger.debug("play called with file " + next.getName() + " and vol " + volume);
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().getSoundHandler().pauseSounds();
            });
            String suffix = next.getName().substring(next.getName().length() - 3).toLowerCase();
            if(suffix.equals("mid"))
            {
                sequencer.stop();
                clip.stop();
                clip.close();
                try {
                    sequencer.setSequence(new BufferedInputStream(new FileInputStream(next)));
                } catch (Throwable t)
                {
                    logger.error("Failed to play battle music (midi)");
                    return;
                }
                
                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                sequencer.start();
            }
            else if(suffix.equals("wav"))
            {
                if(sequencer.isRunning())
                {
                    sequencer.stop();
                }
                if(clip.isActive())
                {
                    clip.stop();
                    clip.close();
                }
                try
                {
                    clip.open(AudioSystem.getAudioInputStream(next));
                } catch(Throwable t)
                {
                    logger.error("Failed to load battle music (wav)");
                    return;
                }
                
                // set volume
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(volume * 20.0f - 20.0f); // in decibels
                
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.start();
            }
        }
    }
    
    public void stopMusic(boolean resumeMCSounds)
    {
        sequencer.stop();
        clip.stop();
        clip.close();
        if(resumeMCSounds)
        {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().getSoundHandler().resumeSounds();
            });
        }
        isPlaying = false;
    }
    
    public boolean isPlayingSilly()
    {
        return playingIsSilly;
    }
    
    public boolean isPlaying()
    {
        return isPlaying || sequencer.isRunning() || clip.isActive();
    }
    
    public boolean hasBattleMusic()
    {
        return !battleMusic.isEmpty();
    }
    
    public boolean hasSillyMusic()
    {
        return !sillyMusic.isEmpty();
    }
}
