package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.EntityInfo;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PacketEditingMessage implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketEditingMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packeteditingmessage"));

    public static final StreamCodec<ByteBuf, PacketEditingMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT.map(Type::valueOf, Type::getValue),
        PacketEditingMessage::getType,
        StreamCodec.ofMember(EntityInfo::encode, EntityInfo::new),
        PacketEditingMessage::getEntityInfo,
        PacketEditingMessage::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
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

    public PacketEditingMessage(final FriendlyByteBuf buf) {
        this.type = Type.valueOf(buf.readInt());
        this.entityInfo = new EntityInfo();
        try {
            this.entityInfo.classType = this.entityInfo.getClass().getClassLoader().loadClass(buf.readUtf());
        } catch (ClassNotFoundException e) { /* ignored */ }
        this.entityInfo.ignoreBattle = buf.readBoolean();
        this.entityInfo.attackPower = buf.readInt();
        this.entityInfo.attackProbability = buf.readInt();
        this.entityInfo.attackVariance = buf.readInt();
        this.entityInfo.attackEffect = EntityInfo.Effect.fromString(buf.readUtf());
        this.entityInfo.attackEffectProbability = buf.readInt();
        this.entityInfo.defenseDamage = buf.readInt();
        this.entityInfo.defenseDamageProbability = buf.readInt();
        this.entityInfo.evasion = buf.readInt();
        this.entityInfo.speed = buf.readInt();
        this.entityInfo.category = buf.readUtf();
        this.entityInfo.decisionAttack = buf.readInt();
        this.entityInfo.decisionDefend = buf.readInt();
        this.entityInfo.decisionFlee = buf.readInt();
        this.entityInfo.customName = buf.readUtf();
    }

    public static class PayloadHandler implements IPayloadHandler<PacketEditingMessage> {
        @Override
        public void handle(final @NotNull PacketEditingMessage pkt, final IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx);
                }
            }).exceptionally(e -> {
                ctx.disconnect(Component.literal("Exception handling PacketEditingMessage! " + e.getMessage()));
                return null;
            });
        }
    }
}