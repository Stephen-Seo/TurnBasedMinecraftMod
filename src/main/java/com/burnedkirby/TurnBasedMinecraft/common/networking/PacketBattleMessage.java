package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.HashMap;
import java.util.Map;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import com.burnedkirby.TurnBasedMinecraft.common.Utility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class PacketBattleMessage implements CustomPacketPayload
{
    public static final ResourceLocation ID = new ResourceLocation(TurnBasedMinecraftMod.MODID, "network_packetbattlemessage");

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(messageType.getValue());
        buf.writeInt(entityIDFrom);
        buf.writeInt(entityIDTo);
        buf.writeUtf(Utility.serializeDimension(dimension));
        buf.writeInt(amount);
        buf.writeUtf(custom);
    }

    @Override
    public ResourceLocation id() {
        return ID;
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

    public PacketBattleMessage(final FriendlyByteBuf buf) {
        this.messageType = MessageType.valueOf(buf.readInt());
        this.entityIDFrom = buf.readInt();
        this.entityIDTo = buf.readInt();
        this.dimension = Utility.deserializeDimension(buf.readUtf());
        this.amount = buf.readInt();
        this.custom = buf.readUtf();
    }

    public static class PayloadHandler {
        private static final PayloadHandler INSTANCE = new PayloadHandler();

        public static PayloadHandler getInstance() {
            return INSTANCE;
        }

        public void handleData(final PacketBattleMessage pkt, final PlayPayloadContext ctx) {
            ctx.workHandler().submitAsync(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx);
                }
            }).exceptionally(e -> {
                ctx.packetHandler().disconnect(Component.literal("Exception handling PacketBattleMessage! " + e.getMessage()));
                return null;
            });
        }
    }
}
