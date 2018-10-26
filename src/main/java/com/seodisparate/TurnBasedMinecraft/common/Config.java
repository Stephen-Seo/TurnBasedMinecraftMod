package com.seodisparate.TurnBasedMinecraft.common;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.consensys.cava.toml.*;
import org.apache.logging.log4j.Logger;

public class Config
{
    public static final long BATTLE_DECISION_DURATION_NANO_MIN = 5000000000L;
    public static final long BATTLE_DECISION_DURATION_NANO_MAX = 60000000000L;
    public static final long BATTLE_DECISION_DURATION_NANO_DEFAULT = 15000000000L;
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

    public Config(Logger logger)
    {
        entityInfoMap = new HashMap<String, EntityInfo>();
        customEntityInfoMap = new HashMap<String, EntityInfo>();
        ignoreBattleTypes = new HashSet<String>();
        this.logger = logger;
        musicBattleTypes = new HashSet<String>();
        musicSillyTypes = new HashSet<String>();
        battleIgnoringPlayers = new HashSet<Integer>();

        int internalVersion = getConfigFileVersion(getClass().getResourceAsStream(TurnBasedMinecraftMod.CONFIG_INTERNAL_PATH));

        if(internalVersion == 0)
        {
            logger.error("Failed to check version of internal config file");
        }
        else
        {
            configVersion = internalVersion;
        }

        try
        {
            File testLoad = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
            if(!testLoad.exists())
            {
                writeConfig();
            }
        }
        catch (Throwable t)
        {
            logger.error("Failed to check/create-new config file");
        }

        // parse config
        File configFile = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
        if(!configFile.exists() || !configFile.canRead())
        {
            logger.error("Failed to read/parse config file " + TurnBasedMinecraftMod.CONFIG_FILE_PATH);
            return;
        }

        int configVersion = getConfigFileVersion(configFile);
        if(configVersion < this.configVersion)
        {
            logger.warn("Config file " + TurnBasedMinecraftMod.CONFIG_FILENAME + " is older version, renaming...");
            moveOldConfig();
            try
            {
                writeConfig();
            } catch (Throwable t)
            {
                logger.error("Failed to write config file!");
            }
        }
        try
        {
            parseConfig(configFile);
        } catch (Throwable t)
        {
            logger.error("Failed to parse config file!");
        }
    }

