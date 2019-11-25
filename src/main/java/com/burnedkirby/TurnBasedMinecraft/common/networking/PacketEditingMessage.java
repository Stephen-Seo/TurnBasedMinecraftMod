package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.EntityInfo;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PacketEditingMessage
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
        if(entityInfo != null)
        {
            this.entityInfo = entityInfo;
        }
    }
    
    public static void encode(PacketEditingMessage pkt, PacketBuffer buf) {
    	buf.writeInt(pkt.type.getValue());
    	if(pkt.entityInfo.classType != null) {
    		buf.writeString(pkt.entityInfo.classType.getName());
    	} else {
    		buf.writeString("unknown");
    	}
    	buf.writeBoolean(pkt.entityInfo.ignoreBattle);
    	buf.writeInt(pkt.entityInfo.attackPower);
    	buf.writeInt(pkt.entityInfo.attackProbability);
    	buf.writeInt(pkt.entityInfo.attackVariance);
    	buf.writeString(pkt.entityInfo.attackEffect.toString());
    	buf.writeInt(pkt.entityInfo.attackEffectProbability);
    	buf.writeInt(pkt.entityInfo.defenseDamage);
    	buf.writeInt(pkt.entityInfo.defenseDamageProbability);
    	buf.writeInt(pkt.entityInfo.evasion);
    	buf.writeInt(pkt.entityInfo.speed);
    	buf.writeString(pkt.entityInfo.category);
    	buf.writeInt(pkt.entityInfo.decisionAttack);
    	buf.writeInt(pkt.entityInfo.decisionDefend);
    	buf.writeInt(pkt.entityInfo.decisionFlee);
    	buf.writeString(pkt.entityInfo.customName);
    }
    
    public static PacketEditingMessage decode(PacketBuffer buf) {
    	Type type = Type.valueOf(buf.readInt());
    	EntityInfo einfo = new EntityInfo();
    	try {
    		einfo.classType = einfo.getClass().getClassLoader().loadClass(buf.readString());
    	} catch (ClassNotFoundException e) { /* ignored */ }
        einfo.ignoreBattle = buf.readBoolean();
        einfo.attackPower = buf.readInt();
        einfo.attackProbability = buf.readInt();
        einfo.attackVariance = buf.readInt();
        einfo.attackEffect = EntityInfo.Effect.fromString(buf.readString());
        einfo.attackEffectProbability = buf.readInt();
        einfo.defenseDamage = buf.readInt();
        einfo.defenseDamageProbability = buf.readInt();
        einfo.evasion = buf.readInt();
        einfo.speed = buf.readInt();
        einfo.category = buf.readString();
        einfo.decisionAttack = buf.readInt();
        einfo.decisionDefend = buf.readInt();
        einfo.decisionFlee = buf.readInt();
        einfo.customName = buf.readString();
    	return new PacketEditingMessage(type, einfo);
    }
    
    public static class Handler {
    	public static void handle(final PacketEditingMessage pkt, Supplier<NetworkEvent.Context> ctx) {
    		ctx.get().enqueueWork(() -> {
    			switch(pkt.type)
                {
                case ATTACK_ENTITY:
                {
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("Attack the entity you want to edit for TurnBasedMinecraftMod. ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    ITextComponent cancel = new StringTextComponent("Cancel");
                    cancel.getStyle().setColor(TextFormatting.RED).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel"));
                    text.appendSibling(cancel);

                    prefix.appendSibling(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case PICK_EDIT:
                {
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("Edit what value? ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    ITextComponent option = new StringTextComponent("IgB");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("IgnoreBattle")));
                    ITextComponent value = new StringTextComponent("(" + pkt.entityInfo.ignoreBattle + ") ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("AP");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("AttackPower")));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackPower + ") ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("APr");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("AttackProbability")));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackProbability + "%) ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("AV");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("AttackVariance")));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackVariance + ") ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("AE");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("AttackEffect")));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackEffect.toString() + ") ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("AEPr");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("AttackEffectProbability")));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackEffectProbability + "%) ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("DD");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("DefenseDamage")));
                    value = new StringTextComponent("(" + pkt.entityInfo.defenseDamage + ") ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("DDPr");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("DefenseDamageProbability")));
                    value = new StringTextComponent("(" + pkt.entityInfo.defenseDamageProbability + "%) ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("E");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Evasion")));
                    value = new StringTextComponent("(" + pkt.entityInfo.evasion + "%) ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("S");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Speed")));
                    value = new StringTextComponent("(" + pkt.entityInfo.speed + ") ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("C");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Category")));
                    value = new StringTextComponent("(" + pkt.entityInfo.category + ") ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("DecA");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("DecisionAttack")));
                    value = new StringTextComponent("(" + pkt.entityInfo.decisionAttack + "%) ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("DecD");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("DecisionDefend")));
                    value = new StringTextComponent("(" + pkt.entityInfo.decisionDefend + "%) ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("DecF");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee"))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("DecisionFlee")));
                    value = new StringTextComponent("(" + pkt.entityInfo.decisionFlee + "%) ");
                    value.getStyle().setColor(TextFormatting.WHITE);
                    option.appendSibling(value);
                    text.appendSibling(option);

                    option = new StringTextComponent("Finished Editing");
                    option.getStyle().setColor(TextFormatting.GREEN).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit finish"));
                    text.appendSibling(option).appendText(" ");

                    option = new StringTextComponent("Cancel");
                    option.getStyle().setColor(TextFormatting.RED).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel"));
                    text.appendSibling(option);

                    prefix.appendSibling(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_IGNORE_BATTLE:
                {
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("ignoreBattle: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    ITextComponent option = new StringTextComponent("true");
                    option.getStyle().setColor(TextFormatting.GREEN).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle true"));
                    text.appendSibling(option);

                    text.appendText(" ");

                    option = new StringTextComponent("false");
                    option.getStyle().setColor(TextFormatting.RED).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle false"));
                    text.appendSibling(option);

                    prefix.appendSibling(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_POWER:
                {
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("attackPower: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 0; i <= 15; ++i)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i));
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("attackProbability: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 10; i <= 100; i += 10)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i) + "%");
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("attackVariance: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 0; i <= 10; ++i)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i));
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("attackEffect: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(EntityInfo.Effect e : EntityInfo.Effect.values())
                    {
                        ITextComponent option = new StringTextComponent(e.toString());
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("attackEffectProbability: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 0; i <= 100; i += 10)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i) + "%");
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("defenseDamage: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 0; i <= 15; ++i)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i));
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("defenseDamageProbability: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 0; i <= 100; i += 10)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i) + "%");
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("evasion: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 0; i <= 100; i += 10)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i) + "%");
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("speed: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 0; i <= 100; i += 10)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i));
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("category: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    ITextComponent option = new StringTextComponent("monster");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category monster"));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("monster"))
                    {
                        ITextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.getStyle().setColor(TextFormatting.WHITE);
                        ITextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.getStyle().setColor(TextFormatting.RED);
                        optionInfo.appendSibling(optionInfoBool).appendText(")");
                        option.appendSibling(optionInfo);
                    }
                    else
                    {
                        ITextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.getStyle().setColor(TextFormatting.WHITE);
                        ITextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.getStyle().setColor(TextFormatting.GREEN);
                        optionInfo.appendSibling(optionInfoBool).appendText(")");
                        option.appendSibling(optionInfo);
                    }
                    text.appendSibling(option).appendText(", ");

                    option = new StringTextComponent("animal");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category animal"));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("animal"))
                    {
                        ITextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.getStyle().setColor(TextFormatting.WHITE);
                        ITextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.getStyle().setColor(TextFormatting.RED);
                        optionInfo.appendSibling(optionInfoBool).appendText(")");
                        option.appendSibling(optionInfo);
                    }
                    else
                    {
                        ITextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.getStyle().setColor(TextFormatting.WHITE);
                        ITextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.getStyle().setColor(TextFormatting.GREEN);
                        optionInfo.appendSibling(optionInfoBool).appendText(")");
                        option.appendSibling(optionInfo);
                    }
                    text.appendSibling(option).appendText(", ");

                    option = new StringTextComponent("passive");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category passive"));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("passive"))
                    {
                        ITextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.getStyle().setColor(TextFormatting.WHITE);
                        ITextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.getStyle().setColor(TextFormatting.RED);
                        optionInfo.appendSibling(optionInfoBool).appendText(")");
                        option.appendSibling(optionInfo);
                    }
                    else
                    {
                        ITextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.getStyle().setColor(TextFormatting.WHITE);
                        ITextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.getStyle().setColor(TextFormatting.GREEN);
                        optionInfo.appendSibling(optionInfoBool).appendText(")");
                        option.appendSibling(optionInfo);
                    }
                    text.appendSibling(option).appendText(", ");

                    option = new StringTextComponent("boss");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category boss"));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("boss"))
                    {
                        ITextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.getStyle().setColor(TextFormatting.WHITE);
                        ITextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.getStyle().setColor(TextFormatting.RED);
                        optionInfo.appendSibling(optionInfoBool).appendText(")");
                        option.appendSibling(optionInfo);
                    }
                    else
                    {
                        ITextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.getStyle().setColor(TextFormatting.WHITE);
                        ITextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.getStyle().setColor(TextFormatting.GREEN);
                        optionInfo.appendSibling(optionInfoBool).appendText(")");
                        option.appendSibling(optionInfo);
                    }
                    text.appendSibling(option).appendText(", ");

                    option = new StringTextComponent("player");
                    option.getStyle().setColor(TextFormatting.YELLOW).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category player"));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("player"))
                    {
                        ITextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.getStyle().setColor(TextFormatting.WHITE);
                        ITextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.getStyle().setColor(TextFormatting.RED);
                        optionInfo.appendSibling(optionInfoBool).appendText(")");
                        option.appendSibling(optionInfo);
                    }
                    else
                    {
                        ITextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.getStyle().setColor(TextFormatting.WHITE);
                        ITextComponent optionInfoBool = new StringTextComponent("enabled");
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("decisionAttack: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 0; i <= 100; i += 10)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i) + "%");
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("decisionDefend: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 0; i <= 100; i += 10)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i) + "%");
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
                    ITextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.getStyle().setColor(TextFormatting.GREEN).setBold(true);
                    ITextComponent text = new StringTextComponent("decisionFlee: ");
                    text.getStyle().setColor(TextFormatting.WHITE).setBold(false);

                    for(int i = 0; i <= 100; i += 10)
                    {
                        ITextComponent option = new StringTextComponent(Integer.toString(i) + "%");
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
    		});
    		ctx.get().setPacketHandled(true);
    	}
    }
}
