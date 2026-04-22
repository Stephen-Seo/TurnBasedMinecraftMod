package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public record PacketClientGui(int reserved) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketClientGui> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetclientgui"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketClientGui> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PacketClientGui::reserved,
            PacketClientGui::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Consumer implements BiConsumer<PacketClientGui, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketClientGui pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.showClientConfigGui();
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