    private void writeConfig() throws IOException
    {
        File configFile = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
        File dirs = configFile.getParentFile();
        dirs.mkdirs();
        InputStream configStream = this.getClass().getResourceAsStream(TurnBasedMinecraftMod.CONFIG_INTERNAL_PATH);
        FileOutputStream configOutput = new FileOutputStream(configFile);
        byte[] buf = new byte[4096];
        int read = 0;
        while(read != -1)
        {
            read = configStream.read(buf);
            if(read > 0)
            {
                configOutput.write(buf, 0, read);
            }
        }
        configStream.close();
        configOutput.close();
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
        TomlParseResult parseResult = Toml.parse(configFile.toPath(), TomlVersion.V0_5_0);

        // client_config
        {
            TomlArray battleMusicCategories = parseResult.getArray("client_config.battle_music");
            if(battleMusicCategories != null)
            {
                for (int i = 0; i < battleMusicCategories.size(); ++i)
                {
                    musicBattleTypes.add(battleMusicCategories.getString(i));
                }
            }
            else
            {
                musicBattleTypes.add("monster");
                musicBattleTypes.add("animal");
                musicBattleTypes.add("boss");
                musicBattleTypes.add("player");
                logNotFound("client_config.battle_music");
            }
        }
        {
            TomlArray sillyMusicCategories = parseResult.getArray("client_config.silly_music");
            if(sillyMusicCategories != null)
            {
                for (int i = 0; i < sillyMusicCategories.size(); ++i)
                {
                    musicSillyTypes.add(sillyMusicCategories.getString(i));
                }
            }
            else
            {
                musicSillyTypes.add("passive");
                logNotFound("client_config.silly_music");
            }
        }

        try
        {
            sillyMusicThreshold = parseResult.getLong("client_config.silly_music_threshold").intValue();
        }
        catch (NullPointerException e)
        {
            sillyMusicThreshold = 40;
            logNotFound("client_config.silly_music_threshold", "40");
        }

        // server_config
        try
        {
            leaveBattleCooldownSeconds = parseResult.getLong("server_config.leave_battle_cooldown").intValue();
            if (leaveBattleCooldownSeconds < 1)
            {
                leaveBattleCooldownSeconds = 1;
            } else if (leaveBattleCooldownSeconds > 10)
            {
                leaveBattleCooldownSeconds = 10;
            }
        }
        catch (NullPointerException e)
        {
            leaveBattleCooldownSeconds = 5;
            logNotFound("server_config.leave_battle_cooldown", "5");
        }

        try
        {
            aggroStartBattleDistance = parseResult.getLong("server_config.aggro_start_battle_max_distance").intValue();
            if (aggroStartBattleDistance < 5)
            {
                aggroStartBattleDistance = 5;
            } else if (aggroStartBattleDistance > 50)
            {
                aggroStartBattleDistance = 50;
            }
        }
        catch (NullPointerException e)
        {
            aggroStartBattleDistance = 8;
            logNotFound("server_config.aggro_start_battle_max_distance", "8");
        }

        try
        {
            oldBattleBehaviorEnabled = parseResult.getBoolean("server_config.old_battle_behavior");
        }
        catch (NullPointerException e)
        {
            oldBattleBehaviorEnabled = false;
            logNotFound("server_config.old_battle_behavior", "false");
        }

        try
        {
            onlyOPsSelfDisableTB = !parseResult.getBoolean("server_config.anyone_can_disable_tbm_for_self");
        }
        catch (NullPointerException e)
        {
            onlyOPsSelfDisableTB = true;
            logNotFound("server_config.anyone_can_disable_tbm_for_self", "false");
        }

        try
        {
            maxInBattle = parseResult.getLong("server_config.max_in_battle").intValue();
            if (maxInBattle < 2)
            {
                maxInBattle = 2;
            }
        }
        catch (NullPointerException e)
        {
            maxInBattle = 8;
            logNotFound("server_config.max_in_battle", "8");
        }

        try
        {
            freezeCombatantsInBattle = parseResult.getBoolean("server_config.freeze_battle_combatants");
        }
        catch (NullPointerException e)
        {
            freezeCombatantsInBattle = false;
            logNotFound("server_config.freeze_battle_combatants", "false");
        }

        try
        {
            TomlArray ignoreTypes = parseResult.getArray("server_config.ignore_battle_types");
            for(int i = 0; i < ignoreTypes.size(); ++i)
            {
                ignoreBattleTypes.add(ignoreTypes.getString(i));
            }
        }
        catch (NullPointerException e)
        {
            ignoreBattleTypes.add("passive");
            ignoreBattleTypes.add("boss");
            logNotFound("server_config.ignore_battle_types");
        }

        try
        {
            playerSpeed = parseResult.getLong("server_config.player_speed").intValue();
        }
        catch (NullPointerException e)
        {
            playerSpeed = 50;
            logNotFound("server_config.player_speed", "50");
        }
        try
        {
            playerHasteSpeed = parseResult.getLong("server_config.player_haste_speed").intValue();
        }
        catch (NullPointerException e)
        {
            playerHasteSpeed = 80;
            logNotFound("server_config.player_haste_speed", "80");
        }
        try
        {
            playerSlowSpeed = parseResult.getLong("server_config.player_slow_speed").intValue();
        }
        catch (NullPointerException e)
        {
            playerSlowSpeed = 20;
            logNotFound("server_config.player_slow_speed", "20");
        }
        try
        {
            playerAttackProbability = parseResult.getLong("server_config.player_attack_probability").intValue();
        }
        catch (NullPointerException e)
        {
            playerAttackProbability = 90;
            logNotFound("server_config.player_attack_probability", "90");
        }
        try
        {
            playerEvasion = parseResult.getLong("server_config.player_evasion").intValue();
        }
        catch (NullPointerException e)
        {
            playerEvasion = 10;
            logNotFound("server_config.player_evasion", "10");
        }

        try
        {
            defenseDuration = parseResult.getLong("server_config.defense_duration").intValue();
        }
        catch (NullPointerException e)
        {
            defenseDuration = 1;
            logNotFound("server_config.defense_duration", "1");
        }

        try
        {
            fleeGoodProbability = parseResult.getLong("server_config.flee_good_probability").intValue();
        }
        catch (NullPointerException e)
        {
            fleeGoodProbability = 90;
            logNotFound("server_config.flee_good_probability", "90");
        }
        try
        {
            fleeBadProbability = parseResult.getLong("server_config.flee_bad_probability").intValue();
        }
        catch (NullPointerException e)
        {
            fleeBadProbability = 35;
            logNotFound("server_config.flee_bad_probability", "35");
        }

        try
        {
            minimumHitPercentage = parseResult.getLong("server_config.minimum_hit_percentage").intValue();
            if (minimumHitPercentage < 1)
            {
                minimumHitPercentage = 1;
            }
        }
        catch (NullPointerException e)
        {
            minimumHitPercentage = 4;
            logNotFound("server_config.minimum_hit_percentage", "4");
        }

        try
        {
            battleDecisionDurationNanos = parseResult.getLong("server_config.battle_turn_time_seconds") * 1000000000L;
            if(battleDecisionDurationNanos < BATTLE_DECISION_DURATION_NANO_MIN)
            {
                battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_MIN;
                logger.warn("Config \"server_config.battle_turn_time_seconds\" too low, defaulting to minimum \"5\"");
            }
            else if(battleDecisionDurationNanos > BATTLE_DECISION_DURATION_NANO_MAX)
            {
                battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_MAX;
                logger.warn("Config \"server_config.battle_turn_time_seconds\" too high, defaulting to maximum \"60\"");
            }
        }
        catch (NullPointerException e)
        {
            battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_DEFAULT;
            logNotFound("server_config.battle_turn_time_seconds", "15");
        }

        // entities
        TomlArray entityArray = parseResult.getArray("server_config.entity");
        if(entityArray != null)
        {
            for(int i = 0; i < entityArray.size(); ++i)
            {
                TomlTable entity = entityArray.getTable(i);
                EntityInfo eInfo = new EntityInfo();
                String name = null;
                if(entity.contains("name") && entity.contains("custom_name"))
                {
                    logger.error("Entity cannot have both \"name\" and \"custom_name\" entries");
                    continue;
                }
                else if(entity.contains("name"))
                {
                    try
                    {
                        eInfo.classType = Class.forName(entity.getString("name"));
                        name = eInfo.classType.getName();
                    } catch(ClassNotFoundException e)
                    {
                        logger.error("Entity with class name \"" + entity.getString("name") + "\" not found, skipping...");
                        continue;
                    }
                }
                else if(entity.contains("custom_name"))
                {
                    eInfo.customName = entity.getString("custom_name");
                    name = eInfo.customName;
                }
                else
                {
                    logger.error("Entity must have \"name\" or \"custom_name\" entry");
                    continue;
                }

                try
                {
                    eInfo.attackPower = entity.getLong("attack_power").intValue();
                    if(eInfo.attackPower < 0)
                    {
                        eInfo.attackPower = 0;
                        logEntityInvalidValue("attack_power", name, "0");
                    }
                }
                catch (NullPointerException e)
                {
                    logEntityMissingRequiredValue("attack_power", name);
                    continue;
                }

                try
                {
                    eInfo.attackProbability = entity.getLong("attack_probability").intValue();
                    if(eInfo.attackProbability < 0 || eInfo.attackProbability > 100)
                    {
                        eInfo.attackProbability = 35;
                        logEntityInvalidValue("attack_probability", name, "35");
                    }
                }
                catch (NullPointerException e)
                {
                    logEntityMissingRequiredValue("attack_probability", name);
                    continue;
                }

                try
                {
                    eInfo.attackEffect = EntityInfo.Effect.fromString(entity.getString("attack_effect"));
                    if(eInfo.attackEffect != EntityInfo.Effect.UNKNOWN)
                    {
                        eInfo.attackEffectProbability = entity.getLong("attack_effect_probability").intValue();
                        if(eInfo.attackEffectProbability < 0 || eInfo.attackEffectProbability > 100)
                        {
                            eInfo.attackEffectProbability = 35;
                            logEntityInvalidValue("attack_effect", name, "35");
                        }
                    }
                }
                catch (NullPointerException e)
                {
                    eInfo.attackEffect = EntityInfo.Effect.UNKNOWN;
                    logEntityMissingOptionalValue("attack_effect", name, "unknown");
                }

                try
                {
                    eInfo.attackVariance = entity.getLong("attack_variance").intValue();
                    if(eInfo.attackVariance < 0)
                    {
                        eInfo.attackVariance = 0;
                        logEntityInvalidValue("attack_variance", name, "0");
                    }
                }
                catch (NullPointerException e)
                {
                    eInfo.attackVariance = 0;
                    logEntityMissingOptionalValue("attack_variance", name, "0");
                }

                try
                {
                    eInfo.defenseDamage = entity.getLong("defense_damage").intValue();
                    if(eInfo.defenseDamage < 0)
                    {
                        eInfo.defenseDamage = 0;
                        logEntityInvalidValue("defense_damage", name, "0");
                    }
                    else
                    {
                        eInfo.defenseDamageProbability = entity.getLong("defense_damage_probability").intValue();
                        if(eInfo.defenseDamageProbability < 0 || eInfo.defenseDamageProbability > 100)
                        {
                            eInfo.defenseDamageProbability = 35;
                            logEntityInvalidValue("defense_damage_probability", name, "35");
                        }
                    }
                }
                catch (NullPointerException e)
                {
                    eInfo.defenseDamage = 0;
                    logEntityMissingOptionalValue("defense_damage", name, "0");
                }

                try
                {
                    eInfo.evasion = entity.getLong("evasion").intValue();
                    if(eInfo.evasion < 0 || eInfo.evasion > 100)
                    {
                        eInfo.evasion = 20;
                        logEntityInvalidValue("evasion", name, "20");
                    }
                }
                catch (NullPointerException e)
                {
                    logEntityMissingRequiredValue("evasion", name);
                    continue;
                }

                try
                {
                    eInfo.speed = entity.getLong("speed").intValue();
                }
                catch (NullPointerException e)
                {
                    logEntityMissingRequiredValue("speed", name);
                    continue;
                }

                try
                {
                    eInfo.ignoreBattle = entity.getBoolean("ignore_battle");
                }
                catch (NullPointerException e)
                {
                    eInfo.ignoreBattle = false;
                    logEntityMissingOptionalValue("ignore_battle", name, "false");
                }

                eInfo.category = entity.getString("category");
                if(eInfo.category == null)
                {
                    logEntityMissingRequiredValue("category", name);
                    continue;
                }

                try
                {
                    eInfo.decisionAttack = entity.getLong("decision_attack_probability").intValue();
                }
                catch (NullPointerException e)
                {
                    logEntityMissingRequiredValue("decision_attack_probability", name);
                    continue;
                }

                try
                {
                    eInfo.decisionDefend = entity.getLong("decision_defend_probability").intValue();
                }
                catch (NullPointerException e)
                {
                    logEntityMissingRequiredValue("decision_defend_probability", name);
                    continue;
                }

                try
                {
                    eInfo.decisionFlee = entity.getLong("decision_flee_probability").intValue();
                }
                catch (NullPointerException e)
                {
                    logEntityMissingRequiredValue("decision_flee_probability", name);
                    continue;
                }

                if(eInfo.classType != null)
                {
                    entityInfoMap.put(eInfo.classType.getName(), eInfo);
                }
                else if(!eInfo.customName.isEmpty())
                {
                    customEntityInfoMap.put(eInfo.customName, eInfo);
                }
                else
                {
                    logger.error("Cannot add entity to internal config, no \"name\" or \"custom_name\"");
                }
            }
        }
        return true;
    }

