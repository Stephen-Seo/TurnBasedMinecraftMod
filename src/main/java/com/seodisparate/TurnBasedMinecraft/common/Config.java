package com.seodisparate.TurnBasedMinecraft.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.logging.log4j.Logger;

import com.seodisparate.TurnBasedMinecraft.TurnBasedMinecraftMod;
import com.seodisparate.TurnBasedMinecraft.common.EntityInfo.Category;

public class Config
{
    private Map<Class, EntityInfo> entityInfoMap;
    private Set<EntityInfo.Category> ignoreBattleTypes;
    private Logger logger;
    
    private enum ConfigParseResult
    {
        IS_OLD,
        SUCCESS
    }
    
    public Config(Logger logger)
    {
        entityInfoMap = new HashMap<Class, EntityInfo>();
        ignoreBattleTypes = new HashSet<EntityInfo.Category>();
        this.logger = logger;
        
        try
        {
            File testLoad = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
            if(!testLoad.exists())
            {
                writeConfig();
            }
        }
        catch (Exception e)
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
        try
        {
            ConfigParseResult result = parseConfig(configFile);
            if(result == ConfigParseResult.IS_OLD)
            {
                moveOldConfig();
                writeConfig();
                ConfigParseResult resultSecond = parseConfig(configFile);
                if(resultSecond != ConfigParseResult.SUCCESS)
                {
                    logger.error("Failed to parse config file " + TurnBasedMinecraftMod.CONFIG_FILE_PATH);
                }
            }
            else if(result != ConfigParseResult.SUCCESS)
            {
                logger.error("Failed to parse config file " + TurnBasedMinecraftMod.CONFIG_FILE_PATH);
            }
        } catch (Exception e)
        {
            logger.error("Failed to parse config file " + TurnBasedMinecraftMod.CONFIG_FILE_PATH);
        }
    }
    
    private void writeConfig() throws IOException
    {
        File configFile = new File(TurnBasedMinecraftMod.CONFIG_FILE_PATH);
        File dirs = configFile.getParentFile();
        dirs.mkdirs();
        InputStream configStream = this.getClass().getResourceAsStream(TurnBasedMinecraftMod.CONFIG_FILENAME);
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
            configFile.renameTo(new File(TurnBasedMinecraftMod.CONFIG_DIRECTORY + "_"
                    + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(Instant.now())));
        }
    }
    
    private ConfigParseResult parseConfig(File configFile) throws XMLStreamException, FactoryConfigurationError, IOException
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
                    if(Integer.parseInt(xmlReader.getElementText()) < TurnBasedMinecraftMod.CONFIG_FILE_VERSION)
                    {
                        logger.info("Config file is older version, moving it and writing a new one in its place");
                        xmlReader.close();
                        fis.close();
                        return ConfigParseResult.IS_OLD;
                    }
                    continue;
                }
                else if(xmlReader.getLocalName().equals("IgnoreBattleTypes"))
                {
                    do
                    {
                        xmlReader.next();
                        if(xmlReader.isStartElement())
                        {
                            ignoreBattleTypes.add(Category.fromString(xmlReader.getLocalName()));
                        }
                    } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals("IgnoreBattleTypes")));
                }
                else if(xmlReader.getLocalName().equals("EntityStats"))
                {
                    do
                    {
                        xmlReader.next();
                        if(xmlReader.isStartElement())
                        {
                            String classType = xmlReader.getLocalName();
                            EntityInfo eInfo = new EntityInfo();
                            try
                            {
                                eInfo.classType = Class.forName(classType);
                            } catch (ClassNotFoundException e)
                            {
                                logger.error("Failed to get class of name " + classType);
                                continue;
                            }
                            do
                            {
                                xmlReader.next();
                                if(xmlReader.isStartElement())
                                {
                                    if(xmlReader.getLocalName().equals("AttackPower"))
                                    {
                                        for(int i = 0; i < xmlReader.getAttributeCount(); ++i)
                                        {
                                            if(xmlReader.getAttributeLocalName(i).equals("Probability"))
                                            {
                                                eInfo.attackProbability = Integer.parseInt(xmlReader.getAttributeValue(i));
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
                                        eInfo.category = Category.fromString(xmlReader.getElementText());
                                    }
                                    else if(xmlReader.getLocalName().equals("Conflicts"))
                                    {
                                        do
                                        {
                                            xmlReader.next();
                                            if(xmlReader.isStartElement())
                                            {
                                                try
                                                {
                                                    Class conflictingType = Class.forName(xmlReader.getLocalName());
                                                    eInfo.conflictingTypes.add(conflictingType);
                                                } catch(ClassNotFoundException e)
                                                {
                                                    logger.warn("Invalid conflicting type for entity " + eInfo.classType.getName());
                                                }
                                            }
                                        } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals("Conflicts")));
                                    }
                                }
                            } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals(classType)));
                            entityInfoMap.put(eInfo.classType, eInfo);
                        }
                    } while(!(xmlReader.isEndElement() && xmlReader.getLocalName().equals("EntityStats")));
                }
            }
        }
        xmlReader.close();
        fis.close();
        return ConfigParseResult.SUCCESS;
    }
}
