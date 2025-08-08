package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public record PacketGeneralMessage(String message) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketGeneralMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetgeneralmessage"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketGeneralMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            PacketGeneralMessage::message,
            PacketGeneralMessage::new
    );
    public String getMessage() {
        return message;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Consumer implements BiConsumer<PacketGeneralMessage, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketGeneralMessage pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
