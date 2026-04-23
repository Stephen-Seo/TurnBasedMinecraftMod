package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraftforge.event.network.CustomPayloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public record PacketBattleRequestInfo(int battleID) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketBattleRequestInfo> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetbattlerequestinfo"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PacketBattleRequestInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PacketBattleRequestInfo::battleID,
            PacketBattleRequestInfo::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Consumer implements BiConsumer<PacketBattleRequestInfo, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketBattleRequestInfo pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
                if(b == null) {
                    return;
                }
                TurnBasedMinecraftMod.getHandler().reply(new PacketBattleInfo(b.getId(), b.getSideAIDs(), b.getSideBIDs(), b.getTimerNanos(), TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos(), !TurnBasedMinecraftMod.proxy.getConfig().isBattleDecisionDurationForever()), ctx);
            });
            ctx.setPacketHandled(true);
        }
    }
}
