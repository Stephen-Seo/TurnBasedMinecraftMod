package com.burnedkirby.TurnBasedMinecraft.common;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import org.apache.logging.log4j.Logger;

import com.electronwill.nightconfig.core.file.FileConfig;

public class Config
{
    public static final long BATTLE_DECISION_DURATION_SEC_MIN = 5L;
    public static final long BATTLE_DECISION_DURATION_SEC_MAX = 60L;
    public static final long BATTLE_DECISION_DURATION_SEC_DEFAULT = 15L;
    public static final long BATTLE_DECISION_DURATION_NANO_MIN = BATTLE_DECISION_DURATION_SEC_MIN * 1000000000L;
    public static final long BATTLE_DECISION_DURATION_NANO_MAX = BATTLE_DECISION_DURATION_SEC_MAX * 1000000000L;
    public static final long BATTLE_DECISION_DURATION_NANO_DEFAULT = BATTLE_DECISION_DURATION_SEC_DEFAULT * 1000000000L;
    private long battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_DEFAULT;
    private Map<String, EntityInfo> entityInfoMap;
    private Map<String, EntityInfo> customEntityInfoMap;
    private Set<String> ignoreBattleTypes;
    private Logger logger;
    private int playerSpeed = 50;
    private int playerHasteSpeed = 80;
    private int playerSlowSpeed = 20;
    private int playerAttackProbability = 100;
    private int playerEvasion = 10;
    private int defenseDuration = 1;
    private int fleeGoodProbability = 90;
    private int fleeBadProbability = 35;
    private int minimumHitPercentage = 4;
    private int maxInBattle = 8;
    private Set<String> musicBattleTypes;
    private Set<String> musicSillyTypes;
    private boolean freezeCombatantsInBattle = false;
    private int sillyMusicThreshold = 40;
    private int configVersion = 0;
    private Set<Integer> battleIgnoringPlayers = null;
    private boolean onlyOPsSelfDisableTB = true;
    private boolean battleDisabledForAll = false;
    private boolean oldBattleBehaviorEnabled = false;
    private int leaveBattleCooldownSeconds = 5;
    private int aggroStartBattleDistance = 8;
    private int creeperExplodeTurn = 5;
    private boolean creeperStopExplodeOnLeaveBattle = true;
    private boolean creeperAlwaysAllowDamage = true;

    public Config(Logger logger)
    {
        entityInfoMap = new HashMap<String, EntityInfo>();
        customEntityInfoMap = new HashMap<String, EntityInfo>();
        ignoreBattleTypes = new HashSet<String>();
        this.logger = logger;
        musicBattleTypes = new HashSet<String>();
        musicSillyTypes = new HashSet<String>();
        battleIgnoringPlayers = new HashSet<Integer>();

        {
            File confPath = new File(TurnBasedMinecraftMod.CONFIG_DIRECTORY);
            if(!confPath.exists()) {
                if(!confPath.mkdirs()) {
                    logger.error("Failed to create config dir \"" + TurnBasedMinecraftMod.CONFIG_DIRECTORY + "\"");
                    return;
                }
            }
        }

        writeDefaultConfig(getClass().getResourceAsStream(TurnBasedMinecraftMod.CONFIG_INTERNAL_PATH));

        int internalVersion = getConfigFileVersion(new File(TurnBasedMinecraftMod.DEFAULT_CONFIG_FILE_PATH));

        if(internalVersion == 0) {
            logger.error("Failed to check version of internal config file");
            logger.error("Tried path \"" + TurnBasedMinecraftMod.DEFAULT_CONFIG_FILE_PATH + "\"");
        } else {
            configVersion = internalVersion;
        }

        try {
            File testLoad = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
            if(!testLoad.exists()) {
                writeConfig();
            }
        }
        catch (Throwable t) {
            logger.error("Failed to check/create-new config file");
        }

        // parse config
        File configFile = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
        if(!configFile.exists() || !configFile.canRead()) {
            logger.error("Failed to read/parse config file " + TurnBasedMinecraftMod.CONFIG_FILE_PATH);
            return;
        }

        int configVersion = getConfigFileVersion(configFile);
        boolean canOverwrite = getCanOverwrite(configFile);
        if(configVersion < this.configVersion && canOverwrite) {
            logger.warn("Config file " + TurnBasedMinecraftMod.CONFIG_FILENAME + " is older version, renaming...");
            moveOldConfig();
            try {
                writeConfig();
            } catch (Throwable t) {
                logger.error("Failed to write config file!");
            }
        }
        try {
            parseConfig(configFile);
        } catch (Throwable t) {
            logger.error("Failed to parse config file!");
        }
    }

