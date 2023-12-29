package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

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

    public static class Encoder implements BiConsumer<PacketGeneralMessage, FriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketGeneralMessage pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.message);
        }
    }

    public static class Decoder implements Function<FriendlyByteBuf, PacketGeneralMessage> {
        public Decoder() {}

        @Override
        public PacketGeneralMessage apply(FriendlyByteBuf buf) {
            return new PacketGeneralMessage(buf.readUtf());
        }
    }

    public static class Consumer implements BiConsumer<PacketGeneralMessage, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketGeneralMessage pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx));
            });
            ctx.setPacketHandled(true);
        }
    }
}
