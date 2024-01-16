package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class PacketBattleRequestInfo implements CustomPacketPayload
{
    public static final ResourceLocation ID = new ResourceLocation(TurnBasedMinecraftMod.MODID, "network_packetbattlerequestinfo");

    private int battleID;
    
    public PacketBattleRequestInfo() {}
    
    public PacketBattleRequestInfo(int battleID)
    {
        this.battleID = battleID;
    }

    public PacketBattleRequestInfo(final FriendlyByteBuf buf) {
        battleID = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(battleID);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static class PayloadHandler {
        private static final PayloadHandler INSTANCE = new PayloadHandler();

        public static PayloadHandler getInstance() {
            return INSTANCE;
        }

        public void handleData(final PacketBattleRequestInfo pkt, final PlayPayloadContext ctx) {
            ctx.workHandler().submitAsync(() -> {
                Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
                if(b == null) {
                    return;
                }
                ctx.replyHandler().send(new PacketBattleInfo(
                    b.getSideAIDs(),
                    b.getSideBIDs(),
                    b.getTimerNanos(),
                    TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos(),
                    !TurnBasedMinecraftMod.proxy.getConfig().isBattleDecisionDurationForever()));
            }).exceptionally(e -> {
                ctx.packetHandler().disconnect(Component.literal("Exception handling PacketBattleRequestInfo! " + e.getMessage()));
                return null;
            });
        }
    }
}