    private void logNotFound(String option)
    {
        logger.warn("Config option \"" + option + "\" not found, setting defaults");
    }

    private void logNotFound(String option, String defaultValue)
    {
        logger.warn("Config option \"" + option + "\" not found, defaulting to \"" + defaultValue + "\"");
    }

    private void logEntityInvalidValue(String option, String name, String defaultValue)
    {
        logger.warn("Invalid \"" + option + "\" for \"" + name + "\", defaulting to " + defaultValue);
    }

    private void logEntityMissingRequiredValue(String option, String name)
    {
        logger.error("Entity \"" + name + "\" does not have option \"" + option + "\", skipping...");
    }

    private void logEntityMissingOptionalValue(String option, String name, String defaultValue)
    {
        logger.info("Entity \"" + name + "\" does not have optional option \"" + option + "\", defaulting to \"" + defaultValue + "\"...");
    }

    private String getRegexEntityName(String name)
    {
        String regex = "^\\s*name\\s*=\\s*";
        regex += "(\"" + name + "\"";
        regex += "|'" + name + "'";
        regex += "|\"\"\"" + name + "\"\"\"";
        regex += "|'''" + name + "''')";
        return regex;
    }

    private String getRegexCustomEntityName(String name)
    {
        String regex = "^\\s*custom_name\\s*=\\s*";
        regex += "(\"" + name + "\"";
        regex += "|'" + name + "'";
        regex += "|\"\"\"" + name + "\"\"\"";
        regex += "|'''" + name + "''')";
        return regex;
    }

