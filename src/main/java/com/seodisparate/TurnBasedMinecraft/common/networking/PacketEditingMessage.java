package com.seodisparate.TurnBasedMinecraft.common.networking;

import com.seodisparate.TurnBasedMinecraft.common.EntityInfo;
import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashMap;
import java.util.Map;

public class PacketEditingMessage implements IMessage
{
    public enum Type
    {
        ATTACK_ENTITY(0),
        PICK_EDIT(1),
        EDIT_IGNORE_BATTLE(2),
        EDIT_ATTACK_POWER(3),
        EDIT_ATTACK_PROBABILITY(4),
        EDIT_ATTACK_VARIANCE(5),
        EDIT_ATTACK_EFFECT(6),
        EDIT_ATTACK_EFFECT_PROBABILITY(7),
        EDIT_DEFENSE_DAMAGE(8),
        EDIT_DEFENSE_DAMAGE_PROBABILITY(9),
        EDIT_EVASION(10),
        EDIT_SPEED(11),
        EDIT_CATEGORY(12),
        EDIT_DECISION_ATTACK(13),
        EDIT_DECISION_DEFEND(14),
        EDIT_DECISION_FLEE(15);

        Type(int value)
        {
            this.value = value;
        }

        private static Map<Integer, Type> map;
        private int value;

        static
        {
            map = new HashMap<Integer, Type>();
            for(Type t : values())
            {
                map.put(t.value, t);
            }
        }

        public int getValue()
        {
            return value;
        }

        public static Type valueOf(int value)
        {
            return map.get(value);
        }
    }

    Type type = Type.ATTACK_ENTITY;
    EntityInfo entityInfo = new EntityInfo();

    public PacketEditingMessage() {}

    public PacketEditingMessage(Type type)
    {
        this.type = type;
    }

    public PacketEditingMessage(Type type, EntityInfo entityInfo)
    {
        this.type = type;
        this.entityInfo = entityInfo;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        type = Type.valueOf(buf.readInt());
        try
        {
            entityInfo.classType = getClass().getClassLoader().loadClass(ByteBufUtils.readUTF8String(buf));
        }
        catch (ClassNotFoundException e) { /* ignored */ }
        entityInfo.ignoreBattle = buf.readBoolean();
        entityInfo.attackPower = buf.readInt();
        entityInfo.attackProbability = buf.readInt();
        entityInfo.attackVariance = buf.readInt();
        entityInfo.attackEffect = EntityInfo.Effect.fromString(ByteBufUtils.readUTF8String(buf));
        entityInfo.attackEffectProbability = buf.readInt();
        entityInfo.defenseDamage = buf.readInt();
        entityInfo.defenseDamageProbability = buf.readInt();
        entityInfo.evasion = buf.readInt();
        entityInfo.speed = buf.readInt();
        entityInfo.category = ByteBufUtils.readUTF8String(buf);
        entityInfo.decisionAttack = buf.readInt();
        entityInfo.decisionDefend = buf.readInt();
        entityInfo.decisionFlee = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(type.getValue());
        if(entityInfo.classType != null)
        {
            ByteBufUtils.writeUTF8String(buf, entityInfo.classType.getName());
        }
        else
        {
            ByteBufUtils.writeUTF8String(buf, "unknown");
        }
        buf.writeBoolean(entityInfo.ignoreBattle);
        buf.writeInt(entityInfo.attackPower);
        buf.writeInt(entityInfo.attackProbability);
        buf.writeInt(entityInfo.attackVariance);
        ByteBufUtils.writeUTF8String(buf, entityInfo.attackEffect.toString());
        buf.writeInt(entityInfo.attackEffectProbability);
        buf.writeInt(entityInfo.defenseDamage);
        buf.writeInt(entityInfo.defenseDamageProbability);
        buf.writeInt(entityInfo.evasion);
        buf.writeInt(entityInfo.speed);
        ByteBufUtils.writeUTF8String(buf, entityInfo.category);
        buf.writeInt(entityInfo.decisionAttack);
        buf.writeInt(entityInfo.decisionDefend);
        buf.writeInt(entityInfo.decisionFlee);
    }

