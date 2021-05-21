package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.EntityInfo;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
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
    		buf.writeUtf(pkt.entityInfo.classType.getName());
    	} else {
    		buf.writeUtf("unknown");
    	}
    	buf.writeBoolean(pkt.entityInfo.ignoreBattle);
    	buf.writeInt(pkt.entityInfo.attackPower);
    	buf.writeInt(pkt.entityInfo.attackProbability);
    	buf.writeInt(pkt.entityInfo.attackVariance);
    	buf.writeUtf(pkt.entityInfo.attackEffect.toString());
    	buf.writeInt(pkt.entityInfo.attackEffectProbability);
    	buf.writeInt(pkt.entityInfo.defenseDamage);
    	buf.writeInt(pkt.entityInfo.defenseDamageProbability);
    	buf.writeInt(pkt.entityInfo.evasion);
    	buf.writeInt(pkt.entityInfo.speed);
    	buf.writeUtf(pkt.entityInfo.category);
    	buf.writeInt(pkt.entityInfo.decisionAttack);
    	buf.writeInt(pkt.entityInfo.decisionDefend);
    	buf.writeInt(pkt.entityInfo.decisionFlee);
    	buf.writeUtf(pkt.entityInfo.customName);
    }
    
    public static PacketEditingMessage decode(PacketBuffer buf) {
    	Type type = Type.valueOf(buf.readInt());
    	EntityInfo einfo = new EntityInfo();
    	try {
    		einfo.classType = einfo.getClass().getClassLoader().loadClass(buf.readUtf());
    	} catch (ClassNotFoundException e) { /* ignored */ }
        einfo.ignoreBattle = buf.readBoolean();
        einfo.attackPower = buf.readInt();
        einfo.attackProbability = buf.readInt();
        einfo.attackVariance = buf.readInt();
        einfo.attackEffect = EntityInfo.Effect.fromString(buf.readUtf());
        einfo.attackEffectProbability = buf.readInt();
        einfo.defenseDamage = buf.readInt();
        einfo.defenseDamageProbability = buf.readInt();
        einfo.evasion = buf.readInt();
        einfo.speed = buf.readInt();
        einfo.category = buf.readUtf();
        einfo.decisionAttack = buf.readInt();
        einfo.decisionDefend = buf.readInt();
        einfo.decisionFlee = buf.readInt();
        einfo.customName = buf.readUtf();
    	return new PacketEditingMessage(type, einfo);
    }

    public static class Handler {
    	public static void handle(final PacketEditingMessage pkt, Supplier<NetworkEvent.Context> ctx) {
    		ctx.get().enqueueWork(() -> {
    			switch(pkt.type)
                {
                case ATTACK_ENTITY:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("Attack the entity you want to edit for TurnBasedMinecraftMod. ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    StringTextComponent cancel = new StringTextComponent("Cancel");
                    cancel.setStyle(cancel.getStyle().withColor(Color.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel")));
                    text.getSiblings().add(cancel);

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case PICK_EDIT:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("Edit what value? ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    StringTextComponent option = new StringTextComponent("IgB");
                    // HoverEvent.Action.SHOW_TEXT is probably SHOW_TEXT
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("IgnoreBattle"))));
                    StringTextComponent value = new StringTextComponent("(" + pkt.entityInfo.ignoreBattle + ") ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("AP");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("AttackPower"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackPower + ") ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("APr");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("AttackProbability"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackProbability + "%) ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("AV");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("AttackVariance"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackVariance + ") ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("AE");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("AttackEffect"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackEffect.toString() + ") ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("AEPr");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("AttackEffectProbability"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackEffectProbability + "%) ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("DD");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("DefenseDamage"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.defenseDamage + ") ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("DDPr");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("DefenseDamageProbability"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.defenseDamageProbability + "%) ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("E");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Evasion"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.evasion + "%) ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("S");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Speed"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.speed + ") ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("C");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Category"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.category + ") ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("DecA");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("DecisionAttack"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.decisionAttack + "%) ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("DecD");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("DecisionDefend"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.decisionDefend + "%) ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("DecF");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("DecisionFlee"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.decisionFlee + "%) ");
                    value.setStyle(value.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("Finished Editing");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit finish")));
                    text.getSiblings().add(option);
                    text.getSiblings().add(new StringTextComponent(" "));

                    option = new StringTextComponent("Cancel");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel")));
                    text.getSiblings().add(option);

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_IGNORE_BATTLE:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("ignoreBattle: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    StringTextComponent option = new StringTextComponent("true");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle true")));
                    text.getSiblings().add(option);

                    text.getSiblings().add(new StringTextComponent(" "));

                    option = new StringTextComponent("false");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFF0000)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle false")));
                    text.getSiblings().add(option);

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_POWER:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("attackPower: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 15; ++i)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 15)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new StringTextComponent(" (or use command \"/tbm-edit edit attackPower <integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_PROBABILITY:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("attackProbability: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 10; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new StringTextComponent(" (or use command \"/tbm-edit edit attackProbability <percentage-integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_VARIANCE:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("attackVariance: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 10; ++i)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 10)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new StringTextComponent(" (or use command \"/tbm-edit edit attackVariance <integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_EFFECT:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("attackEffect: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(EntityInfo.Effect e : EntityInfo.Effect.values())
                    {
                        StringTextComponent option = new StringTextComponent(e.toString());
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect " + e.toString())));
                        text.getSiblings().add(option);
                        if(e != EntityInfo.Effect.UNKNOWN)
                        {
                            // TODO find a better way to handle printing comma for items before last
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_EFFECT_PROBABILITY:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("attackEffectProbability: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new StringTextComponent(" (or use command \"/tbm-edit edit attackEffectProbability <percentage-integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_DEFENSE_DAMAGE:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("defenseDamage: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 15; ++i)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 15)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new StringTextComponent(" (or use command \"/tbm-edit edit defenseDamage <integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_DEFENSE_DAMAGE_PROBABILITY:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("defenseDamageProbability: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new StringTextComponent(" (or use command \"/tbm-edit edit defenseDamageProbability <percentage-integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_EVASION:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("evasion: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new StringTextComponent(" (or use command \"/tbm-edit edit evasion <percentage-integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_SPEED:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("speed: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i));
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    text.getSiblings().add(new StringTextComponent(" (or use command \"/tbm-edit edit speed <integer>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_CATEGORY:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("category: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    StringTextComponent option = new StringTextComponent("monster");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category monster")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("monster"))
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(Color.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(Color.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new StringTextComponent(", "));

                    option = new StringTextComponent("animal");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category animal")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("animal"))
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(Color.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(Color.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new StringTextComponent(", "));

                    option = new StringTextComponent("passive");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category passive")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("passive"))
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(Color.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(Color.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new StringTextComponent(", "));

                    option = new StringTextComponent("boss");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category boss")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("boss"))
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(Color.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(Color.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new StringTextComponent(", "));

                    option = new StringTextComponent("player");
                    option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category player")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("player"))
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(Color.fromRgb(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.setStyle(optionInfo.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.setStyle(optionInfoBool.getStyle().withColor(Color.fromRgb(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);

                    text.getSiblings().add(new StringTextComponent(" (or use command \"/tbm-edit edit category <string>\")"));

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_DECISION_ATTACK:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("decisionAttack: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_DECISION_DEFEND:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("decisionDefend: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_DECISION_FLEE:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent text = new StringTextComponent("decisionFlee: ");
                    text.setStyle(text.getStyle().withColor(Color.fromRgb(0xFFFFFFFF)).withBold(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.setStyle(option.getStyle().withColor(Color.fromRgb(0xFFFFFF00)).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee " + Integer.toString(i))));
                        text.getSiblings().add(option);
                        if(i < 100)
                        {
                            text.getSiblings().add(new StringTextComponent(", "));
                        }
                    }

                    prefix.getSiblings().add(text);
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
