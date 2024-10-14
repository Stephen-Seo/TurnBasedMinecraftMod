package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record PacketClientGUI(int reserved) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketClientGUI> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetclientgui"));

    public static final StreamCodec<ByteBuf, PacketClientGUI> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        PacketClientGUI::reserved,
        PacketClientGUI::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class PayloadHandler implements IPayloadHandler<PacketClientGUI> {
        @Override
        public void handle(final @NotNull PacketClientGUI pkt, final IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.showClientConfigGui();
                }
            }).exceptionally(e -> {
                ctx.disconnect(Component.literal("Exception handling PacketClientGUI! " + e.getMessage()));
                return null;
            });
        }
    }
}
