# TurnBasedMinecraftMod

This mod puts turn-based-combat of RPGs into Minecraft!

# Downloads

Precompiled jars are available here:
https://seodisparate.com/static/tbm_releases/
or here:
https://minecraft.curseforge.com/projects/turnbasedminecraft/files

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
timestamp).~~ I will try my best to not move the previous version config, but rather
edit the previous version config to have new options.

Some options in the config file only affect the Server, and some only affect the Client.
When playing multiplayer, some configuration of the config on the server may be needed.

# Features

- Combat between players and mobs or other players will invoke a turn based battle
between them
- Supports use of the vanilla Minecraft bow and arrows (have bow selected when
in battle)
- Supports custom battle music to be played when fighting enemies. (They must be
placed in `.minecraft/config/TurnBasedMinecraft/Music/battle` or
`.minecraft/config/TurnBasedMinecraft/Music/silly`. Client-side config determines
which song plays in battle for the client. only `.wav`, `.mid`, and `.mp3` files
supported.  Only `.mid` files are not affected by volume options (master and
music sliders))
- Config allows limiting number of combatants in turn-based battle.
- Config can be modified (server-side) to add entries of mobs from other mods.
(by default an unknown mob cannot enter turn-based battle, so the config must be
configured for them.)
  - [Alternatively, the command "/tbm-edit" can be used in-game to add/edit
  entities for the mod.](https://www.youtube.com/watch?v=MK648OVHddE)
  - [Also, one can make entries for specific custom names](https://youtu.be/9lBETQFMd3A)

# Building

Simply invoke `./gradlew build` in the mod directory and after some time the
finished jar will be saved at "build/reobfShadowJar/output.jar"

# Other notes

This mod uses [shadow](https://github.com/johnrengelman/shadow) which is
licenced under the [Apache License 2.0](https://github.com/johnrengelman/shadow/blob/master/LICENSE).

This mod also uses [JavaMP3](https://github.com/kevinstadler/JavaMP3)
which is licensed under the [MIT License](https://github.com/kevinstadler/JavaMP3/blob/master/LICENSE).

# Related Videos

[See related videos here](https://burnedkirby.com/tbmm_vids/)
