package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.EntityInfo;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.simple.MessageFunctions;

import java.util.HashMap;
import java.util.Map;

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
        EDIT_DECISION_FLEE(15),
        SERVER_EDIT(16);

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

    public Type getType() {
        return type;
    }

    public EntityInfo getEntityInfo() {
        return entityInfo;
    }

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

    public static class Encoder implements MessageFunctions.MessageEncoder<PacketEditingMessage> {
        public Encoder() {}

        @Override
        public void encode(PacketEditingMessage pkt, FriendlyByteBuf buf) {
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
    }

    public static class Decoder implements MessageFunctions.MessageDecoder<PacketEditingMessage> {
        public Decoder() {}

        @Override
        public PacketEditingMessage decode(FriendlyByteBuf buf) {
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
    }

    public static class Consumer implements MessageFunctions.MessageConsumer<PacketEditingMessage> {
        public Consumer() {}

        @Override
        public void handle(PacketEditingMessage pkt, NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
