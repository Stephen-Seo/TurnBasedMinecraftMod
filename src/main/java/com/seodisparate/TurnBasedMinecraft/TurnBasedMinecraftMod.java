package com.seodisparate.TurnBasedMinecraft;

import java.time.Duration;

import org.apache.logging.log4j.Logger;

import com.seodisparate.TurnBasedMinecraft.common.Battle;
import com.seodisparate.TurnBasedMinecraft.common.BattleManager;
import com.seodisparate.TurnBasedMinecraft.common.Config;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleDecision;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleEntered;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleExited;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleInfo;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketBattleRequestInfo;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketHandler;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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
    public static final int CONFIG_FILE_VERSION = 1; // TODO derive this from internal config

    private static Logger logger;
    private static BattleManager battleManager;
    private static int packetHandlerID = 0;
    public static Entity attackingEntity;
    private static Config config;
    
    public static Battle currentBattle;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        currentBattle = null;
        if(event.getSide() == Side.SERVER)
        {
            battleManager = new BattleManager();
        }
        else
        {
            battleManager = null;
        }
        
        // register packets
        PacketHandler.INSTANCE.registerMessage(
            PacketBattleEntered.HandlerBattleEntered.class,
            PacketBattleEntered.class,
            packetHandlerID++,
            Side.CLIENT);
        PacketHandler.INSTANCE.registerMessage(
            PacketBattleExited.HandlerBattleExited.class,
            PacketBattleExited.class,
            packetHandlerID++,
            Side.CLIENT);
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
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        if(battleManager != null)
        {
            config = new Config(logger);
        }
    }

    @EventHandler
    public void entityAttacked(LivingAttackEvent event)
    {
        if(battleManager == null)
        {
            return;
        }
        if(!event.getSource().getTrueSource().equals(attackingEntity) && battleManager.checkAttack(event))
        {
            logger.debug("Canceled LivingAttackEvent between " + attackingEntity + " and " + event.getEntity());
            event.setCanceled(true);
        }
    }
    
    public static BattleManager getBattleManager()
    {
        return battleManager;
    }
}
