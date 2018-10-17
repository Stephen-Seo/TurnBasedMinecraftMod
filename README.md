# TurnBasedMinecraftMod

This mod puts turn-based-combat of RPGs into Minecraft!

# Downloads

Precompiled jars are available here:
https://seodisparate.com/static/tbm_releases/

# What changed in what version

See the [Changelog](https://github.com/Stephen-Seo/TurnBasedMinecraftMod/blob/master/Changelog.md)

# Things you may need to know about this mod

On first run, this mod will create a config file and some directories in your
Minecraft directory. They will typically be located at
`.minecraft/config/TurnBasedMinecraft`. (for the server they will be in the
`config` directory in the server directory.)

The config file `.minecraft/config/TurnBasedMinecraft/TBM_Config.xml` is commented
with info on what each option does. It will also be moved if a newer version
of this mod has a newer version of the config file (usually renamed with a
timestamp).

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
which song plays in battle for the client. only `.wav` and `.mid` files supported.
Only `.wav` files are affected by volume options (master and music sliders))
- Config allows limiting number of combatants in turn-based battle.
- Config can be modified (server-side) to add entries of mobs from other mods.
(by default an unknown mob cannot enter turn-based battle, so the config must be
configured for them.)

# Building

Simply invoke `./gradlew build` in the mod directory and after some time the
finished jar will be placed in build/libs/
