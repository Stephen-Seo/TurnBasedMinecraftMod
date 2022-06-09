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
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

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
        battleMusic.playBattle(gs.getSoundSourceVolume(SoundSource.MUSIC) * gs.getSoundSourceVolume(SoundSource.MASTER));
    }

    @Override
    public void playSillyMusic() {
        Options gs = Minecraft.getInstance().options;
        battleMusic.playSilly(gs.getSoundSourceVolume(SoundSource.MUSIC) * gs.getSoundSourceVolume(SoundSource.MASTER));
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
        if (type == null || type.isEmpty() || getConfig().isBattleMusicType(type)) {
            ++battleMusicCount;
        } else if (getConfig().isSillyMusicType(type)) {
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
        } else if (type == null || type.isEmpty() || getConfig().isBattleMusicType(type)) {
            --battleMusicCount;
        } else if (getConfig().isSillyMusicType(type)) {
            --sillyMusicCount;
        } else {
            --battleMusicCount;
        }
        checkBattleTypes(true);
    }

    @Override
    public void displayString(String message) {
        Component parentComponent = new TextComponent("");

        TextComponent prefix = new TextComponent("TBM: ");
        prefix.withStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
        TextComponent text = new TextComponent(message);
        text.withStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

        parentComponent.getSiblings().add(prefix);
        parentComponent.getSiblings().add(text);
        // UUID is required by sendMessage, but appears to be unused, so just give dummy UUID
        Minecraft.getInstance().player.sendMessage(parentComponent, new UUID(0, 0));
    }

    @Override
    public void displayComponent(Component text) {
        Component parentComponent = new TextComponent("");

        TextComponent prefix = new TextComponent("TBM: ");
        prefix.withStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));

        parentComponent.getSiblings().add(prefix);
        parentComponent.getSiblings().add(text);
        // UUID is required by sendMessage, but appears to be unused, so just give dummy UUID
        Minecraft.getInstance().player.sendMessage(parentComponent, new UUID(0, 0));
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

        if (percentage >= (float) getConfig().getSillyMusicThreshold()) {
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
    public <MSG> void handlePacket(MSG msg, Supplier<NetworkEvent.Context> ctx) {
        if (msg.getClass() == PacketBattleMessage.class) {
            PacketBattleMessage pkt = (PacketBattleMessage) msg;
            Entity fromEntity = getEntity(pkt.getEntityIDFrom(), pkt.getDimension());
            Component from = new TextComponent("Unknown");
            if (fromEntity != null) {
                from = fromEntity.getDisplayName();
            } else if (TurnBasedMinecraftMod.proxy.getLocalBattle() != null) {
                fromEntity = TurnBasedMinecraftMod.proxy.getLocalBattle().getCombatantEntity(pkt.getEntityIDFrom());
                if (fromEntity != null) {
                    from = fromEntity.getDisplayName();
                }
            }
            Entity toEntity = TurnBasedMinecraftMod.proxy.getEntity(pkt.getEntityIDTo(), pkt.getDimension());
            Component to = new TextComponent("Unknown");
            if (toEntity != null) {
                to = toEntity.getDisplayName();
            } else if (TurnBasedMinecraftMod.proxy.getLocalBattle() != null) {
                toEntity = TurnBasedMinecraftMod.proxy.getLocalBattle().getCombatantEntity(pkt.getEntityIDTo());
                if (toEntity != null) {
                    to = toEntity.getDisplayName();
                }
            }

            Component parentComponent = new TextComponent("");
            switch (pkt.getMessageType()) {
                case ENTERED:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" entered battle!"));
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
                        parentComponent.getSiblings().add(new TextComponent(" fled battle!"));
                        TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                        TurnBasedMinecraftMod.proxy.typeLeftBattle(pkt.getCustom());
                    } else {
                        parentComponent.getSiblings().add(from);
                        parentComponent.getSiblings().add(new TextComponent(" tried to flee battle but failed!"));
                        TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    }
                    break;
                case DIED:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" died in battle!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    TurnBasedMinecraftMod.proxy.typeLeftBattle(pkt.getCustom());
                    break;
                case ENDED:
                    TurnBasedMinecraftMod.proxy.displayString("Battle has ended!");
                    TurnBasedMinecraftMod.proxy.battleEnded();
                    break;
                case ATTACK:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" attacked "));
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(new TextComponent(" and dealt "));
                    parentComponent.getSiblings().add(new TextComponent(Integer.valueOf(pkt.getAmount()).toString()));
                    parentComponent.getSiblings().add(new TextComponent(" damage!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case DEFEND:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" blocked "));
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(new TextComponent("'s attack!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case DEFENSE_DAMAGE:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" retaliated from "));
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(new TextComponent("'s attack and dealt "));
                    parentComponent.getSiblings().add(new TextComponent(Integer.valueOf(pkt.getAmount()).toString()));
                    parentComponent.getSiblings().add(new TextComponent(" damage!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case MISS:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" attacked "));
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(new TextComponent(" but missed!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case DEFENDING:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" is defending!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case DID_NOTHING:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" did nothing!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case USED_ITEM:
                    parentComponent.getSiblings().add(from);
                    switch (PacketBattleMessage.UsedItemAction.valueOf(pkt.getAmount())) {
                        case USED_NOTHING:
                            parentComponent.getSiblings().add(new TextComponent(" tried to use nothing!"));
                            TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                            break;
                        case USED_INVALID:
                            if (pkt.getCustom().length() > 0) {
                                parentComponent.getSiblings().add(new TextComponent(" tried to consume "));
                                parentComponent.getSiblings().add(new TextComponent(pkt.getCustom()));
                                parentComponent.getSiblings().add(new TextComponent(" and failed!"));
                                TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                            } else {
                                parentComponent.getSiblings().add(new TextComponent(" tried to consume an invalid item and failed!"));
                                TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                            }
                            break;
                        case USED_FOOD:
                            parentComponent.getSiblings().add(new TextComponent(" ate a "));
                            parentComponent.getSiblings().add(new TextComponent(pkt.getCustom()));
                            parentComponent.getSiblings().add(new TextComponent("!"));
                            TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                            break;
                        case USED_POTION:
                            parentComponent.getSiblings().add(new TextComponent(" drank a "));
                            parentComponent.getSiblings().add(new TextComponent(pkt.getCustom()));
                            parentComponent.getSiblings().add(new TextComponent("!"));
                            TurnBasedMinecraftMod.proxy.displayString(from + " drank a " + pkt.getCustom() + "!");
                            break;
                    }
                    break;
                case TURN_BEGIN:
                    TurnBasedMinecraftMod.proxy.displayString("The turn begins!");
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
                        parentComponent.getSiblings().add(new TextComponent(" switched to a different item!"));
                        TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    } else {
                        parentComponent.getSiblings().add(from);
                        parentComponent.getSiblings().add(new TextComponent(" switched to a different item but failed because it was invalid!"));
                        TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    }
                    break;
                case WAS_AFFECTED:
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(new TextComponent(" was " + pkt.getCustom() + " by "));
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent("!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case BECAME_CREATIVE:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" entered creative mode and left battle!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case FIRED_ARROW:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" let loose an arrow towards "));
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(new TextComponent("!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case ARROW_HIT:
                    parentComponent.getSiblings().add(to);
                    parentComponent.getSiblings().add(new TextComponent(" was hit by "));
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent("'s arrow!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case BOW_NO_AMMO:
                    parentComponent.getSiblings().add(from);
                    parentComponent.getSiblings().add(new TextComponent(" tried to use their bow but ran out of ammo!"));
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                case CREEPER_WAIT: {
                    parentComponent.getSiblings().add(from);
                    TextComponent message = new TextComponent(" is charging up!");
                    message.setStyle(message.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)));
                    parentComponent.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                }
                break;
                case CREEPER_WAIT_FINAL: {
                    parentComponent.getSiblings().add(from);
                    TextComponent message = new TextComponent(" is about to explode!");
                    message.setStyle(message.getStyle().withColor(TextColor.fromRgb(0xFFFF5050)));
                    parentComponent.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                }
                break;
                case CREEPER_EXPLODE: {
                    parentComponent.getSiblings().add(from);
                    TextComponent message = new TextComponent(" exploded!");
                    message.setStyle(message.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                    parentComponent.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                }
                break;
            }
        } else if (msg.getClass() == PacketGeneralMessage.class) {
            PacketGeneralMessage pkt = (PacketGeneralMessage) msg;
            displayString(pkt.getMessage());
        } else if (msg.getClass() == PacketEditingMessage.class) {
            PacketEditingMessage pkt = (PacketEditingMessage) msg;
            Component parentComponent = new TextComponent("");
            switch (pkt.getType()) {
                case ATTACK_ENTITY: {
                    TextComponent text = new TextComponent("Attack the entity you want to edit for TurnBasedMinecraftMod. ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    TextComponent cancel = new TextComponent("Cancel");
                    cancel.setStyle(cancel.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel")));

                    parentComponent.getSiblings().add(text);
                    parentComponent.getSiblings().add(cancel);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case PICK_EDIT: {
                    TextComponent text = new TextComponent("Edit what value? ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    TextComponent option = new TextComponent("IgB");
                    // HoverEvent.Action.SHOW_TEXT is probably SHOW_TEXT
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("IgnoreBattle"))));
                    TextComponent value = new TextComponent("(" + pkt.getEntityInfo().ignoreBattle + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("AP");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("AttackPower"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().attackPower + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("APr");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("AttackProbability"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().attackProbability + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("AV");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("AttackVariance"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().attackVariance + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("AE");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("AttackEffect"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().attackEffect.toString() + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("AEPr");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("AttackEffectProbability"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().attackEffectProbability + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("DD");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("DefenseDamage"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().defenseDamage + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("DDPr");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("DefenseDamageProbability"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().defenseDamageProbability + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("E");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Evasion"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().evasion + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("S");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Speed"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().speed + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("C");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Category"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().category + ") ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("DecA");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("DecisionAttack"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().decisionAttack + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("DecD");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("DecisionDefend"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().decisionDefend + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("DecF");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("DecisionFlee"))));
                    value = new TextComponent("(" + pkt.getEntityInfo().decisionFlee + "%) ");
                    value.setStyle(value.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new TextComponent("Finished Editing");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit finish")));
                    text.getSiblings().add(option);
                    text.getSiblings().add(new TextComponent(" "));

                    option = new TextComponent("Cancel");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel")));
                    text.getSiblings().add(option);

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case SERVER_EDIT: {
                    TextComponent parent = new TextComponent("Edit what server value? ");
                    parent.setStyle(parent.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    TextComponent sub = new TextComponent("leave_battle_cooldown ");
                    sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW).withBold(true));
                    parent.append(sub);

                    for (int i = 1; i <= 10; ++i) {
                        sub = new TextComponent(String.valueOf(i) + ' ');
                        sub.setStyle(
                            sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit leave_battle_cooldown " + i)));
                        parent.append(sub);
                    }

                    sub = new TextComponent("aggro_start_battle_max_distance ");
                    sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW).withBold(true));
                    parent.append(sub);

                    sub = new TextComponent("5 ");
                    sub.setStyle(
                        sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit aggro_start_battle_max_distance 5")));
                    parent.append(sub);

                    sub = new TextComponent("8 ");
                    sub.setStyle(
                        sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit aggro_start_battle_max_distance 8")));
                    parent.append(sub);

                    for (int i = 10; i <= 50; i += 5) {
                        sub = new TextComponent(String.valueOf(i) + ' ');
                        sub.setStyle(
                            sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit aggro_start_battle_max_distance " + String.valueOf(i))));
                        parent.append(sub);
                    }

                    sub = new TextComponent("old_battle_behavior ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("If enabled, battles only start on a hit, not including mobs targeting players")))
                        .withBold(true));
                    parent.append(sub);

                    sub = new TextComponent("true ");
                    sub.setStyle(
                        sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit old_battle_behavior true")));
                    parent.append(sub);

                    sub = new TextComponent("false ");
                    sub.setStyle(
                        sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit old_battle_behavior false")));
                    parent.append(sub);

                    sub = new TextComponent("anyone_can_disable_tbm_for_self ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Allows use for /tbm-disable and /tbm-enable for all")))
                        .withBold(true));
                    parent.append(sub);

                    sub = new TextComponent("true ");
                    sub.setStyle(
                        sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit anyone_can_disable_tbm_for_self true")));
                    parent.append(sub);

                    sub = new TextComponent("false ");
                    sub.setStyle(
                        sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit anyone_can_disable_tbm_for_self false")));
                    parent.append(sub);

                    sub = new TextComponent("max_in_battle ");
                    sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW).withBold(true));
                    parent.append(sub);

                    sub = new TextComponent("2 ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit max_in_battle 2")));
                    parent.append(sub);

                    for (int i = 5; i < 30; i += 5) {
                        sub = new TextComponent(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit max_in_battle " + String.valueOf(i))));
                        parent.append(sub);
                    }

                    sub = new TextComponent("freeze_battle_combatants ");
                    sub.setStyle(sub.getStyle().withColor(ChatFormatting.YELLOW).withBold(true));
                    parent.append(sub);

                    sub = new TextComponent("true ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit freeze_battle_combatants true"
                        )));
                    parent.append(sub);

                    sub = new TextComponent("false ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit freeze_battle_combatants false"
                        )));
                    parent.append(sub);

                    sub = new TextComponent("ignore_battle_types ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.DARK_GREEN)
                        .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            "/tbm-server-edit ignore_battle_types"))
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Click to show current ignored categories, or use /tbm-server-edit ignore_battle_types add/remove <category_name>")
                        ))
                        .withBold(true));
                    parent.append(sub);

                    sub = new TextComponent("player_speed ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Player default speed"))));
                    parent.append(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        sub = new TextComponent(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit player_speed " + i)));
                        parent.append(sub);
                    }

                    sub = new TextComponent("player_haste_speed ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Player speed when under the effects of \"Speed\"")
                        )));
                    parent.append(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        sub = new TextComponent(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit player_haste_speed " + i)));
                        parent.append(sub);
                    }

                    sub = new TextComponent("player_slow_speed ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Player speed when under the effects of \"Slow\"")
                        )));
                    parent.append(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        sub = new TextComponent(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit player_slow_speed " + i)));
                        parent.append(sub);
                    }

                    sub = new TextComponent("player_attack_probability ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Base Player attack probability in percentage")
                        )));
                    parent.append(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        if (i == 0) {
                            sub = new TextComponent("1 ");
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit player_attack_probability 1")));
                        } else {
                            sub = new TextComponent(String.valueOf(i) + ' ');
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit player_attack_probability " + i)));
                        }
                        parent.append(sub);
                    }

                    sub = new TextComponent("player_evasion ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Base Player evasion rate in percentage")
                        )));
                    parent.append(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        sub = new TextComponent(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit player_evasion " + i)));
                        parent.append(sub);
                    }

                    sub = new TextComponent("defense_duration ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Number of attacks that a \"Defend\" move blocks (lasts until next action)")
                        )));
                    parent.append(sub);

                    for (int i = 0; i <= 5; ++i) {
                        sub = new TextComponent(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit defense_duration " + i)));
                        parent.append(sub);
                    }

                    sub = new TextComponent("flee_good_probability ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Probability of flee success when Player's speed is higher than the fastest opposing Entity")
                        )));
                    parent.append(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        if (i == 0) {
                            sub = new TextComponent("1 ");
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit flee_good_probability 1")));
                        } else {
                            sub = new TextComponent(String.valueOf(i) + ' ');
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit flee_good_probability " + i)));
                        }
                        parent.append(sub);
                    }

                    sub = new TextComponent("flee_bad_probability ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("Probability of flee success when Player's speed is lower than the fastest opposing Entity")
                        )));
                    parent.append(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        if (i == 0) {
                            sub = new TextComponent("1 ");
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit flee_bad_probability 1")));
                        } else {
                            sub = new TextComponent(String.valueOf(i) + ' ');
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit flee_bad_probability " + i)));
                        }
                        parent.append(sub);
                    }

                    sub = new TextComponent("minimum_hit_percentage ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("The minimum percentage possible when calculating hit percentage for any attacker")
                        )));
                    parent.append(sub);

                    for (int i = 0; i <= 100; i += 5) {
                        if (i == 0) {
                            sub = new TextComponent("1 ");
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit minimum_hit_percentage 1")));
                        } else {
                            sub = new TextComponent(String.valueOf(i) + ' ');
                            sub.setStyle(sub.getStyle()
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/tbm-server-edit minimum_hit_percentage " + i)));
                        }
                        parent.append(sub);
                    }

                    sub = new TextComponent("battle_turn_time_seconds ");
                    sub.setStyle(sub.getStyle()
                        .withColor(ChatFormatting.YELLOW)
                        .withBold(true)
                        .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent("The time in seconds to wait for all Players to choose their move")
                        )));
                    parent.append(sub);

                    for (int i = 5; i <= 60; i += 5) {
                        sub = new TextComponent(String.valueOf(i) + ' ');
                        sub.setStyle(sub.getStyle()
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/tbm-server-edit battle_turn_time_seconds " + i)));
                        parent.append(sub);
                    }

                    TurnBasedMinecraftMod.proxy.displayComponent(parent);
                    break;
                }
                case EDIT_IGNORE_BATTLE: {
                    TextComponent text = new TextComponent("ignoreBattle: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    TextComponent option = new TextComponent("true");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle true")));
                    text.getSiblings().add(option);

                    text.getSiblings().add(new TextComponent(" "));

                    option = new TextComponent("false");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle false")));
                    text.getSiblings().add(option);

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_ATTACK_POWER: {
                    TextComponent text = new TextComponent("attackPower: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 15; ++i) {
                        TextComponent option = new TextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 15) {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit attackPower <integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_ATTACK_PROBABILITY: {
                    TextComponent text = new TextComponent("attackProbability: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 10; i <= 100; i += 10) {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit attackProbability <percentage-integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_ATTACK_VARIANCE: {
                    TextComponent text = new TextComponent("attackVariance: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 10; ++i) {
                        TextComponent option = new TextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 10) {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit attackVariance <integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_ATTACK_EFFECT: {
                    TextComponent text = new TextComponent("attackEffect: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (EntityInfo.Effect e : EntityInfo.Effect.values()) {
                        TextComponent option = new TextComponent(e.toString());
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect " + e.toString())));
                        text.getSiblings().add(option);
                        if (e != EntityInfo.Effect.UNKNOWN) {
                            // TODO find a better way to handle printing comma for items before last
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_ATTACK_EFFECT_PROBABILITY: {
                    TextComponent text = new TextComponent("attackEffectProbability: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit attackEffectProbability <percentage-integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_DEFENSE_DAMAGE: {
                    TextComponent text = new TextComponent("defenseDamage: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 15; ++i) {
                        TextComponent option = new TextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 15) {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit defenseDamage <integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_DEFENSE_DAMAGE_PROBABILITY: {
                    TextComponent text = new TextComponent("defenseDamageProbability: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit defenseDamageProbability <percentage-integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_EVASION: {
                    TextComponent text = new TextComponent("evasion: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit evasion <percentage-integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_SPEED: {
                    TextComponent text = new TextComponent("speed: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        TextComponent option = new TextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit speed <integer>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_CATEGORY: {
                    TextComponent text = new TextComponent("category: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    TextComponent option = new TextComponent("monster");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category monster")));
                    if (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("monster")) {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    } else {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new TextComponent(", "));

                    option = new TextComponent("animal");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category animal")));
                    if (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("animal")) {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    } else {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new TextComponent(", "));

                    option = new TextComponent("passive");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category passive")));
                    if (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("passive")) {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    } else {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new TextComponent(", "));

                    option = new TextComponent("boss");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category boss")));
                    if (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("boss")) {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    } else {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new TextComponent(", "));

                    option = new TextComponent("player");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category player")));
                    if (TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("player")) {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    } else {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit category <string>\")"));

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_DECISION_ATTACK: {
                    TextComponent text = new TextComponent("decisionAttack: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_DECISION_DEFEND: {
                    TextComponent text = new TextComponent("decisionDefend: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    parentComponent.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayComponent(parentComponent);
                    break;
                }
                case EDIT_DECISION_FLEE: {
                    TextComponent text = new TextComponent("decisionFlee: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for (int i = 0; i <= 100; i += 10) {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if (i < 100) {
                            text.getSiblings().add(new TextComponent(", "));
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
}
