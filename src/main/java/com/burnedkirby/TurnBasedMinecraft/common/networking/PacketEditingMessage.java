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
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("Attack the entity you want to edit for TurnBasedMinecraftMod. ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    StringTextComponent cancel = new StringTextComponent("Cancel");
                    cancel.func_230530_a_(cancel.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFF0000)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel")));
                    text.getSiblings().add(cancel);

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case PICK_EDIT:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("Edit what value? ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    StringTextComponent option = new StringTextComponent("IgB");
                    // HoverEvent.Action.field_230550_a_ is probably SHOW_TEXT
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("IgnoreBattle"))));
                    StringTextComponent value = new StringTextComponent("(" + pkt.entityInfo.ignoreBattle + ") ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("AP");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("AttackPower"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackPower + ") ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("APr");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("AttackProbability"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackProbability + "%) ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("AV");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("AttackVariance"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackVariance + ") ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("AE");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("AttackEffect"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackEffect.toString() + ") ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("AEPr");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("AttackEffectProbability"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.attackEffectProbability + "%) ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("DD");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("DefenseDamage"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.defenseDamage + ") ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("DDPr");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("DefenseDamageProbability"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.defenseDamageProbability + "%) ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("E");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("Evasion"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.evasion + "%) ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("S");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("Speed"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.speed + ") ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("C");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("Category"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.category + ") ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("DecA");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("DecisionAttack"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.decisionAttack + "%) ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("DecD");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("DecisionDefend"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.decisionDefend + "%) ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("DecF");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee"))
                        .func_240716_a_(new HoverEvent(HoverEvent.Action.field_230550_a_, new StringTextComponent("DecisionFlee"))));
                    value = new StringTextComponent("(" + pkt.entityInfo.decisionFlee + "%) ");
                    value.func_230530_a_(value.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                    option.getSiblings().add(value);
                    text.getSiblings().add(option);

                    option = new StringTextComponent("Finished Editing");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit finish")));
                    text.getSiblings().add(option);
                    text.getSiblings().add(new StringTextComponent(" "));

                    option = new StringTextComponent("Cancel");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFF0000)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit cancel")));
                    text.getSiblings().add(option);

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_IGNORE_BATTLE:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("ignoreBattle: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    StringTextComponent option = new StringTextComponent("true");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle true")));
                    text.getSiblings().add(option);

                    text.getSiblings().add(new StringTextComponent(" "));

                    option = new StringTextComponent("false");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFF0000)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit ignoreBattle false")));
                    text.getSiblings().add(option);

                    prefix.getSiblings().add(text);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                    break;
                }
                case EDIT_ATTACK_POWER:
                {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("attackPower: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 0; i <= 15; ++i)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i));
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackPower " + Integer.toString(i))));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("attackProbability: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 10; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackProbability " + Integer.toString(i))));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("attackVariance: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 0; i <= 10; ++i)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i));
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackVariance " + Integer.toString(i))));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("attackEffect: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(EntityInfo.Effect e : EntityInfo.Effect.values())
                    {
                        StringTextComponent option = new StringTextComponent(e.toString());
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffect " + e.toString())));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("attackEffectProbability: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit attackEffectProbability " + Integer.toString(i))));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("defenseDamage: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 0; i <= 15; ++i)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i));
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamage " + Integer.toString(i))));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("defenseDamageProbability: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit defenseDamageProbability " + Integer.toString(i))));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("evasion: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit evasion " + Integer.toString(i))));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("speed: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i));
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit speed " + Integer.toString(i))));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("category: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    StringTextComponent option = new StringTextComponent("monster");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category monster")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("monster"))
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.func_230530_a_(optionInfo.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.func_230530_a_(optionInfoBool.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.func_230530_a_(optionInfo.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.func_230530_a_(optionInfoBool.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new StringTextComponent(", "));

                    option = new StringTextComponent("animal");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category animal")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("animal"))
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.func_230530_a_(optionInfo.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.func_230530_a_(optionInfoBool.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.func_230530_a_(optionInfo.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.func_230530_a_(optionInfoBool.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new StringTextComponent(", "));

                    option = new StringTextComponent("passive");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category passive")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("passive"))
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.func_230530_a_(optionInfo.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.func_230530_a_(optionInfoBool.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.func_230530_a_(optionInfo.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.func_230530_a_(optionInfoBool.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new StringTextComponent(", "));

                    option = new StringTextComponent("boss");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category boss")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("boss"))
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.func_230530_a_(optionInfo.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.func_230530_a_(optionInfoBool.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.func_230530_a_(optionInfo.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.func_230530_a_(optionInfoBool.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    text.getSiblings().add(option);
                    text.getSiblings().add(new StringTextComponent(", "));

                    option = new StringTextComponent("player");
                    option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit category player")));
                    if(TurnBasedMinecraftMod.proxy.getConfig().isIgnoreBattleType("player"))
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.func_230530_a_(optionInfo.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("disabled");
                        optionInfoBool.func_230530_a_(optionInfoBool.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFF0000)));
                        optionInfo.getSiblings().add(optionInfoBool);
                        optionInfo.getSiblings().add(new StringTextComponent(")"));
                        option.getSiblings().add(optionInfo);
                    }
                    else
                    {
                        StringTextComponent optionInfo = new StringTextComponent("(battle-");
                        optionInfo.func_230530_a_(optionInfo.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)));
                        StringTextComponent optionInfoBool = new StringTextComponent("enabled");
                        optionInfoBool.func_230530_a_(optionInfoBool.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("decisionAttack: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionAttack " + Integer.toString(i))));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("decisionDefend: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionDefend " + Integer.toString(i))));
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
                    prefix.func_230530_a_(prefix.getStyle().func_240718_a_(Color.func_240743_a_(0xFF00FF00)).func_240713_a_(true));
                    StringTextComponent text = new StringTextComponent("decisionFlee: ");
                    text.func_230530_a_(text.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFFFF)).func_240713_a_(false));

                    for(int i = 0; i <= 100; i += 10)
                    {
                        StringTextComponent option = new StringTextComponent(Integer.toString(i) + "%");
                        option.func_230530_a_(option.getStyle().func_240718_a_(Color.func_240743_a_(0xFFFFFF00)).func_240715_a_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tbm-edit edit decisionFlee " + Integer.toString(i))));
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
