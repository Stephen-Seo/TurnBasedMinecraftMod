package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record PacketBattlePing(int battleID, int remainingSeconds) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketBattlePing> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetbattleping"));

    public static final StreamCodec<ByteBuf, PacketBattlePing> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketBattlePing::battleID,
        ByteBufCodecs.VAR_INT,
        PacketBattlePing::remainingSeconds,
        PacketBattlePing::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class PayloadHandler implements IPayloadHandler<PacketBattlePing> {
        @Override
        public void handle(final @NotNull PacketBattlePing pkt, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (TurnBasedMinecraftMod.proxy.getLocalBattle() == null) {
                    TurnBasedMinecraftMod.proxy.createLocalBattle(pkt.battleID);
                }
                TurnBasedMinecraftMod.proxy.setBattleGuiAsGui();
                TurnBasedMinecraftMod.proxy.setBattleGuiBattleChanged();
                TurnBasedMinecraftMod.proxy.setBattleGuiTime(pkt.remainingSeconds);
            }).exceptionally(e -> {
                ctx.disconnect(Component.literal("Exception handling PacketBattlePing! " + e.getMessage()));
                return null;
            });
        }
    }
}
