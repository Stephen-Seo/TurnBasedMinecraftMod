package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record PacketBattleDecision(int battleID, int decision, int targetIDorItemID) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketBattleDecision> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetbattledecision"));

    public static final StreamCodec<ByteBuf, PacketBattleDecision> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketBattleDecision::battleID,
        ByteBufCodecs.VAR_INT,
        PacketBattleDecision::decision,
        ByteBufCodecs.INT,
        PacketBattleDecision::targetIDorItemID,
        PacketBattleDecision::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class PayloadHandler implements IPayloadHandler<PacketBattleDecision> {
        @Override
        public void handle(final @NotNull PacketBattleDecision pkt, final IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
                if(b != null) {
                    Player player = ctx.player();
                    b.setDecision(player.getId(), Battle.Decision.valueOf(pkt.decision), pkt.targetIDorItemID);
                }
            }).exceptionally(e -> {
                ctx.disconnect(Component.literal("Exception handling PacketBattleDecision! " + e.getMessage()));
                return null;
            });
        }
    }
}
