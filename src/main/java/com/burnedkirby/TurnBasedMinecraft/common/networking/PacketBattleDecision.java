package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.burnedkirby.TurnBasedMinecraft.common.Battle.Decision;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class PacketBattleDecision implements CustomPacketPayload
{
    public static final ResourceLocation ID = new ResourceLocation(TurnBasedMinecraftMod.MODID, "network_packetbattledecision");

    private int battleID;
    private Battle.Decision decision;
    private int targetIDOrItemID;
    
    public PacketBattleDecision() {}
    
    public PacketBattleDecision(int battleID, Battle.Decision decision, int targetIDOrItemID)
    {
        this.battleID = battleID;
        this.decision = decision;
        this.targetIDOrItemID = targetIDOrItemID;
    }

    public PacketBattleDecision(final FriendlyByteBuf buf) {
        this.battleID = buf.readInt();
        this.decision = Decision.valueOf(buf.readInt());
        this.targetIDOrItemID = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(battleID);
        buf.writeInt(decision.getValue());
        buf.writeInt(targetIDOrItemID);
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

        public void handleData(final PacketBattleDecision pkt, final PlayPayloadContext ctx) {
           ctx.workHandler().submitAsync(() -> {
               Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
               if(b != null)
               {
                   Player player = ctx.player().get();
                   b.setDecision(player.getId(), pkt.decision, pkt.targetIDOrItemID);
               }
           }).exceptionally(e -> {
               ctx.packetHandler().disconnect(Component.literal("Exception handling PacketBattleDecision! " + e.getMessage()));
               return null;
           });
        }
    }
}
