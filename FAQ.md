## How do I use this mod to have turn-based-battle with any mob (including mobs from other mods)?

To have turn-based-battle with a mob, it must have a config entry in the server
config. This can either be done manually or be [done in-game via a
command](https://www.youtube.com/watch?v=MK648OVHddE).

## Is it possible to have a mob's config applied to any mob with a specific name?

Yes, [this video explains this feature](https://www.youtube.com/watch?v=9lBETQFMd3A).

## Can the mod play music while a battle is happening?

Yes, you have to put the music (`.wav`, `.ogg`, or `.mp3`) in
`.minecraft/config/TurnBasedMinecraft/Music/battle` and
`.minecraft/config/TurnBasedMinecraft/Music/silly` . Note that `.wav`, `.ogg`,
and `.mp3` music files are supported, but `.mid` files are disabled due to lack
of volume control with the default Java library api. The config file can be
edited to change what categories of entities trigger what type of music, but
generally "passive" mobs trigger the "silly" music and everything else triggers
"battle" music. Note that the default server config has turn-based-battle
disabled for "passive" mobs.

**Note that while .ogg Vorbis files are supported, .ogg Opus files are NOT
supported.**

## Why can't the mod play my mp3 files?

The third-party-library used to load mp3 files seems to have issues with
loading any mp3 file that isn't "barebones". Try removing the metadata of the
mp3 file. I've found that using mp3s without album art embedded in it seems to
work.

**It is recommended to use .ogg Vorbis files instead of .mp3 files.**

## Why do passive mobs don't start turn-based battle?

By default, the `passive` category is set to "ignore turn-based-battle" in the
server config. Use `/tbm-server-edit` to change this. (Click on
`ignore_battle_types` which should be dark-green. A list of "categories" will
appear at the bottom of the text. Click on `passive` to remove the "passive"
category.)
![Screenshot one of two showing how to unset passive category from ignore battle types list](https://seodisparate.com/static/uploads/TBMM_ignore_battle_types_screenshot0.png)
![Screenshot two of two showing how to unset passive category from ignore battle types list](https://seodisparate.com/static/uploads/TBMM_ignore_battle_types_screenshot1.png)

Alternatively, edit the [server config](https://github.com/Stephen-Seo/TurnBasedMinecraftMod/blob/ad78063a16c768f660dd086cba857a3be43a84b2/src/main/resources/assets/com_burnedkirby_turnbasedminecraft/TBM_Config.toml#L46)
and remove "passive" from the ignore\_battle\_types list.

## Why is the mod's config file missing?

The mod needs to be run once to generate the default config file and
directories for battle music. After running it once, you can now close
Minecraft and edit the config found at
`.minecraft/config/TurnBasedMinecraft/TBM_Config.toml` Note that some options
only apply to the server and some only to the client, as specified in the
config. This means that server config must be changed on the server side for it
to take effect (local singleplayer will use all of the local config, but
multiplayer setups will require the server config to be changed on the server
side). [You can edit the server-side config in game via the "/tbm-server-edit"
command](https://youtu.be/9xkbHNWkcIY).

## I updated the mod, but now my config changes are back to default, what happened?

Sometimes, I add new mob entries to the config, and increment the version
number of the config. When the server/client starts, it checks the default
config's version number with the existing config's version number. If the
existing config is determined to be outdated, then it is renamed to a different
name (which usually includes the date/time of when it was renamed), and the new
default config is placed in its place. There is a config option to prevent this
from happening, but it is strongly recommended to not disable this since this
will cause updates to the config to never be placed in the mod's config
directory. If you have changes you want to keep, but the mod renamed the
original config, you will have to edit the `TBM_Config.toml` to have the
changes you want from the renamed older config file.