    private boolean addEntityEntry(EntityInfo eInfo)
    {
        if(eInfo.classType == null && eInfo.customName.isEmpty())
        {
            logger.error("addEntityEntry: Got invalid eInfo, no name of any type");
            return false;
        }
        try
        {
            File config = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
            FileWriter fw = new FileWriter(config, true);
            fw.write("[[server_config.entity]]\n");
            if(eInfo.classType != null)
            {
                fw.write("name = \"" + eInfo.classType.getName() + "\"\n");
            }
            else
            {
                fw.write("custom_name = \"" + eInfo.customName + "\"\n");
            }
            fw.write("attack_power = " + eInfo.attackPower + "\n");
            fw.write("attack_probability = " + eInfo.attackProbability + "\n");
            if(eInfo.attackVariance > 0)
            {
                fw.write("attack_variance = " + eInfo.attackVariance + "\n");
            }
            if(eInfo.attackEffect != EntityInfo.Effect.UNKNOWN && eInfo.attackEffectProbability > 0)
            {
                fw.write("attack_effect = \"" + eInfo.attackEffect.toString() + "\"\n");
                fw.write("attack_effect_probability = " + eInfo.attackEffectProbability + "\n");
            }
            if(eInfo.defenseDamage > 0 && eInfo.defenseDamageProbability > 0)
            {
                fw.write("defense_damage = " + eInfo.defenseDamage + "\n");
                fw.write("defense_damage_probability = " + eInfo.defenseDamageProbability + "\n");
            }
            fw.write("evasion = " + eInfo.evasion + "\n");
            fw.write("speed = " + eInfo.speed + "\n");
            if(eInfo.ignoreBattle)
            {
                fw.write("ignore_battle = true\n");
            }
            fw.write("category = \"" + eInfo.category + "\"\n");
            fw.write("decision_attack_probability = " + eInfo.decisionAttack + "\n");
            fw.write("decision_defend_probability = " + eInfo.decisionDefend + "\n");
            fw.write("decision_flee_probability = " + eInfo.decisionFlee + "\n");
            fw.close();

			if(eInfo.classType != null)
			{
				entityInfoMap.put(eInfo.classType.getName(), eInfo);
			}
			else
			{
				customEntityInfoMap.put(eInfo.customName, eInfo);
			}
        }
        catch (Throwable t)
        {
			if(eInfo.classType != null)
			{
				logger.error("Failed to add entity entry (name = \"" + eInfo.classType.getName() + "\")");
			}
			else
			{
				logger.error("Failed to add custom entity entry (custom_name = \"" + eInfo.customName + "\")");
			}
            return false;
        }
        return true;
    }

