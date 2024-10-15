package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.BiConsumer;
import java.util.function.Function;

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

    public static class Encoder implements BiConsumer<PacketGeneralMessage, RegistryFriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketGeneralMessage pkt, RegistryFriendlyByteBuf buf) {
            buf.writeUtf(pkt.message);
        }
    }

    public static class Decoder implements Function<RegistryFriendlyByteBuf, PacketGeneralMessage> {
        public Decoder() {}

        @Override
        public PacketGeneralMessage apply(RegistryFriendlyByteBuf buf) {
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
