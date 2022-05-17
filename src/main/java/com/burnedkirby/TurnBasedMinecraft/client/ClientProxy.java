package com.burnedkirby.TurnBasedMinecraft.client;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.CommonProxy;
import com.burnedkirby.TurnBasedMinecraft.common.EntityInfo;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketBattleMessage;
import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketEditingMessage;
import com.burnedkirby.TurnBasedMinecraft.common.networking.PacketGeneralMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ClientProxy extends CommonProxy
{
    private BattleGui battleGui = null;
    private BattleMusic battleMusic = null;
    private int battleMusicCount = 0;
    private int sillyMusicCount = 0;
    private Battle localBattle = null;
    
    @Override
    protected void initializeClient()
    {
        battleGui = new BattleGui();
        battleMusic = null; // will be initialized in postInit()
        battleMusicCount = 0;
        sillyMusicCount = 0;
        localBattle = null;
        logger.debug("Init client");
    }

    @Override
    public void setBattleGuiTime(int timeRemaining)
    {
        battleGui.setTimeRemaining(timeRemaining);
    }

    @Override
    public void setBattleGuiBattleChanged()
    {
        battleGui.battleChanged();
    }

    @Override
    public void setBattleGuiAsGui()
    {
        if(Minecraft.getInstance().screen != battleGui)
        {
            battleGui.turnEnd();
            Minecraft.getInstance().setScreen(battleGui);
        }
    }

    @Override
    public void battleGuiTurnBegin()
    {
        battleGui.turnBegin();
    }

    @Override
    public void battleGuiTurnEnd()
    {
        battleGui.turnEnd();
    }

    @Override
    public void battleStarted()
    {
        setBattleGuiAsGui();
    }

    @Override
    public void battleEnded()
    {
        localBattle = null;
        Minecraft.getInstance().setScreen(null);
        stopMusic(true);
        battleMusicCount = 0;
        sillyMusicCount = 0;
    }

    @Override
    protected void postInitClient()
    {
        battleMusic = new BattleMusic(getLogger());
    }

    @Override
    public void playBattleMusic()
    {
        Options gs = Minecraft.getInstance().options;
        battleMusic.playBattle(gs.getSoundSourceVolume(SoundSource.MUSIC) * gs.getSoundSourceVolume(SoundSource.MASTER));
    }

    @Override
    public void playSillyMusic()
    {
        Options gs = Minecraft.getInstance().options;
        battleMusic.playSilly(gs.getSoundSourceVolume(SoundSource.MUSIC) * gs.getSoundSourceVolume(SoundSource.MASTER));
    }

    @Override
    public void stopMusic(boolean resumeMCSounds)
    {
        battleMusic.stopMusic(resumeMCSounds);
    }

    /**
     * Sets what music to play based on type and loaded Config
     */
    @Override
    public void typeEnteredBattle(String type)
    {
        if(localBattle == null) {
            return;
        } if(type == null || type.isEmpty() || getConfig().isBattleMusicType(type)) {
            ++battleMusicCount;
        } else if(getConfig().isSillyMusicType(type)) {
            ++sillyMusicCount;
        } else {
            ++battleMusicCount;
        }
        checkBattleTypes(false);
    }

    @Override
    public void typeLeftBattle(String type)
    {
        if(localBattle == null || localBattle.getSideA().isEmpty() || localBattle.getSideB().isEmpty())
        {
            battleMusicCount = 0;
            sillyMusicCount = 0;
            return;
        } else if(type == null || type.isEmpty() || getConfig().isBattleMusicType(type)) {
            --battleMusicCount;
        } else if(getConfig().isSillyMusicType(type)) {
            --sillyMusicCount;
        } else {
            --battleMusicCount;
        }
        checkBattleTypes(true);
    }

    @Override
    public void displayString(String message)
    {
        TextComponent prefix = new TextComponent("TBM: ");
        prefix.withStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
        TextComponent text = new TextComponent(message);
        prefix.getSiblings().add(text);
        text.withStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));
        // UUID is required by sendMessage, but appears to be unused, so just give dummy UUID
        Minecraft.getInstance().player.sendMessage(prefix, UUID.randomUUID());
    }

    @Override
    public void displayTextComponent(TextComponent text)
    {
        // UUID is required by sendMessage, but appears to be unused, so just give dummy UUID
        Minecraft.getInstance().player.sendMessage(text, UUID.randomUUID());
    }

    private void checkBattleTypes(boolean entityLeft)
    {
        // check that battle is still valid
        if(localBattle == null && entityLeft && (localBattle.getSideA().isEmpty() || localBattle.getSideB().isEmpty())) {
            return;
        }

    float percentage = 0.0f;
        if(sillyMusicCount == 0 && battleMusicCount == 0)
        {
            percentage = 0.0f;
        }
        else if(battleMusicCount == 0)
        {
            percentage = 100.0f;
        }
        else
        {
            percentage = 100.0f * (float)sillyMusicCount / (float)(sillyMusicCount + battleMusicCount);
        }
        
        if(percentage >= (float)getConfig().getSillyMusicThreshold())
        {
            if(battleMusic.isPlaying())
            {
                if(!battleMusic.isPlayingSilly() && battleMusic.hasSillyMusic())
                {
                    stopMusic(false);
                    playSillyMusic();
                }
            }
            else if(battleMusic.hasSillyMusic())
            {
                playSillyMusic();
            }
        }
        else
        {
            if(battleMusic.isPlaying())
            {
                if(battleMusic.isPlayingSilly() && battleMusic.hasBattleMusic())
                {
                    stopMusic(false);
                    playBattleMusic();
                }
            }
            else if(battleMusic.hasBattleMusic())
            {
                playBattleMusic();
            }
        }
    }

    @Override
    public Battle getLocalBattle()
    {
        return localBattle;
    }

    @Override
    public void createLocalBattle(int id)
    {
        localBattle = new Battle(null, id, null, null, false, Minecraft.getInstance().level.dimension());
    }

    @Override
    public Entity getEntity(int id, ResourceKey<Level> dim) {
        return Minecraft.getInstance().level.getEntity(id);
    }

    @Override
    public <MSG> void handlePacket(MSG msg, Supplier<NetworkEvent.Context> ctx) {
        if (msg.getClass() == PacketBattleMessage.class) {
            PacketBattleMessage pkt = (PacketBattleMessage)msg;
            Entity fromEntity = getEntity(pkt.getEntityIDFrom(), pkt.getDimension());
            String from = "Unknown";
            if(fromEntity != null)
            {
                from = fromEntity.getDisplayName().getString();
            }
            else if(TurnBasedMinecraftMod.proxy.getLocalBattle() != null)
            {
                fromEntity = TurnBasedMinecraftMod.proxy.getLocalBattle().getCombatantEntity(pkt.getEntityIDFrom());
                if(fromEntity != null)
                {
                    from = fromEntity.getDisplayName().getString();
                }
            }
            Entity toEntity = TurnBasedMinecraftMod.proxy.getEntity(pkt.getEntityIDTo(), pkt.getDimension());
            String to = "Unknown";
            if(toEntity != null)
            {
                to = toEntity.getDisplayName().getString();
            }
            else if(TurnBasedMinecraftMod.proxy.getLocalBattle() != null)
            {
                toEntity = TurnBasedMinecraftMod.proxy.getLocalBattle().getCombatantEntity(pkt.getEntityIDTo());
                if(toEntity != null)
                {
                    to = toEntity.getDisplayName().getString();
                }
            }

            switch(pkt.getMessageType())
            {
                case ENTERED:
                    TurnBasedMinecraftMod.proxy.displayString(from + " entered battle!");
                    if(TurnBasedMinecraftMod.proxy.getLocalBattle() == null || TurnBasedMinecraftMod.proxy.getLocalBattle().getId() != pkt.getAmount())
                    {
                        TurnBasedMinecraftMod.proxy.createLocalBattle(pkt.getAmount());
                    }
                    TurnBasedMinecraftMod.proxy.battleStarted();
                    TurnBasedMinecraftMod.proxy.typeEnteredBattle(pkt.getCustom());
                    break;
                case FLEE:
                    if(pkt.getAmount() != 0)
                    {
                        TurnBasedMinecraftMod.proxy.displayString(from + " fled battle!");
                        TurnBasedMinecraftMod.proxy.typeLeftBattle(pkt.getCustom());
                    }
                    else
                    {
                        TurnBasedMinecraftMod.proxy.displayString(from + " tried to flee battle but failed!");
                    }
                    break;
                case DIED:
                    TurnBasedMinecraftMod.proxy.displayString(from + " died in battle!");
                    TurnBasedMinecraftMod.proxy.typeLeftBattle(pkt.getCustom());
                    break;
                case ENDED:
                    TurnBasedMinecraftMod.proxy.displayString("Battle has ended!");
                    TurnBasedMinecraftMod.proxy.battleEnded();
                    break;
                case ATTACK:
                    TurnBasedMinecraftMod.proxy.displayString(from + " attacked " + to + " and dealt " + pkt.getAmount() + " damage!");
                    break;
                case DEFEND:
                    TurnBasedMinecraftMod.proxy.displayString(from + " blocked " + to + "'s attack!");
                    break;
                case DEFENSE_DAMAGE:
                    TurnBasedMinecraftMod.proxy.displayString(from + " retaliated from " + to + "'s attack and dealt " + pkt.getAmount() + " damage!");
                    break;
                case MISS:
                    TurnBasedMinecraftMod.proxy.displayString(from + " attacked " + to + " but missed!");
                    break;
                case DEFENDING:
                    TurnBasedMinecraftMod.proxy.displayString(from + " is defending!");
                    break;
                case DID_NOTHING:
                    TurnBasedMinecraftMod.proxy.displayString(from + " did nothing!");
                    break;
                case USED_ITEM:
                    switch(PacketBattleMessage.UsedItemAction.valueOf(pkt.getAmount()))
                    {
                        case USED_NOTHING:
                            TurnBasedMinecraftMod.proxy.displayString(from + " tried to use nothing!");
                            break;
                        case USED_INVALID:
                            if(pkt.getCustom().length() > 0)
                            {
                                TurnBasedMinecraftMod.proxy.displayString(from + " tried to consume " + pkt.getCustom() + " and failed!");
                            }
                            else
                            {
                                TurnBasedMinecraftMod.proxy.displayString(from + " tried to consume an invalid item and failed!");
                            }
                            break;
                        case USED_FOOD:
                            TurnBasedMinecraftMod.proxy.displayString(from + " ate a " + pkt.getCustom() + "!");
                            break;
                        case USED_POTION:
                            TurnBasedMinecraftMod.proxy.displayString(from + " drank a " + pkt.getCustom() + "!");
                            break;
                    }
                    break;
                case TURN_BEGIN:
                    TurnBasedMinecraftMod.proxy.displayString("The turn begins!");
                    TurnBasedMinecraftMod.proxy.battleGuiTurnBegin();
                    break;
                case TURN_END:
                    if(TurnBasedMinecraftMod.proxy.getLocalBattle() != null)
                    {
                        if(pkt.getAmount() == 0)
                        {
                            TurnBasedMinecraftMod.proxy.displayString("The turn ended!");
                        }
                        else
                        {
                            TurnBasedMinecraftMod.proxy.displayString("The turn ended (abnormally due to internal error)!");
                        }
                    }
                    TurnBasedMinecraftMod.proxy.battleGuiTurnEnd();
                    break;
                case SWITCHED_ITEM:
                    if(pkt.getAmount() != 0)
                    {
                        TurnBasedMinecraftMod.proxy.displayString(from + " switched to a different item!");
                    }
                    else
                    {
                        TurnBasedMinecraftMod.proxy.displayString(from + " switched to a different item but failed because it was invalid!");
                    }
                    break;
                case WAS_AFFECTED:
                    TurnBasedMinecraftMod.proxy.displayString(to + " was " + pkt.getCustom() + " by " + from + "!");
                    break;
                case BECAME_CREATIVE:
                    TurnBasedMinecraftMod.proxy.displayString(from + " entered creative mode and left battle!");
                    break;
                case FIRED_ARROW:
                    TurnBasedMinecraftMod.proxy.displayString(from + " let loose an arrow towards " + to + "!");
                    break;
                case ARROW_HIT:
                    TurnBasedMinecraftMod.proxy.displayString(to + " was hit by " + from + "'s arrow!");
                    break;
                case BOW_NO_AMMO:
                    TurnBasedMinecraftMod.proxy.displayString(from + " tried to use their bow but ran out of ammo!");
                    break;
                case CREEPER_WAIT: {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent message = new TextComponent(from + " is charging up!");
                    message.setStyle(message.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)));
                    prefix.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                }
                break;
                case CREEPER_WAIT_FINAL: {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent message = new TextComponent(from + " is about to explode!");
                    message.setStyle(message.getStyle().withColor(TextColor.fromRgb(0xFFFF5050)));
                    prefix.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                }
                break;
                case CREEPER_EXPLODE: {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent message = new TextComponent(from + " exploded!");
                    message.setStyle(message.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                    prefix.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                }
                break;
            }
        } else if (msg.getClass() == PacketGeneralMessage.class) {
            PacketGeneralMessage pkt = (PacketGeneralMessage)msg;
            displayString(pkt.getMessage());
        } else if (msg.getClass() == PacketEditingMessage.class) {
            PacketEditingMessage pkt = (PacketEditingMessage)msg;
            switch(pkt.getType())
            {
                case ATTACK_ENTITY:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("Attack the entity you want to edit for TurnBasedMinecraftMod. ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    TextComponent cancel = new TextComponent("Cancel");
                    cancel.setStyle(cancel.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel")));
                    text.getSiblings().add(cancel);

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case PICK_EDIT:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
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

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_IGNORE_BATTLE:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("ignoreBattle: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    TextComponent option = new TextComponent("true");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle true")));
                    text.getSiblings().add(option);

                    text.getSiblings().add(new TextComponent(" "));

                    option = new TextComponent("false");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle false")));
                    text.getSiblings().add(option);

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_POWER:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("attackPower: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 15; ++i)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 15)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit attackPower <integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_PROBABILITY:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("attackProbability: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 10; i <= 100; i += 10)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit attackProbability <percentage-integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_VARIANCE:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("attackVariance: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 10; ++i)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 10)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit attackVariance <integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_EFFECT:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("attackEffect: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(EntityInfo.Effect e : EntityInfo.Effect.values())
                    {
                        TextComponent option = new TextComponent(e.toString());
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect " + e.toString())));
                        text.getSiblings().add(option);
                        if(e != EntityInfo.Effect.UNKNOWN)
                        {
                            // TODO find a better way to handle printing comma for items before last
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_EFFECT_PROBABILITY:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("attackEffectProbability: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit attackEffectProbability <percentage-integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_DEFENSE_DAMAGE:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("defenseDamage: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 15; ++i)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 15)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit defenseDamage <integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_DEFENSE_DAMAGE_PROBABILITY:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("defenseDamageProbability: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit defenseDamageProbability <percentage-integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_EVASION:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("evasion: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit evasion <percentage-integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_SPEED:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("speed: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new TextComponent(" (or use command \"/tbm-edit edit speed <integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_CATEGORY:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("category: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    TextComponent option = new TextComponent("monster");
                    option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category monster")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("monster"))
                    {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
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
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("animal"))
                    {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
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
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("passive"))
                    {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
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
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("boss"))
                    {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
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
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("player"))
                    {
                        TextComponent optionInfo = new TextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)));
                        TextComponent optionInfoBool = new TextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(TextColor.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new TextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
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

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_DECISION_ATTACK:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("decisionAttack: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_DECISION_DEFEND:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("decisionDefend: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_DECISION_FLEE:
                {
                    TextComponent prefix = new TextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(TextColor.fromRgb(0xFF00FF00)).withBold(true));
                    TextComponent text = new TextComponent("decisionFlee: ");
                    text.setStyle(text.getStyle().withColor(TextColor.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        TextComponent option = new TextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(TextColor.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new TextComponent(", "));
                        }
                    }

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                default:
                    break;
            }
        }
    }
}