    protected boolean editEntityEntry(EntityInfo eInfo)
    {
        try
        {
            String cached = new String();
            char buf[] = new char[1024];
            int read = 0;
            File config = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
            {
                FileReader fr = new FileReader(config);
                read = fr.read(buf);
                while (read != -1)
                {
                    cached += String.valueOf(buf, 0, read);
                    read = fr.read(buf);
                }
                fr.close();
            }

            int nameIndex = -1;
            if(eInfo.classType != null)
            {
                Pattern p = Pattern.compile(getRegexEntityName(eInfo.classType.getName()), Pattern.MULTILINE);
                Matcher m = p.matcher(cached);
                if(m.find())
                {
                    nameIndex = m.start();
                }
            }
            else if(!eInfo.customName.isEmpty())
            {
                Pattern p = Pattern.compile(getRegexCustomEntityName(eInfo.customName), Pattern.MULTILINE);
                Matcher m = p.matcher(cached);
                if(m.find())
                {
                    nameIndex = m.start();
                }
            }
            else
            {
                logger.error("EntityInfo does not have classType or customName, cannot edit/add");
                return false;
            }
            int entryIndex = -1;
            int nextIndex = -1;
            if(nameIndex != -1)
            {
                {
                    Pattern p = Pattern.compile("^\\s*\\[\\[\\s*server_config\\s*\\.\\s*entity\\s*]]", Pattern.MULTILINE);
                    Matcher m = p.matcher(cached.substring(0, nameIndex));
                    while(m.find())
                    {
                        entryIndex = m.start();
                    }
                    if(entryIndex == -1)
                    {
                        logger.warn("editEntityEntry: could not find header for entry \"" + eInfo.classType.getName() + "\", skipping to adding it...");
                        return addEntityEntry(eInfo);
                    }
                }
                {
                    Pattern p = Pattern.compile("^\\s*\\[", Pattern.MULTILINE);
                    Matcher m = p.matcher(cached.substring(nameIndex));
                    if(m.find())
                    {
                        nextIndex = m.start() + nameIndex;
                    }
                }
            }
            else
            {
                if(eInfo.classType != null)
                {
                    logger.warn("editEntityEntry: could not find entry for \"" + eInfo.classType.getName() + "\", skipping to adding it...");
                }
                else if(!eInfo.customName.isEmpty())
                {
                    logger.warn("editEntityEntry: could not find entry for \"" + eInfo.customName + "\", skipping to adding it...");
                }
                return addEntityEntry(eInfo);
            }

            String cut = null;
            if(nextIndex != -1)
            {
                cut = cached.substring(0, entryIndex) + cached.substring(nextIndex);
            }
            else
            {
                cut = cached.substring(0, entryIndex);
            }

            {
                FileWriter fw = new FileWriter(config);
                fw.write(cut);
                fw.close();
            }

            return addEntityEntry(eInfo);
        }
        catch (Throwable t)
        {
            return false;
        }
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
        EntityInfo eInfo = entityInfoMap.get(classFullName);
        if(eInfo != null)
        {
            eInfo = eInfo.clone();
        }
        return eInfo;
    }

