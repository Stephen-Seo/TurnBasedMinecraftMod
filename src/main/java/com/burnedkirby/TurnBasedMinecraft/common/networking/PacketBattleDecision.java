package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.function.Supplier;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.burnedkirby.TurnBasedMinecraft.common.Battle.Decision;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

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
    
    public static void encode(PacketBattleDecision pkt, PacketBuffer buf) {
    	buf.writeInt(pkt.battleID);
    	buf.writeInt(pkt.decision.getValue());
    	buf.writeInt(pkt.targetIDOrItemID);
    }
    
    public static PacketBattleDecision decode(PacketBuffer buf) {
    	return new PacketBattleDecision(buf.readInt(), Decision.valueOf(buf.readInt()), buf.readInt());
    }
    
    public static class Handler {
    	public static void handle(final PacketBattleDecision pkt, Supplier<NetworkEvent.Context> ctx) {
    		ctx.get().enqueueWork(() -> {
                Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
                if(b != null)
                {
                    ServerPlayerEntity player = ctx.get().getSender();
                    b.setDecision(player.getId(), pkt.decision, pkt.targetIDOrItemID);
                }
    		});
    		ctx.get().setPacketHandled(true);
    	}
    }
}
