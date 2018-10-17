package com.seodisparate.TurnBasedMinecraft.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.logging.log4j.Logger;

public class Config
{
    public static final long BATTLE_DECISION_DURATION_NANO_MIN = 5000000000L;
    public static final long BATTLE_DECISION_DURATION_NANO_MAX = 60000000000L;
    public static final long BATTLE_DECISION_DURATION_NANO_DEFAULT = 15000000000L;
    private long battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_DEFAULT;
    private Map<String, EntityInfo> entityInfoMap;
    private Set<String> ignoreBattleTypes;
    private Logger logger;
    private int playerSpeed = 50;
    private int playerHasteSpeed = 80;
    private int playerSlowSpeed = 20;
    private int playerAttackProbability = 100;
    private int playerEvasion = 10;
    private int defenseDuration = 1;
    private int fleeGoodProbability = 90;
    private int fleeBadProbability = 40;
    private int minimumHitPercentage = 1;
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
    
    public Config(Logger logger)
    {
        entityInfoMap = new HashMap<String, EntityInfo>();
        ignoreBattleTypes = new HashSet<String>();
        this.logger = logger;
        musicBattleTypes = new HashSet<String>();
        musicSillyTypes = new HashSet<String>();
        battleIgnoringPlayers = new HashSet<Integer>();
        
        int internalVersion = 0;
        try
        {
            InputStream is = getClass().getResourceAsStream(TurnBasedMinecraftMod.CONFIG_INTERNAL_PATH);
            if(is == null)
            {
                logger.error("Internal resource is null");
            }
            else
            {
                internalVersion = getConfigFileVersion(is);
            }
        } catch (Throwable t) {}
        
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
        
        // parse xml
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
                    + ".xml"));
        }
    }
    
    private boolean parseConfig(File configFile) throws XMLStreamException, FactoryConfigurationError, IOException
    {
        FileInputStream fis = new FileInputStream(configFile);
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(fis);
        while(xmlReader.hasNext())
        {
            xmlReader.next();
            if(xmlReader.isStartElement())
            {
                if(xmlReader.getLocalName().equals("TurnBasedMinecraftConfig"))
                {
                    continue;
                }
                else if(xmlReader.getLocalName().equals("Version"))
                {
                    continue;
                }
                else if(xmlReader.getLocalName().equals("OldBattleBehavior"))
                {
                    if(xmlReader.getElementText().toLowerCase().equals("false"))
                    {
                        oldBattleBehaviorEnabled = false;
                    }
                    else
                    {
                        oldBattleBehaviorEnabled = true;
                    }
                }
                else if(xmlReader.getLocalName().equals("WhoCanDisableTurnBasedForSelf"))
                {
                    if(xmlReader.getElementText().toLowerCase().equals("any"))
                    {
                        onlyOPsSelfDisableTB = false;
                    }
                    else
                    {
                        onlyOPsSelfDisableTB = true;
                    }
                }
                else if(xmlReader.getLocalName().equals("MaxInBattle"))
                {
                    maxInBattle = Integer.parseInt(xmlReader.getElementText());
                }
                else if(xmlReader.getLocalName().equals("FreezeBattleCombatants"))
                {
                    freezeCombatantsInBattle = !xmlReader.getElementText().toLowerCase().equals("false");
                }
                else if(xmlReader.getLocalName().equals("IgnoreBattleTypes"))
                {
                    do
                    {
                        xmlReader.next();
                        if(xmlReader.isStartElement())
                        {
                            ignoreBattleTypes.add(xmlReader.getLocalName().toLowerCase());
                        }
                    } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals("IgnoreBattleTypes")));
                }
                else if(xmlReader.getLocalName().equals("BattleMusic"))
                {
                    do
                    {
                        xmlReader.next();
                        if(xmlReader.isStartElement())
                        {
                            if(xmlReader.getLocalName().equals("Normal"))
                            {
                                do
                                {
                                    xmlReader.next();
                                    if(xmlReader.isStartElement())
                                    {
                                        musicBattleTypes.add(xmlReader.getLocalName().toLowerCase());
                                    }
                                } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals("Normal")));
                            }
                            else if(xmlReader.getLocalName().equals("Silly"))
                            {
                                do
                                {
                                    xmlReader.next();
                                    if(xmlReader.isStartElement())
                                    {
                                        musicSillyTypes.add(xmlReader.getLocalName().toLowerCase());
                                    }
                                } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals("Silly")));
                            }
                        }
                    } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals("BattleMusic")));
                }
                else if(xmlReader.getLocalName().equals("PlayerStats"))
                {
                    do
                    {
                        xmlReader.next();
                        if(xmlReader.isStartElement())
                        {
                            if(xmlReader.getLocalName().equals("Speed"))
                            {
                                playerSpeed = Integer.parseInt(xmlReader.getElementText());
                            }
                            else if(xmlReader.getLocalName().equals("HasteSpeed"))
                            {
                                playerHasteSpeed = Integer.parseInt(xmlReader.getElementText());
                            }
                            else if(xmlReader.getLocalName().equals("SlowSpeed"))
                            {
                                playerSlowSpeed = Integer.parseInt(xmlReader.getElementText());
                            }
                            else if(xmlReader.getLocalName().equals("AttackProbability"))
                            {
                                playerAttackProbability = Integer.parseInt(xmlReader.getElementText());
                            }
                            else if(xmlReader.getLocalName().equals("Evasion"))
                            {
                                playerEvasion = Integer.parseInt(xmlReader.getElementText());
                            }
                        }
                    } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals("PlayerStats")));
                }
                else if(xmlReader.getLocalName().equals("DefenseDuration"))
                {
                    defenseDuration = Integer.parseInt(xmlReader.getElementText());
                }
                else if(xmlReader.getLocalName().equals("FleeGoodProbability"))
                {
                    fleeGoodProbability = Integer.parseInt(xmlReader.getElementText());
                }
                else if(xmlReader.getLocalName().equals("FleeBadProbability"))
                {
                    fleeBadProbability = Integer.parseInt(xmlReader.getElementText());
                }
                else if(xmlReader.getLocalName().equals("MinimumHitPercentage"))
                {
                    minimumHitPercentage = Integer.parseInt(xmlReader.getElementText());
                    if(minimumHitPercentage < 1)
                    {
                        minimumHitPercentage = 1;
                    }
                }
                else if(xmlReader.getLocalName().equals("BattleTurnTimeSeconds"))
                {
                    try
                    {
                        int seconds = Integer.parseInt(xmlReader.getElementText());
                        battleDecisionDurationNanos = (long)(seconds) * 1000000000L;
                        if(battleDecisionDurationNanos < BATTLE_DECISION_DURATION_NANO_MIN)
                        {
                            battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_MIN;
                        }
                        else if(battleDecisionDurationNanos > BATTLE_DECISION_DURATION_NANO_MAX)
                        {
                            battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_MAX;
                        }
                    } catch (Throwable t)
                    {
                        logger.warn("Unable to get value for \"BattleTurnTimeSeconds\" from config, using default");
                        battleDecisionDurationNanos = BATTLE_DECISION_DURATION_NANO_DEFAULT;
                    }
                }
                else if(xmlReader.getLocalName().equals("SillyMusicThreshold"))
                {
                    sillyMusicThreshold = Integer.parseInt(xmlReader.getElementText());
                    if(sillyMusicThreshold < 0)
                    {
                        sillyMusicThreshold = 0;
                    }
                    else if(sillyMusicThreshold > 100)
                    {
                        sillyMusicThreshold = 100;
                    }
                }
                else if(xmlReader.getLocalName().equals("EntityEntry"))
                {
                    EntityInfo eInfo = new EntityInfo();
                    do
                    {
                        xmlReader.next();
                        if(xmlReader.isStartElement())
                        {
                            if(xmlReader.getLocalName().equals("Name"))
                            {
                                try
                                {
                                    eInfo.classType = Class.forName(xmlReader.getElementText());
                                } catch (ClassNotFoundException e)
                                {
                                    logger.error("Failed to get class of name " + xmlReader.getElementText());
                                }
                            }
                            else if(xmlReader.getLocalName().equals("AttackPower"))
                            {
                                for(int i = 0; i < xmlReader.getAttributeCount(); ++i)
                                {
                                    if(xmlReader.getAttributeLocalName(i).equals("Probability"))
                                    {
                                        eInfo.attackProbability = Integer.parseInt(xmlReader.getAttributeValue(i));
                                    }
                                    else if(xmlReader.getAttributeLocalName(i).equals("Variance"))
                                    {
                                        eInfo.attackVariance = Integer.parseInt(xmlReader.getAttributeValue(i));
                                    }
                                }
                                eInfo.attackPower = Integer.parseInt(xmlReader.getElementText());
                            }
                            else if(xmlReader.getLocalName().equals("AttackEffect"))
                            {
                                for(int i = 0; i < xmlReader.getAttributeCount(); ++i)
                                {
                                    if(xmlReader.getAttributeLocalName(i).equals("Probability"))
                                    {
                                        eInfo.attackEffectProbability = Integer.parseInt(xmlReader.getAttributeValue(i));
                                    }
                                }
                                eInfo.attackEffect = EntityInfo.Effect.fromString(xmlReader.getElementText());
                            }
                            else if(xmlReader.getLocalName().equals("Evasion"))
                            {
                                eInfo.evasion = Integer.parseInt(xmlReader.getElementText());
                            }
                            else if(xmlReader.getLocalName().equals("DefenseDamage"))
                            {
                                for(int i = 0; i < xmlReader.getAttributeCount(); ++i)
                                {
                                    if(xmlReader.getAttributeLocalName(i).equals("Probability"))
                                    {
                                        eInfo.defenseDamageProbability = Integer.parseInt(xmlReader.getAttributeValue(i));
                                    }
                                }
                                eInfo.defenseDamage = Integer.parseInt(xmlReader.getElementText());
                            }
                            else if(xmlReader.getLocalName().equals("Category"))
                            {
                                eInfo.category = xmlReader.getElementText().toLowerCase();
                            }
                            else if(xmlReader.getLocalName().equals("IgnoreBattle"))
                            {
                                if(xmlReader.getElementText().toLowerCase().equals("true"))
                                {
                                    eInfo.ignoreBattle = true;
                                }
                            }
                            else if(xmlReader.getLocalName().equals("Speed"))
                            {
                                eInfo.speed = Integer.parseInt(xmlReader.getElementText());
                            }
                            else if(xmlReader.getLocalName().equals("Decision"))
                            {
                                do
                                {
                                    xmlReader.next();
                                    if(xmlReader.isStartElement())
                                    {
                                        if(xmlReader.getLocalName().equals("Attack"))
                                        {
                                            eInfo.decisionAttack = Integer.parseInt(xmlReader.getElementText());
                                        }
                                        else if(xmlReader.getLocalName().equals("Defend"))
                                        {
                                            eInfo.decisionDefend = Integer.parseInt(xmlReader.getElementText());
                                        }
                                        else if(xmlReader.getLocalName().equals("Flee"))
                                        {
                                            eInfo.decisionFlee = Integer.parseInt(xmlReader.getElementText());
                                        }
                                    }
                                } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals("Decision")));
                            }
                        }
                    } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals("EntityEntry")));
                    if(eInfo.classType != null)
                    {
                        entityInfoMap.put(eInfo.classType.getName(), eInfo);
                    }
                }
            }
        }
        xmlReader.close();
        fis.close();
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
        return entityInfoMap.get(classFullName).clone();
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
    
    private int getConfigFileVersion(File configFile)
    {
        try
        {
            return getConfigFileVersion(new FileInputStream(configFile));
        } catch(FileNotFoundException e)
        {
            return 0;
        }
    }
    
    private int getConfigFileVersion(InputStream configStream)
    {
        int configVersion = 1;

        try
        {
            XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(configStream);
            while(xmlReader.hasNext())
            {
                xmlReader.next();
                if(xmlReader.isStartElement() && xmlReader.getLocalName().equals("Version"))
                {
                    configVersion = Integer.parseInt(xmlReader.getElementText());
                    break;
                }
            }
            xmlReader.close();
        } catch (Throwable t)
        {
            return 0;
        }
        
        return configVersion;
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
}
