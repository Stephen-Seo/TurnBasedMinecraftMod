package com.seodisparate.TurnBasedMinecraft;

import org.apache.logging.log4j.Logger;

import com.seodisparate.TurnBasedMinecraft.common.BattleManager;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = TurnBasedMinecraftMod.MODID, name = TurnBasedMinecraftMod.NAME, version = TurnBasedMinecraftMod.VERSION)
public class TurnBasedMinecraftMod
{
    public static final String MODID = "com.seodisparate.turnbasedminecraft";
    public static final String NAME = "Turn Based Minecraft Mod";
    public static final String VERSION = "1.0";

    private static Logger logger;
    private static BattleManager battleManager;

    public static Entity attackingEntity;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        battleManager = new BattleManager();
    }

    @EventHandler
    public void entityAttacked(LivingAttackEvent event)
    {
        if(!event.getEntity().equals(attackingEntity) && battleManager.checkAttack(event))
        {
            logger.debug("Canceled LivingAttackEvent between " + attackingEntity + " and " + event.getEntity());
            event.setCanceled(true);
        }
    }
}
