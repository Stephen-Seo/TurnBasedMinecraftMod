package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.function.Supplier;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketBattleRequestInfo
{
    private int battleID;
    
    public PacketBattleRequestInfo() {}
    
    public PacketBattleRequestInfo(int battleID)
    {
        this.battleID = battleID;
    }
    
    public static void encode(PacketBattleRequestInfo pkt, FriendlyByteBuf buf) {
    	buf.writeInt(pkt.battleID);
    }
    
    public static PacketBattleRequestInfo decode(FriendlyByteBuf buf) {
    	return new PacketBattleRequestInfo(buf.readInt());
    }
    
    public static void handle(final PacketBattleRequestInfo pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
            if(b == null) {
                return;
            }
            TurnBasedMinecraftMod.getHandler().reply(new PacketBattleInfo(b.getSideAIDs(), b.getSideBIDs(), b.getTimerNanos(), TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos(), !TurnBasedMinecraftMod.proxy.getConfig().isBattleDecisionDurationForever()), ctx.get());
        });
        ctx.get().setPacketHandled(true);
    }
}