    public static class HandlerEditingMessage implements IMessageHandler<PacketEditingMessage, IMessage>
    {
        @Override
        public IMessage onMessage(PacketEditingMessage message, MessageContext ctx)
        {
            switch(message.type)
            {
            case ATTACK_ENTITY:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("Attack the entity you want to edit for TurnBasedMinecraftMod. ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                ITextComponent cancel = new TextComponentString("Cancel");
                cancel.getStyle().setColor(TextFormatting.RED).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel"));
                text.appendSibling(cancel);

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case PICK_EDIT:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("Edit what value? ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                ITextComponent option = new TextComponentString("IgB");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("IgnoreBattle")));
                ITextComponent value = new TextComponentString("(" + message.entityInfo.ignoreBattle + ") ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("AP");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("AttackPower")));
                value = new TextComponentString("(" + message.entityInfo.attackPower + ") ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("APr");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("AttackProbability")));
                value = new TextComponentString("(" + message.entityInfo.attackProbability + "%) ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("AV");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("AttackVariance")));
                value = new TextComponentString("(" + message.entityInfo.attackVariance + ") ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("AE");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("AttackEffect")));
                value = new TextComponentString("(" + message.entityInfo.attackEffect.toString() + ") ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("AEPr");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("AttackEffectProbability")));
                value = new TextComponentString("(" + message.entityInfo.attackEffectProbability + "%) ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("DD");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("DefenseDamage")));
                value = new TextComponentString("(" + message.entityInfo.defenseDamage + ") ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("DDPr");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("DefenseDamageProbability")));
                value = new TextComponentString("(" + message.entityInfo.defenseDamageProbability + "%) ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("E");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Evasion")));
                value = new TextComponentString("(" + message.entityInfo.evasion + ") ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("S");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Speed")));
                value = new TextComponentString("(" + message.entityInfo.speed + ") ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("C");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Category")));
                value = new TextComponentString("(" + message.entityInfo.category + ") ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("DecA");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("DecisionAttack")));
                value = new TextComponentString("(" + message.entityInfo.decisionAttack + "%) ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("DecD");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("DecisionDefend")));
                value = new TextComponentString("(" + message.entityInfo.decisionDefend + "%) ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("DecF");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee"))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("DecisionFlee")));
                value = new TextComponentString("(" + message.entityInfo.decisionFlee + "%) ");
                value.getStyle().setColor(TextFormatting.WHITE);
                option.appendSibling(value);
                text.appendSibling(option);

                option = new TextComponentString("Finished Editing");
                option.getStyle().setColor(TextFormatting.GREEN).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit finish"));
                text.appendSibling(option).appendText(" ");

                option = new TextComponentString("Cancel");
                option.getStyle().setColor(TextFormatting.RED).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel"));
                text.appendSibling(option);

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_IGNORE_BATTLE:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("ignoreBattle: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                ITextComponent option = new TextComponentString("true");
                option.getStyle().setColor(TextFormatting.GREEN).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle true"));
                text.appendSibling(option);

                text.appendText(" ");

                option = new TextComponentString("false");
                option.getStyle().setColor(TextFormatting.RED).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle false"));
                text.appendSibling(option);

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_ATTACK_POWER:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("attackPower: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 0; i <= 15; ++i)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i));
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 15)
                    {
                        text.appendText(", ");
                    }
                }

                text.appendText(" (or use command \"/tbm-edit edit attackPower <integer>\")");

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_ATTACK_PROBABILITY:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("attackProbability: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 10; i <= 100; i += 10)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i) + "%");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 100)
                    {
                        text.appendText(", ");
                    }
                }

                text.appendText(" (or use command \"/tbm-edit edit attackProbability <percentage-integer>\")");

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_ATTACK_VARIANCE:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("attackVariance: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 0; i <= 10; ++i)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i));
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 10)
                    {
                        text.appendText(", ");
                    }
                }

                text.appendText(" (or use command \"/tbm-edit edit attackVariance <integer>\")");

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_ATTACK_EFFECT:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("attackEffect: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(EntityInfo.Effect e : EntityInfo.Effect.values())
                {
                    ITextComponent option = new TextComponentString(e.toString());
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect " + e.toString()));
                    text.appendSibling(option);
                    if(e != EntityInfo.Effect.UNKNOWN)
                    {
                        // TODO find a better way to handle printing comma for items before last
                        text.appendText(", ");
                    }
                }

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_ATTACK_EFFECT_PROBABILITY:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("attackEffectProbability: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 0; i <= 100; i += 10)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i) + "%");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 100)
                    {
                        text.appendText(", ");
                    }
                }

                text.appendText(" (or use command \"/tbm-edit edit attackEffectProbability <percentage-integer>\")");

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_DEFENSE_DAMAGE:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("defenseDamage: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 0; i <= 15; ++i)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i));
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 15)
                    {
                        text.appendText(", ");
                    }
                }

                text.appendText(" (or use command \"/tbm-edit edit defenseDamage <integer>\")");

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_DEFENSE_DAMAGE_PROBABILITY:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("defenseDamageProbability: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 0; i <= 100; i += 10)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i) + "%");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 100)
                    {
                        text.appendText(", ");
                    }
                }

                text.appendText(" (or use command \"/tbm-edit edit defenseDamageProbability <percentage-integer>\")");

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_EVASION:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("evasion: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 0; i <= 100; i += 10)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i) + "%");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 100)
                    {
                        text.appendText(", ");
                    }
                }

                text.appendText(" (or use command \"/tbm-edit edit evasion <percentage-integer>\")");

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_SPEED:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("speed: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 0; i <= 100; i += 10)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i));
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 100)
                    {
                        text.appendText(", ");
                    }
                }

                text.appendText(" (or use command \"/tbm-edit edit speed <integer>\")");

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_CATEGORY:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("category: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                ITextComponent option = new TextComponentString("monster");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category monster"));
                if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("monster"))
                {
                    ITextComponent optionInfo = new TextComponentString("(battle-");
                    optionInfo.getStyle().setColor(TextFormatting.WHITE);
                    ITextComponent optionInfoBool = new TextComponentString("disabled");
                    optionInfoBool.getStyle().setColor(TextFormatting.RED);
                    optionInfo.appendSibling(optionInfoBool).appendText(")");
                    option.appendSibling(optionInfo);
                }
                else
                {
                    ITextComponent optionInfo = new TextComponentString("(battle-");
                    optionInfo.getStyle().setColor(TextFormatting.WHITE);
                    ITextComponent optionInfoBool = new TextComponentString("enabled");
                    optionInfoBool.getStyle().setColor(TextFormatting.GREEN);
                    optionInfo.appendSibling(optionInfoBool).appendText(")");
                    option.appendSibling(optionInfo);
                }
                text.appendSibling(option).appendText(", ");

