package com.burnedkirby.TurnBasedMinecraft.common;

import com.burnedkirby.TurnBasedMinecraft.client.ClientProxy;
import com.burnedkirby.TurnBasedMinecraft.common.networking.*;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TurnBasedMinecraftMod.MODID)
public class TurnBasedMinecraftMod {
    public static final String MODID = "com_burnedkirby_turnbasedminecraft";
    public static final String NAME = "Turn Based Minecraft Mod";
    public static final String VERSION = "1.21.0";
    public static final String CONFIG_FILENAME = "TBM_Config.toml";
    public static final String DEFAULT_CONFIG_FILENAME = "TBM_Config_DEFAULT.toml";
    public static final String CONFIG_DIRECTORY = "config/TurnBasedMinecraft/";
    public static final String CONFIG_FILE_PATH = CONFIG_DIRECTORY + CONFIG_FILENAME;
    public static final String DEFAULT_CONFIG_FILE_PATH = CONFIG_DIRECTORY + DEFAULT_CONFIG_FILENAME;
    public static final String CONFIG_INTERNAL_PATH = "/assets/com_burnedkirby_turnbasedminecraft/" + CONFIG_FILENAME;
    public static final String MUSIC_ROOT = CONFIG_DIRECTORY + "Music/";
    public static final String MUSIC_SILLY = MUSIC_ROOT + "silly/";
    public static final String MUSIC_BATTLE = MUSIC_ROOT + "battle/";

    private static final String PROTOCOL_VERSION = Integer.toString(2);
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

