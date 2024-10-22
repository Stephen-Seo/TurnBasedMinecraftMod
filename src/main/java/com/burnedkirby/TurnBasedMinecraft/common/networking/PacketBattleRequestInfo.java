package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
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

public record PacketBattleRequestInfo(int battleID) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketBattleRequestInfo> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetbattlerequestinfo"));

    public static final StreamCodec<ByteBuf, PacketBattleRequestInfo> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketBattleRequestInfo::battleID,
        PacketBattleRequestInfo::new
    );

    public PacketBattleRequestInfo(int battleID)
    {
        this.battleID = battleID;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class PayloadHandler implements IPayloadHandler<PacketBattleRequestInfo> {
        @Override
        public void handle(final @NotNull PacketBattleRequestInfo pkt, final IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
                if(b == null) {
                    return;
                }
                ctx.reply(new PacketBattleInfo(
                        b.getId(),
                        b.getSideAIDs(),
                        b.getSideBIDs(),
                        b.getTimerNanos(),
                        TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos(),
                        !TurnBasedMinecraftMod.proxy.getConfig().isBattleDecisionDurationForever()));
            }).exceptionally(e -> {
                ctx.disconnect(Component.literal("Exception handling PacketBattleRequestInfo! " + e.getMessage()));
                return null;
            });
        }
    }
}