    private void writeConfig() throws IOException
    {
        InputStream is = getClass().getResourceAsStream(TurnBasedMinecraftMod.CONFIG_INTERNAL_PATH);
        FileOutputStream fos = new FileOutputStream(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
        byte[] buf = new byte[1024];
        int read;
        while((read = is.read(buf)) > 0) {
            fos.write(buf, 0, read);
        }
        fos.close();
        is.close();
    }

    private void moveOldConfig()
    {
        File configFile = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
        if(configFile.exists())
        {
            configFile.renameTo(new File(TurnBasedMinecraftMod.CONFIG_DIRECTORY
                    + "TBM_Config_"
                    + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
                    + ".toml"));
        }
    }

    private boolean parseConfig(File configFile) throws IOException
    {
        CommentedFileConfig conf = getConfigObj(configFile);

        // client config
        try {
            Collection<String> battle_music_categories = conf.get("client_config.battle_music");
            if (battle_music_categories != null) {
                for (String category : battle_music_categories) {
                    musicBattleTypes.add(category);
                }
            } else {
                musicBattleTypes.add("monster");
                musicBattleTypes.add("animal");
                musicBattleTypes.add("boss");
                musicBattleTypes.add("player");
                logNotFound("client_config.battle_music");
            }
        } catch (ClassCastException e) {
            musicBattleTypes.add("monster");
            musicBattleTypes.add("animal");
            musicBattleTypes.add("boss");
            musicBattleTypes.add("player");
            logTOMLInvalidValue("client_config.battle_music");
        }

        try {
            Collection<String> silly_music_categories = conf.get("client_config.silly_music");
            if (silly_music_categories != null) {
                for (String category : silly_music_categories) {
                    musicSillyTypes.add(category);
                }
            } else {
                musicSillyTypes.add("passive");
                logNotFound("client_config.silly_music");
            }
        } catch (ClassCastException e) {
            musicSillyTypes.add("passive");
            logTOMLInvalidValue("client_config.silly_music");
        }

        try {
            OptionalInt silly_music_threshold = conf.getOptionalInt("client_config.silly_music_threshold");
            if(silly_music_threshold.isPresent()) {
                this.sillyMusicThreshold = silly_music_threshold.getAsInt();
            } else {
                this.sillyMusicThreshold = 40;
                logNotFound("client_config.silly_music_threshold", "40");
            }
        } catch (ClassCastException e) {
            this.sillyMusicThreshold = 40;
            logTOMLInvalidValue("client_config.silly_music_threshold", "40");
        }

        // server_config
        try {
            OptionalInt leave_battle_cooldown = conf.getOptionalInt("server_config.leave_battle_cooldown");
            if (leave_battle_cooldown.isPresent()) {
                this.leaveBattleCooldownSeconds = leave_battle_cooldown.getAsInt();
                if (this.leaveBattleCooldownSeconds < 1) {
                    logClampedValue("server_config.leave_battle_cooldown", Integer.toString(this.leaveBattleCooldownSeconds), "1");
                    this.leaveBattleCooldownSeconds = 1;
                } else if (this.leaveBattleCooldownSeconds > 10) {
                    logClampedValue("server_config.leave_battle_cooldown", Integer.toString(this.leaveBattleCooldownSeconds), "10");
                    this.leaveBattleCooldownSeconds = 10;
                }
            } else {
                this.leaveBattleCooldownSeconds = 5;
                logNotFound("server_config.leave_battle_cooldown", "5");
            }
        } catch (ClassCastException e) {
            this.leaveBattleCooldownSeconds = 5;
            logTOMLInvalidValue("server_config.leave_battle_cooldown", "5");
        }

        try {
            OptionalInt aggro_start_battle_max_distance = conf.getOptionalInt("server_config.aggro_start_battle_max_distance");
            if (aggro_start_battle_max_distance.isPresent()) {
                this.aggroStartBattleDistance = aggro_start_battle_max_distance.getAsInt();
                if (this.aggroStartBattleDistance < 5) {
                    logClampedValue("server_config.aggro_start_battle_max_distance", Integer.toString(this.aggroStartBattleDistance), "5");
                    this.aggroStartBattleDistance = 5;
                } else if (this.aggroStartBattleDistance > 50) {
                    logClampedValue("server_config.aggro_start_battle_max_distance", Integer.toString(this.aggroStartBattleDistance), "50");
                    this.aggroStartBattleDistance = 50;
                }
            } else {
                this.aggroStartBattleDistance = 8;
                logNotFound("server_config.aggro_start_battle_max_distance", "8");
            }
        } catch (ClassCastException e) {
            this.aggroStartBattleDistance = 8;
            logTOMLInvalidValue("server_config.aggro_start_battle_max_distance", "8");
        }

        try {
            OptionalInt creeper_explode_turn = conf.getOptionalInt("server_config.creeper_explode_turn");
            if(creeper_explode_turn.isPresent()) {
                this.creeperExplodeTurn = creeper_explode_turn.getAsInt();
                if(this.creeperExplodeTurn < 1) {
                    logClampedValue("server_config.creeper_explode_turn", Integer.toString(this.creeperExplodeTurn), "1");
                    this.creeperExplodeTurn = 1;
                }
            } else {
                this.creeperExplodeTurn = 5;
                logNotFound("server_config.creeper_explode_turn", "5");
            }
        } catch(ClassCastException e) {
            this.creeperExplodeTurn = 5;
            logTOMLInvalidValue("server_config.creeper_explode_turn", "5");
        }

        try {
            Boolean creeper_stop_explode_on_leave_battle = conf.get("server_config.creeper_stop_explode_on_leave_battle");
            if(creeper_stop_explode_on_leave_battle != null) {
                this.creeperStopExplodeOnLeaveBattle = creeper_stop_explode_on_leave_battle;
            } else {
                this.creeperStopExplodeOnLeaveBattle = true;
                logNotFound("server_config.creeper_stop_explode_on_leave_battle", "true");
            }
        } catch (ClassCastException e) {
            this.creeperStopExplodeOnLeaveBattle = true;
            logTOMLInvalidValue("server_config.creeper_stop_explode_on_leave_battle", "true");
        }

        try {
            Boolean creeper_always_allow_damage = conf.get("server_config.creeper_always_allow_damage");
            if(creeper_always_allow_damage != null) {
                this.creeperAlwaysAllowDamage = creeper_always_allow_damage;
            } else {
                this.creeperAlwaysAllowDamage = true;
                logNotFound("server_config.creeper_always_allow_damage", "true");
            }
        } catch (ClassCastException e) {
            this.creeperAlwaysAllowDamage = true;
            logTOMLInvalidValue("server_config.creeper_always_allow_damage", "true");
        }

        try {
            Boolean old_battle_behavior = conf.get("server_config.old_battle_behavior");
            if(old_battle_behavior != null) {
                this.oldBattleBehaviorEnabled = old_battle_behavior;
            } else {
                this.oldBattleBehaviorEnabled = false;
                logNotFound("server_config.old_battle_behavior", "false");
            }
        } catch (ClassCastException e) {
            this.oldBattleBehaviorEnabled = false;
            logTOMLInvalidValue("server_config.old_battle_behavior", "false");
        }

        try {
            Boolean anyone_can_disable_tbm_for_self = conf.get("server_config.anyone_can_disable_tbm_for_self");
            if(anyone_can_disable_tbm_for_self != null) {
                this.onlyOPsSelfDisableTB = !anyone_can_disable_tbm_for_self;
            } else {
                this.onlyOPsSelfDisableTB = true;
                logNotFound("server_config.anyone_can_disable_tbm_for_self", "false");
            }
        } catch (ClassCastException e) {
            this.onlyOPsSelfDisableTB = true;
            logTOMLInvalidValue("server_config.anyone_can_disable_tbm_for_self", "false");
        }

        try {
            OptionalInt max_in_battle = conf.getOptionalInt("server_config.max_in_battle");
            if(max_in_battle.isPresent()) {
                this.maxInBattle = max_in_battle.getAsInt();
                if(this.maxInBattle < 2) {
                    logClampedValue("server_config.max_in_battle", Integer.toString(this.maxInBattle), "2");
                    this.maxInBattle = 2;
                }
            } else {
                maxInBattle = 8;
                logNotFound("server_config.max_in_battle", "8");
            }
        } catch (ClassCastException e) {
            maxInBattle = 8;
            logTOMLInvalidValue("server_config.max_in_battle", "8");
        }

        try {
            Boolean freeze_battle_combatants = conf.get("server_config.freeze_battle_combatants");
            if(freeze_battle_combatants != null) {
                this.freezeCombatantsInBattle = freeze_battle_combatants;
            } else {
                freezeCombatantsInBattle = false;
                logNotFound("server_config.freeze_battle_combatants", "false");
            }
        } catch (ClassCastException e) {
            freezeCombatantsInBattle = false;
            logTOMLInvalidValue("server_config.freeze_battle_combatants", "false");
        }

        try {
            Collection<String> ignore_battle_types = conf.get("server_config.ignore_battle_types");
            if(ignore_battle_types != null) {
                this.ignoreBattleTypes.addAll(ignore_battle_types);
            } else {
                ignoreBattleTypes.add("passive");
                ignoreBattleTypes.add("boss");
                logNotFound("server_config.ignore_battle_types");
            }
        } catch (ClassCastException e) {
            ignoreBattleTypes.add("passive");
            ignoreBattleTypes.add("boss");
            logTOMLInvalidValue("server_config.ignore_battle_types");
        }

        try {
            OptionalInt player_speed = conf.getOptionalInt("server_config.player_speed");
            if(player_speed.isPresent()) {
                this.playerSpeed = player_speed.getAsInt();
            } else {
                this.playerSpeed = 50;
                logNotFound("server_config.player_speed", "50");
            }
        } catch (ClassCastException e) {
            this.playerSpeed = 50;
            logTOMLInvalidValue("server_config.player_speed", "50");
        }

        try {
            OptionalInt player_haste_speed = conf.getOptionalInt("server_config.player_haste_speed");
            if(player_haste_speed.isPresent()) {
                this.playerHasteSpeed = player_haste_speed.getAsInt();
            } else {
                this.playerHasteSpeed = 80;
                logNotFound("server_config.player_haste_speed", "80");
            }
        } catch (ClassCastException e) {
            this.playerHasteSpeed = 80;
            logTOMLInvalidValue("server_config.player_haste_speed", "80");
        }

        try {
            OptionalInt player_slow_speed = conf.getOptionalInt("server_config.player_slow_speed");
            if(player_slow_speed.isPresent()) {
                this.playerSlowSpeed = player_slow_speed.getAsInt();
            } else {
                this.playerSlowSpeed = 20;
                logNotFound("server_config.player_slow_speed", "20");
            }
        } catch (ClassCastException e) {
            this.playerSlowSpeed = 20;
            logTOMLInvalidValue("server_config.player_slow_speed", "20");
        }

        try {
            OptionalInt player_attack_probability = conf.getOptionalInt("server_config.player_attack_probability");
            if(player_attack_probability.isPresent()) {
                this.playerAttackProbability = player_attack_probability.getAsInt();
            } else {
                this.playerAttackProbability = 90;
                logNotFound("server_config.player_attack_probability", "90");
            }
        } catch (ClassCastException e) {
            this.playerAttackProbability = 90;
            logTOMLInvalidValue("server_config.player_attack_probability", "90");
        }

        try {
            OptionalInt player_evasion = conf.getOptionalInt("server_config.player_evasion");
            if(player_evasion.isPresent()) {
                this.playerEvasion = player_evasion.getAsInt();
            } else {
                this.playerEvasion = 10;
                logNotFound("server_config.player_evasion", "10");
            }
        } catch (ClassCastException e) {
            this.playerEvasion = 10;
            logTOMLInvalidValue("server_config.player_evasion", "10");
        }

        try {
            OptionalInt defense_duration = conf.getOptionalInt("server_config.defense_duration");
            if(defense_duration.isPresent()) {
                this.defenseDuration = defense_duration.getAsInt();
                if(this.defenseDuration < 0) {
                    logClampedValue("server_config.defense_duration", Integer.toString(this.defenseDuration), "0");
                    this.defenseDuration = 0;
                }
            } else {
                this.defenseDuration = 1;
                logNotFound("server_config.defense_duration", "1");
            }
        } catch (ClassCastException e) {
            this.defenseDuration = 1;
            logTOMLInvalidValue("server_config.defense_duration", "1");
        }

        try {
            OptionalInt flee_good_probability = conf.getOptionalInt("server_config.flee_good_probability");
            if(flee_good_probability.isPresent()) {
                this.fleeGoodProbability = flee_good_probability.getAsInt();
            } else {
                this.fleeGoodProbability = 90;
                logNotFound("server_config.flee_good_probability", "90");
            }
        } catch (ClassCastException e) {
            this.fleeGoodProbability = 90;
            logTOMLInvalidValue("server_config.flee_good_probability", "90");
        }

        try {
            OptionalInt flee_bad_probability = conf.getOptionalInt("server_config.flee_bad_probability");
            if(flee_bad_probability.isPresent()) {
                this.fleeBadProbability = flee_bad_probability.getAsInt();
            } else {
                this.fleeBadProbability = 35;
                logNotFound("server_config.flee_bad_probability", "35");
            }
        } catch (ClassCastException e) {
            this.fleeBadProbability = 35;
            logTOMLInvalidValue("server_config.flee_bad_probability", "35");
        }

        try {
            OptionalInt minimum_hit_percentage = conf.getOptionalInt("server_config.minimum_hit_percentage");
            if(minimum_hit_percentage.isPresent()) {
                this.minimumHitPercentage = minimum_hit_percentage.getAsInt();
                if(this.minimumHitPercentage < 1) {
                    logClampedValue("server_config.minimum_hit_percentage", Integer.toString(this.minimumHitPercentage), "1");
                    this.minimumHitPercentage = 1;
                }
            } else {
                this.minimumHitPercentage = 4;
                logNotFound("server_config.minimum_hit_percentage", "4");
            }
        } catch (ClassCastException e) {
            this.minimumHitPercentage = 4;
            logTOMLInvalidValue("server_config.minimum_hit_percentage", "4");
        }

        try {
            OptionalInt battle_turn_time_seconds = conf.getOptionalInt("server_config.battle_turn_time_seconds");
            if(battle_turn_time_seconds.isPresent()) {
                this.battleDecisionDurationNanos = (long)battle_turn_time_seconds.getAsInt() * 1000000000L;
                if(this.battleDecisionDurationNanos < BATTLE_DECISION_DURATION_NANO_MIN) {
                    this.battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_MIN;
                    logClampedValue("server_config.battle_turn_time_seconds", Integer.toString(battle_turn_time_seconds.getAsInt()), Long.toString(BATTLE_DECISION_DURATION_SEC_MIN));
                } else if(this.battleDecisionDurationNanos > BATTLE_DECISION_DURATION_NANO_MAX) {
                    this.battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_MAX;
                    logClampedValue("server_config.battle_turn_time_seconds", Integer.toString(battle_turn_time_seconds.getAsInt()), Long.toString(BATTLE_DECISION_DURATION_SEC_MAX));
                }
            } else {
                this.battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_DEFAULT;
                logNotFound("server_config.battle_turn_time_seconds", "15");
            }
        } catch (ClassCastException e) {
            this.battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_DEFAULT;
            logTOMLInvalidValue("server_config.battle_turn_time_seconds", "15");
        }

        Collection<com.electronwill.nightconfig.core.Config> entities = null;
        try {
            entities = conf.get("server_config.entity");
        } catch (ClassCastException e) {
            logTOMLInvalidValue("server_config.entity");
        }
        if(entities != null) {
            for(com.electronwill.nightconfig.core.Config nestedConf : entities) {
                EntityInfo eInfo = new EntityInfo();
                String name;
                if(nestedConf.contains("name") && nestedConf.contains("custom_name")) {
                    logger.error("Entity cannot have both \"name\" (" + nestedConf.get("name")
                            + ") and \"custom_name\" (" + nestedConf.get("custom_name") + ") entries");
                    continue;
                } else if(nestedConf.contains("name")) {
                    try {
                        eInfo.classType = Class.forName(nestedConf.get("name"));
                        name = eInfo.classType.getName();
                    } catch (ClassNotFoundException e) {
                        logger.error("Entity with class name \"" + nestedConf.get("name") + "\" not found, skipping...");
                        continue;
                    }
                } else if(nestedConf.contains("custom_name")) {
                    try {
                        eInfo.customName = nestedConf.get("custom_name");
                        name = eInfo.customName;
                    } catch (ClassCastException e) {
                        logger.error("Entity with invalid custom_name (must be a string), skipping...");
                        continue;
                    }
                } else {
                    logger.error("Entity must have \"name\" or \"custom_name\" entry");
                    continue;
                }

                try {
                    eInfo.attackPower = nestedConf.getInt("attack_power");
                    if(eInfo.attackPower < 0) {
                        logClampedValueEntity("attack_power", name, Integer.toString(eInfo.attackPower), "0");
                        eInfo.attackPower = 0;
                    }
                } catch (ClassCastException e) {
                    logEntityInvalidValue("attack_power", name, "3");
                    eInfo.attackPower = 3;
                } catch (NullPointerException e) {
                    logEntityMissingRequiredValue("attack_power", name, "3");
                    eInfo.attackPower = 3;
                }

                try {
                    eInfo.attackProbability = nestedConf.getInt("attack_probability");
                    if(eInfo.attackProbability < 0) {
                        logClampedValueEntity("attack_probability", name, Integer.toString(eInfo.attackProbability), "0");
                        eInfo.attackProbability = 0;
                    } else if(eInfo.attackProbability > 100) {
                        logClampedValueEntity("attack_probability", name, Integer.toString(eInfo.attackProbability), "100");
                        eInfo.attackProbability = 100;
                    }
                } catch (ClassCastException e) {
                    logEntityInvalidValue("attack_probability", name, "30");
                    eInfo.attackProbability = 30;
                } catch (NullPointerException e) {
                    logEntityMissingRequiredValue("attack_probability", name, "30");
                    eInfo.attackProbability = 30;
                }

                try {
                    eInfo.attackEffect = EntityInfo.Effect.fromString(nestedConf.get("attack_effect"));
                    if(eInfo.attackEffect != EntityInfo.Effect.UNKNOWN) {
                        try {
                            eInfo.attackEffectProbability = nestedConf.getInt("attack_effect_probability");
                            if(eInfo.attackEffectProbability < 0) {
                                logClampedValueEntity("attack_effect_probability", name, Integer.toString(eInfo.attackEffectProbability), "1");
                                eInfo.attackEffectProbability = 1;
                            } else if(eInfo.attackEffectProbability > 100) {
                                logClampedValueEntity("attack_effect_probability", name, Integer.toString(eInfo.attackEffectProbability), "100");
                                eInfo.attackEffectProbability = 100;
                            }
                        } catch (ClassCastException e) {
                            eInfo.attackEffect = EntityInfo.Effect.UNKNOWN;
                            logger.warn("Entity \"" + name + "\" has specified attack_effect but attack_effect_probability is invalid, unsetting attack_effect");
                        } catch (NullPointerException e) {
                            eInfo.attackEffect = EntityInfo.Effect.UNKNOWN;
                            logger.warn("Entity \"" + name + "\" has specified attack_effect but attack_effect_probability is missing, unsetting attack_effect");
                        }
                    }
                } catch (ClassCastException e) {
                    eInfo.attackEffect = EntityInfo.Effect.UNKNOWN;
                    logEntityInvalidValue("attack_effect", name, "unknown");
                } catch (NullPointerException e) {
                    eInfo.attackEffect = EntityInfo.Effect.UNKNOWN;
                    logEntityMissingOptionalValue("attack_effect", name, "unknown");
                }

                try {
                    eInfo.attackVariance = nestedConf.getInt("attack_variance");
                    if(eInfo.attackVariance < 0) {
                        logClampedValueEntity("attack_variance", name, Integer.toString(eInfo.attackVariance), "0");
                        eInfo.attackVariance = 0;
                    }
                } catch (ClassCastException e) {
                    eInfo.attackVariance = 0;
                    logEntityInvalidValue("attack_variance", name, "0");
                } catch (NullPointerException e) {
                    eInfo.attackVariance = 0;
                    logEntityMissingOptionalValue("attack_variance", name, "0");
                }

                try {
                    eInfo.defenseDamage = nestedConf.getInt("defense_damage");
                    if(eInfo.defenseDamage < 0) {
                        logClampedValueEntity("defense_damage", name, Integer.toString(eInfo.defenseDamage), "0");
                        eInfo.defenseDamage = 0;
                    } else if(eInfo.defenseDamage != 0) {
                        try {
                            eInfo.defenseDamageProbability = nestedConf.getInt("defense_damage_probability");
                            if(eInfo.defenseDamageProbability < 1) {
                                logClampedValueEntity("defense_damage_probability", name, Integer.toString(eInfo.defenseDamageProbability), "1");
                                eInfo.defenseDamageProbability = 1;
                            } else if(eInfo.defenseDamageProbability > 100) {
                                logClampedValueEntity("defense_damage_probability", name, Integer.toString(eInfo.defenseDamageProbability), "100");
                                eInfo.defenseDamageProbability = 100;
                            }
                        } catch (ClassCastException e) {
                            eInfo.defenseDamage = 0;
                            logger.warn("Entity \"" + name + "\" has specified defense_damage but defense_damage_probability is invalid, disabling defense_damage");
                        } catch (NullPointerException e) {
                            eInfo.defenseDamage = 0;
                            logger.warn("Entity \"" + name + "\" has specified defense_damage but defense_damage_probability is missing, disabling defense_damage");
                        }
                    }
                } catch (ClassCastException e) {
                    eInfo.defenseDamage = 0;
                    logEntityInvalidValue("defense_damage", name, "0");
                } catch (NullPointerException e) {
                    eInfo.defenseDamage = 0;
                    logEntityMissingOptionalValue("defense_damage", name, "0");
                }

                try {
                    eInfo.evasion = nestedConf.getInt("evasion");
                    if(eInfo.evasion < 0) {
                        logClampedValueEntity("evasion", name, Integer.toString(eInfo.evasion), "0");
                        eInfo.evasion = 0;
                    } else if(eInfo.evasion > 100) {
                        logClampedValueEntity("evasion", name, Integer.toString(eInfo.evasion), "100");
                        eInfo.evasion = 100;
                    }
                } catch (ClassCastException e) {
                    logEntityInvalidValue("evasion", name, "7");
                    eInfo.evasion = 7;
                } catch (NullPointerException e) {
                    logEntityMissingRequiredValue("evasion", name, "7");
                    eInfo.evasion = 7;
                }

                try {
                    eInfo.speed = nestedConf.getInt("speed");
                } catch (ClassCastException e) {
                    logEntityInvalidValue("speed", name, "49");
                    eInfo.speed = 49;
                } catch (NullPointerException e) {
                    logEntityMissingRequiredValue("speed", name, "49");
                    eInfo.speed = 49;
                }

                try {
                    eInfo.ignoreBattle = nestedConf.get("ignore_battle");
                } catch (ClassCastException e) {
                    logEntityInvalidValue("ignore_battle", name, "false");
                    eInfo.ignoreBattle = false;
                } catch (NullPointerException e) {
                    logEntityMissingRequiredValue("ignore_battle", name, "false");
                    eInfo.ignoreBattle = false;
                }

                try {
                    eInfo.category = nestedConf.get("category");
                } catch (ClassCastException e) {
                    logEntityInvalidValue("category", name, "unknown");
                    eInfo.category = "unknown";
                } catch (NullPointerException e) {
                    logEntityMissingRequiredValue("category", name, "unknown");
                    eInfo.category = "unknown";
                }

                try {
                    eInfo.decisionAttack = nestedConf.getInt("decision_attack_probability");
                    if(eInfo.decisionAttack < 0) {
                        logClampedValueEntity("decision_attack_probability", name, Integer.toString(eInfo.decisionAttack), "0");
                        eInfo.decisionAttack = 0;
                    } else if(eInfo.decisionAttack > 100) {
                        logClampedValueEntity("decision_attack_probability", name, Integer.toString(eInfo.decisionAttack), "100");
                        eInfo.decisionAttack = 100;
                    }
                } catch (ClassCastException e) {
                    logEntityInvalidValue("decision_attack_probability", name, "70");
                    eInfo.decisionAttack = 70;
                } catch (NullPointerException e) {
                    logEntityMissingRequiredValue("decision_attack_probability", name, "70");
                    eInfo.decisionAttack = 70;
                }

                try {
                    eInfo.decisionDefend = nestedConf.getInt("decision_defend_probability");
                    if(eInfo.decisionDefend < 0) {
                        logClampedValueEntity("decision_defend_probability", name, Integer.toString(eInfo.decisionDefend), "0");
                        eInfo.decisionDefend = 0;
                    } else if(eInfo.decisionDefend > 100) {
                        logClampedValueEntity("decision_defend_probability", name, Integer.toString(eInfo.decisionDefend), "100");
                        eInfo.decisionDefend = 100;
                    }
                } catch (ClassCastException e) {
                    logEntityInvalidValue("decision_defend_probability", name, "20");
                    eInfo.decisionDefend = 20;
                } catch (NullPointerException e) {
                    logEntityMissingRequiredValue("decision_defend_probability", name, "20");
                    eInfo.decisionDefend = 20;
                }

                try {
                    eInfo.decisionFlee = nestedConf.getInt("decision_flee_probability");
                    if(eInfo.decisionFlee < 0) {
                        logClampedValueEntity("decision_flee_probability", name, Integer.toString(eInfo.decisionFlee), "0");
                        eInfo.decisionFlee = 0;
                    } else if(eInfo.decisionFlee > 100) {
                        logClampedValueEntity("decision_flee_probability", name, Integer.toString(eInfo.decisionFlee), "100");
                        eInfo.decisionFlee = 100;
                    }
                } catch (ClassCastException e) {
                    logEntityInvalidValue("decision_flee_probability", name, "10");
                    eInfo.decisionFlee = 10;
                } catch (NullPointerException e) {
                    logEntityMissingRequiredValue("decision_flee_probability", name, "10");
                    eInfo.decisionFlee = 10;
                }

                if(eInfo.classType != null) {
                    entityInfoMap.put(eInfo.classType.getName(), eInfo);
                } else if(!eInfo.customName.isEmpty()) {
                    customEntityInfoMap.put(eInfo.customName, eInfo);
                } else {
                    logger.error("Cannot add entity to internal config, no \"name\" or \"custom_name\"");
                }
            }
        }
        return true;
    }

    private void logNotFound(String option) {
        logger.warn("Config option \"" + option + "\" not found, setting defaults");
    }

    private void logNotFound(String option, String defaultValue) {
        logger.warn("Config option \"" + option + "\" not found, defaulting to \"" + defaultValue + "\"");
    }

    private void logEntityInvalidValue(String option, String name, String defaultValue) {
        logger.warn("Invalid \"" + option + "\" for \"" + name + "\", defaulting to \"" + defaultValue + "\"");
    }

    private void logEntityMissingRequiredValue(String option, String name, String defaultValue) {
        logger.warn("Entity \"" + name + "\" does not have option \"" + option + "\", defaulting to \"" + defaultValue + "\"");
    }

    private void logEntityMissingOptionalValue(String option, String name, String defaultValue) {
        logger.info("Entity \"" + name + "\" does not have optional option \"" + option + "\", defaulting to \"" + defaultValue + "\"...");
    }

    private void logClampedValue(String option, String from, String clampedTo) {
        logger.warn("Option \"" + option + "\" is out of bounds, clamping value from \"" + from + "\" to \"" + clampedTo + "\"");
    }

    private void logClampedValueEntity(String option, String name, String from, String clampedTo) {
        logger.warn("Option \"" + option + "\" is out of bounds for \"" + name + "\", clamping value from \"" + from + "\" to \"" + clampedTo + "\"");
    }

    private void logTOMLInvalidValue(String option) {
        logger.warn("Config option \"" + option + "\" is an invalid value, setting defaults");
    }

    private void logTOMLInvalidValue(String option, String defaultValue) {
        logger.warn("Config option \"" + option + "\" is an invalid value, defaulting to \"" + defaultValue + "\"");
    }

    private boolean addEntityEntry(EntityInfo eInfo)
    {
        CommentedFileConfig conf = CommentedFileConfig.builder(TurnBasedMinecraftMod.CONFIG_FILE_PATH).build();
        conf.load();

        Collection<com.electronwill.nightconfig.core.Config> entities;
        try {
            entities = conf.get("server_config.entity");
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }

        com.electronwill.nightconfig.core.Config newConf = conf.createSubConfig();
        newConf.set("attack_power", eInfo.attackPower);
        newConf.set("attack_probability", eInfo.attackProbability);
        newConf.set("attack_variance", eInfo.attackVariance);
        newConf.set("attack_effect", eInfo.attackEffect.toString());
        newConf.set("attack_effect_probability", eInfo.attackEffectProbability);
        newConf.set("defense_damage", eInfo.defenseDamage);
        newConf.set("defense_damage_probability", eInfo.defenseDamageProbability);
        newConf.set("evasion", eInfo.evasion);
        newConf.set("speed", eInfo.speed);
        newConf.set("ignore_battle", eInfo.ignoreBattle);
        newConf.set("category", eInfo.category);
        newConf.set("decision_attack_probability", eInfo.decisionAttack);
        newConf.set("decision_defend_probability", eInfo.decisionDefend);
        newConf.set("decision_flee_probability", eInfo.decisionFlee);

        entities.add(newConf);

        conf.save();
        conf.close();

        return true;
    }

    protected boolean editEntityEntry(EntityInfo eInfo)
    {
        CommentedFileConfig conf = CommentedFileConfig.builder(TurnBasedMinecraftMod.CONFIG_FILE_PATH).build();
        conf.load();

        Collection<com.electronwill.nightconfig.core.Config> entities;
        try {
            entities = conf.get("server_config.entity");
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }

        boolean saved = false;
        try {
            if (eInfo.classType != null || !eInfo.customName.isEmpty()) {
                for (com.electronwill.nightconfig.core.Config entity : entities) {
                    String entityName = entity.get("name");
                    if ((eInfo.classType != null && entityName != null && entityName.equals(eInfo.classType.getName()))) {
                        entity.set("attack_power", eInfo.attackPower);
                        entity.set("attack_probability", eInfo.attackProbability);
                        entity.set("attack_variance", eInfo.attackVariance);
                        entity.set("attack_effect", eInfo.attackEffect.toString());
                        entity.set("attack_effect_probability", eInfo.attackEffectProbability);
                        entity.set("defense_damage", eInfo.defenseDamage);
                        entity.set("defense_damage_probability", eInfo.defenseDamageProbability);
                        entity.set("evasion", eInfo.evasion);
                        entity.set("speed", eInfo.speed);
                        entity.set("ignore_battle", eInfo.ignoreBattle);
                        entity.set("category", eInfo.category);
                        entity.set("decision_attack_probability", eInfo.decisionAttack);
                        entity.set("decision_defend_probability", eInfo.decisionDefend);
                        entity.set("decision_flee_probability", eInfo.decisionFlee);
                        saved = true;
                        break;
                    } else {
                        String customName = entity.get("custom_name");
                        if(!eInfo.customName.isEmpty() && customName != null && customName.equals(eInfo.customName)) {
                            entity.set("attack_power", eInfo.attackPower);
                            entity.set("attack_probability", eInfo.attackProbability);
                            entity.set("attack_variance", eInfo.attackVariance);
                            entity.set("attack_effect", eInfo.attackEffect.toString());
                            entity.set("attack_effect_probability", eInfo.attackEffectProbability);
                            entity.set("defense_damage", eInfo.defenseDamage);
                            entity.set("defense_damage_probability", eInfo.defenseDamageProbability);
                            entity.set("evasion", eInfo.evasion);
                            entity.set("speed", eInfo.speed);
                            entity.set("ignore_battle", eInfo.ignoreBattle);
                            entity.set("category", eInfo.category);
                            entity.set("decision_attack_probability", eInfo.decisionAttack);
                            entity.set("decision_defend_probability", eInfo.decisionDefend);
                            entity.set("decision_flee_probability", eInfo.decisionFlee);
                            saved = true;
                            break;
                        }
                    }
                }
                if(!saved) {
                    com.electronwill.nightconfig.core.Config newEntry = conf.createSubConfig();
                    if(eInfo.classType != null) {
                        newEntry.set("name", eInfo.classType.getName());
                    } else if(!eInfo.customName.isEmpty()) {
                        newEntry.set("custom_name", eInfo.customName);
                    } else {
                        logger.error("Failed to save new entity entry into config, no name or custom_name");
                        conf.close();
                        return false;
                    }
                    newEntry.set("attack_power", eInfo.attackPower);
                    newEntry.set("attack_probability", eInfo.attackProbability);
                    newEntry.set("attack_variance", eInfo.attackVariance);
                    newEntry.set("attack_effect", eInfo.attackEffect.toString());
                    newEntry.set("attack_effect_probability", eInfo.attackEffectProbability);
                    newEntry.set("defense_damage", eInfo.defenseDamage);
                    newEntry.set("defense_damage_probability", eInfo.defenseDamageProbability);
                    newEntry.set("evasion", eInfo.evasion);
                    newEntry.set("speed", eInfo.speed);
                    newEntry.set("ignore_battle", eInfo.ignoreBattle);
                    newEntry.set("category", eInfo.category);
                    newEntry.set("decision_attack_probability", eInfo.decisionAttack);
                    newEntry.set("decision_defend_probability", eInfo.decisionDefend);
                    newEntry.set("decision_flee_probability", eInfo.decisionFlee);
                    entities.add(newEntry);
                    saved = true;
                }
            } else {
                return false;
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
            return false;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        conf.save();
        conf.close();

        if(!saved) {
            logger.warn("Failed to save \"" + eInfo.classType.getName() + "\"");
            return false;
        }

        if(eInfo.classType != null) {
            entityInfoMap.put(eInfo.classType.getName(), eInfo);
        } else if(!eInfo.customName.isEmpty()){
            customEntityInfoMap.put(eInfo.customName, eInfo);
        } else {
            logger.warn("Failed to update entity info in memory");
        }

        return true;
    }

    public int getPlayerSpeed()
    {
        return playerSpeed;
    }

    public int getPlayerHasteSpeed()
    {
        return playerHasteSpeed;
    }

    public int getPlayerSlowSpeed()
    {
        return playerSlowSpeed;
    }

    public int getPlayerAttackProbability()
    {
        return playerAttackProbability;
    }

    public int getPlayerEvasion()
    {
        return playerEvasion;
    }

    public int getDefenseDuration()
    {
        return defenseDuration;
    }

    public int getFleeGoodProbability()
    {
        return fleeGoodProbability;
    }

    public int getFleeBadProbability()
    {
        return fleeBadProbability;
    }

    /**
     * Returns a clone of an EntityInfo (to prevent editing it).
     * @param classFullName
     * @return a clone of the stored EntityInfo or null if invalid String
     */
    public EntityInfo getEntityInfo(String classFullName)
    {
        if(classFullName == null) {
            return null;
        }
        EntityInfo eInfo = entityInfoMap.get(classFullName);
        if(eInfo != null)
        {
            eInfo = eInfo.clone();
        }
        return eInfo;
    }

    protected EntityInfo getEntityInfoReference(String classFullName)
    {
        if(classFullName == null) {
            return null;
        }
        return entityInfoMap.get(classFullName);
    }

    protected EntityInfo getMatchingEntityInfo(Object entity)
    {
        if(entity == null) {
            return null;
        }
        EntityInfo matching = entityInfoMap.get(entity.getClass().getName());
        if(matching != null && matching.classType.isInstance(entity)) {
            return matching;
        }
        return null;
    }

    /**
     * Returns a clone of an EntityInfo (to prevent editing it).
     * @param customName
     * @return a clone of the stored custom EntityInfo or null if invalid String
     */
    public EntityInfo getCustomEntityInfo(String customName)
    {
        if(customName == null) {
            return null;
        }
        EntityInfo eInfo = customEntityInfoMap.get(customName);
        if(eInfo != null)
        {
            eInfo = eInfo.clone();
        }
        return eInfo;
    }

    protected EntityInfo getCustomEntityInfoReference(String customName)
    {
        if(customName == null) {
            return null;
        }
        return customEntityInfoMap.get(customName);
    }

    private int getConfigFileVersion(File configFile)
    {
        int version = 0;

        FileConfig conf = FileConfig.of(configFile, TomlFormat.instance());
        conf.load();
        version = conf.getIntOrElse("version", 0);
        conf.close();

        return version;
    }

    private void writeDefaultConfig(InputStream io) {
        try {
            FileOutputStream fos = new FileOutputStream(TurnBasedMinecraftMod.DEFAULT_CONFIG_FILE_PATH);
            byte[] buffer = new byte[1024];
            int count;
            while((count = io.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }
            fos.close();
            io.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean getCanOverwrite(File configFile) {
        boolean canOverwrite;

        FileConfig conf = FileConfig.of(configFile, TomlFormat.instance());
        conf.load();
        canOverwrite = !(Boolean)conf.getOrElse("do_not_overwrite", false);
        conf.close();

        return canOverwrite;
    }

    private CommentedFileConfig getConfigObj(File configFile) {
        CommentedFileConfig conf = CommentedFileConfig
            .builder(configFile)
            .defaultResource(TurnBasedMinecraftMod.DEFAULT_CONFIG_FILE_PATH)
            .build();
        conf.load();

        return conf;
    }

    public boolean updateConfig(String path, Object value) {
        File configFile = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
        CommentedFileConfig conf = getConfigObj(configFile);

        conf.set(path, value);
        conf.save();
        conf.close();

        return true;
    }

    public boolean isIgnoreBattleType(String type)
    {
        return ignoreBattleTypes.contains(type);
    }

    public int getMinimumHitPercentage()
    {
        return minimumHitPercentage;
    }

    public int getMaxInBattle()
    {
        return maxInBattle;
    }

    public void setMaxInBattle(int maxInBattle) {
        if (maxInBattle < 2) {
            maxInBattle = 2;
        } else if (maxInBattle > 30) {
            maxInBattle = 30;
        }
        this.maxInBattle = maxInBattle;
    }

    public boolean isBattleMusicType(String type)
    {
        return musicBattleTypes.contains(type.toLowerCase());
    }

    public boolean isSillyMusicType(String type)
    {
        return musicSillyTypes.contains(type.toLowerCase());
    }

    public boolean isFreezeCombatantsEnabled()
    {
        return freezeCombatantsInBattle;
    }

    public void setFreezeCombatantsInBattle(boolean enabled) {
        freezeCombatantsInBattle = enabled;
    }

    public int getSillyMusicThreshold()
    {
        return sillyMusicThreshold;
    }

    public int getConfigVersion()
    {
        return configVersion;
    }

    public long getDecisionDurationNanos()
    {
        return battleDecisionDurationNanos;
    }

    public int getDecisionDurationSeconds()
    {
        return (int)(battleDecisionDurationNanos / 1000000000L);
    }

    protected void addBattleIgnoringPlayer(int id)
    {
        battleIgnoringPlayers.add(id);
    }

    protected void removeBattleIgnoringPlayer(int id)
    {
        battleIgnoringPlayers.remove(id);
    }

    protected void clearBattleIgnoringPlayers()
    {
        battleIgnoringPlayers.clear();
    }

    protected Set<Integer> getBattleIgnoringPlayers()
    {
        return battleIgnoringPlayers;
    }

    public boolean getIfOnlyOPsCanDisableTurnBasedForSelf()
    {
        return onlyOPsSelfDisableTB;
    }

    public void setIfOnlyOPsCanDisableTurnBasedForSelf(boolean enabled_for_only_ops) {
        onlyOPsSelfDisableTB = enabled_for_only_ops;
    }

    protected void setBattleDisabledForAll(boolean isDisabled)
    {
        battleDisabledForAll = isDisabled;
    }

    protected boolean getBattleDisabledForAll()
    {
        return battleDisabledForAll;
    }

    public boolean isOldBattleBehaviorEnabled()
    {
        return oldBattleBehaviorEnabled;
    }

    public void setOldBattleBehavior(boolean enabled) {
        oldBattleBehaviorEnabled = enabled;
    }

    public int getLeaveBattleCooldownSeconds()
    {
        return leaveBattleCooldownSeconds;
    }

    public void setLeaveBattleCooldownSeconds(int seconds) {
        if (seconds < 1) {
            seconds = 1;
        } else if (seconds > 10) {
            seconds = 10;
        }
        leaveBattleCooldownSeconds = seconds;
    }

    public long getLeaveBattleCooldownNanos()
    {
        return (long)leaveBattleCooldownSeconds * 1000000000L;
    }

    public int getAggroStartBattleDistance()
    {
        return aggroStartBattleDistance;
    }

    public void setAggroStartBattleDistance(int distance) {
        if (distance < 5) {
            distance = 5;
        } else if (distance > 50) {
            distance = 50;
        }
        aggroStartBattleDistance = distance;
    }

    public int getCreeperExplodeTurn() { return creeperExplodeTurn; }

    public boolean getCreeperStopExplodeOnLeaveBattle() { return creeperStopExplodeOnLeaveBattle; }

    public boolean getCreeperAlwaysAllowDamage() { return creeperAlwaysAllowDamage; }
}
