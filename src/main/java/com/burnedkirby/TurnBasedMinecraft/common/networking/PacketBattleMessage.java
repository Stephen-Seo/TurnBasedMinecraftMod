package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.burnedkirby.TurnBasedMinecraft.common.Utility;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PacketBattleMessage implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketBattleMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetbattlemessage"));

    public static final StreamCodec<ByteBuf, PacketBattleMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(MessageType::valueOf, MessageType::getValue),
        PacketBattleMessage::getMessageType,
        ByteBufCodecs.INT,
        PacketBattleMessage::getEntityIDFrom,
        ByteBufCodecs.INT,
        PacketBattleMessage::getEntityIDTo,
        ByteBufCodecs.STRING_UTF8.map(Utility::deserializeDimension, Utility::serializeDimension),
        PacketBattleMessage::getDimension,
        ByteBufCodecs.INT,
        PacketBattleMessage::getAmount,
        ByteBufCodecs.STRING_UTF8,
        PacketBattleMessage::getCustom,
        PacketBattleMessage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum MessageType
    {
        ENTERED(0),
        FLEE(1),
        DIED(2),
        ENDED(3),
        ATTACK(4),
        DEFEND(5),
        DEFENSE_DAMAGE(6),
        MISS(7),
        DEFENDING(8),
        DID_NOTHING(9),
        USED_ITEM(10),
        TURN_BEGIN(11),
        TURN_END(12),
        SWITCHED_ITEM(13),
        WAS_AFFECTED(14),
        BECAME_CREATIVE(15),
        FIRED_ARROW(16),
        ARROW_HIT(17),
        BOW_NO_AMMO(18),
        CREEPER_WAIT(19),
        CREEPER_WAIT_FINAL(20),
        CREEPER_EXPLODE(21),
        CROSSBOW_NO_AMMO(22);
        
        private int value;
        private static Map<Integer, MessageType> map = new HashMap<Integer, MessageType>();
        
        MessageType(int value)
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
        
        static
        {
            for(MessageType type : MessageType.values())
            {
                map.put(type.getValue(), type);
            }
        }
        
        public static MessageType valueOf(int value)
        {
            return map.get(value);
        }
    }
    
    public enum UsedItemAction
    {
        USED_NOTHING(0),
        USED_INVALID(1),
        USED_FOOD(2),
        USED_POTION(3);
        
        private int value;
        private static Map<Integer, UsedItemAction> map = new HashMap<Integer, UsedItemAction>();
        
        UsedItemAction(int value)
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
        
        static
        {
            for(UsedItemAction type : UsedItemAction.values())
            {
                map.put(type.getValue(), type);
            }
        }
        
        public static UsedItemAction valueOf(int value)
        {
            return map.get(value);
        }
    }

    MessageType messageType;
    int entityIDFrom;
    int entityIDTo;
    int amount;
    String custom;
    ResourceKey<Level> dimension;

    public MessageType getMessageType() {
        return messageType;
    }

    public int getEntityIDFrom() {
        return entityIDFrom;
    }

    public int getEntityIDTo() {
        return entityIDTo;
    }

    public int getAmount() {
        return amount;
    }

    public String getCustom() {
        return custom;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public String getDimensionSerialized() {
        return Utility.serializeDimension(dimension);
    }

    public PacketBattleMessage() { custom = new String(); }
    
    public PacketBattleMessage(MessageType messageType, int entityIDFrom, int entityIDTo, ResourceKey<Level> dimension, int amount)
    {
        this.messageType = messageType;
        this.entityIDFrom = entityIDFrom;
        this.entityIDTo = entityIDTo;
        this.dimension = dimension;
        this.amount = amount;
        custom = new String();
    }
    
    public PacketBattleMessage(MessageType messageType, int entityIDFrom, int entityIDTo, ResourceKey<Level> dimension, int amount, String custom)
    {
        this.messageType = messageType;
        this.entityIDFrom = entityIDFrom;
        this.entityIDTo = entityIDTo;
        this.dimension = dimension;
        this.amount = amount;
        this.custom = custom;
    }

    public static class PayloadHandler implements IPayloadHandler<PacketBattleMessage> {
        @Override
        public void handle(final @NotNull PacketBattleMessage pkt, final IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx);
                }
            }).exceptionally(e -> {
                ctx.disconnect(Component.literal("Exception handling PacketBattleMessage! " + e.getMessage()));
                return null;
            });
        }
    }
}
