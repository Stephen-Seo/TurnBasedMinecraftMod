package com.seodisparate.TurnBasedMinecraft.common;

import java.time.Duration;

import org.apache.logging.log4j.Logger;

import com.seodisparate.TurnBasedMinecraft.client.BattleGui;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleDecision;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleInfo;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleMessage;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleRequestInfo;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketHandler;

import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = TurnBasedMinecraftMod.MODID, name = TurnBasedMinecraftMod.NAME, version = TurnBasedMinecraftMod.VERSION)
public class TurnBasedMinecraftMod
{
    public static final String MODID = "com.seodisparate.turnbasedminecraft";
    public static final String NAME = "Turn Based Minecraft Mod";
    public static final String VERSION = "1.0";
    public static final Duration BattleDecisionTime = Duration.ofSeconds(15);
    public static final String CONFIG_FILENAME = "TBM_Config.xml";
    public static final String CONFIG_DIRECTORY = "config/TurnBasedMinecraft/";
    public static final String CONFIG_FILE_PATH = CONFIG_DIRECTORY + CONFIG_FILENAME;
    public static final String CONFIG_INTERNAL_PATH = "/assets/TurnBasedMinecraft/" + CONFIG_FILENAME;
    
    private static int CONFIG_FILE_VERSION = 0;

    protected static Logger logger;
    protected static BattleManager battleManager;
    private static int packetHandlerID = 0;
    protected static Entity attackingEntity;
    protected static int attackingDamage = 0;
    protected static Config config;
    
    public static Battle currentBattle = null;
    public static BattleGui currentBattleGui = null;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        logger.debug("PREINIT");
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        currentBattle = null;
        battleManager = null;
        config = new Config(logger);
        
        // register packets
        PacketHandler.INSTANCE.registerMessage(
            PacketBattleInfo.HandlerBattleInfo.class,
            PacketBattleInfo.class,
            packetHandlerID++,
            Side.CLIENT);
        PacketHandler.INSTANCE.registerMessage(
            PacketBattleRequestInfo.HandlerBattleRequestInfo.class,
            PacketBattleRequestInfo.class,
            packetHandlerID++,
            Side.SERVER);
        PacketHandler.INSTANCE.registerMessage(
            PacketBattleDecision.HandleBattleDecision.class,
            PacketBattleDecision.class,
            packetHandlerID++,
            Side.SERVER);
        PacketHandler.INSTANCE.registerMessage(
            PacketBattleMessage.HandlerBattleMessage.class,
            PacketBattleMessage.class,
            packetHandlerID++,
            Side.CLIENT);
        logger.debug("INIT");
        
        // register event handler(s)
        MinecraftForge.EVENT_BUS.register(new AttackEventHandler());
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        logger.debug("POSTINIT");
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
}
