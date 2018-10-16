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
