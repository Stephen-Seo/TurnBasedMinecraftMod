package com.seodisparate.TurnBasedMinecraft.common.networking;

import java.util.function.Supplier;

import com.seodisparate.TurnBasedMinecraft.common.Battle;
import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class PacketBattleRequestInfo
{
    private int battleID;
    
    public PacketBattleRequestInfo() {}
    
    public PacketBattleRequestInfo(int battleID)
    {
        this.battleID = battleID;
    }
    
    public static void encode(PacketBattleRequestInfo pkt, PacketBuffer buf) {
    	buf.writeInt(pkt.battleID);
    }
    
    public static PacketBattleRequestInfo decode(PacketBuffer buf) {
    	return new PacketBattleRequestInfo(buf.readInt());
    }
    
    public static class Handler {
    	public static void handle(final PacketBattleRequestInfo pkt, Supplier<NetworkEvent.Context> ctx) {
    		ctx.get().enqueueWork(() -> {
                Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
                if(b == null) {
                	return;
                }
                TurnBasedMinecraftMod.getHandler().reply(new PacketBattleInfo(b.getSideAIDs(), b.getSideBIDs(), b.getTimerSeconds()), ctx.get());
    		});
    		ctx.get().setPacketHandled(true);
    	}
    }
}
