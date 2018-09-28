package com.seodisparate.TurnBasedMinecraft.common;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleDecision;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleInfo;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleMessage;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleRequestInfo;

import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = TurnBasedMinecraftMod.MODID, name = TurnBasedMinecraftMod.NAME, version = TurnBasedMinecraftMod.VERSION)
public class TurnBasedMinecraftMod
{
    public static final String MODID = "com.seodisparate.turnbasedminecraft";
    public static final String NAME = "Turn Based Minecraft Mod";
    public static final String VERSION = "0.3";
    public static final String CONFIG_FILENAME = "TBM_Config.xml";
    public static final String CONFIG_DIRECTORY = "config/TurnBasedMinecraft/";
    public static final String CONFIG_FILE_PATH = CONFIG_DIRECTORY + CONFIG_FILENAME;
    public static final String CONFIG_INTERNAL_PATH = "/assets/TurnBasedMinecraft/" + CONFIG_FILENAME;
    public static final String MUSIC_ROOT = CONFIG_DIRECTORY + "Music/";
    public static final String MUSIC_SILLY = MUSIC_ROOT + "silly/";
    public static final String MUSIC_BATTLE = MUSIC_ROOT + "battle/";
    
    private static int CONFIG_FILE_VERSION = 0;
    
    public static final SimpleNetworkWrapper NWINSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("seodisparate.tbmc");

    protected static Logger logger;
    protected static BattleManager battleManager;
    private static int packetHandlerID = 0;
    protected static Entity attackingEntity;
    protected static int attackingDamage = 0;
    protected static Set<AttackerViaBow> attackerViaBow;
    protected static Config config;
    public static final long BATTLE_DECISION_DURATION_NANO_MIN = 5000000000L;
    public static final long BATTLE_DECISION_DURATION_NANO_MAX = 60000000000L;
    public static final long BATTLE_DECISION_DURATION_NANO_DEFAULT = 15000000000L;
    private static long BATTLE_DECISION_DURATION_NANOSECONDS = BATTLE_DECISION_DURATION_NANO_DEFAULT;
    
    @SidedProxy(modId=MODID, serverSide="com.seodisparate.TurnBasedMinecraft.common.CommonProxy", clientSide="com.seodisparate.TurnBasedMinecraft.client.ClientProxy")
    public static CommonProxy commonProxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        commonProxy.initialize();
        battleManager = null;
        attackerViaBow = new HashSet<AttackerViaBow>();
        commonProxy.setLogger(logger);
        
        // register packets
        NWINSTANCE.registerMessage(
            PacketBattleInfo.HandlerBattleInfo.class,
            PacketBattleInfo.class,
            packetHandlerID++,
            Side.CLIENT);
        NWINSTANCE.registerMessage(
            PacketBattleRequestInfo.HandlerBattleRequestInfo.class,
            PacketBattleRequestInfo.class,
            packetHandlerID++,
            Side.SERVER);
        NWINSTANCE.registerMessage(
            PacketBattleDecision.HandleBattleDecision.class,
            PacketBattleDecision.class,
            packetHandlerID++,
            Side.SERVER);
        NWINSTANCE.registerMessage(
            PacketBattleMessage.HandlerBattleMessage.class,
            PacketBattleMessage.class,
            packetHandlerID++,
            Side.CLIENT);
        
        // register event handler(s)
        MinecraftForge.EVENT_BUS.register(new AttackEventHandler());
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        config = new Config(logger);
        commonProxy.setConfig(config);
        commonProxy.postInit();
    }
    
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        logger.debug("About to initialize BattleManager");
        if(commonProxy.initializeBattleManager())
        {
            logger.debug("Initialized BattleManager");
        }
    }
    
    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event)
    {
        logger.debug("About to cleanup BattleManager");
        if(commonProxy.cleanupBattleManager())
        {
            logger.debug("Cleaned up BattleManager");
        }
    }
    
    public static BattleManager getBattleManager()
    {
        return battleManager;
    }
    
    public static void setConfigVersion(int version)
    {
        CONFIG_FILE_VERSION = version;
    }
    
    public static int getConfigVersion()
    {
        return CONFIG_FILE_VERSION;
    }
    
    public static long getBattleDurationNanos()
    {
        return BATTLE_DECISION_DURATION_NANOSECONDS;
    }
    
    public static int getBattleDurationSeconds()
    {
        return (int)(BATTLE_DECISION_DURATION_NANOSECONDS / 1000000000L);
    }
    
    protected static void setBattleDurationSeconds(long seconds)
    {
        BATTLE_DECISION_DURATION_NANOSECONDS = seconds * 1000000000L;
        if(BATTLE_DECISION_DURATION_NANOSECONDS < BATTLE_DECISION_DURATION_NANO_MIN)
        {
            BATTLE_DECISION_DURATION_NANOSECONDS = BATTLE_DECISION_DURATION_NANO_MIN;
        }
        else if(BATTLE_DECISION_DURATION_NANOSECONDS > BATTLE_DECISION_DURATION_NANO_MAX)
        {
            BATTLE_DECISION_DURATION_NANOSECONDS = BATTLE_DECISION_DURATION_NANO_MAX;
        }
    }
    
    public static Config getConfig()
    {
        return config;
    }
}
