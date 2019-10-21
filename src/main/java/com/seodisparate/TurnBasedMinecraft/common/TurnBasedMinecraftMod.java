package com.seodisparate.TurnBasedMinecraft.common;

import com.seodisparate.TurnBasedMinecraft.client.ClientProxy;
import com.seodisparate.TurnBasedMinecraft.common.networking.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(value = TurnBasedMinecraftMod.MODID)
public class TurnBasedMinecraftMod
{
    public static final String MODID = "com.seodisparate.turnbasedminecraft";
    public static final String NAME = "Turn Based Minecraft Mod";
    public static final String VERSION = "1.8";
    public static final String CONFIG_FILENAME = "TBM_Config.toml";
    public static final String DEFAULT_CONFIG_FILENAME = "TBM_Config_DEFAULT.toml";
    public static final String CONFIG_DIRECTORY = "config/TurnBasedMinecraft/";
    public static final String CONFIG_FILE_PATH = CONFIG_DIRECTORY + CONFIG_FILENAME;
    public static final String DEFAULT_CONFIG_FILE_PATH = CONFIG_DIRECTORY + DEFAULT_CONFIG_FILENAME;
    public static final String CONFIG_INTERNAL_PATH = "/assets/TurnBasedMinecraft/" + CONFIG_FILENAME;
    public static final String MUSIC_ROOT = CONFIG_DIRECTORY + "Music/";
    public static final String MUSIC_SILLY = MUSIC_ROOT + "silly/";
    public static final String MUSIC_BATTLE = MUSIC_ROOT + "battle/";
    
    private static final String PROTOCOL_VERSION = Integer.toString(1);
    private static final ResourceLocation HANDLER_ID = new ResourceLocation(MODID, "main_channel");
    private static final SimpleChannel HANDLER = NetworkRegistry.ChannelBuilder
    		.named(HANDLER_ID)
    		.clientAcceptedVersions(PROTOCOL_VERSION::equals)
    		.serverAcceptedVersions(PROTOCOL_VERSION::equals)
    		.networkProtocolVersion(() -> PROTOCOL_VERSION)
    		.simpleChannel();
    protected static Logger logger = LogManager.getLogger();
    
    public static ResourceLocation getNetResourceLocation() {
    	return HANDLER_ID;
    }
    
    public static SimpleChannel getHandler() {
    	return HANDLER;
    }
    
    public static CommonProxy proxy;

    @SubscribeEvent
    public void firstInit(FMLCommonSetupEvent event)
    {
    	proxy = DistExecutor.runForDist(()->()->new ClientProxy(), ()->()->new CommonProxy());
        proxy.initialize();
        proxy.setLogger(logger);
        
        // register packets
        int packetHandlerID = 0;
        HANDLER.registerMessage(
        	packetHandlerID++,
        	PacketBattleInfo.class,
        	PacketBattleInfo::encode,
        	PacketBattleInfo::decode,
        	PacketBattleInfo.Handler::handle);
        HANDLER.registerMessage(
    		packetHandlerID++,
    		PacketBattleRequestInfo.class,
    		PacketBattleRequestInfo::encode,
    		PacketBattleRequestInfo::decode,
    		PacketBattleRequestInfo.Handler::handle);
        HANDLER.registerMessage(
    		packetHandlerID++,
    		PacketBattleDecision.class,
    		PacketBattleDecision::encode,
    		PacketBattleDecision::decode,
    		PacketBattleDecision.Handler::handle);
        HANDLER.registerMessage(
    		packetHandlerID++,
    		PacketBattleMessage.class,
    		PacketBattleMessage::encode,
    		PacketBattleMessage::decode,
    		PacketBattleMessage.Handler::handle);
        HANDLER.registerMessage(
    		packetHandlerID++,
    		PacketGeneralMessage.class,
    		PacketGeneralMessage::encode,
    		PacketGeneralMessage::decode,
    		PacketGeneralMessage.Handler::handle);
        HANDLER.registerMessage(
    		packetHandlerID++,
    		PacketEditingMessage.class,
    		PacketEditingMessage::encode,
    		PacketEditingMessage::decode,
    		PacketEditingMessage.Handler::handle);
        
        // register event handler(s)
        MinecraftForge.EVENT_BUS.register(new AttackEventHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerJoinEventHandler());
    }

    @SubscribeEvent
    public void secondInitClient(FMLClientSetupEvent event)
    {
    	proxy.postInit();
    }
    
    @SubscribeEvent
    public void secondInitServer(FMLDedicatedServerSetupEvent event)
    {
    	proxy.postInit();
    }
    
    @SubscribeEvent
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
        event.registerServerCommand(new CommandTBMDisableAll(proxy.getConfig()));
        event.registerServerCommand(new CommandTBMEnableAll(proxy.getConfig()));
        event.registerServerCommand(new CommandTBMEdit(proxy.getConfig()));
    }
    
    @SubscribeEvent
    public void serverStopping(FMLServerStoppingEvent event)
    {
        logger.debug("About to cleanup BattleManager");
        if(proxy.cleanupBattleManager())
        {
            logger.debug("Cleaned up BattleManager");
        }
    }
}
