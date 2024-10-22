package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketBattleRequestInfo
{
    private int battleID;
    
    public PacketBattleRequestInfo() {}
    
    public PacketBattleRequestInfo(int battleID)
    {
        this.battleID = battleID;
    }

    public static class Encoder implements BiConsumer<PacketBattleRequestInfo, RegistryFriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketBattleRequestInfo pkt, RegistryFriendlyByteBuf buf) {
            buf.writeInt(pkt.battleID);
        }
    }

    public static class Decoder implements Function<RegistryFriendlyByteBuf, PacketBattleRequestInfo> {
        public Decoder() {}

        @Override
        public PacketBattleRequestInfo apply(RegistryFriendlyByteBuf buf) {
            return new PacketBattleRequestInfo(buf.readInt());
        }
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
