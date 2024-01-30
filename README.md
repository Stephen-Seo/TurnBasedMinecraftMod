# TurnBasedMinecraftMod

This mod puts turn-based-combat of RPGs into Minecraft!

# Links/Downloads

Precompiled jars are available here:  
https://seodisparate.com/static/tbm_releases/  
https://burnedkirby.com/tbmm_downloads/  
https://www.curseforge.com/minecraft/mc-mods/turnbasedminecraft/files  
https://modrinth.com/mod/turnbasedmc  
https://git.seodisparate.com/stephenseo/TurnBasedMinecraftMod/releases

# Forge or NeoForge

The `forge` branch tracks the version of the mod for Minecraft Forge.

The `neoforge` branch tracks the version of the mod for Minecraft NeoForge.

# What changed in what version

See the [Changelog](https://github.com/Stephen-Seo/TurnBasedMinecraftMod/blob/master/Changelog.md)

# Things you may need to know about this mod

On first run, this mod will create a config file and some directories in your
Minecraft directory. They will typically be located at
`.minecraft/config/TurnBasedMinecraft`. (for the server they will be in the
`config` directory in the server directory.)

The config file `.minecraft/config/TurnBasedMinecraft/TBM_Config.toml` is commented
with info on what each option does. ~~It will also be moved if a newer version
of this mod has a newer version of the config file (usually renamed with a
timestamp).~~ ~~I will try my best to not move the previous version config, but rather
edit the previous version config to have new options.~~ When a new config version is made,
usually because a new entry has been added, the existing config is renamed to a file with
a timestamp in the filename of when it was replaced. One can set a config option in the
config to prevent it being overwritten if necessary.

Some options in the config file only affect the Server, and some only affect the Client.
When playing multiplayer, some configuration of the config on the server may be needed.

# Features

- Combat between players and mobs or other players will invoke a turn based battle
between them
- Supports use of the vanilla Minecraft bow and arrows (have bow selected when
in battle)
- Supports custom battle music to be played when fighting enemies. (They must
  be placed in `.minecraft/config/TurnBasedMinecraft/Music/battle` or
  `.minecraft/config/TurnBasedMinecraft/Music/silly`. Client-side config
  determines which song plays in battle for the client. only `.wav`,
  ~~`.mid`~~, `.mp3`, and `.ogg` files supported.  ~~Only `.mid` files are not
  affected by volume options (master and music sliders))~~ Midi file playback
  has been disabled for now due to lack of volume control issues. MP3 file
  playback sometimes fails, but seems to work better when the file is as
  "barebones" as possible (no album art metadata in the file).
  - It is recommended to use `.ogg` files for music.
  - Note that ogg Vorbis is supported, and NOT ogg Opus.
  - One can convert to ogg Vorbis with ffmpeg like this: `ffmpeg -i
    <my_music_file_to_convert> -map a:0 -c:a libvorbis output.ogg`.
- Config allows limiting number of combatants in turn-based battle.
- Config can be modified (server-side) to add entries of mobs from other mods.
(by default an unknown mob cannot enter turn-based battle, so the config must be
configured for them.)
  - [Alternatively, the command "/tbm-edit" can be used in-game to add/edit
  entities for the mod.](https://www.youtube.com/watch?v=MK648OVHddE)
  - [Also, one can make entries for specific custom names](https://youtu.be/9lBETQFMd3A)
  - [Server-side config can be edited in-game with the "/tbm-server-edit" command](https://youtu.be/9xkbHNWkcIY)

# Building

Simply invoke `./gradlew build` in the mod directory and after some time the
finished jar will be saved at
`build/libs/TurnBasedMinecraft-NeoForge-1.25.2-all.jar`

# Reproducibility

This mod should support reproducible builds. See `Reproducibility.md` to see
more details.

# Other notes

This mod uses [j-ogg-vorbis](https://github.com/stephengold/j-ogg-all)
available from [http://www.j-ogg.de](http://www.j-ogg.de) and copyrighted by
Tor-Einar Jarnbjo.

This mod also uses [JavaMP3](https://github.com/kevinstadler/JavaMP3)
which is licensed under the [MIT License](https://github.com/kevinstadler/JavaMP3/blob/master/LICENSE).

# Frequently Asked Questions

[See the FAQ page.](https://github.com/Stephen-Seo/TurnBasedMinecraftMod/blob/master/FAQ.md)

# Related Videos

[See related videos here](https://burnedkirby.com/posts/tbmm/)
