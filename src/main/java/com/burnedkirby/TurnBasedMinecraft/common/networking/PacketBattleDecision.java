package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.burnedkirby.TurnBasedMinecraft.common.Battle.Decision;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketBattleDecision
{
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

    public static class Encoder implements BiConsumer<PacketBattleDecision, FriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketBattleDecision pkt, FriendlyByteBuf buf) {
            buf.writeInt(pkt.battleID);
            buf.writeInt(pkt.decision.getValue());
            buf.writeInt(pkt.targetIDOrItemID);
        }
    }

    public static class Decoder implements Function<FriendlyByteBuf, PacketBattleDecision> {
        public Decoder() {}

        @Override
        public PacketBattleDecision apply(FriendlyByteBuf buf) {
            return new PacketBattleDecision(buf.readInt(), Decision.valueOf(buf.readInt()), buf.readInt());
        }
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
                    b.setDecision(player.getId(), pkt.decision, pkt.targetIDOrItemID);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