    public TurnBasedMinecraftMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::firstInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::secondInitClient);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::secondInitServer);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void firstInit(final FMLCommonSetupEvent event) {
        proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
        proxy.setLogger(logger);
        proxy.initialize();

        // register packets
        int packetHandlerID = 0;
        HANDLER.registerMessage(
            packetHandlerID++,
            PacketBattleInfo.class,
            PacketBattleInfo::encode,
            PacketBattleInfo::decode,
            PacketBattleInfo::handle);
        HANDLER.registerMessage(
            packetHandlerID++,
            PacketBattleRequestInfo.class,
            PacketBattleRequestInfo::encode,
            PacketBattleRequestInfo::decode,
            PacketBattleRequestInfo::handle);
        HANDLER.registerMessage(
            packetHandlerID++,
            PacketBattleDecision.class,
            PacketBattleDecision::encode,
            PacketBattleDecision::decode,
            PacketBattleDecision::handle);
        HANDLER.registerMessage(
            packetHandlerID++,
            PacketBattleMessage.class,
            PacketBattleMessage::encode,
            PacketBattleMessage::decode,
            PacketBattleMessage::handle);
        HANDLER.registerMessage(
            packetHandlerID++,
            PacketGeneralMessage.class,
            PacketGeneralMessage::encode,
            PacketGeneralMessage::decode,
            PacketGeneralMessage::handle);
        HANDLER.registerMessage(
            packetHandlerID++,
            PacketEditingMessage.class,
            PacketEditingMessage::encode,
            PacketEditingMessage::decode,
            PacketEditingMessage::handle);

        // register event handler(s)
        MinecraftForge.EVENT_BUS.register(new AttackEventHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerJoinEventHandler());
        MinecraftForge.EVENT_BUS.register(new DimensionChangedHandler());
        MinecraftForge.EVENT_BUS.register(new HurtEventHandler());

        logger.debug("Init com_burnedkirby_turnbasedminecraft");
    }

    private void secondInitClient(final FMLClientSetupEvent event) {
        proxy.postInit();
    }

    private void secondInitServer(final FMLDedicatedServerSetupEvent event) {
        proxy.postInit();
    }

    @SubscribeEvent
    public void serverStarting(ServerStartingEvent event) {
        logger.debug("About to initialize BattleManager");
        if (proxy.initializeBattleManager()) {
            logger.debug("Initialized BattleManager");
        }

        proxy.getConfig().clearBattleIgnoringPlayers();
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        // tbm-disable
        event.getDispatcher().register(
            Commands.literal("tbm-disable")
                .requires(c -> {
                    return !proxy.getConfig().getIfOnlyOPsCanDisableTurnBasedForSelf() || c.hasPermission(2);
                })
                .executes(c -> {
                    proxy.getConfig().addBattleIgnoringPlayer(c.getSource().getPlayerOrException().getId());
                    c.getSource().sendSuccess(Component.literal("Disabled turn-based-combat for current player"), true);
                    return 1;
                }));
        // tbm-disable-all
        event.getDispatcher().register(
            Commands.literal("tbm-disable-all")
                .requires(c -> {
                    return c.hasPermission(2);
                })
                .executes(c -> {
                    proxy.getConfig().setBattleDisabledForAll(true);
                    for (ServerPlayer player : c.getSource().getServer().getPlayerList().getPlayers()) {
                        proxy.getConfig().addBattleIgnoringPlayer(player.getId());
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("OP disabled turn-based-combat for everyone"));
                    }
                    return 1;
                }));
        // tbm-enable
        event.getDispatcher().register(
            Commands.literal("tbm-enable")
                .requires(c -> !proxy.getConfig().getIfOnlyOPsCanDisableTurnBasedForSelf() || c.hasPermission(2))
                .executes(c -> {
                    proxy.getConfig().removeBattleIgnoringPlayer(c.getSource().getPlayerOrException().getId());
                    c.getSource().sendSuccess(Component.literal("Enabled turn-based-combat for current player"), true);
                    return 1;
                }));
        // tbm-enable-all
        event.getDispatcher().register(
            Commands.literal("tbm-enable-all")
                .requires(c -> c.hasPermission(2))
                .executes(c -> {
                    proxy.getConfig().setBattleDisabledForAll(false);
                    proxy.getConfig().clearBattleIgnoringPlayers();
                    for (ServerPlayer player : c.getSource().getServer().getPlayerList().getPlayers()) {
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("OP enabled turn-based-combat for everyone"));
                    }
                    return 1;
                }));
        // tbm-set-enable
        event.getDispatcher().register(
            Commands.literal("tbm-set-enable")
                .requires(c -> c.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players()).executes(c -> {
                    for (ServerPlayer player : EntityArgument.getPlayers(c, "targets")) {
                        proxy.getConfig().addBattleIgnoringPlayer(player.getId());
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("OP enabled turn-based-combat for you"));
                        c.getSource().sendSuccess(Component.literal("Enabled turn-based-combat for " + player.getDisplayName().getString()), true);
                    }
                    return 1;
                })));
        // tbm-set-disable
        event.getDispatcher().register(
            Commands.literal("tbm-set-disable")
                .requires(c -> c.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players()).executes(c -> {
                    for (ServerPlayer player : EntityArgument.getPlayers(c, "targets")) {
                        proxy.getConfig().removeBattleIgnoringPlayer(player.getId());
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("OP disabled turn-based-combat for you"));
                        c.getSource().sendSuccess(Component.literal("Disabled turn-based-combat for " + player.getDisplayName().getString()), true);
                    }
                    return 1;
                })));
        // tbm-edit
        event.getDispatcher().register(
            Commands.literal("tbm-edit")
                .requires(c -> c.hasPermission(2))
                .executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    EditingInfo editingInfo = proxy.getEditingInfo(player.getId());
                    if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                    } else if (editingInfo != null) {
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                    } else {
                        proxy.setEditingPlayer(player);
                        getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                        logger.info("Begin editing TBM Entity for player \"" + player.getDisplayName().getString() + "\" (\"" + c.getSource().getDisplayName() + "\")");
                    }
                    return 1;
                })
                .then(Commands.literal("finish")
                    .executes(c -> {
                        ServerPlayer player = c.getSource().getPlayerOrException();
                        EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                        if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                            if (!proxy.getConfig().editEntityEntry(editingInfo.entityInfo)) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("An error occurred while attempting to save an entry to the config"));
                                proxy.removeEditingInfo(player.getId());
                            } else {
                                proxy.removeEditingInfo(player.getId());
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("Entity info saved in config and loaded."));
                            }
                        } else if (editingInfo != null) {
                            getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                        } else {
                            Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                            throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                        }
                        return 1;
                    }))
                .then(Commands.literal("cancel")
                    .executes(c -> {
                        ServerPlayer player = c.getSource().getPlayerOrException();
                        EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                        if (editingInfo != null) {
                            proxy.removeEditingInfo(player.getId());
                            getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketGeneralMessage("Cancelled editing entry."));
                        }
                        return 1;
                    }))
                .then(Commands.literal("custom")
                    .executes(c -> {
                        ServerPlayer player = c.getSource().getPlayerOrException();
                        EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                        if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                            Message exceptionMessage = new LiteralMessage("Invalid action for tbm-edit");
                            throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                        } else if (editingInfo != null) {
                            getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                        } else {
                            proxy.setEditingPlayer(player);
                            proxy.getEditingInfo(player.getId()).isEditingCustomName = true;
                            getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            logger.info("Begin editing custom TBM Entity for player \"" + player.getDisplayName().getString() + "\" (\"" + c.getSource().getDisplayName() + "\")");
                        }
                        return 1;
                    }))
                .then(Commands.literal("edit")
                    .executes(c -> {
                        ServerPlayer player = c.getSource().getPlayerOrException();
                        EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                        if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                            getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                        } else if (editingInfo != null) {
                            getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                        } else {
                            Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                            throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                        }
                        return 1;
                    })
                    .then(Commands.literal("ignoreBattle")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_IGNORE_BATTLE));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("ignoreBattle", BoolArgumentType.bool())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                boolean ignoreBattle = BoolArgumentType.getBool(c, "ignoreBattle");
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.ignoreBattle = ignoreBattle;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("attackPower")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_POWER));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("attackPower", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int attackPower = IntegerArgumentType.getInteger(c, "attackPower");
                                if (attackPower < 0) {
                                    attackPower = 0;
                                }
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.attackPower = attackPower;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("attackProbability")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_PROBABILITY));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("attackProbability", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int attackProbability = IntegerArgumentType.getInteger(c, "attackProbability");
                                if (attackProbability < 0) {
                                    attackProbability = 0;
                                } else if (attackProbability > 100) {
                                    attackProbability = 100;
                                }
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.attackProbability = attackProbability;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("attackVariance")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_VARIANCE));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("attackVariance", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int attackVariance = IntegerArgumentType.getInteger(c, "attackVariance");
                                if (attackVariance < 0) {
                                    attackVariance = 0;
                                }
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.attackVariance = attackVariance;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("attackEffect")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_EFFECT));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("attackEffect", StringArgumentType.word())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                EntityInfo.Effect effect = EntityInfo.Effect.fromString(StringArgumentType.getString(c, "attackEffect"));
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.attackEffect = effect;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("attackEffectProbability")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_EFFECT_PROBABILITY));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("attackEffectProbability", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int attackEffectProbability = IntegerArgumentType.getInteger(c, "attackEffectProbability");
                                if (attackEffectProbability < 0) {
                                    attackEffectProbability = 0;
                                } else if (attackEffectProbability > 100) {
                                    attackEffectProbability = 100;
                                }
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.attackEffectProbability = attackEffectProbability;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("defenseDamage")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DEFENSE_DAMAGE));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("defenseDamage", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int defenseDamage = IntegerArgumentType.getInteger(c, "defenseDamage");
                                if (defenseDamage < 0) {
                                    defenseDamage = 0;
                                }
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.defenseDamage = defenseDamage;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("defenseDamageProbability")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DEFENSE_DAMAGE_PROBABILITY));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("defenseDamageProbability", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int defenseDamageProbability = IntegerArgumentType.getInteger(c, "defenseDamageProbability");
                                if (defenseDamageProbability < 0) {
                                    defenseDamageProbability = 0;
                                } else if (defenseDamageProbability > 100) {
                                    defenseDamageProbability = 100;
                                }
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.defenseDamageProbability = defenseDamageProbability;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("evasion")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_EVASION));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("evasion", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int evasion = IntegerArgumentType.getInteger(c, "evasion");
                                if (evasion < 0) {
                                    evasion = 0;
                                } else if (evasion > 100) {
                                    evasion = 100;
                                }
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.evasion = evasion;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("speed")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_SPEED));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("speed", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int speed = IntegerArgumentType.getInteger(c, "speed");
                                if (speed < 0) {
                                    speed = 0;
                                }
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.speed = speed;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("category")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_CATEGORY));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("category", StringArgumentType.word())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                String category = StringArgumentType.getString(c, "category");
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.category = category;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("decisionAttack")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DECISION_ATTACK));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("decisionAttack", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int decisionAttack = IntegerArgumentType.getInteger(c, "decisionAttack");
                                if (decisionAttack < 0) {
                                    decisionAttack = 0;
                                } else if (decisionAttack > 100) {
                                    decisionAttack = 100;
                                }
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.decisionAttack = decisionAttack;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("decisionDefend")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DECISION_DEFEND));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("decisionDefend", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int decisionDefend = IntegerArgumentType.getInteger(c, "decisionDefend");
                                if (decisionDefend < 0) {
                                    decisionDefend = 0;
                                } else if (decisionDefend > 100) {
                                    decisionDefend = 100;
                                }
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.decisionDefend = decisionDefend;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                    .then(Commands.literal("decisionFlee")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                            if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DECISION_FLEE));
                            } else if (editingInfo != null) {
                                getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                            } else {
                                Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                            }
                            return 1;
                        })
                        .then(Commands.argument("decisionFlee", IntegerArgumentType.integer())
                            .executes(c -> {
                                ServerPlayer player = c.getSource().getPlayerOrException();
                                EditingInfo editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(player.getId());
                                int decisionFlee = IntegerArgumentType.getInteger(c, "decisionFlee");
                                if (editingInfo != null && !editingInfo.isPendingEntitySelection) {
                                    editingInfo.entityInfo.decisionFlee = decisionFlee;
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo));
                                } else if (editingInfo != null) {
                                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY));
                                } else {
                                    Message exceptionMessage = new LiteralMessage("Cannot edit entity without starting editing (use \"/tbm-edit\").");
                                    throw new CommandSyntaxException(new SimpleCommandExceptionType(exceptionMessage), exceptionMessage);
                                }
                                return 1;
                            }))
                    )
                )
        );
        // tbm-server-edit
        event.getDispatcher().register(
            Commands.literal("tbm-server-edit")
                .requires(c -> c.hasPermission(2))
                .executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    getHandler().send(PacketDistributor.PLAYER.with(() -> player), new PacketEditingMessage(PacketEditingMessage.Type.SERVER_EDIT));
                    return 1;
                })
                .then(Commands.literal("leave_battle_cooldown").executes(c -> {
                        MutableComponent response = Component.literal("leave_battle_cooldown requires an integer argument. ");
                        MutableComponent subResponse = Component.literal("leave_battle_cooldown is currently: ");
                        response.getSiblings().add(subResponse);
                        subResponse = Component.literal(String.valueOf(TurnBasedMinecraftMod.proxy.getConfig().getLeaveBattleCooldownSeconds()));
                        subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                        response.getSiblings().add(subResponse);
                        c.getSource().sendSuccess(response, false);
                        return 1;
                    })
                    .then(Commands.argument("cooldown_seconds", IntegerArgumentType.integer())
                        .executes(c -> {
                            int cooldown = IntegerArgumentType.getInteger(c, "cooldown_seconds");
                            // setting cooldown validates the value. Set it, then fetch it again.
                            TurnBasedMinecraftMod.proxy.getConfig().setLeaveBattleCooldownSeconds(cooldown);
                            cooldown = TurnBasedMinecraftMod.proxy.getConfig().getLeaveBattleCooldownSeconds();
                            if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig(
                                "server_config.leave_battle_cooldown",
                                cooldown)) {
                                TurnBasedMinecraftMod.logger.warn(
                                    "Failed to set \"server_config.leave_battle_cooldown\" in config file!");
                                c.getSource().sendFailure(Component.literal("" +
                                    "Failed to set leave_battle_cooldown to \""
                                    + cooldown
                                    + "\" in config file!"));
                            } else {
                                MutableComponent response = Component.literal("Successfully set leave_battle_cooldown to: ");
                                MutableComponent subResponse = Component.literal(String.valueOf(cooldown));
                                subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                                response.getSiblings().add(subResponse);
                                c.getSource().sendSuccess(response, true);
                            }
                            return 1;
                        })))
                .then(Commands.literal("aggro_start_battle_max_distance").executes(c -> {
                        MutableComponent response = Component.literal("aggro_start_battle_max_distance requires an integer argument. ");
                        MutableComponent subResponse = Component.literal("aggro_start_battle_max_distance is currently: ");
                        response.getSiblings().add(subResponse);
                        subResponse = Component.literal(String.valueOf(
                            TurnBasedMinecraftMod.proxy.getConfig().getAggroStartBattleDistance()));
                        subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                        response.getSiblings().add(subResponse);
                        c.getSource().sendSuccess(response, false);
                        return 1;
                    })
                    .then(Commands.argument("aggro_distance", IntegerArgumentType.integer())
                        .executes(c -> {
                            int distance = IntegerArgumentType.getInteger(c, "aggro_distance");
                            // setDistance in Config validates the value. Set it, then fetch it again.
                            TurnBasedMinecraftMod.proxy.getConfig().setAggroStartBattleDistance(distance);
                            distance = TurnBasedMinecraftMod.proxy.getConfig().getAggroStartBattleDistance();
                            if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig(
                                "server_config.aggro_start_battle_max_distance",
                                distance)) {
                                TurnBasedMinecraftMod.logger.warn(
                                    "Failed to set \"server_config.aggro_start_battle_max_distance\" in config file!");
                                c.getSource().sendFailure(Component.literal(
                                    "Failed to set aggro_start_battle_max_distance to \""
                                        + distance
                                        + "\" in config file!"));
                            } else {
                                MutableComponent response = Component.literal("Successfully set aggro_start_battle_max_distance to: ");
                                MutableComponent subResponse = Component.literal(String.valueOf(distance));
                                subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                                response.getSiblings().add(subResponse);
                                c.getSource().sendSuccess(response, true);
                            }
                            return 1;
                        })))
                .then(Commands.literal("old_battle_behavior").executes(c -> {
                        MutableComponent response = Component.literal("old_battle_behavior requires a boolean argument. ");
                        MutableComponent subResponse = Component.literal("old_battle_behavior is currently: ");
                        response.getSiblings().add(subResponse);
                        subResponse = Component.literal(String.valueOf(
                            TurnBasedMinecraftMod.proxy.getConfig().isOldBattleBehaviorEnabled()));
                        subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                        response.getSiblings().add(subResponse);
                        c.getSource().sendSuccess(response, false);
                        return 1;
                    })
                    .then(Commands.argument("old_battle_behavior_enabled", BoolArgumentType.bool())
                        .executes(c -> {
                            boolean enabled = BoolArgumentType.getBool(c, "old_battle_behavior_enabled");
                            TurnBasedMinecraftMod.proxy.getConfig().setOldBattleBehavior(enabled);
                            if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig(
                                "server_config.old_battle_behavior",
                                enabled)) {
                                TurnBasedMinecraftMod.logger.warn(
                                    "Failed to set \"server_config.old_battle_behavior\" in config file!");
                                c.getSource().sendFailure(Component.literal(
                                    "Failed to set old_battle_behavior to \""
                                        + enabled
                                        + "\" in config file!"));
                            } else {
                                MutableComponent response = Component.literal("Successfully set old_battle_behavior to: ");
                                MutableComponent subResponse = Component.literal(String.valueOf(enabled));
                                subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                                response.getSiblings().add(subResponse);
                                c.getSource().sendSuccess(response, true);
                            }
                            return 1;
                        })))
                .then(Commands.literal("anyone_can_disable_tbm_for_self").executes(c -> {
                        MutableComponent response = Component.literal("anyone_can_disable_tbm_for_self requires a boolean argument. ");
                        MutableComponent subResponse = Component.literal("anyone_can_disable_tbm_for_self is currently: ");
                        response.getSiblings().add(subResponse);
                        subResponse = Component.literal(String.valueOf(
                            !TurnBasedMinecraftMod.proxy.getConfig().getIfOnlyOPsCanDisableTurnBasedForSelf()));
                        subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                        response.getSiblings().add(subResponse);
                        c.getSource().sendSuccess(response, false);
                        return 1;
                    })
                    .then(Commands.argument("enabled_for_all", BoolArgumentType.bool())
                        .executes(c -> {
                            boolean enabled_for_all = BoolArgumentType.getBool(c, "enabled_for_all");
                            TurnBasedMinecraftMod.proxy.getConfig().setIfOnlyOPsCanDisableTurnBasedForSelf(!enabled_for_all);
                            if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig(
                                "server_config.anyone_can_disable_tbm_for_self",
                                enabled_for_all
                            )) {
                                TurnBasedMinecraftMod.logger.warn(
                                    "Failed to set \"server_config.anyone_can_disable_tbm_for_self\" in config file!");
                                c.getSource().sendFailure(Component.literal(
                                    "Failed to set anyone_can_disable_tbm_for_self to \""
                                        + enabled_for_all
                                        + "\" in config file!"));
                            } else {
                                MutableComponent response = Component.literal("Successfully set anyone_can_disable_tbm_for_self to: ");
                                MutableComponent subResponse = Component.literal(String.valueOf(enabled_for_all));
                                subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                                response.getSiblings().add(subResponse);
                                c.getSource().sendSuccess(response, true);
                            }
                            return 1;
                        })))
                .then(Commands.literal("max_in_battle").executes(c -> {
                        MutableComponent response = Component.literal("max_in_battle requires an integer argument. ");
                        MutableComponent subResponse = Component.literal("max_in_battle is currently: ");
                        response.getSiblings().add(subResponse);
                        subResponse = Component.literal(String.valueOf(
                            TurnBasedMinecraftMod.proxy.getConfig().getMaxInBattle()));
                        subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                        response.getSiblings().add(subResponse);
                        c.getSource().sendSuccess(response, false);
                        return 1;
                    })
                    .then(Commands.argument("max_amount", IntegerArgumentType.integer())
                        .executes(c -> {
                            int max_amount = IntegerArgumentType.getInteger(c, "max_amount");
                            // setMaxInBattle in Config validates the value. Set it, then fetch it again.
                            TurnBasedMinecraftMod.proxy.getConfig().setMaxInBattle(max_amount);
                            max_amount = TurnBasedMinecraftMod.proxy.getConfig().getMaxInBattle();
                            if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig(
                                "server_config.max_in_battle",
                                max_amount)) {
                                TurnBasedMinecraftMod.logger.warn(
                                    "Failed to set \"server_config.max_in_battle\" in config file!");
                                c.getSource().sendFailure(Component.literal(
                                    "Failed to set max_in_battle to \""
                                        + max_amount
                                        + "\" in config file!"));
                            } else {
                                MutableComponent response = Component.literal("Successfully set max_in_battle to: ");
                                MutableComponent subResponse = Component.literal(String.valueOf(max_amount));
                                subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                                response.getSiblings().add(subResponse);
                                c.getSource().sendSuccess(response, true);
                            }
                            return 1;
                        })))
                .then(Commands.literal("freeze_battle_combatants").executes(c -> {
                        MutableComponent response = Component.literal("freeze_battle_combatants requires a boolean argument. ");
                        MutableComponent subResponse = Component.literal("freeze_battle_combatants is currently: ");
                        response.getSiblings().add(subResponse);
                        subResponse = Component.literal(String.valueOf(
                            !TurnBasedMinecraftMod.proxy.getConfig().isFreezeCombatantsEnabled()));
                        subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                        response.getSiblings().add(subResponse);
                        c.getSource().sendSuccess(response, false);
                        return 1;
                    })
                    .then(Commands.argument("freeze_enabled", BoolArgumentType.bool())
                        .executes(c -> {
                            boolean enabled = BoolArgumentType.getBool(c, "freeze_enabled");
                            TurnBasedMinecraftMod.proxy.getConfig().setFreezeCombatantsInBattle(enabled);
                            if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.freeze_battle_combatants", enabled)) {
                                TurnBasedMinecraftMod.logger.warn(
                                    "Failed to set \"server_config.freeze_battle_combatants\" in config file!");
                                c.getSource().sendFailure(Component.literal(
                                    "Failed to set freeze_battle_combatants to \""
                                        + enabled
                                        + "\" in config file!"));
                            } else {
                                MutableComponent response = Component.literal("Successfully set freeze_battle_combatants to: ");
                                MutableComponent subResponse = Component.literal(String.valueOf(enabled));
                                subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                                response.getSiblings().add(subResponse);
                                c.getSource().sendSuccess(response, true);
                            }
                            return 1;
                        })))
                .then(Commands.literal("ignore_battle_types").executes(c -> {
                        MutableComponent response = Component.literal("Use ");
                        MutableComponent subResponse = Component.literal("/tbm-server-edit ignore_battle_types add/remove <category> ");
                        subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.YELLOW));
                        response.getSiblings().add(subResponse);

                        subResponse = Component.literal("ignore_battle_types is currently: [");
                        response.getSiblings().add(subResponse);

                        boolean isFirst = true;
                        for (String category : TurnBasedMinecraftMod.proxy.getConfig().getIgnoreBattleTypes()) {
                            if (!isFirst) {
                                response.getSiblings().add(Component.literal(", "));
                            }
                            subResponse = Component.literal(category);
                            subResponse.setStyle(subResponse.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit ignore_battle_types remove " + category))
                                .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to remove category"))));
                            response.getSiblings().add(subResponse);
                            isFirst = false;
                        }
                        response.getSiblings().add(Component.literal("] "));
                        c.getSource().sendSuccess(response, false);
                        return 1;
                    })
                    .then(Commands.literal("add").executes(c -> {
                            c.getSource().sendFailure(Component.literal("/tbm-server-edit ignore_battle_types add <category>"));
                            return 1;
                        })
                        .then(Commands.argument("category", StringArgumentType.greedyString()).executes(c -> {
                            String category = StringArgumentType.getString(c, "category");
                            if (TurnBasedMinecraftMod.proxy.getConfig().addIgnoreBattleType(category)
                                && TurnBasedMinecraftMod.proxy.getConfig().updateConfigAppendToStringArray("server_config.ignore_battle_types", category)) {
                                MutableComponent response = Component.literal("Successfully appended category \"");

                                MutableComponent sub = Component.literal(category);
                                sub.setStyle(sub.getStyle().withColor(ChatFormatting.GREEN));
                                response.getSiblings().add(sub);

                                sub = Component.literal("\" to ignore_battle_types");
                                response.getSiblings().add(sub);

                                c.getSource().sendSuccess(response, true);
                                return 1;
                            }

                            c.getSource().sendFailure(Component.literal(
                                "Failed to append category \"" + category + "\" to ignore_battle_types"));
                            return 1;
                        })))
                    .then(Commands.literal("remove").executes(c -> {
                            c.getSource().sendFailure(Component.literal("/tbm-server-edit ignore_battle_types remove <category>"));
                            return 1;
                        })
                        .then(Commands.argument("category", StringArgumentType.greedyString()).executes(c -> {
                            String category = StringArgumentType.getString(c, "category");
                            if (TurnBasedMinecraftMod.proxy.getConfig().removeIgnoreBattleType(category)
                                && TurnBasedMinecraftMod.proxy.getConfig().updateConfigRemoveFromStringArray("server_config.ignore_battle_types", category)) {
                                MutableComponent response = Component.literal("Successfully removed category \"");

                                MutableComponent sub = Component.literal(category);
                                sub.setStyle(sub.getStyle().withColor(ChatFormatting.GREEN));
                                response.getSiblings().add(sub);

                                sub = Component.literal("\" from ignore_battle_types");
                                response.getSiblings().add(sub);

                                c.getSource().sendSuccess(response, true);
                                return 1;
                            }

                            c.getSource().sendFailure(Component.literal(
                                "Failed to remove category \"" + category + "\" from ignore_battle_types"));
                            return 1;
                        }))))
                .then(Commands.literal("player_speed").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit player_speed <0-100>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("speed", IntegerArgumentType.integer()).executes(c -> {
                        int speed = IntegerArgumentType.getInteger(c, "speed");
                        // setPlayerSpeed() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setPlayerSpeed(speed);
                        speed = TurnBasedMinecraftMod.proxy.getConfig().getPlayerSpeed();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.player_speed", speed)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.player_speed\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set player_speed to \""
                                    + speed
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set player_speed to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(speed));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("player_haste_speed").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit player_haste_speed <0-100>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("haste_speed", IntegerArgumentType.integer()).executes(c -> {
                        int haste_speed = IntegerArgumentType.getInteger(c, "haste_speed");
                        // setPlayerHasteSpeed() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setPlayerHasteSpeed(haste_speed);
                        haste_speed = TurnBasedMinecraftMod.proxy.getConfig().getPlayerHasteSpeed();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.player_haste_speed", haste_speed)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.player_haste_speed\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set player_haste_speed to \""
                                    + haste_speed
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set player_haste_speed to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(haste_speed));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("player_slow_speed").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit player_slow_speed <0-100>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("slow_speed", IntegerArgumentType.integer()).executes(c -> {
                        int slow_speed = IntegerArgumentType.getInteger(c, "slow_speed");
                        // setPlayerSlowSpeed() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setPlayerSlowSpeed(slow_speed);
                        slow_speed = TurnBasedMinecraftMod.proxy.getConfig().getPlayerSlowSpeed();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.player_slow_speed", slow_speed)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.player_slow_speed\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set player_slow_speed to \""
                                    + slow_speed
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set player_slow_speed to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(slow_speed));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("player_attack_probability").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit player_attack_probability <1-100>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("probability", IntegerArgumentType.integer()).executes(c -> {
                        int probability = IntegerArgumentType.getInteger(c, "probability");
                        // setPlayerAttackProbability() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setPlayerAttackProbability(probability);
                        probability = TurnBasedMinecraftMod.proxy.getConfig().getPlayerAttackProbability();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.player_attack_probability", probability)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.player_attack_probability\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set player_attack_probability to \""
                                    + probability
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set player_attack_probability to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(probability));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("player_evasion").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit player_evasion <0-100>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("evasion", IntegerArgumentType.integer()).executes(c -> {
                        int evasion = IntegerArgumentType.getInteger(c, "evasion");
                        // setPlayerEvasion() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setPlayerEvasion(evasion);
                        evasion = TurnBasedMinecraftMod.proxy.getConfig().getPlayerEvasion();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.player_evasion", evasion)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.player_evasion\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set player_evasion to \""
                                    + evasion
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set player_evasion to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(evasion));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("defense_duration").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit defense_duration <0-5>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("defends", IntegerArgumentType.integer()).executes(c -> {
                        int defends = IntegerArgumentType.getInteger(c, "defends");
                        // setDefenseDuration() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setDefenseDuration(defends);
                        defends = TurnBasedMinecraftMod.proxy.getConfig().getDefenseDuration();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.defense_duration", defends)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.defense_duration\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set defense_druation to \""
                                    + defends
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set defense_duration to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(defends));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("flee_good_probability").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit flee_good_probability <1-100>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("probability", IntegerArgumentType.integer()).executes(c -> {
                        int probability = IntegerArgumentType.getInteger(c, "probability");
                        // setFleeGoodProbability() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setFleeGoodProbability(probability);
                        probability = TurnBasedMinecraftMod.proxy.getConfig().getFleeGoodProbability();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.flee_good_probability", probability)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.flee_good_probability\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set flee_good_probability to \""
                                    + probability
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set flee_good_probability to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(probability));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("flee_bad_probability").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit flee_bad_probability <1-100>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("probability", IntegerArgumentType.integer()).executes(c -> {
                        int probability = IntegerArgumentType.getInteger(c, "probability");
                        // setFleeBadProbability() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setFleeBadProbability(probability);
                        probability = TurnBasedMinecraftMod.proxy.getConfig().getFleeBadProbability();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.flee_bad_probability", probability)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.flee_bad_probability\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set flee_bad_probability to \""
                                    + probability
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set flee_bad_probability to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(probability));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("minimum_hit_percentage").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit minimum_hit_percentage <1-100>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("percentage", IntegerArgumentType.integer()).executes(c -> {
                        int percentage = IntegerArgumentType.getInteger(c, "percentage");
                        // setMinimumHitPercentage() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setMinimumHitPercentage(percentage);
                        percentage = TurnBasedMinecraftMod.proxy.getConfig().getMinimumHitPercentage();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.minimum_hit_percentage", percentage)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.minimum_hit_percentage\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set minimum_hit_percentage to \""
                                    + percentage
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set minimum_hit_percentage to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(percentage));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("battle_turn_wait_forever").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit battle_turn_wait_forever <true/false>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(c -> {
                        boolean enabled = BoolArgumentType.getBool(c, "enabled");
                        TurnBasedMinecraftMod.proxy.getConfig().setBattleDecisionDurationForever(enabled);
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.battle_turn_wait_forever", enabled)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.battle_turn_wait_forever\" in config file!"
                            );
                            c.getSource().sendFailure(Component.literal("Failed to set battle_turn_wait_forever to \"" + (enabled ? "true" : "false") + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set battle_turn_wait_forever to: ");
                            MutableComponent subResponse = Component.literal((enabled ? "true" : "false"));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("battle_turn_time_seconds").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit battle_turn_time_seconds <5-60>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("seconds", IntegerArgumentType.integer()).executes(c -> {
                        int seconds = IntegerArgumentType.getInteger(c, "seconds");
                        // setDecisionDurationSeconds() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setDecisionDurationSeconds(seconds);
                        seconds = TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationSeconds();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.battle_turn_time_seconds", seconds)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.battle_turn_time_seconds\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set battle_turn_time_seconds to \""
                                    + seconds
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set battle_turn_time_seconds to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(seconds));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("creeper_explode_turn").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit creeper_explode_turn <1-10>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("turns", IntegerArgumentType.integer()).executes(c -> {
                        int turns = IntegerArgumentType.getInteger(c, "turns");
                        // setCreeperExplodeTurn() in Config validates the value. Set it, then fetch it again.
                        TurnBasedMinecraftMod.proxy.getConfig().setCreeperExplodeTurn(turns);
                        turns = TurnBasedMinecraftMod.proxy.getConfig().getCreeperExplodeTurn();
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.creeper_explode_turn", turns)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.creeper_explode_turn\" in config file!");
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set creeper_explode_turn to \""
                                    + turns
                                    + "\" in config file!"));
                        } else {
                            MutableComponent response = Component.literal("Successfully set creeper_explode_turn to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(turns));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("creeper_stop_explode_on_leave_battle").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit creeper_stop_explode_on_leave_battle <true/false>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("stop_explode_on_leave", BoolArgumentType.bool()).executes(c -> {
                        boolean stop_explode_on_leave = BoolArgumentType.getBool(c, "stop_explode_on_leave");
                        TurnBasedMinecraftMod.proxy.getConfig().setCreeperStopExplodeOnLeaveBattle(stop_explode_on_leave);
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.creeper_stop_explode_on_leave_battle", stop_explode_on_leave)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.creeper_stop_explode_on_leave_battle\" in config file!"
                            );
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set creeper_stop_explode_on_leave_battle to \""
                                    + stop_explode_on_leave
                                    + "\" in config file!"
                            ));
                        } else {
                            MutableComponent response = Component.literal("Successfully set creeper_stop_explode_on_leave_battle to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(stop_explode_on_leave));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("creeper_always_allow_damage").executes(c -> {
                        MutableComponent parent = Component.literal("Use ");
                        MutableComponent sub = Component.literal("/tbm-server-edit creeper_always_allow_damage <true/false>");
                        sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW));
                        parent.getSiblings().add(sub);

                        c.getSource().sendSuccess(parent, false);
                        return 1;
                    })
                    .then(Commands.argument("allow_damage", BoolArgumentType.bool()).executes(c -> {
                        boolean allow_damage = BoolArgumentType.getBool(c, "allow_damage");
                        TurnBasedMinecraftMod.proxy.getConfig().setCreeperAlwaysAllowDamage(allow_damage);
                        if (!TurnBasedMinecraftMod.proxy.getConfig().updateConfig("server_config.creeper_always_allow_damage", allow_damage)) {
                            TurnBasedMinecraftMod.logger.warn(
                                "Failed to set \"server_config.creeper_always_allow_damage\" in config file!"
                            );
                            c.getSource().sendFailure(Component.literal(
                                "Failed to set creeper_always_allow_damage to \""
                                    + allow_damage
                                    + "\" in config file!"
                            ));
                        } else {
                            MutableComponent response = Component.literal("Successfully set creeper_always_allow_damage to: ");
                            MutableComponent subResponse = Component.literal(String.valueOf(allow_damage));
                            subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.GREEN));
                            response.getSiblings().add(subResponse);
                            c.getSource().sendSuccess(response, true);
                        }
                        return 1;
                    })))
                .then(Commands.literal("ignore_damage_sources").executes(c -> {
                        MutableComponent response = Component.literal("Use ");
                        MutableComponent subResponse = Component.literal("/tbm-server-edit ignore_damage_sources add/remove <type> ");
                        subResponse.setStyle(subResponse.getStyle().withColor(ChatFormatting.YELLOW));
                        response.getSiblings().add(subResponse);

                        subResponse = Component.literal("ignore_damage_sources is currently: [");
                        response.getSiblings().add(subResponse);

                        boolean isFirst = true;
                        for (String type : TurnBasedMinecraftMod.proxy.getConfig().getIgnoreHurtDamageSources()) {
                            if (!isFirst) {
                                response.getSiblings().add(Component.literal(", "));
                            }
                            subResponse = Component.literal(type);
                            subResponse.setStyle(subResponse.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit ignore_damage_sources remove " + type))
                                .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to remove type"))));
                            response.getSiblings().add(subResponse);
                            isFirst = false;
                        }
                        response.getSiblings().add(Component.literal("] "));

                        subResponse = Component.literal("Possible Damage Sources: [");
                        response.getSiblings().add(subResponse);

                        isFirst = true;
                        for (String type : TurnBasedMinecraftMod.proxy.getConfig().getPossibleIgnoreHurtDamageSources()) {
                            if (!isFirst) {
                                response.getSiblings().add(Component.literal(", "));
                            }
                            subResponse = Component.literal(type);
                            subResponse.setStyle(subResponse.getStyle()
                                .withColor(ChatFormatting.YELLOW)
                                .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit ignore_damage_sources add " + type))
                                .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to add type")
                                )));
                            response.getSiblings().add(subResponse);
                            isFirst = false;
                        }
                        response.getSiblings().add(Component.literal("] "));

                        c.getSource().sendSuccess(response, false);
                        return 1;
                    })
                    .then(Commands.literal("add").executes(c -> {
                            c.getSource().sendFailure(Component.literal("/tbm-server-edit ignore_damage_sources add <type>"));
                            return 1;
                        })
                        .then(Commands.argument("type", StringArgumentType.greedyString()).executes(c -> {
                            String type = StringArgumentType.getString(c, "type");
                            if (TurnBasedMinecraftMod.proxy.getConfig().addIgnoreHurtDamageSource(type)
                                && TurnBasedMinecraftMod.proxy.getConfig().updateConfigAppendToStringArray("server_config.ignore_damage_sources", type)) {
                                MutableComponent response = Component.literal("Successfully appended Damage Source type \"");

                                MutableComponent sub = Component.literal(type);
                                sub.setStyle(sub.getStyle().withColor(ChatFormatting.GREEN));
                                response.getSiblings().add(sub);

                                sub = Component.literal("\" to ignore_damage_sources");
                                response.getSiblings().add(sub);

                                c.getSource().sendSuccess(response, true);
                                return 1;
                            }

                            c.getSource().sendFailure(Component.literal(
                                "Failed to append type \"" + type + "\" to ignore_damage_sources"
                            ));
                            return 1;
                        })))
                    .then(Commands.literal("remove").executes(c -> {
                            c.getSource().sendFailure(Component.literal("/tbm-server-edit ignore_damage_sources remove <type>"));
                            return 1;
                        })
                        .then(Commands.argument("type", StringArgumentType.greedyString()).executes(c -> {
                            String type = StringArgumentType.getString(c, "type");
                            if (TurnBasedMinecraftMod.proxy.getConfig().removeIgnoreHurtDamageSource(type)
                                && TurnBasedMinecraftMod.proxy.getConfig().updateConfigRemoveFromStringArray("server_config.ignore_damage_sources", type)) {
                                MutableComponent response = Component.literal("Successfully removed category \"");

                                MutableComponent sub = Component.literal(type);
                                sub.setStyle(sub.getStyle().withColor(ChatFormatting.GREEN));
                                response.getSiblings().add(sub);

                                sub = Component.literal("\" from ignore_damage_sources");
                                response.getSiblings().add(sub);

                                c.getSource().sendSuccess(response, true);
                                return 1;
                            }

                            c.getSource().sendFailure(Component.literal("Failed to remove type \"" + type + "\" from ignore_damage_sources"));
                            return 1;
                        }))))
        );
    }

    @SubscribeEvent
    public void serverStopping(ServerStoppingEvent ignoredEvent) {
        logger.debug("About to cleanup BattleManager");
        if (proxy.cleanupBattleManager()) {
            logger.debug("Cleaned up BattleManager");
        }
    }
}
