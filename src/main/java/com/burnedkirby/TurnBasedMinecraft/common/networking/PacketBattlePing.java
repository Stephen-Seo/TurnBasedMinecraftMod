package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public record PacketBattlePing(int battleID, int decisionSeconds) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacketBattlePing> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetbattleping"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketBattlePing> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PacketBattlePing::battleID,
            ByteBufCodecs.VAR_INT,
            PacketBattlePing::decisionSeconds,
            PacketBattlePing::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Consumer implements BiConsumer<PacketBattlePing, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketBattlePing pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                if (TurnBasedMinecraftMod.proxy.getLocalBattle() != null) {
                    TurnBasedMinecraftMod.proxy.setBattleGuiAsGui();
                    TurnBasedMinecraftMod.proxy.setBattleGuiBattleChanged();
                    TurnBasedMinecraftMod.proxy.setBattleGuiTime(pkt.decisionSeconds);
                    TurnBasedMinecraftMod.proxy.pauseMCMusic();
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
