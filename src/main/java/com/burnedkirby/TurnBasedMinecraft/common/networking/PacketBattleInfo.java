package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.CommonProxy;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public record PacketBattleInfo(int battleID, Collection<Integer> sideA, Collection<Integer> sideB, long decisionNanos, long maxDecisionNanos, boolean turnTimerEnabled) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<PacketBattleInfo> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TurnBasedMinecraftMod.MODID, "network_packetbattleinfo"));

    public static final StreamCodec<ByteBuf, PacketBattleInfo> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        PacketBattleInfo::battleID,
        CommonProxy.COLLECTION_INT_CODEC,
        PacketBattleInfo::sideA,
        CommonProxy.COLLECTION_INT_CODEC,
        PacketBattleInfo::sideB,
        ByteBufCodecs.VAR_LONG,
        PacketBattleInfo::decisionNanos,
        ByteBufCodecs.VAR_LONG,
        PacketBattleInfo::maxDecisionNanos,
        ByteBufCodecs.BOOL,
        PacketBattleInfo::turnTimerEnabled,
        PacketBattleInfo::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class PayloadHandler implements IPayloadHandler<PacketBattleInfo> {
        @Override
        public void handle(final @NotNull PacketBattleInfo pkt, final IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if(TurnBasedMinecraftMod.proxy.getLocalBattle() == null)
                {
                    TurnBasedMinecraftMod.proxy.createLocalBattle(pkt.battleID);
                }
                TurnBasedMinecraftMod.proxy.getLocalBattle().clearCombatants();
                for(Integer id : pkt.sideA)
                {
                    Entity e = Minecraft.getInstance().level.getEntity(id);
                    if(e != null)
                    {
                        TurnBasedMinecraftMod.proxy.getLocalBattle().addCombatantToSideA(e);
                    }
                }
                for(Integer id : pkt.sideB)
                {
                    Entity e = Minecraft.getInstance().level.getEntity(id);
                    if(e != null)
                    {
                        TurnBasedMinecraftMod.proxy.getLocalBattle().addCombatantToSideB(e);
                    }
                }
                TurnBasedMinecraftMod.proxy.setBattleGuiAsGui();
                TurnBasedMinecraftMod.proxy.setBattleGuiTime((int)(pkt.decisionNanos / 1000000000L));
                TurnBasedMinecraftMod.proxy.setBattleGuiBattleChanged();
                TurnBasedMinecraftMod.proxy.setBattleGuiTurnTimerEnabled(pkt.turnTimerEnabled);
                TurnBasedMinecraftMod.proxy.setBattleGuiTurnTimerMax((int)(pkt.maxDecisionNanos / 1000000000L));
            }).exceptionally(e -> {
                ctx.disconnect(Component.literal("Exception handling PacketBattleInfo! " + e.getMessage()));
                return null;
            });
        }
    }
}
