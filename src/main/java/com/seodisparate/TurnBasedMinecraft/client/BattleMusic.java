package com.seodisparate.TurnBasedMinecraft.client;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.*;

import fr.delthas.javamp3.Sound;
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
    private Thread mp3StreamThread;
    private MP3Streamer mp3StreamRunnable;

    public BattleMusic(Logger logger)
    {
        initialized = false;
        this.logger = logger;
        battleMusic = new ArrayList<File>();
        sillyMusic = new ArrayList<File>();
        isPlaying = false;
        mp3StreamThread = null;
        mp3StreamRunnable = null;

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
                return ext.equals("mid") || ext.equals("wav") || ext.equals("mp3");
            }
        });
        for(File f : battleFiles)
        {
            battleMusic.add(f);
        }
        logger.info("Got " + battleMusic.size() + " battle music files");

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
                return ext.equals("mid") || ext.equals("wav") || ext.equals("mp3");
            }
        });
        for(File f : sillyFiles)
        {
            sillyMusic.add(f);
        }
        logger.info("Got " + sillyMusic.size() + " battle music files");

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
        play(nextBattle, volume, true);
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
        play(nextSilly, volume, false);
        pickNextSilly();
        playingIsSilly = true;
        isPlaying = true;
    }
    
    private void play(File next, float volume, boolean isBattleType)
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
                if(sequencer.isRunning())
                {
                    sequencer.stop();
                }
                if(clip.isActive())
                {
                    clip.stop();
                    clip.close();
                }
                if(mp3StreamThread != null && mp3StreamThread.isAlive())
                {
                    mp3StreamRunnable.setKeepPlaying(false);
                    try { mp3StreamThread.join(); } catch (Throwable t) { /* ignored */ }
                }

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
                if(mp3StreamThread != null && mp3StreamThread.isAlive())
                {
                    mp3StreamRunnable.setKeepPlaying(false);
                    try { mp3StreamThread.join(); } catch (Throwable t) { /* ignored */ }
                }

                try
                {
                    clip.open(AudioSystem.getAudioInputStream(next));
                } catch(Throwable t)
                {
                    logger.error("Failed to play battle music (wav)");
                    return;
                }
                
                // set volume
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(volume * 20.0f - 20.0f); // in decibels

                clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.start();
            }
            else if(suffix.equals("mp3"))
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
                if(mp3StreamThread != null && mp3StreamThread.isAlive())
                {
                    mp3StreamRunnable.setKeepPlaying(false);
                    try { mp3StreamThread.join(); } catch (Throwable t) { /* ignored */ }
                }

                try
                {
                    if(mp3StreamRunnable == null)
                    {
                        mp3StreamRunnable = new MP3Streamer(next, logger, volume);
                    }
                    else
                    {
                        mp3StreamRunnable.setMp3File(next);
                        mp3StreamRunnable.setVolume(volume);
                    }
                    mp3StreamThread = new Thread(mp3StreamRunnable);
                    mp3StreamThread.start();

                    logger.info("Started playing mp3 " + next.getName());
                }
                catch (Throwable t)
                {
                    logger.error("Failed to play battle music (mp3)");
                    return;
                }
            }
        }
    }
    
    public void stopMusic(boolean resumeMCSounds)
    {
        sequencer.stop();
        clip.stop();
        clip.close();
        if(mp3StreamThread != null && mp3StreamThread.isAlive())
        {
            mp3StreamRunnable.setKeepPlaying(false);
            try { mp3StreamThread.join(); } catch (Throwable t) { /* ignored */ }
        }
        if(resumeMCSounds)
        {
            Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().getSoundHandler().resumeSounds() );
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

    private class MP3Streamer implements Runnable
    {
        private AtomicBoolean keepPlaying;
        private File mp3File;
        private Logger logger;
        private float volume;

        public MP3Streamer(File mp3File, Logger logger, float volume)
        {
            keepPlaying = new AtomicBoolean(true);
            this.mp3File = mp3File;
            this.logger = logger;
            this.volume = volume;
            if(this.volume > 1.0f)
            {
                this.volume = 1.0f;
            }
            else if(this.volume < 0.0f)
            {
                this.volume = 0.0f;
            }
        }

        public void setKeepPlaying(boolean playing)
        {
            keepPlaying.set(playing);
        }

        public void setMp3File(File mp3File)
        {
            this.mp3File = mp3File;
        }

        public void setVolume(float volume)
        {
            this.volume = volume;
        }

        @Override
        public void run()
        {
            keepPlaying.set(true);
            SourceDataLine sdl = null;
            try
            {
                Sound mp3Sound = new Sound(new FileInputStream(mp3File));
                AudioFormat audioFormat = mp3Sound.getAudioFormat();
                sdl = AudioSystem.getSourceDataLine(audioFormat);
                sdl.open(audioFormat);
                {
                    FloatControl volumeControl = (FloatControl) sdl.getControl(FloatControl.Type.MASTER_GAIN);
                    volumeControl.setValue(volume * 20.0f - 20.0f); // in decibels
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] cached = null;
                int cachedOffset = 0;
                int cachedSize = 0;
                byte[] buf = new byte[4096];
                sdl.start();
                int read = mp3Sound.read(buf, 0, 4096);
                while(keepPlaying.get())
                {
                    if(baos != null)
                    {
                        if(read != -1)
                        {
                            sdl.write(buf, 0, read);
                            baos.write(buf, 0, read);
                            read = mp3Sound.read(buf, 0, 4096);
                        }
                        else
                        {
                            mp3Sound.close();
                            mp3Sound = null;
                            cached = baos.toByteArray();
                            baos = null;
                        }
                    }
                    else
                    {
                        cachedSize = cached.length - cachedOffset;
                        if(cachedSize > 4096)
                        {
                            cachedSize = 4096;
                        }
                        sdl.write(cached, cachedOffset, cachedSize);
                        cachedOffset += cachedSize;
                        if(cachedOffset >= cached.length)
                        {
                            cachedOffset = 0;
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                logger.error("Stream play mp3", t);
            }
            if(sdl != null)
            {
                sdl.stop();
                sdl.flush();
                sdl.close();
            }
        }
    }
}
