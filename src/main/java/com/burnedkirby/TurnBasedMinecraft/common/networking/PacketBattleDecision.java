package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.burnedkirby.TurnBasedMinecraft.common.Battle.Decision;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.simple.MessageFunctions;

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

    public static class Encoder implements MessageFunctions.MessageEncoder<PacketBattleDecision> {
        public Encoder() {}

        @Override
        public void encode(PacketBattleDecision pkt, FriendlyByteBuf buf) {
            buf.writeInt(pkt.battleID);
            buf.writeInt(pkt.decision.getValue());
            buf.writeInt(pkt.targetIDOrItemID);
        }
    }

    public static class Decoder implements MessageFunctions.MessageDecoder<PacketBattleDecision> {
        public Decoder() {}

        @Override
        public PacketBattleDecision decode(FriendlyByteBuf buf) {
            return new PacketBattleDecision(buf.readInt(), Decision.valueOf(buf.readInt()), buf.readInt());
        }
    }

    public static class Consumer implements MessageFunctions.MessageConsumer<PacketBattleDecision> {
        public Consumer() {}

        @Override
        public void handle(PacketBattleDecision pkt, NetworkEvent.Context ctx) {
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
