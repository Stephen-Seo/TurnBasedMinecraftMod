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
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.TextComponent;
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
    public static final String VERSION = "1.18.2";
    public static final String CONFIG_FILENAME = "TBM_Config.toml";
    public static final String DEFAULT_CONFIG_FILENAME = "TBM_Config_DEFAULT.toml";
    public static final String CONFIG_DIRECTORY = "config/TurnBasedMinecraft/";
    public static final String CONFIG_FILE_PATH = CONFIG_DIRECTORY + CONFIG_FILENAME;
    public static final String DEFAULT_CONFIG_FILE_PATH = CONFIG_DIRECTORY + DEFAULT_CONFIG_FILENAME;
    public static final String CONFIG_INTERNAL_PATH = "/assets/com_burnedkirby_turnbasedminecraft/" + CONFIG_FILENAME;
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
                    c.getSource().sendSuccess(new TextComponent("Disabled turn-based-combat for current player"), true);
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
                    c.getSource().sendSuccess(new TextComponent("Enabled turn-based-combat for current player"), true);
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
                        c.getSource().sendSuccess(new TextComponent("Enabled turn-based-combat for " + player.getDisplayName().getString()), true);
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
                        c.getSource().sendSuccess(new TextComponent("Disabled turn-based-combat for " + player.getDisplayName().getString()), true);
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
    }

    @SubscribeEvent
    public void serverStopping(ServerStoppingEvent event) {
        logger.debug("About to cleanup BattleManager");
        if (proxy.cleanupBattleManager()) {
            logger.debug("Cleaned up BattleManager");
        }
    }
}
