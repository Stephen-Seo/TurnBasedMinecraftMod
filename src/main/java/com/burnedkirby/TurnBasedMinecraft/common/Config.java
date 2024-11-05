package com.burnedkirby.TurnBasedMinecraft.common;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Config
{
    public static final long BATTLE_DECISION_DURATION_SEC_MIN = 5L;
    public static final long BATTLE_DECISION_DURATION_SEC_MAX = 60L;
    public static final long BATTLE_DECISION_DURATION_SEC_DEFAULT = 15L;
    public static final long BATTLE_DECISION_DURATION_NANO_MIN = BATTLE_DECISION_DURATION_SEC_MIN * 1000000000L;
    public static final long BATTLE_DECISION_DURATION_NANO_MAX = BATTLE_DECISION_DURATION_SEC_MAX * 1000000000L;
    public static final long BATTLE_DECISION_DURATION_NANO_DEFAULT = BATTLE_DECISION_DURATION_SEC_DEFAULT * 1000000000L;
    private long battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_DEFAULT;
    private boolean battleDecisionDurationForever = false;
    private Map<String, EntityInfo> entityInfoMap;
    private Map<String, EntityInfo> customEntityInfoMap;
    private Map<String, EntityInfo> customPlayerInfoMap;
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
    private boolean freezeCombatantsInBattle = false;
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

    private Set<String> possibleIgnoreHurtDamageSources;
    private Set<String> ignoreHurtDamageSources;

    private boolean playerOnlyBattles = false;

    public Config() {
        entityInfoMap = new HashMap<>();
        customEntityInfoMap = new HashMap<>();
        customPlayerInfoMap = new HashMap<>();
        ignoreBattleTypes = new HashSet<>();
        logger = null;
        battleIgnoringPlayers = new HashSet<>();
        possibleIgnoreHurtDamageSources = new HashSet<>();
        ignoreHurtDamageSources = new HashSet<>();
    }

    public Config(Logger logger)
    {
        entityInfoMap = new HashMap<String, EntityInfo>();
        customEntityInfoMap = new HashMap<String, EntityInfo>();
        customPlayerInfoMap = new HashMap<String, EntityInfo>();
        ignoreBattleTypes = new HashSet<String>();
        this.logger = logger;
        battleIgnoringPlayers = new HashSet<Integer>();
        possibleIgnoreHurtDamageSources = new HashSet<String>();
        ignoreHurtDamageSources = new HashSet<String>();

        loadDamageSources();

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
        Config defaultConfig = FromDefault(logger);
        try {
            parseConfig(configFile, defaultConfig);
        } catch (Throwable t) {
            logger.error("Failed to parse config file!", t);
        }
    }

    public static Config FromDefault(Logger logger) {
        Config defaultConf = new Config();
        defaultConf.logger = logger;
        defaultConf.loadDamageSources();

        File configFile = new File(TurnBasedMinecraftMod.DEFAULT_CONFIG_FILE_PATH);
        try {
            defaultConf.parseConfig(configFile, null);
        } catch(IOException e) {
            logger.warn("IOException while parsing default config file", e);
        }

        return defaultConf;
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

    private boolean parseConfig(File configFile, @Nullable Config defaultConfig) throws IOException
    {
        CommentedFileConfig conf = getConfigObj(configFile);

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
            } else if (defaultConfig != null) {
                this.leaveBattleCooldownSeconds = defaultConfig.leaveBattleCooldownSeconds;
                conf.set("server_config.leave_battle_cooldown", defaultConfig.leaveBattleCooldownSeconds);
                logNotFound("server_config.leave_battle_cooldown", String.valueOf(defaultConfig.leaveBattleCooldownSeconds));
            } else {
                this.leaveBattleCooldownSeconds = 5;
                logNotFound("server_config.leave_battle_cooldown", "5");
            }
        } catch (Throwable e) {
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
            } else if (defaultConfig != null) {
                this.aggroStartBattleDistance = defaultConfig.aggroStartBattleDistance;
                conf.set("server_config.aggro_start_battle_max_distance", defaultConfig.aggroStartBattleDistance);
                logNotFound("server_config.aggro_start_battle_max_distance", String.valueOf(defaultConfig.aggroStartBattleDistance));
            } else {
                this.aggroStartBattleDistance = 8;
                logNotFound("server_config.aggro_start_battle_max_distance", "8");
            }
        } catch (Throwable e) {
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
            } else if (defaultConfig != null) {
                this.creeperExplodeTurn = defaultConfig.creeperExplodeTurn;
                conf.set("server_config.creeper_explode_turn", defaultConfig.creeperExplodeTurn);
                logNotFound("server_config.creeper_explode_turn", String.valueOf(defaultConfig.creeperExplodeTurn));
            } else {
                this.creeperExplodeTurn = 5;
                logNotFound("server_config.creeper_explode_turn", "5");
            }
        } catch(Throwable e) {
            this.creeperExplodeTurn = 5;
            logTOMLInvalidValue("server_config.creeper_explode_turn", "5");
        }

        try {
            Boolean creeper_stop_explode_on_leave_battle = conf.get("server_config.creeper_stop_explode_on_leave_battle");
            if(creeper_stop_explode_on_leave_battle != null) {
                this.creeperStopExplodeOnLeaveBattle = creeper_stop_explode_on_leave_battle;
            } else if (defaultConfig != null) {
                this.creeperStopExplodeOnLeaveBattle = defaultConfig.creeperStopExplodeOnLeaveBattle;
                conf.set("server_config.creeper_stop_explode_on_leave_battle", defaultConfig.creeperStopExplodeOnLeaveBattle);
                logNotFound("server_config.creeper_stop_explode_on_leave_battle", String.valueOf(defaultConfig.creeperStopExplodeOnLeaveBattle));
            } else {
                this.creeperStopExplodeOnLeaveBattle = true;
                logNotFound("server_config.creeper_stop_explode_on_leave_battle", "true");
            }
        } catch (Throwable e) {
            this.creeperStopExplodeOnLeaveBattle = true;
            logTOMLInvalidValue("server_config.creeper_stop_explode_on_leave_battle", "true");
        }

        try {
            Boolean creeper_always_allow_damage = conf.get("server_config.creeper_always_allow_damage");
            if(creeper_always_allow_damage != null) {
                this.creeperAlwaysAllowDamage = creeper_always_allow_damage;
            } else if (defaultConfig != null) {
                this.creeperAlwaysAllowDamage = defaultConfig.creeperAlwaysAllowDamage;
                conf.set("server_config.creeper_always_allow_damage", defaultConfig.creeperAlwaysAllowDamage);
                logNotFound("server_config.creeper_always_allow_damage", String.valueOf(defaultConfig.creeperAlwaysAllowDamage));
            } else {
                this.creeperAlwaysAllowDamage = true;
                logNotFound("server_config.creeper_always_allow_damage", "true");
            }
        } catch (Throwable e) {
            this.creeperAlwaysAllowDamage = true;
            logTOMLInvalidValue("server_config.creeper_always_allow_damage", "true");
        }

        try {
            Boolean old_battle_behavior = conf.get("server_config.old_battle_behavior");
            if(old_battle_behavior != null) {
                this.oldBattleBehaviorEnabled = old_battle_behavior;
            } else if (defaultConfig != null) {
                this.oldBattleBehaviorEnabled = defaultConfig.oldBattleBehaviorEnabled;
                conf.set("server_config.old_battle_behavior", defaultConfig.oldBattleBehaviorEnabled);
                logNotFound("server_config.old_battle_behavior", String.valueOf(defaultConfig.oldBattleBehaviorEnabled));
            } else {
                this.oldBattleBehaviorEnabled = false;
                logNotFound("server_config.old_battle_behavior", "false");
            }
        } catch (Throwable e) {
            this.oldBattleBehaviorEnabled = false;
            logTOMLInvalidValue("server_config.old_battle_behavior", "false");
        }

        try {
            Boolean anyone_can_disable_tbm_for_self = conf.get("server_config.anyone_can_disable_tbm_for_self");
            if(anyone_can_disable_tbm_for_self != null) {
                this.onlyOPsSelfDisableTB = !anyone_can_disable_tbm_for_self;
            } else if (defaultConfig != null) {
                this.onlyOPsSelfDisableTB = defaultConfig.onlyOPsSelfDisableTB;
                conf.set("server_config.anyone_can_disable_tbm_for_self", defaultConfig.onlyOPsSelfDisableTB);
                logNotFound("server_config.anyone_can_disable_tbm_for_self", String.valueOf(defaultConfig.onlyOPsSelfDisableTB));
            } else {
                this.onlyOPsSelfDisableTB = true;
                logNotFound("server_config.anyone_can_disable_tbm_for_self", "false");
            }
        } catch (Throwable e) {
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
            } else if (defaultConfig != null) {
                this.maxInBattle = defaultConfig.maxInBattle;
                conf.set("server_config.max_in_battle", defaultConfig.maxInBattle);
                logNotFound("server_config.max_in_battle", String.valueOf(defaultConfig.maxInBattle));
            } else {
                maxInBattle = 8;
                logNotFound("server_config.max_in_battle", "8");
            }
        } catch (Throwable e) {
            maxInBattle = 8;
            logTOMLInvalidValue("server_config.max_in_battle", "8");
        }

        try {
            Boolean freeze_battle_combatants = conf.get("server_config.freeze_battle_combatants");
            if(freeze_battle_combatants != null) {
                this.freezeCombatantsInBattle = freeze_battle_combatants;
            } else if (defaultConfig != null) {
                this.freezeCombatantsInBattle = defaultConfig.freezeCombatantsInBattle;
                conf.set("server_config.freeze_battle_combatants", defaultConfig.freezeCombatantsInBattle);
                logNotFound("server_config.freeze_battle_combatants", String.valueOf(defaultConfig.freezeCombatantsInBattle));
            } else {
                freezeCombatantsInBattle = false;
                logNotFound("server_config.freeze_battle_combatants", "false");
            }
        } catch (Throwable e) {
            freezeCombatantsInBattle = false;
            logTOMLInvalidValue("server_config.freeze_battle_combatants", "false");
        }

        try {
            Collection<String> ignore_battle_types = conf.get("server_config.ignore_battle_types");
            if(ignore_battle_types != null) {
                this.ignoreBattleTypes.addAll(ignore_battle_types);
            } else if (defaultConfig != null) {
                this.ignoreBattleTypes = defaultConfig.ignoreBattleTypes;
                List<String> ignoreList = defaultConfig.ignoreBattleTypes.stream().toList();
                conf.set("server_config.ignore_battle_types", ignoreList);
                logNotFound("server_config.ignore_battle_types");
            } else {
                ignoreBattleTypes.add("passive");
                ignoreBattleTypes.add("boss");
                logNotFound("server_config.ignore_battle_types");
            }
        } catch (Throwable e) {
            ignoreBattleTypes.add("passive");
            ignoreBattleTypes.add("boss");
            logTOMLInvalidValue("server_config.ignore_battle_types");
        }

        try {
            OptionalInt player_speed = conf.getOptionalInt("server_config.player_speed");
            if(player_speed.isPresent()) {
                this.playerSpeed = player_speed.getAsInt();
            } else if (defaultConfig != null) {
                this.playerSpeed = defaultConfig.playerSpeed;
                conf.set("server_config.player_speed", defaultConfig.playerSpeed);
                logNotFound("server_config.player_speed", String.valueOf(defaultConfig.playerSpeed));
            } else {
                this.playerSpeed = 50;
                logNotFound("server_config.player_speed", "50");
            }
        } catch (Throwable e) {
            this.playerSpeed = 50;
            logTOMLInvalidValue("server_config.player_speed", "50");
        }

        try {
            OptionalInt player_haste_speed = conf.getOptionalInt("server_config.player_haste_speed");
            if(player_haste_speed.isPresent()) {
                this.playerHasteSpeed = player_haste_speed.getAsInt();
            } else if (defaultConfig != null) {
                this.playerHasteSpeed = defaultConfig.playerHasteSpeed;
                conf.set("server_config.player_haste_speed", defaultConfig.playerHasteSpeed);
                logNotFound("server_config.player_haste_speed", String.valueOf(defaultConfig.playerHasteSpeed));
            } else {
                this.playerHasteSpeed = 80;
                logNotFound("server_config.player_haste_speed", "80");
            }
        } catch (Throwable e) {
            this.playerHasteSpeed = 80;
            logTOMLInvalidValue("server_config.player_haste_speed", "80");
        }

        try {
            OptionalInt player_slow_speed = conf.getOptionalInt("server_config.player_slow_speed");
            if(player_slow_speed.isPresent()) {
                this.playerSlowSpeed = player_slow_speed.getAsInt();
            } else if (defaultConfig != null) {
                this.playerSlowSpeed = defaultConfig.playerSlowSpeed;
                conf.set("server_config.player_slow_speed", defaultConfig.playerSlowSpeed);
                logNotFound("server_config.player_slow_speed", String.valueOf(defaultConfig.playerSlowSpeed));
            } else {
                this.playerSlowSpeed = 20;
                logNotFound("server_config.player_slow_speed", "20");
            }
        } catch (Throwable e) {
            this.playerSlowSpeed = 20;
            logTOMLInvalidValue("server_config.player_slow_speed", "20");
        }

        try {
            OptionalInt player_attack_probability = conf.getOptionalInt("server_config.player_attack_probability");
            if(player_attack_probability.isPresent()) {
                this.playerAttackProbability = player_attack_probability.getAsInt();
            } else if (defaultConfig != null) {
                this.playerAttackProbability = defaultConfig.playerAttackProbability;
                conf.set("server_config.player_attack_probability", defaultConfig.playerAttackProbability);
                logNotFound("server_config.player_attack_probability", String.valueOf(defaultConfig.playerAttackProbability));
            } else {
                this.playerAttackProbability = 90;
                logNotFound("server_config.player_attack_probability", "90");
            }
        } catch (Throwable e) {
            this.playerAttackProbability = 90;
            logTOMLInvalidValue("server_config.player_attack_probability", "90");
        }

        try {
            OptionalInt player_evasion = conf.getOptionalInt("server_config.player_evasion");
            if(player_evasion.isPresent()) {
                this.playerEvasion = player_evasion.getAsInt();
            } else if (defaultConfig != null) {
                this.playerEvasion = defaultConfig.playerEvasion;
                conf.set("server_config.player_evasion", defaultConfig.playerEvasion);
                logNotFound("server_config.player_evasion", String.valueOf(defaultConfig.playerEvasion));
            } else {
                this.playerEvasion = 10;
                logNotFound("server_config.player_evasion", "10");
            }
        } catch (Throwable e) {
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
            } else if (defaultConfig != null) {
                this.defenseDuration = defaultConfig.defenseDuration;
                conf.set("server_config.defense_duration", defaultConfig.defenseDuration);
                logNotFound("server_config.defense_duration", String.valueOf(defaultConfig.defenseDuration));
            } else {
                this.defenseDuration = 1;
                logNotFound("server_config.defense_duration", "1");
            }
        } catch (Throwable e) {
            this.defenseDuration = 1;
            logTOMLInvalidValue("server_config.defense_duration", "1");
        }

        try {
            OptionalInt flee_good_probability = conf.getOptionalInt("server_config.flee_good_probability");
            if(flee_good_probability.isPresent()) {
                this.fleeGoodProbability = flee_good_probability.getAsInt();
            } else if (defaultConfig != null) {
                this.fleeGoodProbability = defaultConfig.fleeGoodProbability;
                conf.set("server_config.flee_good_probability", defaultConfig.fleeGoodProbability);
                logNotFound("server_config.flee_good_probability", String.valueOf(defaultConfig.fleeGoodProbability));
            } else {
                this.fleeGoodProbability = 90;
                logNotFound("server_config.flee_good_probability", "90");
            }
        } catch (Throwable e) {
            this.fleeGoodProbability = 90;
            logTOMLInvalidValue("server_config.flee_good_probability", "90");
        }

        try {
            OptionalInt flee_bad_probability = conf.getOptionalInt("server_config.flee_bad_probability");
            if(flee_bad_probability.isPresent()) {
                this.fleeBadProbability = flee_bad_probability.getAsInt();
            } else if (defaultConfig != null) {
                this.fleeBadProbability = defaultConfig.fleeBadProbability;
                conf.set("server_config.flee_bad_probability", defaultConfig.fleeBadProbability);
                logNotFound("server_config.flee_bad_probability", String.valueOf(defaultConfig.fleeBadProbability));
            } else {
                this.fleeBadProbability = 35;
                logNotFound("server_config.flee_bad_probability", "35");
            }
        } catch (Throwable e) {
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
            } else if (defaultConfig != null) {
                this.minimumHitPercentage = defaultConfig.minimumHitPercentage;
                conf.set("server_config.minimum_hit_percentage", defaultConfig.minimumHitPercentage);
                logNotFound("server_config.minimum_hit_percentage", String.valueOf(defaultConfig.minimumHitPercentage));
            } else {
                this.minimumHitPercentage = 4;
                logNotFound("server_config.minimum_hit_percentage", "4");
            }
        } catch (Throwable e) {
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
            } else if (defaultConfig != null) {
                this.battleDecisionDurationNanos = defaultConfig.battleDecisionDurationNanos;
                conf.set("server_config.battle_turn_time_seconds", defaultConfig.battleDecisionDurationNanos / 1000000000L);
                logNotFound("server_config.battle_turn_time_seconds", String.valueOf(defaultConfig.battleDecisionDurationNanos));
            } else {
                this.battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_DEFAULT;
                logNotFound("server_config.battle_turn_time_seconds", "15");
            }
        } catch (Throwable e) {
            this.battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_DEFAULT;
            logTOMLInvalidValue("server_config.battle_turn_time_seconds", "15");
        }

        try {
            Boolean battle_turn_wait_forever = conf.get("server_config.battle_turn_wait_forever");
            if (battle_turn_wait_forever != null) {
                this.battleDecisionDurationForever = battle_turn_wait_forever;
            } else if (defaultConfig != null)  {
                this.battleDecisionDurationForever = defaultConfig.battleDecisionDurationForever;
                conf.set("server_config.battle_turn_wait_forever", defaultConfig.battleDecisionDurationForever);
                logNotFound("server_config.battle_turn_wait_forever", String.valueOf(defaultConfig.battleDecisionDurationForever));
            } else {
                this.battleDecisionDurationForever = false;
                logNotFound("server_config.battle_turn_wait_forever", "false");
            }
        } catch (Throwable e) {
            this.battleDecisionDurationForever = false;
            logTOMLInvalidValue("server_config.battle_turn_wait_forever", "false");
        }

        try {
            Collection<String> damage_sources = conf.get("server_config.ignore_damage_sources");
            if (damage_sources != null) {
                for (String source : damage_sources) {
                    if (possibleIgnoreHurtDamageSources.contains(source)) {
                        this.ignoreHurtDamageSources.add(source);
                    }
                }
            } else if (defaultConfig != null) {
                this.ignoreHurtDamageSources = defaultConfig.ignoreHurtDamageSources;
                List<String> ignSourcesList = defaultConfig.ignoreHurtDamageSources.stream().toList();
                conf.set("server_config.ignore_damage_sources", ignSourcesList);
                logNotFound("server_config.ignore_damage_sources");
            }
        } catch (Throwable e) {
            logTOMLInvalidValue("server_config.ignore_damage_sources");
        }

        try {
            Boolean is_only_player_battles_enabled = conf.get("server_config.player_only_battles");
            if (is_only_player_battles_enabled != null) {
                playerOnlyBattles = is_only_player_battles_enabled;
            } else if (defaultConfig != null) {
                playerOnlyBattles = defaultConfig.playerOnlyBattles;
                conf.set("server_config.player_only_battles", defaultConfig.playerOnlyBattles);
                logNotFound("server_config.player_only_battles", String.valueOf(defaultConfig.playerOnlyBattles));
            } else {
                playerOnlyBattles = false;
                logNotFound("server_config.player_only_battles", "false");
            }
        } catch (Throwable e) {
            playerOnlyBattles = false;
            logTOMLInvalidValue("server_config.player_only_battles", "false");
        }

        Collection<com.electronwill.nightconfig.core.Config> entities = null;
        try {
            entities = conf.get("server_config.entity");
        } catch (Throwable e) {
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
                } else if (nestedConf.contains("name") && nestedConf.contains("player_name")) {
                    logger.error("Entity cannot have both \"name\" (" + nestedConf.get("name")
                            + ") and \"player_name\" (" + nestedConf.get("player_name") + ") entries");
                    continue;
                } else if (nestedConf.contains("custom_name") && nestedConf.contains("player_name")) {
                    logger.error("Entity cannot have both \"custom_name\" (" + nestedConf.get("custom_name")
                            + ") and \"player_name\" (" + nestedConf.get("player_name") + ") entries");
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
                } else if (nestedConf.contains("player_name")) {
                    try {
                        eInfo.playerName = nestedConf.get("player_name");
                        name = eInfo.playerName;
                    } catch (ClassCastException e) {
                        logger.error("Entity with invalid player_name (must be a string), skipping...");
                        continue;
                    }
                } else {
                    logger.error("Entity must have \"name\" or \"custom_name\" or \"player_name\" entry");
                    continue;
                }

                if (eInfo.playerName.isEmpty()) {
                    try {
                        eInfo.attackPower = nestedConf.getInt("attack_power");
                        if (eInfo.attackPower < 0) {
                            logClampedValueEntity("attack_power", name, Integer.toString(eInfo.attackPower), "0");
                            eInfo.attackPower = 0;
                        }
                    } catch (Throwable e) {
                        if (defaultConfig != null) {
                            if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                                eInfo.attackPower = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).attackPower;
                                logEntityInvalidValue("attack_power", name, String.valueOf(eInfo.attackPower));
                                nestedConf.set("attack_power", eInfo.attackPower);
                            } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                                eInfo.attackPower = defaultConfig.customEntityInfoMap.get(eInfo.customName).attackPower;
                                logEntityInvalidValue("attack_power", name, String.valueOf(eInfo.attackPower));
                                nestedConf.set("attack_power", eInfo.attackPower);
                            } else {
                                logEntityInvalidValue("attack_power", name, "3");
                                eInfo.attackPower = 3;
                                nestedConf.set("attack_power", eInfo.attackPower);
                            }
                        } else {
                            logEntityInvalidValue("attack_power", name, "3");
                            eInfo.attackPower = 3;
                        }
                    }
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
                } catch (Throwable e) {
                    if (defaultConfig != null) {
                        if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                            eInfo.attackProbability = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).attackProbability;
                            logEntityInvalidValue("attack_probability", name, String.valueOf(eInfo.attackProbability));
                            nestedConf.set("attack_probability", eInfo.attackProbability);
                        } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                            eInfo.attackProbability = defaultConfig.customEntityInfoMap.get(eInfo.customName).attackProbability;
                            logEntityInvalidValue("attack_probability", name, String.valueOf(eInfo.attackProbability));
                            nestedConf.set("attack_probability", eInfo.attackProbability);
                        } else if (!eInfo.playerName.isEmpty() && defaultConfig.customPlayerInfoMap.containsKey(eInfo.playerName)) {
                            eInfo.attackProbability = defaultConfig.customPlayerInfoMap.get(eInfo.playerName).attackProbability;
                            logEntityInvalidValue("attack_probability", name, String.valueOf(eInfo.attackProbability));
                            nestedConf.set("attack_probability", eInfo.attackProbability);
                        } else {
                            logEntityInvalidValue("attack_probability", name, "30");
                            eInfo.attackProbability = 30;
                            nestedConf.set("attack_probability", eInfo.attackProbability);
                        }
                    } else {
                        logEntityInvalidValue("attack_probability", name, "30");
                        eInfo.attackProbability = 30;
                    }
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
                        } catch (Throwable e) {
                            eInfo.attackEffect = EntityInfo.Effect.UNKNOWN;
                            logger.warn("Entity \"" + name + "\" has specified attack_effect but attack_effect_probability is invalid, unsetting attack_effect");
                        }
                    }
                } catch (Throwable e) {
                    eInfo.attackEffect = EntityInfo.Effect.UNKNOWN;
                    logEntityInvalidValue("attack_effect", name, "unknown");
                }

                if (eInfo.playerName.isEmpty()) {
                    try {
                        eInfo.attackVariance = nestedConf.getInt("attack_variance");
                        if (eInfo.attackVariance < 0) {
                            logClampedValueEntity("attack_variance", name, Integer.toString(eInfo.attackVariance), "0");
                            eInfo.attackVariance = 0;
                        }
                    } catch (Throwable e) {
                        if (defaultConfig != null) {
                            if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                                eInfo.attackVariance = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).attackVariance;
                                logEntityInvalidValue("attack_variance", name, String.valueOf(eInfo.attackVariance));
                                nestedConf.set("attack_variance", eInfo.attackVariance);
                            } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                                eInfo.attackVariance = defaultConfig.customEntityInfoMap.get(eInfo.customName).attackVariance;
                                logEntityInvalidValue("attack_variance", name, String.valueOf(eInfo.attackVariance));
                                nestedConf.set("attack_variance", eInfo.attackVariance);
                            } else {
                                eInfo.attackVariance = 0;
                                logEntityInvalidValue("attack_variance", name, "0");
                                nestedConf.set("attack_variance", eInfo.attackVariance);
                            }
                        } else {
                            eInfo.attackVariance = 0;
                            logEntityInvalidValue("attack_variance", name, "0");
                        }
                    }

                    try {
                        eInfo.defenseDamage = nestedConf.getInt("defense_damage");
                        if (eInfo.defenseDamage < 0) {
                            logClampedValueEntity("defense_damage", name, Integer.toString(eInfo.defenseDamage), "0");
                            eInfo.defenseDamage = 0;
                        } else if (eInfo.defenseDamage != 0) {
                            try {
                                eInfo.defenseDamageProbability = nestedConf.getInt("defense_damage_probability");
                                if (eInfo.defenseDamageProbability < 1) {
                                    logClampedValueEntity("defense_damage_probability", name, Integer.toString(eInfo.defenseDamageProbability), "1");
                                    eInfo.defenseDamageProbability = 1;
                                } else if (eInfo.defenseDamageProbability > 100) {
                                    logClampedValueEntity("defense_damage_probability", name, Integer.toString(eInfo.defenseDamageProbability), "100");
                                    eInfo.defenseDamageProbability = 100;
                                }
                            } catch (Throwable e) {
                                eInfo.defenseDamage = 0;
                                logger.warn("Entity \"" + name + "\" has specified defense_damage but defense_damage_probability is invalid, disabling defense_damage");
                            }
                        }
                    } catch (Throwable e) {
                        eInfo.defenseDamage = 0;
                        logEntityInvalidValue("defense_damage", name, "0");
                    }
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
                } catch (Throwable e) {
                    if (defaultConfig != null) {
                        if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                            eInfo.evasion = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).evasion;
                            logEntityInvalidValue("evasion", name, String.valueOf(eInfo.evasion));
                            nestedConf.set("evasion", eInfo.evasion);
                        } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                            eInfo.evasion = defaultConfig.customEntityInfoMap.get(eInfo.customName).evasion;
                            logEntityInvalidValue("evasion", name, String.valueOf(eInfo.evasion));
                            nestedConf.set("evasion", eInfo.evasion);
                        } else if (!eInfo.playerName.isEmpty() && defaultConfig.customPlayerInfoMap.containsKey(eInfo.playerName)) {
                            eInfo.evasion = defaultConfig.customPlayerInfoMap.get(eInfo.playerName).evasion;
                            logEntityInvalidValue("evasion", name, String.valueOf(eInfo.evasion));
                            nestedConf.set("evasion", eInfo.evasion);
                        } else {
                            logEntityInvalidValue("evasion", name, "7");
                            eInfo.evasion = 7;
                            nestedConf.set("evasion", eInfo.evasion);
                        }
                    } else {
                        logEntityInvalidValue("evasion", name, "7");
                        eInfo.evasion = 7;
                    }
                }

                try {
                    eInfo.speed = nestedConf.getInt("speed");
                } catch (Throwable e) {
                    if (defaultConfig != null) {
                        if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                            eInfo.speed = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).speed;
                            logEntityInvalidValue("speed", name, String.valueOf(eInfo.speed));
                            nestedConf.set("speed", eInfo.speed);
                        } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                            eInfo.speed = defaultConfig.customEntityInfoMap.get(eInfo.customName).speed;
                            logEntityInvalidValue("speed", name, String.valueOf(eInfo.speed));
                            nestedConf.set("speed", eInfo.speed);
                        } else if (!eInfo.playerName.isEmpty() && defaultConfig.customPlayerInfoMap.containsKey(eInfo.playerName)) {
                            eInfo.speed = defaultConfig.customPlayerInfoMap.get(eInfo.playerName).speed;
                            logEntityInvalidValue("speed", name, String.valueOf(eInfo.speed));
                            nestedConf.set("speed", eInfo.speed);
                        } else {
                            logEntityInvalidValue("speed", name, "49");
                            eInfo.speed = 49;
                            nestedConf.set("speed", eInfo.speed);
                        }
                    } else {
                        logEntityInvalidValue("speed", name, "49");
                        eInfo.speed = 49;
                    }
                }

                try {
                    eInfo.hasteSpeed = nestedConf.getInt("haste_speed");
                } catch (Throwable e) {
                    if (defaultConfig != null) {
                        if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                            eInfo.hasteSpeed = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).hasteSpeed;
                            logEntityInvalidValue("haste_speed", name, String.valueOf(eInfo.hasteSpeed));
                            nestedConf.set("haste_speed", eInfo.hasteSpeed);
                        } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                            eInfo.hasteSpeed = defaultConfig.customEntityInfoMap.get(eInfo.customName).hasteSpeed;
                            logEntityInvalidValue("haste_speed", name, String.valueOf(eInfo.hasteSpeed));
                            nestedConf.set("haste_speed", eInfo.hasteSpeed);
                        } else if (!eInfo.playerName.isEmpty() && defaultConfig.customPlayerInfoMap.containsKey(eInfo.playerName)) {
                            eInfo.hasteSpeed = defaultConfig.customPlayerInfoMap.get(eInfo.playerName).hasteSpeed;
                            logEntityInvalidValue("haste_speed", name, String.valueOf(eInfo.hasteSpeed));
                            nestedConf.set("haste_speed", eInfo.hasteSpeed);
                        } else {
                            logEntityInvalidValue("haste_speed", name, "80");
                            eInfo.hasteSpeed = 80;
                            nestedConf.set("haste_speed", eInfo.hasteSpeed);
                        }
                    } else {
                        logEntityInvalidValue("haste_speed", name, "80");
                        eInfo.hasteSpeed = 80;
                    }
                }

                try {
                    eInfo.slowSpeed = nestedConf.getInt("slow_speed");
                } catch (Throwable e) {
                    if (defaultConfig != null) {
                        if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                            eInfo.slowSpeed = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).slowSpeed;
                            logEntityInvalidValue("slow_speed", name, String.valueOf(eInfo.slowSpeed));
                            nestedConf.set("slow_speed", eInfo.slowSpeed);
                        } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                            eInfo.slowSpeed = defaultConfig.customEntityInfoMap.get(eInfo.customName).slowSpeed;
                            logEntityInvalidValue("slow_speed", name, String.valueOf(eInfo.slowSpeed));
                            nestedConf.set("slow_speed", eInfo.slowSpeed);
                        } else if (!eInfo.playerName.isEmpty() && defaultConfig.customPlayerInfoMap.containsKey(eInfo.playerName)) {
                            eInfo.slowSpeed = defaultConfig.customPlayerInfoMap.get(eInfo.playerName).slowSpeed;
                            logEntityInvalidValue("slow_speed", name, String.valueOf(eInfo.slowSpeed));
                            nestedConf.set("slow_speed", eInfo.slowSpeed);
                        } else {
                            logEntityInvalidValue("slow_speed", name, "20");
                            eInfo.slowSpeed = 20;
                            nestedConf.set("slow_speed", eInfo.slowSpeed);
                        }
                    } else {
                        logEntityInvalidValue("slow_speed", name, "20");
                        eInfo.slowSpeed = 20;
                    }
                }

                if (eInfo.playerName.isEmpty()) {
                    try {
                        eInfo.ignoreBattle = nestedConf.get("ignore_battle");
                    } catch (ClassCastException e) {
                        logEntityInvalidValue("ignore_battle", name, "false");
                        nestedConf.set("ignore_battle", "false");
                        eInfo.ignoreBattle = false;
                    } catch (NullPointerException e) {
                        logEntityMissingRequiredValue("ignore_battle", name, "false");
                        nestedConf.set("ignore_battle", "false");
                        eInfo.ignoreBattle = false;
                    }

                    try {
                        eInfo.category = nestedConf.get("category");
                    } catch (Throwable e) {
                        if (defaultConfig != null) {
                            if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                                eInfo.category = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).category;
                                logEntityInvalidValue("category", name, String.valueOf(eInfo.category));
                                nestedConf.set("category", eInfo.category);
                            } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                                eInfo.category = defaultConfig.customEntityInfoMap.get(eInfo.customName).category;
                                logEntityInvalidValue("category", name, String.valueOf(eInfo.category));
                                nestedConf.set("category", eInfo.category);
                            } else {
                                logEntityInvalidValue("category", name, "30");
                                nestedConf.set("category", "unknown");
                                eInfo.category = "unknown";
                            }
                        } else {
                            logEntityInvalidValue("category", name, "unknown");
                            nestedConf.set("category", "unknown");
                            eInfo.category = "unknown";
                        }
                    }

                    try {
                        eInfo.decisionAttack = nestedConf.getInt("decision_attack_probability");
                        if (eInfo.decisionAttack < 0) {
                            logClampedValueEntity("decision_attack_probability", name, Integer.toString(eInfo.decisionAttack), "0");
                            eInfo.decisionAttack = 0;
                        } else if (eInfo.decisionAttack > 100) {
                            logClampedValueEntity("decision_attack_probability", name, Integer.toString(eInfo.decisionAttack), "100");
                            eInfo.decisionAttack = 100;
                        }
                    } catch (Throwable e) {
                        if (defaultConfig != null) {
                            if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                                eInfo.decisionAttack = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).decisionAttack;
                                logEntityInvalidValue("decision_attack_probability", name, String.valueOf(eInfo.decisionAttack));
                                nestedConf.set("decision_attack_probability", eInfo.decisionAttack);
                            } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                                eInfo.decisionAttack = defaultConfig.customEntityInfoMap.get(eInfo.customName).decisionAttack;
                                logEntityInvalidValue("decision_attack_probability", name, String.valueOf(eInfo.decisionAttack));
                                nestedConf.set("decision_attack_probability", eInfo.decisionAttack);
                            } else {
                                logEntityInvalidValue("decision_attack_probability", name, "70");
                                eInfo.decisionAttack = 70;
                                nestedConf.set("decision_attack_probability", eInfo.decisionAttack);
                            }
                        } else {
                            logEntityInvalidValue("decision_attack_probability", name, "70");
                            eInfo.decisionAttack = 70;
                        }
                    }

                    try {
                        eInfo.decisionDefend = nestedConf.getInt("decision_defend_probability");
                        if (eInfo.decisionDefend < 0) {
                            logClampedValueEntity("decision_defend_probability", name, Integer.toString(eInfo.decisionDefend), "0");
                            eInfo.decisionDefend = 0;
                        } else if (eInfo.decisionDefend > 100) {
                            logClampedValueEntity("decision_defend_probability", name, Integer.toString(eInfo.decisionDefend), "100");
                            eInfo.decisionDefend = 100;
                        }
                    } catch (Throwable e) {
                        if (defaultConfig != null) {
                            if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                                eInfo.decisionDefend = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).decisionDefend;
                                logEntityInvalidValue("decision_defend_probability", name, String.valueOf(eInfo.decisionDefend));
                                nestedConf.set("decision_defend_probability", eInfo.decisionDefend);
                            } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                                eInfo.decisionDefend = defaultConfig.customEntityInfoMap.get(eInfo.customName).decisionDefend;
                                logEntityInvalidValue("decision_defend_probability", name, String.valueOf(eInfo.decisionDefend));
                                nestedConf.set("decision_defend_probability", eInfo.decisionDefend);
                            } else {
                                logEntityInvalidValue("decision_defend_probability", name, "20");
                                eInfo.decisionDefend = 20;
                                nestedConf.set("decision_defend_probability", eInfo.decisionDefend);
                            }
                        } else {
                            logEntityInvalidValue("decision_defend_probability", name, "20");
                            eInfo.decisionDefend = 20;
                        }
                    }

                    try {
                        eInfo.decisionFlee = nestedConf.getInt("decision_flee_probability");
                        if (eInfo.decisionFlee < 0) {
                            logClampedValueEntity("decision_flee_probability", name, Integer.toString(eInfo.decisionFlee), "0");
                            eInfo.decisionFlee = 0;
                        } else if (eInfo.decisionFlee > 100) {
                            logClampedValueEntity("decision_flee_probability", name, Integer.toString(eInfo.decisionFlee), "100");
                            eInfo.decisionFlee = 100;
                        }
                    } catch (Throwable e) {
                        if (defaultConfig != null) {
                            if (eInfo.classType != null && defaultConfig.entityInfoMap.containsKey(eInfo.classType.getName())) {
                                eInfo.decisionFlee = defaultConfig.entityInfoMap.get(eInfo.classType.getName()).decisionFlee;
                                logEntityInvalidValue("decision_flee_probability", name, String.valueOf(eInfo.decisionFlee));
                                nestedConf.set("decision_flee_probability", eInfo.decisionFlee);
                            } else if (!eInfo.customName.isEmpty() && defaultConfig.customEntityInfoMap.containsKey(eInfo.customName)) {
                                eInfo.decisionFlee = defaultConfig.customEntityInfoMap.get(eInfo.customName).decisionFlee;
                                logEntityInvalidValue("decision_flee_probability", name, String.valueOf(eInfo.decisionFlee));
                                nestedConf.set("decision_flee_probability", eInfo.decisionFlee);
                            } else {
                                logEntityInvalidValue("decision_flee_probability", name, "10");
                                eInfo.decisionFlee = 10;
                                nestedConf.set("decision_flee_probability", eInfo.decisionFlee);
                            }
                        } else {
                            logEntityInvalidValue("decision_flee_probability", name, "10");
                            eInfo.decisionFlee = 10;
                        }
                    }
                }

                if(eInfo.classType != null) {
                    entityInfoMap.put(eInfo.classType.getName(), eInfo);
                } else if(!eInfo.customName.isEmpty()) {
                    customEntityInfoMap.put(eInfo.customName, eInfo);
                } else if (!eInfo.playerName.isEmpty()) {
                    customPlayerInfoMap.put(eInfo.playerName, eInfo);
                } else {
                    logger.error("Cannot add entity to internal config, no \"name\" or \"custom_name\" or \"player_name\"");
                }
            }

            if (defaultConfig != null) {
                for (String key : defaultConfig.entityInfoMap.keySet()) {
                    if (!entityInfoMap.containsKey(key)) {
                        EntityInfo eInfo = defaultConfig.entityInfoMap.get(key);
                        entityInfoMap.put(key, eInfo);
                        com.electronwill.nightconfig.core.Config newConf = conf.createSubConfig();
                        newConf.set("name", eInfo.classType.getName());
                        newConf.set("attack_power", eInfo.attackPower);
                        newConf.set("attack_probability", eInfo.attackProbability);
                        newConf.set("attack_variance", eInfo.attackVariance);
                        newConf.set("attack_effect", eInfo.attackEffect);
                        newConf.set("attack_effect_probability", eInfo.attackEffectProbability);
                        newConf.set("defense_damage", eInfo.defenseDamage);
                        newConf.set("defense_damage_probability", eInfo.defenseDamageProbability);
                        newConf.set("evasion", eInfo.evasion);
                        newConf.set("speed", eInfo.speed);
                        newConf.set("haste_speed", eInfo.hasteSpeed);
                        newConf.set("slow_speed", eInfo.slowSpeed);
                        newConf.set("ignore_battle", eInfo.ignoreBattle);
                        newConf.set("category", eInfo.category);
                        newConf.set("decision_attack_probability", eInfo.decisionAttack);
                        newConf.set("decision_defend_probability", eInfo.decisionDefend);
                        newConf.set("decision_flee_probability", eInfo.decisionFlee);
                        entities.add(newConf);
                        logEntityNotFound(key);
                    }
                }
                for (String key : defaultConfig.customEntityInfoMap.keySet()) {
                    if (!customEntityInfoMap.containsKey(key)) {
                        EntityInfo eInfo = defaultConfig.customEntityInfoMap.get(key);
                        customEntityInfoMap.put(key, eInfo);
                        com.electronwill.nightconfig.core.Config newConf = conf.createSubConfig();
                        newConf.set("custom_name", eInfo.customName);
                        newConf.set("attack_power", eInfo.attackPower);
                        newConf.set("attack_probability", eInfo.attackProbability);
                        newConf.set("attack_variance", eInfo.attackVariance);
                        newConf.set("attack_effect", eInfo.attackEffect);
                        newConf.set("attack_effect_probability", eInfo.attackEffectProbability);
                        newConf.set("defense_damage", eInfo.defenseDamage);
                        newConf.set("defense_damage_probability", eInfo.defenseDamageProbability);
                        newConf.set("evasion", eInfo.evasion);
                        newConf.set("speed", eInfo.speed);
                        newConf.set("haste_speed", eInfo.hasteSpeed);
                        newConf.set("slow_speed", eInfo.slowSpeed);
                        newConf.set("ignore_battle", eInfo.ignoreBattle);
                        newConf.set("category", eInfo.category);
                        newConf.set("decision_attack_probability", eInfo.decisionAttack);
                        newConf.set("decision_defend_probability", eInfo.decisionDefend);
                        newConf.set("decision_flee_probability", eInfo.decisionFlee);
                        entities.add(newConf);
                        logCustomEntityNotFound(key);
                    }
                }
                for (String key : defaultConfig.customPlayerInfoMap.keySet()) {
                    if (!customPlayerInfoMap.containsKey(key)) {
                        EntityInfo eInfo = defaultConfig.customPlayerInfoMap.get(key);
                        customPlayerInfoMap.put(key, eInfo);
                        com.electronwill.nightconfig.core.Config newConf = conf.createSubConfig();
                        newConf.set("player_name", eInfo.playerName);
                        newConf.set("attack_probability", eInfo.attackProbability);
                        newConf.set("attack_effect", eInfo.attackEffect);
                        newConf.set("attack_effect_probability", eInfo.attackEffectProbability);
                        newConf.set("evasion", eInfo.evasion);
                        newConf.set("speed", eInfo.speed);
                        newConf.set("haste_speed", eInfo.hasteSpeed);
                        newConf.set("slow_speed", eInfo.slowSpeed);
                        entities.add(newConf);
                        logPlayerEntityNotFound(key);
                    }
                }
            }
        }

        if (defaultConfig != null) {
            conf.save();
            conf.close();
        }

        return true;
    }

    private void logNotFound(String option) {
        if (logger != null) {
            logger.warn("Config option \"" + option + "\" not found, setting defaults");
        }
    }

    private void logNotFound(String option, String defaultValue) {
        if (logger != null) {
            logger.warn("Config option \"" + option + "\" not found, defaulting to \"" + defaultValue + "\"");
        }
    }

    private void logEntityInvalidValue(String option, String name, String defaultValue) {
        if (logger != null) {
            logger.warn("Invalid \"" + option + "\" for \"" + name + "\", defaulting to \"" + defaultValue + "\"");
        }
    }

    private void logEntityMissingRequiredValue(String option, String name, String defaultValue) {
        if (logger != null) {
            logger.warn("Entity \"" + name + "\" does not have option \"" + option + "\", defaulting to \"" + defaultValue + "\"");
        }
    }

    private void logEntityMissingOptionalValue(String option, String name, String defaultValue) {
        if (logger != null) {
            logger.info("Entity \"" + name + "\" does not have optional option \"" + option + "\", defaulting to \"" + defaultValue + "\"...");
        }
    }

    private void logClampedValue(String option, String from, String clampedTo) {
        if (logger != null) {
            logger.warn("Option \"" + option + "\" is out of bounds, clamping value from \"" + from + "\" to \"" + clampedTo + "\"");
        }
    }

    private void logClampedValueEntity(String option, String name, String from, String clampedTo) {
        if (logger != null) {
            logger.warn("Option \"" + option + "\" is out of bounds for \"" + name + "\", clamping value from \"" + from + "\" to \"" + clampedTo + "\"");
        }
    }

    private void logTOMLInvalidValue(String option) {
        if (logger != null) {
            logger.warn("Config option \"" + option + "\" is an invalid value, setting defaults");
        }
    }

    private void logTOMLInvalidValue(String option, String defaultValue) {
        if (logger != null) {
            logger.warn("Config option \"" + option + "\" is an invalid value, defaulting to \"" + defaultValue + "\"");
        }
    }

    private void logEntityNotFound(String entityName) {
        if (logger != null) {
            logger.warn("Entity entry named \"" + entityName + "\" in default config, but not in this config, adding");
        }
    }

    private void logCustomEntityNotFound(String entityName) {
        if (logger != null) {
            logger.warn("Entity custom entry named \"" + entityName + "\" in default config, but not in this config, adding");
        }
    }

    private void logPlayerEntityNotFound(String entityName) {
        if (logger != null) {
            logger.warn("Entity player entry named \"" + entityName + "\" in default config, but not in this config, adding");
        }
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
        if (eInfo.playerName.isEmpty()) {
            newConf.set("attack_power", eInfo.attackPower);
        }
        newConf.set("attack_probability", eInfo.attackProbability);
        if (eInfo.playerName.isEmpty()) {
            newConf.set("attack_variance", eInfo.attackVariance);
        }
        newConf.set("attack_effect", eInfo.attackEffect.toString());
        newConf.set("attack_effect_probability", eInfo.attackEffectProbability);
        if (eInfo.playerName.isEmpty()) {
            newConf.set("defense_damage", eInfo.defenseDamage);
            newConf.set("defense_damage_probability", eInfo.defenseDamageProbability);
        }
        newConf.set("evasion", eInfo.evasion);
        newConf.set("speed", eInfo.speed);
        newConf.set("haste_speed", eInfo.hasteSpeed);
        newConf.set("slow_speed", eInfo.slowSpeed);
        if (eInfo.playerName.isEmpty()) {
            newConf.set("ignore_battle", eInfo.ignoreBattle);
            newConf.set("category", eInfo.category);
            newConf.set("decision_attack_probability", eInfo.decisionAttack);
            newConf.set("decision_defend_probability", eInfo.decisionDefend);
            newConf.set("decision_flee_probability", eInfo.decisionFlee);
        }

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
            if (eInfo.classType != null || !eInfo.customName.isEmpty() || !eInfo.playerName.isEmpty()) {
                for (com.electronwill.nightconfig.core.Config entity : entities) {
                    String entityName = entity.get("name");
                    String customName = entity.get("custom_name");
                    String playerName = entity.get("player_name");
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
                        entity.set("haste_speed", eInfo.hasteSpeed);
                        entity.set("slow_speed", eInfo.slowSpeed);
                        entity.set("ignore_battle", eInfo.ignoreBattle);
                        entity.set("category", eInfo.category);
                        entity.set("decision_attack_probability", eInfo.decisionAttack);
                        entity.set("decision_defend_probability", eInfo.decisionDefend);
                        entity.set("decision_flee_probability", eInfo.decisionFlee);
                        saved = true;
                        break;
                    } else if (!eInfo.customName.isEmpty() && customName != null && customName.equals(eInfo.customName)) {
                        entity.set("attack_power", eInfo.attackPower);
                        entity.set("attack_probability", eInfo.attackProbability);
                        entity.set("attack_variance", eInfo.attackVariance);
                        entity.set("attack_effect", eInfo.attackEffect.toString());
                        entity.set("attack_effect_probability", eInfo.attackEffectProbability);
                        entity.set("defense_damage", eInfo.defenseDamage);
                        entity.set("defense_damage_probability", eInfo.defenseDamageProbability);
                        entity.set("evasion", eInfo.evasion);
                        entity.set("speed", eInfo.speed);
                        entity.set("haste_speed", eInfo.hasteSpeed);
                        entity.set("slow_speed", eInfo.slowSpeed);
                        entity.set("ignore_battle", eInfo.ignoreBattle);
                        entity.set("category", eInfo.category);
                        entity.set("decision_attack_probability", eInfo.decisionAttack);
                        entity.set("decision_defend_probability", eInfo.decisionDefend);
                        entity.set("decision_flee_probability", eInfo.decisionFlee);
                        saved = true;
                        break;
                    } else if (!eInfo.playerName.isEmpty() && playerName != null && playerName.equals(eInfo.playerName)) {
                        entity.set("attack_probability", eInfo.attackProbability);
                        entity.set("attack_effect", eInfo.attackEffect.toString());
                        entity.set("attack_effect_probability", eInfo.attackEffectProbability);
                        entity.set("evasion", eInfo.evasion);
                        entity.set("speed", eInfo.speed);
                        entity.set("haste_speed", eInfo.hasteSpeed);
                        entity.set("slow_speed", eInfo.slowSpeed);
                        saved = true;
                        break;
                    }
                }
                if(!saved) {
                    com.electronwill.nightconfig.core.Config newEntry = conf.createSubConfig();
                    if(eInfo.classType != null) {
                        newEntry.set("name", eInfo.classType.getName());
                    } else if(!eInfo.customName.isEmpty()) {
                        newEntry.set("custom_name", eInfo.customName);
                    } else if (!eInfo.playerName.isEmpty()) {
                        newEntry.set("player_name", eInfo.playerName);
                    } else {
                        logger.error("Failed to save new entity entry into config, no name or custom_name");
                        conf.close();
                        return false;
                    }

                    if (eInfo.playerName.isEmpty()) {
                        newEntry.set("attack_power", eInfo.attackPower);
                    }
                    newEntry.set("attack_probability", eInfo.attackProbability);
                    if (eInfo.playerName.isEmpty()) {
                        newEntry.set("attack_variance", eInfo.attackVariance);
                    }
                    newEntry.set("attack_effect", eInfo.attackEffect.toString());
                    newEntry.set("attack_effect_probability", eInfo.attackEffectProbability);
                    if (eInfo.playerName.isEmpty()) {
                        newEntry.set("defense_damage", eInfo.defenseDamage);
                        newEntry.set("defense_damage_probability", eInfo.defenseDamageProbability);
                    }
                    newEntry.set("evasion", eInfo.evasion);
                    newEntry.set("speed", eInfo.speed);
                    newEntry.set("haste_speed", eInfo.hasteSpeed);
                    newEntry.set("slow_speed", eInfo.slowSpeed);
                    if (eInfo.playerName.isEmpty()) {
                        newEntry.set("ignore_battle", eInfo.ignoreBattle);
                        newEntry.set("category", eInfo.category);
                        newEntry.set("decision_attack_probability", eInfo.decisionAttack);
                        newEntry.set("decision_defend_probability", eInfo.decisionDefend);
                        newEntry.set("decision_flee_probability", eInfo.decisionFlee);
                    }
                    entities.add(newEntry);
                    saved = true;
                }
            } else {
                return false;
            }
        } catch (Throwable e) {
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
        } else if(!eInfo.customName.isEmpty()) {
            customEntityInfoMap.put(eInfo.customName, eInfo);
        } else if (!eInfo.playerName.isEmpty()) {
            customPlayerInfoMap.put(eInfo.playerName, eInfo);
        } else {
            logger.warn("Failed to update entity info in memory");
        }

        return true;
    }

    public int getPlayerSpeed()
    {
        return playerSpeed;
    }

    public void setPlayerSpeed(int speed) {
        if (speed < 0) {
            speed = 0;
        } else if (speed > 100) {
            speed = 100;
        }

        playerSpeed = speed;
    }

    public int getPlayerHasteSpeed()
    {
        return playerHasteSpeed;
    }

    public void setPlayerHasteSpeed(int speed) {
        if (speed < 0) {
            speed = 0;
        } else if (speed > 100) {
            speed = 100;
        }

        playerHasteSpeed = speed;
    }

    public int getPlayerSlowSpeed()
    {
        return playerSlowSpeed;
    }

    public void setPlayerSlowSpeed(int speed) {
        if (speed < 0) {
            speed = 0;
        } else if (speed > 100) {
            speed = 100;
        }

        playerSlowSpeed = speed;
    }

    public int getPlayerAttackProbability()
    {
        return playerAttackProbability;
    }

    public void setPlayerAttackProbability(int probability) {
        if (probability < 1) {
            probability = 1;
        } else if (probability > 100) {
            probability = 100;
        }
        playerAttackProbability = probability;
    }

    public int getPlayerEvasion()
    {
        return playerEvasion;
    }

    public void setPlayerEvasion(int evasion) {
        if (evasion < 0) {
            evasion = 0;
        } else if (evasion > 100) {
            evasion = 100;
        }
        playerEvasion = evasion;
    }

    public int getDefenseDuration()
    {
        return defenseDuration;
    }

    public void setDefenseDuration(int turns) {
        if (turns < 0) {
            turns = 0;
        } else if (turns > 5) {
            turns = 5;
        }

        defenseDuration = turns;
    }

    public int getFleeGoodProbability()
    {
        return fleeGoodProbability;
    }

    public void setFleeGoodProbability(int probability) {
        if (probability < 1) {
            probability = 1;
        } else if (probability > 100) {
            probability = 100;
        }
        fleeGoodProbability = probability;
    }

    public int getFleeBadProbability()
    {
        return fleeBadProbability;
    }

    public void setFleeBadProbability(int probability) {
        if (probability < 1) {
            probability = 1;
        } else if (probability > 100) {
            probability = 100;
        }
        fleeBadProbability = probability;
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

    /**
     * Returns a clone of an EntityInfo (to prevent editing it).
     * @param playerName
     * @return a clone of the stored custom EntityInfo or null if invalid String
     */
    public EntityInfo getPlayerInfo(String playerName) {
        if (playerName == null) {
            return null;
        }
        EntityInfo eInfo = customPlayerInfoMap.get(playerName);
        if (eInfo != null) {
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

    protected EntityInfo getPlayerInfoReference(String playerName) {
        if (playerName == null) {
            return null;
        }
        return customPlayerInfoMap.get(playerName);
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
        } catch (Throwable e) {
            logger.error("Failed to write default config", e);
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

    public boolean updateConfigAppendToStringArray(String path, String string_value) {
        File configFile = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
        CommentedFileConfig conf = getConfigObj(configFile);

        Collection<String> strings;
        try {
            strings = conf.get(path);
        } catch (Exception e) {
            TurnBasedMinecraftMod.logger.warn("Exception during fetching Collection<String> from config (append)");
            TurnBasedMinecraftMod.logger.warn(e);
            return false;
        }

        if (strings.contains(string_value)) {
            return false;
        }
        strings.add(string_value);

        try {
            conf.set(path, strings);
        } catch (Exception e) {
            TurnBasedMinecraftMod.logger.warn("Exception during setting Collection<String> in config (append)");
            TurnBasedMinecraftMod.logger.warn(e);
            return false;
        }
        conf.save();
        conf.close();

        return true;
    }

    public boolean updateConfigRemoveFromStringArray(String path, String string_value) {
        File configFile = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
        CommentedFileConfig conf = getConfigObj(configFile);

        Collection<String> strings;
        try {
            strings = conf.get(path);
        } catch (Exception e) {
            TurnBasedMinecraftMod.logger.warn("Exception during fetching Collection<String> from config (removal)");
            TurnBasedMinecraftMod.logger.warn(e);
            return false;
        }

        if (!strings.contains(string_value)) {
            return false;
        }
        strings.remove(string_value);

        try {
            conf.set(path, strings);
        } catch (Exception e) {
            TurnBasedMinecraftMod.logger.warn("Exception during setting Collection<String> in config (removal)");
            TurnBasedMinecraftMod.logger.warn(e);
            return false;
        }
        conf.save();
        conf.close();

        return true;
    }

    public boolean isIgnoreBattleType(String type)
    {
        return ignoreBattleTypes.contains(type);
    }

    public Collection<String> getIgnoreBattleTypes() {
        return ignoreBattleTypes;
    }

    public boolean removeIgnoreBattleType(String category) {
        return ignoreBattleTypes.remove(category);
    }

    public boolean addIgnoreBattleType(String category) {
        return ignoreBattleTypes.add(category);
    }

    public int getMinimumHitPercentage()
    {
        return minimumHitPercentage;
    }

    public void setMinimumHitPercentage(int percentage) {
        if (percentage < 1) {
            percentage = 1;
        } else if (percentage > 100) {
            percentage = 100;
        }
        minimumHitPercentage = percentage;
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

    public boolean isFreezeCombatantsEnabled()
    {
        return freezeCombatantsInBattle;
    }

    public void setFreezeCombatantsInBattle(boolean enabled) {
        freezeCombatantsInBattle = enabled;
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

    public void setDecisionDurationSeconds(long seconds) {
        if (seconds < 5) {
            seconds = 5;
        } else if (seconds > 60) {
            seconds = 60;
        }
        battleDecisionDurationNanos = seconds * 1000000000L;
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

    public void setCreeperExplodeTurn(int turns) {
        if (turns < 1) {
            turns = 1;
        } else if (turns > 10) {
            turns = 10;
        }
        creeperExplodeTurn = turns;
    }

    public boolean getCreeperStopExplodeOnLeaveBattle() { return creeperStopExplodeOnLeaveBattle; }

    public void setCreeperStopExplodeOnLeaveBattle(boolean stop_explode_on_leave_battle) {
        creeperStopExplodeOnLeaveBattle = stop_explode_on_leave_battle;
    }

    public boolean getCreeperAlwaysAllowDamage() { return creeperAlwaysAllowDamage; }

    public void setCreeperAlwaysAllowDamage(boolean allow_damage) {
        creeperAlwaysAllowDamage = allow_damage;
    }

    public boolean isBattleDecisionDurationForever() {
        return battleDecisionDurationForever;
    }

    public void setBattleDecisionDurationForever(boolean battleDecisionDurationForever) {
        this.battleDecisionDurationForever = battleDecisionDurationForever;
    }

    public final Collection<String> getPossibleIgnoreHurtDamageSources() {
        return possibleIgnoreHurtDamageSources;
    }

    public final Collection<String> getIgnoreHurtDamageSources() {
        return ignoreHurtDamageSources;
    }

    public boolean addIgnoreHurtDamageSource(String source) {
        if (possibleIgnoreHurtDamageSources.contains(source) && !ignoreHurtDamageSources.contains(source)) {
            ignoreHurtDamageSources.add(source);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeIgnoreHurtDamageSource(String source) {
        return ignoreHurtDamageSources.remove(source);
    }

    public boolean isPlayerOnlyBattlesEnabled() {
        return playerOnlyBattles;
    }

    public void setIsPlayerOnlyBattles(boolean enabled) {
        playerOnlyBattles = enabled;
    }

    private void loadDamageSources() {
        possibleIgnoreHurtDamageSources.clear();

        try {
            VanillaRegistries.createLookup().lookupOrThrow(Registries.DAMAGE_TYPE).listElements().forEach(dt -> possibleIgnoreHurtDamageSources.add(dt.value().msgId()));
        } catch (Exception e) {
            logger.warn("Config failed to load possible DamageSources! Undesired things may happen, like Zombies dying from Fire during battle!");
            logger.warn(e);
        }
    }
}
