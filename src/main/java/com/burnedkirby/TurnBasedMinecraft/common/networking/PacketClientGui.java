package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketClientGui {
    int reserved;

    public PacketClientGui() {
        reserved = 0;
    }

    public PacketClientGui(int reserved) {
        this.reserved = reserved;
    }

    public static class Encoder implements BiConsumer<PacketClientGui, RegistryFriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketClientGui pkt, RegistryFriendlyByteBuf buf) {
            buf.writeInt(pkt.reserved);
        }
    }

    public static class Decoder implements Function<RegistryFriendlyByteBuf, PacketClientGui> {
        public Decoder() {}

        @Override
        public PacketClientGui apply(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            return new PacketClientGui(registryFriendlyByteBuf.readInt());
        }
    }

    public static class Consumer implements BiConsumer<PacketClientGui, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketClientGui pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TurnBasedMinecraftMod.proxy.showClientConfigGui());
            });
            ctx.setPacketHandled(true);
        }
    }
}
