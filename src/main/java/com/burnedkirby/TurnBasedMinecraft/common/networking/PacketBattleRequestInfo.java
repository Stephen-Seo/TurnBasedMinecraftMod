package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.simple.MessageFunctions;

public class PacketBattleRequestInfo
{
    private int battleID;
    
    public PacketBattleRequestInfo() {}
    
    public PacketBattleRequestInfo(int battleID)
    {
        this.battleID = battleID;
    }

    public static class Encoder implements MessageFunctions.MessageEncoder<PacketBattleRequestInfo> {
        public Encoder() {}

        @Override
        public void encode(PacketBattleRequestInfo pkt, FriendlyByteBuf buf) {
            buf.writeInt(pkt.battleID);
        }
    }

    public static class Decoder implements MessageFunctions.MessageDecoder<PacketBattleRequestInfo> {
        public Decoder() {}

        @Override
        public PacketBattleRequestInfo decode(FriendlyByteBuf buf) {
            return new PacketBattleRequestInfo(buf.readInt());
        }
    }

    public static class Consumer implements MessageFunctions.MessageConsumer<PacketBattleRequestInfo> {
        public Consumer() {}

        @Override
        public void handle(PacketBattleRequestInfo pkt, NetworkEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
                if(b == null) {
                    return;
                }
                TurnBasedMinecraftMod.getHandler().reply(new PacketBattleInfo(b.getSideAIDs(), b.getSideBIDs(), b.getTimerNanos(), TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos(), !TurnBasedMinecraftMod.proxy.getConfig().isBattleDecisionDurationForever()), ctx);
            });
            ctx.setPacketHandled(true);
        }
    }
}
