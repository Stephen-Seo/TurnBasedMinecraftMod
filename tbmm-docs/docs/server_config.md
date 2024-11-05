# Server-side Config

Invoke `/tbm-server-edit` to print out server settings to the "chat" area.

![tbm-server-edit output](tbm-server-edit-full.png)

In this text area, yellow texts are setting names, green texts are setting
values that can be set when clicked on, and dark-green texts are settings that
display more settings when clicked on.

!!! note
    You can hover/click these texts by pressing the "t" key to open the chatbox,
    and using the mouse.

![tbm-server-edit info when hovered](tbm-server-edit-hover.png)

![tbm-server-edit output when haste-speed is set](tbm-server-edit-set-haste.png)

When clicking on the dark-green texts, a description and list is shown. You can
hover over the text to show what actions may occur when clicked.

![tbm-server-edit ignore-damage-sources
output](tbm-server-edit-damage-sources.png)

On click, the action taken will be shown.

![tbm-server-edit ignore-damage-sources
modified](tbm-server-edit-damage-sources-set.png)

It is also possible to set config settings via a command, but it is recommended
to use the "click-on-chat" interface for general server config settings.

## Per-Mob Settings

To demonstrate setting "per-mob" settings, it will be shown how to do so with
sheep.

![sheep](tbm-edit-sheep.jpg)

Invoke `/tbm-edit`. Some helpful text will show.

![/tbm-edit output](tbm-edit-output.jpg)

At this point, TBMM (TurnBasedMinecraftMod) will be keeping track of who you
will attack next, so that you can edit their settings. Currently this only
applies to mobs like sheep, zombies, skeletons, bees, etc. This is done so that
you can physically select the mob you want to "edit".

![mob settings](tbm-edit-settings.jpg)

In this example, the "AE" (attack effect) setting will be changed to
"levitation", which will cause the sheep to make the player levitate 50% of the
time.

![mob setting attack effect levitation](tbm-edit-levitation.png)

Also, "DecisionAttack" will be set to 100%, so that the sheep will choose to
attack instead of fleeing battle.

![mob setting decision attack 100%](tbm-edit-decision-attack.png)

!!! note
    Make sure to click on "Finished Editing" to save changes to the server-side
    config.

Note that for sheep to enter battle, they must be removed from the "ignore
battle categories" setting (remove the "passive" category). Alternatively, one
can set sheep to a category other than "passive", like "animal" or "monster". A
category that isn't listed in the "ignore battle categories" setting will start
turn-based-battle.

![server edit remove passive from ignore
categories](tbm-edit-server-edit-ignore-battle-types.png)

Now, sheep will attack and cause "levitation" 50% of the time.

![after battle with levitation effect](tbm-edit-post-battle.jpg)

## Custom Name Settings

Additional entries can be added to the server-side config that applies to mobs
named with a specific name (like with a name-tag). Name a mob, then invoke
`/tbm-edit custom`.

![tbm-edit custom command](tbm-edit-custom.png)

Hit the named mob to start the editing process.

![tbm-edit custom editing](tbm-edit-custom-editing.jpg)

Make your changes and click on "Finished Editing", and any mob with that exact
name will have these battle settings applied.

!!! note
    These settings are also in the server-side config.

## Per-Player Settings

As of TurnBasedMinecraftMod 1.26.4, one can add Player-specific config via a
command `/tbm-edit player <PLAYER_NAME>`.

![tbm-edit player command](tbm-edit-player-cmd.png)

You can make changes as usual, though some options for regular mobs do not
apply to the per-Player config.

![tbm-edit player editing](tbm-edit-player-display.png)

Don't forget to click on "Finished Editing" when done. Once saved, the
server/single-player-game should use these settings for the specified Player in
battle.

!!! note
    These settings are also in the server-side config.

## Other Things to Know

~~Sometimes a mod update will "reset" the settings in the server-config to
defaults. This is due to new mob entries in the settings.~~  
*As of version 1.26.5 of the mod, this should happen less frequently!*  
Changes were added in 1.26.5 such that entries that exist in the default config
but not in the current config will be appended to the current config.

Older versions of this mod still retain the previous behavior: Check the
`.minecraft/config` folder (or `config` folder on the server) to see that the
old settings file was renamed and the new settings file is in its place. You
may have to compare the files to keep the settings you want. Though this
probably shouldn't happen anymore in newer versions of this mod.
