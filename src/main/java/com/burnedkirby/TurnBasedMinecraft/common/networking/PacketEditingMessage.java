package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.EntityInfo;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketEditingMessage implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketEditingMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packeteditingmessage"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketEditingMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT.map(Type::valueOf, Type::getValue),
            PacketEditingMessage::getType,
            StreamCodec.ofMember(EntityInfo::encode, EntityInfo::new),
            PacketEditingMessage::getEntityInfo,
            PacketEditingMessage::new
    );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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
        EDIT_HASTE_SPEED(18),
        EDIT_SLOW_SPEED(19),
        EDIT_CATEGORY(12),
        EDIT_DECISION_ATTACK(13),
        EDIT_DECISION_DEFEND(14),
        EDIT_DECISION_FLEE(15),
        SERVER_EDIT(16),
        PICK_PLAYER(17);

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

    public static class Encoder implements BiConsumer<PacketEditingMessage, RegistryFriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketEditingMessage pkt, RegistryFriendlyByteBuf buf) {
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
            buf.writeInt(pkt.entityInfo.hasteSpeed);
            buf.writeInt(pkt.entityInfo.slowSpeed);
            buf.writeUtf(pkt.entityInfo.category);
            buf.writeInt(pkt.entityInfo.decisionAttack);
            buf.writeInt(pkt.entityInfo.decisionDefend);
            buf.writeInt(pkt.entityInfo.decisionFlee);
            buf.writeUtf(pkt.entityInfo.customName);
            buf.writeUtf(pkt.entityInfo.playerName);
        }
    }

    public static class Decoder implements Function<RegistryFriendlyByteBuf, PacketEditingMessage> {
        public Decoder() {}

        @Override
        public PacketEditingMessage apply(RegistryFriendlyByteBuf buf) {
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
            einfo.hasteSpeed = buf.readInt();
            einfo.slowSpeed = buf.readInt();
            einfo.category = buf.readUtf();
            einfo.decisionAttack = buf.readInt();
            einfo.decisionDefend = buf.readInt();
            einfo.decisionFlee = buf.readInt();
            einfo.customName = buf.readUtf();
            einfo.playerName = buf.readUtf();
            return new PacketEditingMessage(type, einfo);
        }
    }

    public static class Consumer implements BiConsumer<PacketEditingMessage, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketEditingMessage pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