    protected EntityInfo getEntityInfoReference(String classFullName)
    {
        return entityInfoMap.get(classFullName);
    }

    protected EntityInfo getMatchingEntityInfo(Object entity)
    {
        if(entity == null)
        {
            return null;
        }
        EntityInfo matching = entityInfoMap.get(entity.getClass().getName());
        if(matching != null && matching.classType.isInstance(entity))
        {
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
        EntityInfo eInfo = customEntityInfoMap.get(customName);
        if(eInfo != null)
        {
            eInfo = eInfo.clone();
        }
        return eInfo;
    }

    protected EntityInfo getCustomEntityInfoReference(String customName)
    {
        return customEntityInfoMap.get(customName);
    }

    private int getConfigFileVersion(InputStream io)
    {
        int version = 0;
        try
        {
            TomlParseResult result = Toml.parse(io, TomlVersion.V0_5_0);
            version = result.getLong("version").intValue();
        }
        catch (Throwable t)
        {
            // ignored
        }
        return version;
    }

    private int getConfigFileVersion(File configFile)
    {
        int version = 0;
        try
        {
            TomlParseResult result = Toml.parse(configFile.toPath(), TomlVersion.V0_5_0);
            version = result.getLong("version").intValue();
        }
        catch (Throwable t)
        {
            // ignored
        }
        return version;
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

    public int getLeaveBattleCooldownSeconds()
    {
        return leaveBattleCooldownSeconds;
    }

    public long getLeaveBattleCooldownNanos()
    {
        return (long)leaveBattleCooldownSeconds * 1000000000L;
    }

    public int getAggroStartBattleDistance()
    {
        return aggroStartBattleDistance;
    }
}
