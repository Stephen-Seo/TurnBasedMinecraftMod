# Upcoming changes

# Version 1.18.7

Incremented network channel's protocol version to 2, because a packet's format
was changed in the previous version.

# Version 1.18.6

Add server config option to disable the turn timer (recommended to not disable
the turn timer, otherwise a player could hang a battle for forever).

Fix turn timer not respecting the server's turn timer value. For example, if the
server had it set to 5 seconds, but the client had it set to 15 seconds, the
turn timer would erronously show 15 seconds at the start of the next turn.

# Version 1.18.5

Fix invalid Battle text output when a Player drinks a potion.

# Version 1.18.4

Fix attacks not hitting due to "invulnerability frames".

Change attacks to be applied approx 150ms apart.

# Version 1.18.3

[The ability to change server-side config from within the game using
"/tbm-server-edit".](https://youtu.be/9xkbHNWkcIY)

Fix Battle not checking Player "speed/slow" status to apply the
"player\_haste\_speed" and "player\_slow\_speed" settings.

# Version 1.18.2

The list of targets in the Battle GUI when selecting a target did not display
Players' names in their team color. This version now shows Player names with
their team color in the target buttons/list.

# Version 1.18.1

Fix battle text output such that players in teams will have their name displayed
with the team's color (and some refactoring of related battle text output).

# Version 1.18.0

Mod now works with Forge-1.18.2-40.1.0 .  
Note that the mod's version is confusingly similar (1.18.0).

TBM should allow players to eat any food from any mod (including food items from Pam's HarvestCraft).

# Version 1.17.2

(try to) Fix potential freeze bug when an entity leaves battle.

# Version 1.17.1

Add experimental support for Pam's Harvestcraft foods.

# Version 1.17

Update mod for Forge 1.16.5-36.1.0 .

# Version 1.16

Add config options regarding creeper behavior.

By default, creepers will not explode if they leave battle (maybe due to a
player fleeing), until the cooldown time ends.

By default, creepers that explode will damage anyone, even if they weren't in
turn-based battle.

# Version 1.15

Add server-side config option that determines on what turn a Creeper will
explode in battle.

# Version 1.14

Implemented support for Creepers in battle.

Added an option to the config to prevent config from being overwritten on
update.

Fixed some display text during battle.

# Version 1.13

Disabled midi playback due to currently being unable to change its volume.

Note mp3 playback breaks sometimes. Using an mp3 file without album art seems to
work though...

# Version 1.12

Fix potential crash if mod is loaded on dedicated server.

# Version 1.11

Fixed text display in BattleGUI.

Updated TBM\_Config.toml with new vanilla mobs.

Fixed version parsing of TBM\_Config.toml.

# Version 1.10

Updated for 1.16.3

Compiled against forge version "1.16.3-34.1.0"

However, MP3 playing seems to not work sometimes.

# Version 1.9

Updated mod for 1.14.4
(Took a long while, and no new features were added due to making sure everything
still works).

Compiled against forge version "1.14.4-28.1.0"

Entity names have changed in the config, so this newer version will replace the
old version. Older existing config should be renamed rather than deleted.

# Version 1.8

Update to forge version "1.12.2-14.23.5.2768".

Fix bug where more than config-set-amount of entities can be in battle.

Add mp3 support, can now play mp3s.

Minor improvements.

# Version 1.7

Fix bug where after using "/tbm-edit", ignore_battle option is saved in
config with the wrong name.

Add "/tbm-edit custom", which lets an OP add an entity entry for entities with
custom names (via name-tags). The entry added into the config file will use
"custom_name" instead of "name" to specify if it is a regular entity entry
or an entry for a specific custom name.

Minor fixes and improvements.

# Version 1.6

Fix bug where player can start battle with self.

Change config to use ".toml" instead of ".xml".

Added command "/tbm-edit" that allows OPs to edit entity entries for TBM.  
Can be used to add mobs from other mods easily.

Change how battle info text is displayed.

# Version 1.5

Fix proper consumption of food/potion items in battle.

Added some debug output on internal freeze occurrence (investigation of the
freeze bug is still ongoing).

# Version 1.4

Fix duplicate "... entered battle" messages.

Added max-distance config option for how close a monster must be to initiate
battle (when triggered by a monster targeting a player or entity in battle).

Some internal fixes and refactorings.

# Version 1.3

Added a battle-cooldown and related config option. Now, when leaving battle, a
cooldown timer (default 5 seconds) prevents entities from
attacking/being-attacked for the duration of the cooldown. Can be set to a
minimum of 1 second and maximum of 10 seconds.

"/tbm-enable-all" and "/tbm-disable-all" now notifies all players when they are
invoked by an OP.

Battles can now be started/joined by hostile mobs when they target a player or
other entity in battle, instead of just entering on attack. Old
battle-starting-behavior can be used by setting the related config option.  
(This change was made to keep zombies from gathering around the player when
the config option for freezing entities in battle is enabled.)

Non-player entities in battle now primarily attack entities they are already
targeting (via a call to EntityLiving's "getAttackTarget()").

Note since config version is now 5, older config will be renamed and the new
config will take its place.

# Version 1.2

Fixed "/tbm-enable" and "/tbm-disable" not working in singleplayer.

Added commands:
- "/tbm-enable-all"
- "/tbm-disable-all"

Only OPs can use these new commands to enable or disable turn-based-battle for
everyone.  
Note that if "/tbm-disable-all" is invoked, joining players will also have
turn-based-battle disabled for them. Invoking "/tbm-enable-all" will change this
back to the default, where turn-based-battle is enabled for joining players.

# Version 1.1

Added commands to enable/disable turn-based-battle on-demand.

Commands are:
- "/tbm-enable"
- "/tbm-disable"
- "/tbm-set \<player> \<true/false>"

There is a config option to allow anyone to use "/tbm-enable" and "/tbm-disable".  
Only OPs can use "/tbm-set" (permission level 2).

Note that since config version has been updated, pre-existing config will be
renamed and the newer config will take its place.

# Version 1.0

Features:
- Turn based combat with hostile mobs (excluding passive and bosses) by default
- Config generated at ".minecraft/config/TurnBasedMinecraft/TBM_Config.xml
  - Old config is renamed if new config exists with newer mod version
- Battle is very configurable per mob and also for all players in config
- Can add mobs unique to other mods in config (using full Java Class name of mob)
- Can set config on server/singleplayer to freeze mobs in combat
- Can add battle/silly music in ".minecraft/config/TurnBasedMinecraft/Music"
that activates depending on mob types in battle
  - What determines battle or silly can be set client-side in config
    - Unknown types defaults to battle music instead of silly music
- Can set max battle combatants in config
- Can use bow/arrows in battle (currently different projectile weapons provided
by different mods are not supported)
- Players in creative-mode will not enter turn-based battle
