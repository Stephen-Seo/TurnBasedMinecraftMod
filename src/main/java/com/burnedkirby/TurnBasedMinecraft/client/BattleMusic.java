package com.burnedkirby.TurnBasedMinecraft.client;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import de.jarnbjo.vorbis.VorbisAudioFileReader;
import fr.delthas.javamp3.Sound;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class BattleMusic
{
    private Logger logger;
    private ArrayList<File> battleMusic;
    private ArrayList<File> sillyMusic;
    private boolean initialized;
    private File nextBattle;
    private File nextSilly;
    private Sequencer sequencer;
    private AudioInputStream wavInputStream;
    private Clip clip;
    private boolean playingIsSilly;
    private boolean isPlaying;
    private Thread mp3StreamThread;
    private Thread oggVorbisStreamThread;
    private MP3Streamer mp3StreamRunnable;
    private OGGVorbisStreamer oggVorbisStreamRunnable;

    public BattleMusic(Logger logger)
    {
        initialized = false;
        this.logger = logger;
        battleMusic = new ArrayList<File>();
        sillyMusic = new ArrayList<File>();
        isPlaying = false;
        mp3StreamThread = null;
        mp3StreamRunnable = null;

//        try {
//            sequencer = MidiSystem.getSequencer();
//            sequencer.open();
//        } catch (Throwable t) {
//            logger.error("Failed to load midi sequencer");
//            t.printStackTrace();
//            sequencer = null;
//        }
        sequencer = null; // midi disabled
        
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
//                return ext.equals("mid") || ext.equals("wav") || ext.equals("mp3");
                return ext.equals("wav") || ext.equals("mp3") || ext.equals("ogg"); // midi disabled
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
//                return ext.equals("mid") || ext.equals("wav") || ext.equals("mp3");
                return ext.equals("wav") || ext.equals("mp3") || ext.equals("ogg"); // midi disabled
            }
        });
        for(File f : sillyFiles)
        {
            sillyMusic.add(f);
        }
        logger.info("Got " + sillyMusic.size() + " silly music files");

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
            TurnBasedMinecraftMod.proxy.pauseMCMusic();
            String suffix = next.getName().substring(next.getName().length() - 3).toLowerCase();
            if(suffix.equals("mid") && sequencer != null)
            {
                if(sequencer.isRunning())
                {
                    sequencer.stop();
                }
                if(clip != null && clip.isActive())
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
                    t.printStackTrace();
                    return;
                }

                try {
                    for (MidiChannel channel : MidiSystem.getSynthesizer().getChannels()) {
                        channel.controlChange(7, (int)(volume * 127));
                    }
                } catch (MidiUnavailableException e) {
                    logger.error("Failed to set Midi volume");
                    e.printStackTrace();
                    return;
                }

                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                sequencer.start();

                logger.info("Played music (midi) " + next.getName());
            }
            else if(suffix.equals("wav"))
            {
                if(sequencer != null && sequencer.isRunning())
                {
                    sequencer.stop();
                }
                if(clip != null && clip.isActive())
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
                    if(wavInputStream != null) {
                        wavInputStream.close();
                    }
                    wavInputStream = AudioSystem.getAudioInputStream(next);
                    AudioFormat format = wavInputStream.getFormat();
                    DataLine.Info info = new DataLine.Info(Clip.class, format);
                    clip = (Clip) AudioSystem.getLine(info);
                    clip.open(wavInputStream);
                } catch(Throwable t)
                {
                    logger.error("Failed to play battle music (wav)");
                    t.printStackTrace();
                    return;
                }
                
                // set volume
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(BattleMusic.percentageToDecibels(volume)); // in decibels

                clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.start();

                logger.info("Playing music (wav) " + next.getName());
            }
            else if(suffix.equals("mp3"))
            {
                if(sequencer != null && sequencer.isRunning())
                {
                    sequencer.stop();
                }
                if(clip != null && clip.isActive())
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
                    t.printStackTrace();
                    return;
                }
            }
            else if (suffix.equals("ogg")) {
                if(sequencer != null && sequencer.isRunning())
                {
                    sequencer.stop();
                }
                if(clip != null && clip.isActive())
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
                    if (oggVorbisStreamRunnable == null) {
                        oggVorbisStreamRunnable = new OGGVorbisStreamer(next, logger, volume);
                    } else {
                        oggVorbisStreamRunnable.setOggVorbisFile(next);
                        oggVorbisStreamRunnable.setVolume(volume);
                    }

                    oggVorbisStreamThread = new Thread(oggVorbisStreamRunnable);
                    oggVorbisStreamThread.start();
                    logger.info("Started playing OggVorbis " + next.getName());
                } catch (Throwable t) {
                    logger.error("Failed to play battle music (ogg)");
                    t.printStackTrace();
                    return;
                }
            }
        }
    }
    
    public void stopMusic(boolean resumeMCSounds)
    {
        if(sequencer != null) {
            sequencer.stop();
        }
        if(clip != null) {
            clip.stop();
            clip.close();
        }
        if(mp3StreamThread != null && mp3StreamThread.isAlive())
        {
            mp3StreamRunnable.setKeepPlaying(false);
            try { mp3StreamThread.join(); } catch (Throwable t) { /* ignored */ }
        }
        if (oggVorbisStreamThread != null && oggVorbisStreamThread.isAlive()) {
            oggVorbisStreamRunnable.setKeepPlaying(false);
            try { oggVorbisStreamThread.join(); } catch (Throwable t) { /* ignored */ }
        }
        if(resumeMCSounds)
        {
            TurnBasedMinecraftMod.proxy.resumeMCMusic();
        }
        isPlaying = false;
    }
    
    public boolean isPlayingSilly()
    {
        return playingIsSilly;
    }
    
    public boolean isPlaying()
    {
        return isPlaying || (sequencer != null && sequencer.isRunning()) || (clip != null && clip.isActive());
    }
    
    public boolean hasBattleMusic()
    {
        return !battleMusic.isEmpty();
    }
    
    public boolean hasSillyMusic()
    {
        return !sillyMusic.isEmpty();
    }

    public static float percentageToDecibels(float percentage) {
        if (percentage > 1.0F) {
            return 0.0F;
        } else if (percentage <= 0.0F) {
            return Float.NEGATIVE_INFINITY;
        } else {
            return (float) (Math.log10(percentage) * 20.0);
        }
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
                    volumeControl.setValue(BattleMusic.percentageToDecibels(volume)); // in decibels
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

    private class OGGVorbisStreamer implements Runnable {
        private AtomicBoolean keepPlaying;
        private File oggVorbisFile;
        private Logger logger;
        private float volume;

        public OGGVorbisStreamer(File oggVorbisFile, Logger logger, float volume) {
            keepPlaying = new AtomicBoolean(true);
            this.oggVorbisFile = oggVorbisFile;
            this.logger = logger;
            this.volume = volume;
            if (this.volume > 1.0F) {
                this.volume = 1.0F;
            } else if (this.volume < 0.0F) {
                this.volume = 0.0F;
            }
        }

        public void setKeepPlaying(boolean playing) {
            keepPlaying.set(playing);
        }

        public void setOggVorbisFile(File oggVorbisFile) {
            this.oggVorbisFile = oggVorbisFile;
        }

        public void setVolume(float volume) {
            this.volume = volume;
        }

        @Override
        public void run() {
            keepPlaying.set(true);
            SourceDataLine sdl = null;
            try {
                VorbisAudioFileReader reader = new VorbisAudioFileReader();
                AudioFormat audioFormat = reader.getAudioFileFormat(oggVorbisFile).getFormat();
                sdl = AudioSystem.getSourceDataLine(audioFormat);
                sdl.open(audioFormat);
                {
                    FloatControl volumeControl = (FloatControl) sdl.getControl(FloatControl.Type.MASTER_GAIN);
                    volumeControl.setValue(BattleMusic.percentageToDecibels(volume)); // in decibels
                }

                AudioInputStream ais = reader.getAudioInputStream(oggVorbisFile);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] cached = null;
                int cachedOffset = 0;
                int cachedSize = 0;
                byte[] buf = new byte[4096];
                sdl.start();
                int read = ais.read(buf);
                while (keepPlaying.get()) {
                    if (baos != null) {
                        if (read != -1) {
                            sdl.write(buf, 0, read);
                            baos.write(buf, 0, read);
                            read = ais.read(buf);
                        } else {
                            ais.close();
                            ais = null;
                            cached = baos.toByteArray();
                            baos = null;
                        }
                    } else {
                        cachedSize = cached.length - cachedOffset;
                        if (cachedSize > 4096) {
                            cachedSize = 4096;
                        }
                        sdl.write(cached, cachedOffset, cachedSize);
                        cachedOffset += cachedSize;
                        if (cachedOffset >= cached.length) {
                            cachedOffset = 0;
                        }
                    }
                }
            } catch (Throwable t) {
                logger.error("Stream play oggVorbis", t);
            }

            if (sdl != null) {
                sdl.stop();
                sdl.flush();
                sdl.close();
            }
        }
    }
}
