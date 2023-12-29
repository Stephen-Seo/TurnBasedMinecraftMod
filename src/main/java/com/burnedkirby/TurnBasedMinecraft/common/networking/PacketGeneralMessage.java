package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.simple.MessageFunctions;

public class PacketGeneralMessage
{
    String message;

    public String getMessage() {
        return message;
    }
    
    public PacketGeneralMessage()
    {
        message = new String();
    }
    
    public PacketGeneralMessage(String message)
    {
        this.message = message;
    }

    public static class Encoder implements MessageFunctions.MessageEncoder<PacketGeneralMessage> {
        public Encoder() {}

        @Override
        public void encode(PacketGeneralMessage pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.message);
        }
    }

    public static class Decoder implements MessageFunctions.MessageDecoder<PacketGeneralMessage> {
        public Decoder() {}

        @Override
        public PacketGeneralMessage decode(FriendlyByteBuf buf) {
            return new PacketGeneralMessage(buf.readUtf());
        }
    }

    public static class Consumer implements MessageFunctions.MessageConsumer<PacketGeneralMessage> {
        public Consumer() {}

        @Override
        public void handle(PacketGeneralMessage pkt, NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
