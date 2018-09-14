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
    
    public BattleMusic(Logger logger)
    {
        initialized = false;
        this.logger = logger;
        battleMusic = new ArrayList<File>();
        sillyMusic = new ArrayList<File>();
        
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
        } catch (Exception e)
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
                if(ext.equals("mid") || ext.equals("wav"))
                {
                    return true;
                }
                return false;
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
                if(ext.equals("mid") || ext.equals("wav"))
                {
                    return true;
                }
                return false;
            }
        });
        for(File f : sillyFiles)
        {
            sillyMusic.add(f);
        }
        
        pickNextBattle();
        pickNextSilly();
        
        initialized = true;
    }
    
    private void pickNextBattle()
    {
        if(!initialized || battleMusic.isEmpty())
        {
            nextBattle = null;
            return;
        }
        nextBattle = battleMusic.get((int)(Math.random() * battleMusic.size()));
    }
    
    private void pickNextSilly()
    {
        if(!initialized || sillyMusic.isEmpty())
        {
            nextSilly = null;
            return;
        }
        nextSilly = sillyMusic.get((int)(Math.random() * sillyMusic.size()));
    }
    
    public void playBattle()
    {
        if(!initialized)
        {
            return;
        }
        play(nextBattle);
        pickNextBattle();
        playingIsSilly = false;
    }
    
    public void playSilly()
    {
        if(!initialized)
        {
            return;
        }
        play(nextSilly);
        pickNextSilly();
        playingIsSilly = true;
    }
    
    private void play(File next)
    {
        if(initialized && next != null)
        {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().getSoundHandler().pauseSounds();
            });
            String suffix = next.getName().substring(next.getName().length() - 3).toLowerCase();
            if(suffix.equals("mid"))
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
                try {
                    sequencer.setSequence(new BufferedInputStream(new FileInputStream(next)));
                } catch (Exception e)
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
                } catch(Exception e)
                {
                    logger.error("Failed to load battle music (wav)");
                    return;
                }
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.start();
            }
        }
    }
    
    public void stopMusic()
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
    }
    
    public boolean isPlayingSilly()
    {
        return playingIsSilly;
    }
    
    public boolean isPlaying()
    {
        return sequencer.isRunning() || clip.isActive();
    }
}
