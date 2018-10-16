package com.seodisparate.TurnBasedMinecraft.common;

import org.apache.logging.log4j.Logger;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleDecision;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleInfo;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleMessage;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleRequestInfo;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketGeneralMessage;

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
    public static final String VERSION = "1.1";
    public static final String CONFIG_FILENAME = "TBM_Config.xml";
    public static final String CONFIG_DIRECTORY = "config/TurnBasedMinecraft/";
    public static final String CONFIG_FILE_PATH = CONFIG_DIRECTORY + CONFIG_FILENAME;
    public static final String CONFIG_INTERNAL_PATH = "/assets/TurnBasedMinecraft/" + CONFIG_FILENAME;
    public static final String MUSIC_ROOT = CONFIG_DIRECTORY + "Music/";
    public static final String MUSIC_SILLY = MUSIC_ROOT + "silly/";
    public static final String MUSIC_BATTLE = MUSIC_ROOT + "battle/";
    
    public static final SimpleNetworkWrapper NWINSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("seodisparate.tbmc");
    protected static Logger logger;
    private static int packetHandlerID = 0;
    
    @SidedProxy(modId=MODID, serverSide="com.seodisparate.TurnBasedMinecraft.common.CommonProxy", clientSide="com.seodisparate.TurnBasedMinecraft.client.ClientProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        proxy.setLogger(logger);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        proxy.initialize();
        
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
        NWINSTANCE.registerMessage(
            PacketGeneralMessage.HandlerGeneralMessage.class,
            PacketGeneralMessage.class,
            packetHandlerID++,
            Side.CLIENT);
        
        // register event handler(s)
        MinecraftForge.EVENT_BUS.register(new AttackEventHandler());
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        proxy.postInit();
    }
    
    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        logger.debug("About to initialize BattleManager");
        if(proxy.initializeBattleManager())
        {
            logger.debug("Initialized BattleManager");
        }
        
        proxy.getConfig().clearBattleIgnoringPlayers();
        
        // register commands
        event.registerServerCommand(new CommandTBMDisable(proxy.getConfig()));
        event.registerServerCommand(new CommandTBMEnable(proxy.getConfig()));
        event.registerServerCommand(new CommandTBMSet(proxy.getConfig()));
    }
    
    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event)
    {
        logger.debug("About to cleanup BattleManager");
        if(proxy.cleanupBattleManager())
        {
            logger.debug("Cleaned up BattleManager");
        }
    }
}
