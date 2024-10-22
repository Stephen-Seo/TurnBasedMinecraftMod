package com.burnedkirby.TurnBasedMinecraft.client;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.CommonProxy;
import com.burnedkirby.TurnBasedMinecraft.common.EntityInfo;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketBattleMessage;
import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketEditingMessage;
import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketGeneralMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class ClientProxy extends CommonProxy {
    private BattleGui battleGui = null;
    private BattleMusic battleMusic = null;
    private int battleMusicCount = 0;
    private int sillyMusicCount = 0;
    private Battle localBattle = null;

    @Override
    protected void initializeClient() {
        battleGui = new BattleGui();
        battleMusic = null; // will be initialized in postInit()
        battleMusicCount = 0;
        sillyMusicCount = 0;
        localBattle = null;
        logger.debug("Init client");
    }

    @Override
    public void setBattleGuiTime(int timeRemaining) {
        battleGui.setTimeRemaining(timeRemaining);
    }

    @Override
    public void setBattleGuiBattleChanged() {
        battleGui.battleChanged();
    }

    @Override
    public void setBattleGuiAsGui() {
        if (Minecraft.getInstance().screen != battleGui) {
            battleGui.turnEnd();
            Minecraft.getInstance().setScreen(battleGui);
        }
    }

    @Override
    public void setBattleGuiTurnTimerEnabled(boolean enabled) {
        battleGui.setTurnTimerEnabled(enabled);
    }

    @Override
    public void setBattleGuiTurnTimerMax(int timeMax) {
        battleGui.setTurnTimerMax(timeMax);
    }

    @Override
    public void battleGuiTurnBegin() {
        battleGui.turnBegin();
    }

    @Override
    public void battleGuiTurnEnd() {
        battleGui.turnEnd();
    }

    @Override
    public void battleStarted() {
        setBattleGuiAsGui();
    }

    @Override
    public void battleEnded() {
        localBattle = null;
        Minecraft.getInstance().setScreen(null);
        stopMusic(true);
        battleMusicCount = 0;
        sillyMusicCount = 0;
    }

    @Override
    protected void postInitClient() {
        battleMusic = new BattleMusic(getLogger());
    }

    @Override
    public void playBattleMusic() {
        Options gs = Minecraft.getInstance().options;
        float volume = ClientConfig.CLIENT.musicVolume.get().floatValue();
        if (ClientConfig.CLIENT.volumeAffectedByMasterVolume.get()) {
            volume *= gs.getSoundSourceVolume(SoundSource.MASTER);
        }
        if (ClientConfig.CLIENT.volumeAffectedByMusicVolume.get()) {
            volume *= gs.getSoundSourceVolume(SoundSource.MUSIC);
        }
        battleMusic.playBattle(volume);
    }

    @Override
    public void playSillyMusic() {
        Options gs = Minecraft.getInstance().options;
        float volume = ClientConfig.CLIENT.musicVolume.get().floatValue();
        if (ClientConfig.CLIENT.volumeAffectedByMasterVolume.get()) {
            volume *= gs.getSoundSourceVolume(SoundSource.MASTER);
        }
        if (ClientConfig.CLIENT.volumeAffectedByMusicVolume.get()) {
            volume *= gs.getSoundSourceVolume(SoundSource.MUSIC);
        }
        battleMusic.playSilly(volume);
    }

    @Override
    public void stopMusic(boolean resumeMCSounds) {
        battleMusic.stopMusic(resumeMCSounds);
    }

    /**
     * Sets what music to play based on type and loaded Config
     */
    @Override
    public void typeEnteredBattle(String type) {
        if (localBattle == null) {
            return;
        }
        if (type == null || type.isEmpty() || ClientConfig.CLIENT.battleMusicList.get().contains(type)) {
            ++battleMusicCount;
        } else if (ClientConfig.CLIENT.sillyMusicList.get().contains(type)) {
            ++sillyMusicCount;
        } else {
            ++battleMusicCount;
        }
        checkBattleTypes(false);
    }

    @Override
    public void typeLeftBattle(String type) {
        if (localBattle == null || localBattle.getSideA().isEmpty() || localBattle.getSideB().isEmpty()) {
            battleMusicCount = 0;
            sillyMusicCount = 0;
            return;
        } else if (type == null || type.isEmpty() || ClientConfig.CLIENT.battleMusicList.get().contains(type)) {
            --battleMusicCount;
        } else if (ClientConfig.CLIENT.sillyMusicList.get().contains(type)) {
            --sillyMusicCount;
        } else {
            --battleMusicCount;
        }
        checkBattleTypes(true);
    }

    @Override
    public void displayString(String message) {
        MutableComponent parentComponent = Component.empty();

        MutableComponent prefix = Component.literal("TBM: ");
        prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
        MutableComponent text = Component.literal(message);
        text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

        parentComponent.getSiblings().add(prefix);
        parentComponent.getSiblings().add(text);
        // UUID is required by sendMessage, but appears to be unused, so just give dummy UUID
        Minecraft.getInstance().player.sendSystemMessage(parentComponent);
    }

    @Override
    public void displayComponent(Component text) {
        MutableComponent parentComponent = Component.empty();

        MutableComponent prefix = Component.literal("TBM: ");
        prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));

        parentComponent.getSiblings().add(prefix);
        parentComponent.getSiblings().add(text);
        // UUID is required by sendMessage, but appears to be unused, so just give dummy UUID
        Minecraft.getInstance().player.sendSystemMessage(parentComponent);
    }

    private void checkBattleTypes(boolean entityLeft) {
        // check that battle is still valid
        if (localBattle == null && entityLeft && (localBattle.getSideA().isEmpty() || localBattle.getSideB().isEmpty())) {
            return;
        }

        float percentage = 0.0f;
        if (sillyMusicCount == 0 && battleMusicCount == 0) {
            percentage = 0.0f;
        } else if (battleMusicCount == 0) {
            percentage = 100.0f;
        } else {
            percentage = 100.0f * (float) sillyMusicCount / (float) (sillyMusicCount + battleMusicCount);
        }

        if (percentage >= ClientConfig.CLIENT.sillyMusicThreshold.get().floatValue()) {
            if (battleMusic.isPlaying()) {
                if (!battleMusic.isPlayingSilly() && battleMusic.hasSillyMusic()) {
                    stopMusic(false);
                    playSillyMusic();
                }
            } else if (battleMusic.hasSillyMusic()) {
                playSillyMusic();
            }
        } else {
            if (battleMusic.isPlaying()) {
                if (battleMusic.isPlayingSilly() && battleMusic.hasBattleMusic()) {
                    stopMusic(false);
                    playBattleMusic();
                }
            } else if (battleMusic.hasBattleMusic()) {
                playBattleMusic();
            }
        }
    }

    @Override
    public Battle getLocalBattle() {
        return localBattle;
    }

    @Override
    public void createLocalBattle(int id) {
        localBattle = new Battle(null, id, null, null, false, Minecraft.getInstance().level.dimension());
    }

    @Override
    public Entity getEntity(int id, ResourceKey<Level> dim) {
        return Minecraft.getInstance().level.getEntity(id);
    }

    @Override
    public <MSG> void handlePacket(MSG msg, CustomPayloadEvent.Context ctx) {
        if (msg.getClass() == PacketBattleMessage.class) {
            PacketBattleMessage pkt = (PacketBattleMessage) msg;
            Entity fromEntity = getEntity(pkt.getEntityIDFrom(), pkt.getDimension());
            Component from = Component.literal("Unknown");
            if (fromEntity != null) {
                from = fromEntity.getDisplayName();
            } else if (TurnBasedMinecraftMod.proxy.getLocalBattle() != null) {
                fromEntity = TurnBasedMinecraftMod.proxy.getLocalBattle().getCombatantEntity(pkt.getEntityIDFrom());
                if (fromEntity != null) {
                    from = fromEntity.getDisplayName();
                }
            }
            Entity toEntity = TurnBasedMinecraftMod.proxy.getEntity(pkt.getEntityIDTo(), pkt.getDimension());
            Component to = Component.literal("Unknown");
            if (toEntity != null) {
                to = toEntity.getDisplayName();
            } else if (TurnBasedMinecraftMod.proxy.getLocalBattle() != null) {
                toEntity = TurnBasedMinecraftMod.proxy.getLocalBattle().getCombatantEntity(pkt.getEntityIDTo());
                if (toEntity != null) {
                    to = toEntity.getDisplayName();
                }
            }

            MutableComponent parentComponent = Component.empty();
            switch (pkt.getMessageType()) {
                case ENTERED:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" entered battle!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    if (TurnBasedMinecraftMod.proxy.getLocalBattle() == null || TurnBasedMinecraftMod.proxy.getLocalBattle().getId() != pkt.getAmount()) {
                        TurnBasedMinecraftMod.proxy.createLocalBattle(pkt.getAmount());
                    }
                    TurnBasedMinecraftMod.proxy.battleStarted();
                    TurnBasedMinecraftMod.proxy.typeEnteredBattle(pkt.getCustom());
                    break;
                case FLEE:
                    if (pkt.getAmount() != 0) {
                        parentComponent.getSiblings().add(from);
                        parentComponent.getSiblings().add(Component.literal(" fled battle!"));
                        TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                        TurnBasedMinecraftMod.proxy.typeLeftBattle(pkt.getCustom());
                    } else {
                        parentComponent.getSiblings().add(from);
                        parentComponent.getSiblings().add(Component.literal(" tried to flee battle but failed!"));
                        TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    }
                    break;
                case DIED:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" died in battle!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    TurnBasedMinecraftMod.proxy.typeLeftBattle(pkt.getCustom());
                    break;
                case ENDED:
                    TurnBasedMinecraftMod.proxy.displayString("Battle has ended!");
                    TurnBasedMinecraftMod.proxy.battleEnded();
                    break;
                case ATTACK:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" attacked "));
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(Component.literal(" and dealt "));
                    parentComponent.getSiblings().add(Component.literal(Integer.valueOf(pkt.getAmount()).toString()));
                    parentComponent.getSiblings().add(Component.literal(" damage!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case DEFEND:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" blocked "));
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(Component.literal("'s attack!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case DEFENSE_DAMAGE:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" retaliated from "));
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(Component.literal("'s attack and dealt "));
                    parentComponent.getSiblings().add(Component.literal(Integer.valueOf(pkt.getAmount()).toString()));
                    parentComponent.getSiblings().add(Component.literal(" damage!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case MISS:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" attacked "));
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(Component.literal(" but missed!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case DEFENDING:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" is defending!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case DID_NOTHING:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" did nothing!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case USED_ITEM:
                    parentComponent.getSiblings().add(from);
                    switch (PacketBattleMessage.UsedItemAction.valueOf(pkt.getAmount())) {
                        case USED_NOTHING:
                            parentComponent.getSiblings().add(Component.literal(" tried to use nothing!"));
                            TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                            break;
                        case USED_INVALID:
                            if (pkt.getCustom().length() > 0) {
                                parentComponent.getSiblings().add(Component.literal(" tried to use "));
                                parentComponent.getSiblings().add(Component.literal(pkt.getCustom()));
                                parentComponent.getSiblings().add(Component.literal("!"));
                                TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                            } else {
                                parentComponent.getSiblings().add(Component.literal(" tried to use an item!"));
                                TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                            }
                            break;
                        case USED_FOOD:
                            parentComponent.getSiblings().add(Component.literal(" ate a "));
                            parentComponent.getSiblings().add(Component.literal(pkt.getCustom()));
                            parentComponent.getSiblings().add(Component.literal("!"));
                            TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                            break;
                        case USED_POTION:
                            parentComponent.getSiblings().add(Component.literal(" drank a "));
                            parentComponent.getSiblings().add(Component.literal(pkt.getCustom()));
                            parentComponent.getSiblings().add(Component.literal("!"));
                            TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                            break;
                    }
                    break;
                case TURN_BEGIN:
                    TurnBasedMinecraftMod.proxy.displayString("The turn begins!");
                    if (TurnBasedMinecraftMod.proxy.getLocalBattle() == null || TurnBasedMinecraftMod.proxy.getLocalBattle().getId() != pkt.getAmount()) {
                        TurnBasedMinecraftMod.proxy.createLocalBattle(pkt.getAmount());
                    }
                    TurnBasedMinecraftMod.proxy.battleStarted();
                    TurnBasedMinecraftMod.proxy.battleGuiTurnBegin();
                    break;
                case TURN_END:
                    if (TurnBasedMinecraftMod.proxy.getLocalBattle() != null) {
                        if (pkt.getAmount() == 0) {
                            TurnBasedMinecraftMod.proxy.displayString("The turn ended!");
                        } else {
                            TurnBasedMinecraftMod.proxy.displayString("The turn ended (abnormally due to internal error)!");
                        }
                    }
                    TurnBasedMinecraftMod.proxy.battleGuiTurnEnd();
                    break;
                case SWITCHED_ITEM:
                    if (pkt.getAmount() != 0) {
                        parentComponent.getSiblings().add(from);
                        parentComponent.getSiblings().add(Component.literal(" switched to a different item!"));
                        TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    } else {
                        parentComponent.getSiblings().add(from);
                        parentComponent.getSiblings().add(Component.literal(" switched to a different item but failed because it was invalid!"));
                        TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    }
                    break;
                case WAS_AFFECTED:
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(Component.literal(" was " + pkt.getCustom() + " by "));
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal("!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case BECAME_CREATIVE:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" entered creative mode and left battle!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case FIRED_ARROW:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" let loose an arrow towards "));
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(Component.literal("!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case ARROW_HIT:
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(Component.literal(" was hit by "));
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal("'s arrow!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case BOW_NO_AMMO:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" tried to use their bow but ran out of ammo!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case CREEPER_WAIT: {
                    parentComponent.getSiblings().add(from);
                    MutableComponent message = Component.literal(" is charging up!");
                    message.setStyle(message.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)));
                    parentComponent.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                }
                break;
                case CREEPER_WAIT_FINAL: {
                    parentComponent.getSiblings().add(from);
                    MutableComponent message = Component.literal(" is about to explode!");
                    message.setStyle(message.getStyle().withColor(TextColor.fromRgb(0xFFFF5050)));
                    parentComponent.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                }
                break;
                case CREEPER_EXPLODE: {
                    parentComponent.getSiblings().add(from);
                    MutableComponent message = Component.literal(" exploded!");
                    message.setStyle(message.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                    parentComponent.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                }
                break;
                case CROSSBOW_NO_AMMO: {
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(Component.literal(" tried to use their crossbow but ran out of ammo!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                }
                break;
            }
        } else if (msg.getClass() == PacketGeneralMessage.class) {
            PacketGeneralMessage pkt = (PacketGeneralMessage) msg;
            displayString(pkt.getMessage());
        } else if (msg.getClass() == PacketEditingMessage.class) {
            PacketEditingMessage pkt = (PacketEditingMessage) msg;
            MutableComponent parentComponent = Component.empty();
            switch (pkt.getType()) {
                case ATTACK_ENTITY: {
                    MutableComponent text = Component.literal("Attack the entity you want to edit for TurnBasedMinecraftMod. ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    MutableComponent cancel = Component.literal("Cancel");
                    cancel.setStyle(cancel.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel")));

                    parentComponent.getSiblings().add(text);
                    parentComponent.getSiblings().add(cancel);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case PICK_EDIT: {
                    MutableComponent text = Component.literal("Edit what value? ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    MutableComponent option = Component.literal("IgB");
                    // HoverEvent.Action.SHOW_TEXT is probably SHOW_TEXT
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("IgnoreBattle"))));
                    MutableComponent value = Component.literal("(" + pkt.getEntityInfo().ignoreBattle + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("AP");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("AttackPower"))));
                    value = Component.literal("(" + pkt.getEntityInfo().attackPower + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("APr");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("AttackProbability"))));
                    value = Component.literal("(" + pkt.getEntityInfo().attackProbability + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("AV");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("AttackVariance"))));
                    value = Component.literal("(" + pkt.getEntityInfo().attackVariance + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("AE");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("AttackEffect"))));
                    value = Component.literal("(" + pkt.getEntityInfo().attackEffect.toString() + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("AEPr");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("AttackEffectProbability"))));
                    value = Component.literal("(" + pkt.getEntityInfo().attackEffectProbability + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("DD");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("DefenseDamage"))));
                    value = Component.literal("(" + pkt.getEntityInfo().defenseDamage + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("DDPr");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("DefenseDamageProbability"))));
                    value = Component.literal("(" + pkt.getEntityInfo().defenseDamageProbability + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("E");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Evasion"))));
                    value = Component.literal("(" + pkt.getEntityInfo().evasion + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("S");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Speed"))));
                    value = Component.literal("(" + pkt.getEntityInfo().speed + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("C");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Category"))));
                    value = Component.literal("(" + pkt.getEntityInfo().category + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("DecA");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("DecisionAttack"))));
                    value = Component.literal("(" + pkt.getEntityInfo().decisionAttack + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("DecD");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("DecisionDefend"))));
                    value = Component.literal("(" + pkt.getEntityInfo().decisionDefend + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("DecF");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("DecisionFlee"))));
                    value = Component.literal("(" + pkt.getEntityInfo().decisionFlee + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = Component.literal("Finished Editing");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit finish")));
                    text.getSiblings().add(option);
                    text.getSiblings().add(Component.literal(" "));

                    option = Component.literal("Cancel");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel")));
                    text.getSiblings().add(option);

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case SERVER_EDIT: {
                    MutableComponent parent = Component.literal("Edit what server value? ");
                    parent.setStyle(parent.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    MutableComponent sub = Component.literal("leave_battle_cooldown ");
                    sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW).withBold(true));
                    parent.getSiblings().add(sub);

                    for (int i = 1; i <= 10; ++i) {
                        sub = Component.literal(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit leave_battle_cooldown " + i)));
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("aggro_start_battle_max_distance ");
                    sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW).withBold(true));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("5 ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit aggro_start_battle_max_distance 5")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("8 ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit aggro_start_battle_max_distance 8")));
                    parent.getSiblings().add(sub);

                    for (int i = 10; i <= 50; i += 5) {
                        sub = Component.literal(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit aggro_start_battle_max_distance " + String.valueOf(i))));
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("old_battle_behavior ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("If enabled, battles only start on a hit, not including mobs targeting players")))
                        .withBold(true));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("true ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit old_battle_behavior true")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("false ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit old_battle_behavior false")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("anyone_can_disable_tbm_for_self ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Allows use for /tbm-disable and /tbm-enable for all")))
                        .withBold(true));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("true ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit anyone_can_disable_tbm_for_self true")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("false ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit anyone_can_disable_tbm_for_self false")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("max_in_battle ");
                    sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW).withBold(true));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("2 ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit max_in_battle 2")));
                    parent.getSiblings().add(sub);

                    for (int i = 5; i < 30; i += 5) {
                        sub = Component.literal(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit max_in_battle " + String.valueOf(i))));
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("freeze_battle_combatants ");
                    sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW).withBold(true));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("true ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit freeze_battle_combatants true"
                        )));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("false ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit freeze_battle_combatants false"
                        )));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("ignore_battle_types ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.DARK_GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit ignore_battle_types"))
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Click to show current ignored categories, or use /tbm-server-edit ignore_battle_types add/remove <category_name>")
                        ))
                        .withBold(true));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("player_speed ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Player default speed"))));
                    parent.getSiblings().add(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        sub = Component.literal(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit player_speed " + i)));
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("player_haste_speed ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Player speed when under the effects of \"Speed\"")
                        )));
                    parent.getSiblings().add(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        sub = Component.literal(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit player_haste_speed " + i)));
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("player_slow_speed ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Player speed when under the effects of \"Slow\"")
                        )));
                    parent.getSiblings().add(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        sub = Component.literal(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit player_slow_speed " + i)));
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("player_attack_probability ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Base Player attack probability in percentage")
                        )));
                    parent.getSiblings().add(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        if (i == 0) {
                            sub = Component.literal("1 ");
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit player_attack_probability 1")));
                        } else {
                            sub = Component.literal(String.valueOf(i) + ' ');
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit player_attack_probability " + i)));
                        }
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("player_evasion ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Base Player evasion rate in percentage")
                        )));
                    parent.getSiblings().add(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        sub = Component.literal(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit player_evasion " + i)));
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("defense_duration ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Number of attacks that a \"Defend\" move blocks (lasts until next action)")
                        )));
                    parent.getSiblings().add(sub);

                    for (int i = 0; i <= 5; ++i) {
                        sub = Component.literal(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit defense_duration " + i)));
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("flee_good_probability ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Probability of flee success when Player's speed is higher than the fastest opposing Entity")
                        )));
                    parent.getSiblings().add(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        if (i == 0) {
                            sub = Component.literal("1 ");
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit flee_good_probability 1")));
                        } else {
                            sub = Component.literal(String.valueOf(i) + ' ');
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit flee_good_probability " + i)));
                        }
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("flee_bad_probability ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Probability of flee success when Player's speed is lower than the fastest opposing Entity")
                        )));
                    parent.getSiblings().add(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        if (i == 0) {
                            sub = Component.literal("1 ");
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit flee_bad_probability 1")));
                        } else {
                            sub = Component.literal(String.valueOf(i) + ' ');
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit flee_bad_probability " + i)));
                        }
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("minimum_hit_percentage ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("The minimum percentage possible when calculating hit percentage for any attacker")
                        )));
                    parent.getSiblings().add(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        if (i == 0) {
                            sub = Component.literal("1 ");
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit minimum_hit_percentage 1")));
                        } else {
                            sub = Component.literal(String.valueOf(i) + ' ');
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit minimum_hit_percentage " + i)));
                        }
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("battle_turn_wait_forever ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Disables the turn timer (recommended to leave this to false)"))
                        ));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("true ");
                    sub.setStyle(sub.getStyle().withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/tbm-server-edit battle_turn_wait_forever true")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("false ");
                    sub.setStyle(sub.getStyle().withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/tbm-server-edit battle_turn_wait_forever false")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("battle_turn_time_seconds ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("The time in seconds to wait for all Players to choose their move")
                        )));
                    parent.getSiblings().add(sub);

                    for (int i = 5; i <= 60; i += 5) {
                        sub = Component.literal(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit battle_turn_time_seconds " + i)));
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("creeper_explode_turn ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("The number of turns it takes for a creeper to explode")
                        )));
                    parent.getSiblings().add(sub);

                    for (int i = 1; i <= 10; ++i) {
                        sub = Component.literal(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit creeper_explode_turn " + i)));
                        parent.getSiblings().add(sub);
                    }

                    sub = Component.literal("creeper_stop_explode_on_leave_battle ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Stops creepers from exploding when they leave battle (during leave battle cooldown)")
                        )));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("true ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit creeper_stop_explode_on_leave_battle true")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("false ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit creeper_stop_explode_on_leave_battle false")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("creeper_always_allow_damage ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Allows creepers to damage anyone who just left battle (in cooldown)")
                        )));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("true ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit creeper_always_allow_damage true")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("false ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit creeper_always_allow_damage false")));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("ignore_damage_sources ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.DARK_GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit ignore_damage_sources"))
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Click to show current ignored damage sources (during battle), or use /tbm-server-edit ignore_damage_sources add/remove <type>")
                        ))
                        .withBold(true));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("player_only_battles ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Disables battle for non-player entities")
                        )));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("enable ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit player_only_battles true"
                        )));
                    parent.getSiblings().add(sub);

                    sub = Component.literal("disable ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit player_only_battles false"
                        )));
                    parent.getSiblings().add(sub);

                    TurnBasedMinecraftMod.proxy.displayComponent(parent);
                    break;
                }
                case EDIT_IGNORE_BATTLE: {
                    MutableComponent text = Component.literal("ignoreBattle: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    MutableComponent option = Component.literal("true");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle true")));
                    text.getSiblings().add(option);

                    text.getSiblings().add(Component.literal(" "));

                    option = Component.literal("false");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle false")));
                    text.getSiblings().add(option);

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_ATTACK_POWER: {
                    MutableComponent text = Component.literal("attackPower: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 15; ++i) {
                        MutableComponent option = Component.literal(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 15) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    text.getSiblings().add(Component.literal(" (or use command \"/tbm-edit edit attackPower <integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_ATTACK_PROBABILITY: {
                    MutableComponent text = Component.literal("attackProbability: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 10; i <= 100; i += 10) {
                        MutableComponent option = Component.literal(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    text.getSiblings().add(Component.literal(" (or use command \"/tbm-edit edit attackProbability <percentage-integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_ATTACK_VARIANCE: {
                    MutableComponent text = Component.literal("attackVariance: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 10; ++i) {
                        MutableComponent option = Component.literal(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 10) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    text.getSiblings().add(Component.literal(" (or use command \"/tbm-edit edit attackVariance <integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_ATTACK_EFFECT: {
                    MutableComponent text = Component.literal("attackEffect: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (EntityInfo.Effect e : EntityInfo.Effect.values()) {
                        MutableComponent option = Component.literal(e.toString());
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect " + e.toString())));
                        text.getSiblings().add(option);
                        if (e != EntityInfo.Effect.UNKNOWN) {
                            // TODO find a better way to handle printing comma for items before last
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_ATTACK_EFFECT_PROBABILITY: {
                    MutableComponent text = Component.literal("attackEffectProbability: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        MutableComponent option = Component.literal(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    text.getSiblings().add(Component.literal(" (or use command \"/tbm-edit edit attackEffectProbability <percentage-integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_DEFENSE_DAMAGE: {
                    MutableComponent text = Component.literal("defenseDamage: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 15; ++i) {
                        MutableComponent option = Component.literal(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 15) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    text.getSiblings().add(Component.literal(" (or use command \"/tbm-edit edit defenseDamage <integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_DEFENSE_DAMAGE_PROBABILITY: {
                    MutableComponent text = Component.literal("defenseDamageProbability: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        MutableComponent option = Component.literal(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    text.getSiblings().add(Component.literal(" (or use command \"/tbm-edit edit defenseDamageProbability <percentage-integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_EVASION: {
                    MutableComponent text = Component.literal("evasion: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        MutableComponent option = Component.literal(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    text.getSiblings().add(Component.literal(" (or use command \"/tbm-edit edit evasion <percentage-integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_SPEED: {
                    MutableComponent text = Component.literal("speed: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        MutableComponent option = Component.literal(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    text.getSiblings().add(Component.literal(" (or use command \"/tbm-edit edit speed <integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_CATEGORY: {
                    MutableComponent text = Component.literal("category: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    MutableComponent option = Component.literal("monster");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category monster")));
                    if (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("monster")) {
                        MutableComponent optionInfo = Component.literal("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        MutableComponent optionInfoBool = Component.literal("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(Component.literal(")"));
                        option.getSiblings().add(optionInfo);
                    } else {
                        MutableComponent optionInfo = Component.literal("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        MutableComponent optionInfoBool = Component.literal("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(Component.literal(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(Component.literal(", "));

                    option = Component.literal("animal");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category animal")));
                    if (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("animal")) {
                        MutableComponent optionInfo = Component.literal("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        MutableComponent optionInfoBool = Component.literal("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(Component.literal(")"));
                        option.getSiblings().add(optionInfo);
                    } else {
                        MutableComponent optionInfo = Component.literal("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        MutableComponent optionInfoBool = Component.literal("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(Component.literal(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(Component.literal(", "));

                    option = Component.literal("passive");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category passive")));
                    if (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("passive")) {
                        MutableComponent optionInfo = Component.literal("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        MutableComponent optionInfoBool = Component.literal("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(Component.literal(")"));
                        option.getSiblings().add(optionInfo);
                    } else {
                        MutableComponent optionInfo = Component.literal("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        MutableComponent optionInfoBool = Component.literal("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(Component.literal(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(Component.literal(", "));

                    option = Component.literal("boss");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category boss")));
                    if (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("boss")) {
                        MutableComponent optionInfo = Component.literal("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        MutableComponent optionInfoBool = Component.literal("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(Component.literal(")"));
                        option.getSiblings().add(optionInfo);
                    } else {
                        MutableComponent optionInfo = Component.literal("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        MutableComponent optionInfoBool = Component.literal("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(Component.literal(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(Component.literal(", "));

                    option = Component.literal("player");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category player")));
                    if (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("player")) {
                        MutableComponent optionInfo = Component.literal("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        MutableComponent optionInfoBool = Component.literal("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(Component.literal(")"));
                        option.getSiblings().add(optionInfo);
                    } else {
                        MutableComponent optionInfo = Component.literal("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        MutableComponent optionInfoBool = Component.literal("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(Component.literal(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);

                    text.getSiblings().add(Component.literal(" (or use command \"/tbm-edit edit category <string>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_DECISION_ATTACK: {
                    MutableComponent text = Component.literal("decisionAttack: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        MutableComponent option = Component.literal(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_DECISION_DEFEND: {
                    MutableComponent text = Component.literal("decisionDefend: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        MutableComponent option = Component.literal(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_DECISION_FLEE: {
                    MutableComponent text = Component.literal("decisionFlee: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        MutableComponent option = Component.literal(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(Component.literal(", "));
                        }
                    }

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                default:
                    break;
            }
        }
    }

    @Override
    public void showClientConfigGui() {
        Minecraft.getInstance().setScreen(new ClientConfigGui());
    }
}