package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public record PacketBattleDecision(int battleID, int decision, int targetIDOrItemID) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketBattleDecision> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetbattledecision"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketBattleDecision> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PacketBattleDecision::battleID,
            ByteBufCodecs.VAR_INT,
            PacketBattleDecision::decision,
            ByteBufCodecs.INT,
            PacketBattleDecision::targetIDOrItemID,
            PacketBattleDecision::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Consumer implements BiConsumer<PacketBattleDecision, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketBattleDecision pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
                if(b != null)
                {
                    ServerPlayer player = ctx.getSender();
                    b.setDecision(player.getId(), Battle.Decision.valueOf(pkt.decision), pkt.targetIDOrItemID);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
