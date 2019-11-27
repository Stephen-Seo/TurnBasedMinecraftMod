package com.burnedkirby.TurnBasedMinecraft.common;

import com.burnedkirby.TurnBasedMinecraft.client.ClientProxy;
import com.burnedkirby.TurnBasedMinecraft.common.networking.*;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
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
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(value = TurnBasedMinecraftMod.MODID)
public class TurnBasedMinecraftMod
{
    public static final String MODID = "com.burnedkirby.turnbasedminecraft";
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
        // tbm-disable
        event.getServer().getCommandManager().getDispatcher().register(
            Commands.literal("tbm-disable")
                .requires(c -> {
                    return !proxy.getConfig().getIfOnlyOPsCanDisableTurnBasedForSelf() || c.hasPermissionLevel(2);
                })
                .executes( c -> {
                    proxy.getConfig().addBattleIgnoringPlayer(c.getSource().asPlayer().getEntityId());
                    c.getSource().sendFeedback(new StringTextComponent("Disabled turn-based-combat for current player"), true);
                    return 1;
                }));
        // tbm-disable-all
        event.getServer().getCommandManager().getDispatcher().register(
            Commands.literal("tbm-disable-all")
                .requires(c -> {
                    return c.hasPermissionLevel(2);
                })
                .executes(c -> {
                    proxy.getConfig().setBattleDisabledForAll(true);
                    for(ServerPlayerEntity player : c.getSource().getServer().getPlayerList().getPlayers()) {
                        proxy.getConfig().addBattleIgnoringPlayer(player.getEntityId());
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("OP disabled turn-based-combat for everyone"));
                    }
                    return 1;
                }));
        // tbm-enable
        event.getServer().getCommandManager().getDispatcher().register(
            Commands.literal("tbm-enable")
                .requires(c -> !proxy.getConfig().getIfOnlyOPsCanDisableTurnBasedForSelf() || c.hasPermissionLevel(2))
                .executes(c -> {
                    proxy.getConfig().removeBattleIgnoringPlayer(c.getSource().asPlayer().getEntityId());
                    c.getSource().sendFeedback(new StringTextComponent("Enabled turn-based-combat for current player"), true);
                    return 1;
                }));
        // tbm-enable-all
        event.getServer().getCommandManager().getDispatcher().register(
            Commands.literal("tbm-enable-all")
                .requires(c -> c.hasPermissionLevel(2))
                .executes(c -> {
                    proxy.getConfig().setBattleDisabledForAll(false);
                    proxy.getConfig().clearBattleIgnoringPlayers();
                    for(ServerPlayerEntity player: c.getSource().getServer().getPlayerList().getPlayers()) {
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("OP enabled turn-based-combat for everyone"));
                    }
                    return 1;
                }));
        // tbm-set-enable
        event.getServer().getCommandManager().getDispatcher().register(
            Commands.literal("tbm-set-enable")
                .requires(c -> c.hasPermissionLevel(2))
                .then(Commands.argument("targets", EntityArgument.players()).executes(c -> {
                    for(ServerPlayerEntity player : EntityArgument.getPlayers(c, "targets")) {
                        proxy.getConfig().addBattleIgnoringPlayer(player.getEntityId());
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("OP enabled turn-based-combat for you"));
                        c.getSource().sendFeedback(new StringTextComponent("Enabled turn-based-combat for " + player.getDisplayName().getUnformattedComponentText()), true);
                    }
                    return 1;
                })));
        // tbm-set-disable
        event.getServer().getCommandManager().getDispatcher().register(
            Commands.literal("tbm-set-disable")
                .requires(c -> c.hasPermissionLevel(2))
                .then(Commands.argument("targets", EntityArgument.players()).executes(c -> {
                    for(ServerPlayerEntity player : EntityArgument.getPlayers(c, "targets")) {
                        proxy.getConfig().removeBattleIgnoringPlayer(player.getEntityId());
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("OP disabled turn-based-combat for you"));
                        c.getSource().sendFeedback(new StringTextComponent("Disabled turn-based-combat for " + player.getDisplayName().getUnformattedComponentText()), true);
                    }
                    return 1;
                })));
        // tbm-edit
        event.getServer().getCommandManager().getDispatcher().register(
            Commands.literal("tbm-edit")
                .requires(c -> c.hasPermissionLevel(2))
                .executes(c -> {
                    ServerPlayerEntity player = c.getSource().asPlayer();
                    EditingInfo editingInfo = proxy.getEditingInfo(player.getEntityId());
                    if(editingInfo != null && !editingInfo.isPendingEntitySelection) {
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                    } else if(editingInfo != null) {
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                    } else {
                        proxy.setEditingPlayer(c.getSource().asPlayer());
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                        logger.info("Begin editing TBM Entity for player \"" + player.getDisplayName().getUnformattedComponentText() + "\"");
                    }
                    return 1;
                })
                .then(Commands.argument("action", StringArgumentType.word())
                    .executes(c -> {
                        String action = StringArgumentType.getString(c, "action").toLowerCase();
                        ServerPlayerEntity player = c.getSource().asPlayer();
                        EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getEntityId());
                        if(editingInfo != null && !editingInfo.isPendingEntitySelection) {
                            if (action.equals("finish")) {
                                if (!proxy.getConfig().editEntityEntry(editingInfo.entityInfo)) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("An error occurred while attempting to save an entry to the config"));
                                    proxy.removeEditingInfo(player.getEntityId());
                                } else {
                                    proxy.removeEditingInfo(player.getEntityId());
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("Entity info saved in config and loaded."));
                                }
                            } else if (action.equals("cancel")) {
                                proxy.removeEditingInfo(player.getEntityId());
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("Cancelled editing entry."));
                            } else if (action.equals("edit")) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Invalid action for tbm-edit");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                        } else if(editingInfo != null) {
                            getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                        } else {
                            if(action.equals("custom")) {
                                proxy.setEditingPlayer(player);
                                proxy.getEditingInfo(player.getEntityId()).isEditingCustomName = true;
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                logger.info("Begin editing custom TBM Entity for player \"" + player.getDisplayName().getUnformattedComponentText() + "\'");
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                        }
                        return 1;
                    })
                    .then(Commands.argument("category", StringArgumentType.word())
                        .executes(c -> {
                            String action = StringArgumentType.getString(c, "action").toLowerCase();
                            String category = StringArgumentType.getString(c, "category");
                            ServerPlayerEntity player = c.getSource().asPlayer();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getEntityId());
                            if(editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                if (action.equals("edit")) {
                                    switch (category) {
                                    case "ignoreBattle":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_IGNORE_BATTLE));
                                        break;
                                    case "attackPower":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_POWER));
                                        break;
                                    case "attackProbability":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_PROBABILITY));
                                        break;
                                    case "attackVariance":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_VARIANCE));
                                        break;
                                    case "attackEffect":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_EFFECT));
                                        break;
                                    case "attackEffectProbability":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_EFFECT_PROBABILITY));
                                        break;
                                    case "defenseDamage":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DEFENSE_DAMAGE));
                                        break;
                                    case "defenseDamageProbability":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DEFENSE_DAMAGE_PROBABILITY));
                                        break;
                                    case "evasion":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_EVASION));
                                        break;
                                    case "speed":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_SPEED));
                                        break;
                                    case "category":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_CATEGORY));
                                        break;
                                    case "decisionAttack":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DECISION_ATTACK));
                                        break;
                                    case "decisionDefend":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DECISION_DEFEND));
                                        break;
                                    case "decisionFlee":
                                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DECISION_FLEE));
                                        break;
                                    default:
                                        Message exceptionMessage = new LiteralMessage("Invalid argument for \"/tbm-edit edit <category>\"");
                                        throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                    }
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Invalid argument");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                            } else if(editingInfo != null){
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("value", StringArgumentType.greedyString()))
                            .executes(c -> {
                                String action = StringArgumentType.getString(c, "action").toLowerCase();
                                String category = StringArgumentType.getString(c, "category");
                                String value = StringArgumentType.getString(c, "value").toLowerCase();
                                ServerPlayerEntity player = c.getSource().asPlayer();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getEntityId());
                                if(editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    if (action.equals("edit")) {
                                        switch (category) {
                                        case "ignoreBattle":
                                            if (value.equals("true")) {
                                                editingInfo.entityInfo.ignoreBattle = true;
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } else if (value.equals("false")) {
                                                editingInfo.entityInfo.ignoreBattle = false;
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } else {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit ignoreBattle <boolean>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "attackPower":
                                            try {
                                                editingInfo.entityInfo.attackPower = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.attackPower < 0) {
                                                    editingInfo.entityInfo.attackPower = 0;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit attackPower <integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "attackProbability":
                                            try {
                                                editingInfo.entityInfo.attackProbability = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.attackProbability < 0) {
                                                    editingInfo.entityInfo.attackProbability = 0;
                                                } else if (editingInfo.entityInfo.attackProbability > 100) {
                                                    editingInfo.entityInfo.attackProbability = 100;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit attackProbability <percentage-integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "attackVariance":
                                            try {
                                                editingInfo.entityInfo.attackVariance = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.attackVariance < 0) {
                                                    editingInfo.entityInfo.attackVariance = 0;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit attackVariance <integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "attackEffect":
                                            editingInfo.entityInfo.attackEffect = EntityInfo.Effect.fromString(value);
                                            getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            break;
                                        case "attackEffectProbability":
                                            try {
                                                editingInfo.entityInfo.attackEffectProbability = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.attackEffectProbability < 0) {
                                                    editingInfo.entityInfo.attackEffectProbability = 0;
                                                } else if (editingInfo.entityInfo.attackEffectProbability > 100) {
                                                    editingInfo.entityInfo.attackEffectProbability = 100;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit attackEffectProbability <percentage-integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "defenseDamage":
                                            try {
                                                editingInfo.entityInfo.defenseDamage = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.defenseDamage < 0) {
                                                    editingInfo.entityInfo.defenseDamage = 0;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit defenseDamage <integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "defenseDamageProbability":
                                            try {
                                                editingInfo.entityInfo.defenseDamageProbability = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.defenseDamageProbability < 0) {
                                                    editingInfo.entityInfo.defenseDamageProbability = 0;
                                                } else if (editingInfo.entityInfo.defenseDamageProbability > 100) {
                                                    editingInfo.entityInfo.defenseDamageProbability = 100;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit defenseDamageProbability <percentage-integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "evasion":
                                            try {
                                                editingInfo.entityInfo.evasion = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.evasion < 0) {
                                                    editingInfo.entityInfo.evasion = 0;
                                                } else if (editingInfo.entityInfo.evasion > 100) {
                                                    editingInfo.entityInfo.evasion = 100;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit evasion <percentage-integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "speed":
                                            try {
                                                editingInfo.entityInfo.speed = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.speed < 0) {
                                                    editingInfo.entityInfo.speed = 0;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit speed <integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "category":
                                            editingInfo.entityInfo.category = value;
                                            getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            break;
                                        case "decisionAttack":
                                            try {
                                                editingInfo.entityInfo.decisionAttack = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.decisionAttack < 0) {
                                                    editingInfo.entityInfo.decisionAttack = 0;
                                                } else if (editingInfo.entityInfo.decisionAttack > 100) {
                                                    editingInfo.entityInfo.decisionAttack = 100;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit decisionAttack <percentage-integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "decisionDefend":
                                            try {
                                                editingInfo.entityInfo.decisionDefend = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.decisionDefend < 0) {
                                                    editingInfo.entityInfo.decisionDefend = 0;
                                                } else if (editingInfo.entityInfo.decisionDefend > 100) {
                                                    editingInfo.entityInfo.decisionDefend = 100;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit decisionDefend <percentage-integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        case "decisionFlee":
                                            try {
                                                editingInfo.entityInfo.decisionFlee = Integer.parseInt(value);
                                                if (editingInfo.entityInfo.decisionFlee < 0) {
                                                    editingInfo.entityInfo.decisionFlee = 0;
                                                } else if (editingInfo.entityInfo.decisionFlee > 100) {
                                                    editingInfo.entityInfo.decisionFlee = 100;
                                                }
                                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                            } catch (NumberFormatException e) {
                                                Message exceptionMessage = new LiteralMessage("Invalid value for \"/tbm-edit edit decisionFlee <percentage-integer>\"");
                                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                            }
                                            break;
                                        default:
                                            Message exceptionMessage = new LiteralMessage("Invalid category for \"/tbm-edit edit <category> <value>\"");
                                            throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                        }
                                    } else {
                                        Message exceptionMessage = new LiteralMessage("Invalid args for \"/tbm-edit <args...>\"");
                                        throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                    }
                                } else if(editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            })
                    ))
        );
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
