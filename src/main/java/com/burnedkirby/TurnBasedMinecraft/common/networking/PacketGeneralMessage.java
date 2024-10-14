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

public record PacketGeneralMessage(String message) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketGeneralMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetgeneralmessage"));

    public static final StreamCodec<ByteBuf, PacketGeneralMessage> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        PacketGeneralMessage::message,
        PacketGeneralMessage::new
    );

    public String getMessage() {
        return message;
    }

    public PacketGeneralMessage(String message)
    {
        this.message = message;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class PayloadHandler implements IPayloadHandler<PacketGeneralMessage> {
        @Override
        public void handle(final @NotNull PacketGeneralMessage pkt, final IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx);
                }
            }).exceptionally(e -> {
                ctx.disconnect(Component.literal("Exception handling PacketGeneralMessage! " + e.getMessage()));
                return null;
            });
        }
    }
}