                option = new TextComponentString("animal");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category animal"));
                if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("animal"))
                {
                    ITextComponent optionInfo = new TextComponentString("(battle-");
                    optionInfo.getStyle().setColor(TextFormatting.WHITE);
                    ITextComponent optionInfoBool = new TextComponentString("disabled");
                    optionInfoBool.getStyle().setColor(TextFormatting.RED);
                    optionInfo.appendSibling(optionInfoBool).appendText(")");
                    option.appendSibling(optionInfo);
                }
                else
                {
                    ITextComponent optionInfo = new TextComponentString("(battle-");
                    optionInfo.getStyle().setColor(TextFormatting.WHITE);
                    ITextComponent optionInfoBool = new TextComponentString("enabled");
                    optionInfoBool.getStyle().setColor(TextFormatting.GREEN);
                    optionInfo.appendSibling(optionInfoBool).appendText(")");
                    option.appendSibling(optionInfo);
                }
                text.appendSibling(option).appendText(", ");

                option = new TextComponentString("passive");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category passive"));
                if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("passive"))
                {
                    ITextComponent optionInfo = new TextComponentString("(battle-");
                    optionInfo.getStyle().setColor(TextFormatting.WHITE);
                    ITextComponent optionInfoBool = new TextComponentString("disabled");
                    optionInfoBool.getStyle().setColor(TextFormatting.RED);
                    optionInfo.appendSibling(optionInfoBool).appendText(")");
                    option.appendSibling(optionInfo);
                }
                else
                {
                    ITextComponent optionInfo = new TextComponentString("(battle-");
                    optionInfo.getStyle().setColor(TextFormatting.WHITE);
                    ITextComponent optionInfoBool = new TextComponentString("enabled");
                    optionInfoBool.getStyle().setColor(TextFormatting.GREEN);
                    optionInfo.appendSibling(optionInfoBool).appendText(")");
                    option.appendSibling(optionInfo);
                }
                text.appendSibling(option).appendText(", ");

                option = new TextComponentString("boss");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category boss"));
                if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("boss"))
                {
                    ITextComponent optionInfo = new TextComponentString("(battle-");
                    optionInfo.getStyle().setColor(TextFormatting.WHITE);
                    ITextComponent optionInfoBool = new TextComponentString("disabled");
                    optionInfoBool.getStyle().setColor(TextFormatting.RED);
                    optionInfo.appendSibling(optionInfoBool).appendText(")");
                    option.appendSibling(optionInfo);
                }
                else
                {
                    ITextComponent optionInfo = new TextComponentString("(battle-");
                    optionInfo.getStyle().setColor(TextFormatting.WHITE);
                    ITextComponent optionInfoBool = new TextComponentString("enabled");
                    optionInfoBool.getStyle().setColor(TextFormatting.GREEN);
                    optionInfo.appendSibling(optionInfoBool).appendText(")");
                    option.appendSibling(optionInfo);
                }
                text.appendSibling(option).appendText(", ");

                option = new TextComponentString("player");
                option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category player"));
                if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("player"))
                {
                    ITextComponent optionInfo = new TextComponentString("(battle-");
                    optionInfo.getStyle().setColor(TextFormatting.WHITE);
                    ITextComponent optionInfoBool = new TextComponentString("disabled");
                    optionInfoBool.getStyle().setColor(TextFormatting.RED);
                    optionInfo.appendSibling(optionInfoBool).appendText(")");
                    option.appendSibling(optionInfo);
                }
                else
                {
                    ITextComponent optionInfo = new TextComponentString("(battle-");
                    optionInfo.getStyle().setColor(TextFormatting.WHITE);
                    ITextComponent optionInfoBool = new TextComponentString("enabled");
                    optionInfoBool.getStyle().setColor(TextFormatting.GREEN);
                    optionInfo.appendSibling(optionInfoBool).appendText(")");
                    option.appendSibling(optionInfo);
                }
                text.appendSibling(option);

                text.appendText(" (or use command \"/tbm-edit edit category <string>\")");

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_DECISION_ATTACK:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("decisionAttack: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 0; i <= 100; i += 10)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i) + "%");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 100)
                    {
                        text.appendText(", ");
                    }
                }

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_DECISION_DEFEND:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("decisionDefend: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 0; i <= 100; i += 10)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i) + "%");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 100)
                    {
                        text.appendText(", ");
                    }
                }

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            case EDIT_DECISION_FLEE:
            {
                ITextComponent prefix = new TextComponentString("TBM: ");
                prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                ITextComponent text = new TextComponentString("decisionFlee: ");
                text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                for(int i = 0; i <= 100; i += 10)
                {
                    ITextComponent option = new TextComponentString(Integer.toString(i) + "%");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee " + Integer.toString(i)));
                    text.appendSibling(option);
                    if(i < 100)
                    {
                        text.appendText(", ");
                    }
                }

                prefix.appendSibling(text);
                TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                break;
            }
            default:
                break;
            }
            return null;
        }
    }
}
