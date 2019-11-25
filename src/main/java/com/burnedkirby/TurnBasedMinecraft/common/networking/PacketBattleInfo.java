package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class PacketBattleInfo
{
    private Collection<Integer> sideA;
    private Collection<Integer> sideB;
    private long decisionNanos;
    
    public PacketBattleInfo()
    {
        sideA = new ArrayList<Integer>();
        sideB = new ArrayList<Integer>();
        decisionNanos = TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos();
    }
    
    public PacketBattleInfo(Collection<Integer> sideA, Collection<Integer> sideB, long decisionNanos)
    {
        this.sideA = sideA;
        this.sideB = sideB;
        this.decisionNanos = decisionNanos;
    }
    
    public static void encode(PacketBattleInfo pkt, PacketBuffer buf) {
    	buf.writeInt(pkt.sideA.size());
    	buf.writeInt(pkt.sideB.size());
    	for(Integer id : pkt.sideA) {
    		buf.writeInt(id);
    	}
    	for(Integer id : pkt.sideB) {
    		buf.writeInt(id);
    	}
    	buf.writeLong(pkt.decisionNanos);
    }
    
    public static PacketBattleInfo decode(PacketBuffer buf) {
    	int sideACount = buf.readInt();
    	int sideBCount = buf.readInt();
    	Collection<Integer> sideA = new ArrayList<Integer>(sideACount);
    	Collection<Integer> sideB = new ArrayList<Integer>(sideBCount);
    	for(int i = 0; i < sideACount; ++i) {
    		sideA.add(buf.readInt());
    	}
    	for(int i = 0; i < sideBCount; ++i) {
    		sideB.add(buf.readInt());
    	}
    	long decisionNanos = buf.readLong();
    	return new PacketBattleInfo(sideA, sideB, decisionNanos);
    }
    
    public static class Handler {
    	public static void handle(final PacketBattleInfo pkt, Supplier<NetworkEvent.Context> ctx) {
    		ctx.get().enqueueWork(() -> {
                if(TurnBasedMinecraftMod.proxy.getLocalBattle() == null)
                {
                    return;
                }
                TurnBasedMinecraftMod.proxy.getLocalBattle().clearCombatants();
                for(Integer id : pkt.sideA)
                {
                    Entity e = Minecraft.getInstance().world.getEntityByID(id);
                    if(e != null)
                    {
                        TurnBasedMinecraftMod.proxy.getLocalBattle().addCombatantToSideA(e);
                    }
                }
                for(Integer id : pkt.sideB)
                {
                    Entity e = Minecraft.getInstance().world.getEntityByID(id);
                    if(e != null)
                    {
                        TurnBasedMinecraftMod.proxy.getLocalBattle().addCombatantToSideB(e);
                    }
                }
                TurnBasedMinecraftMod.proxy.setBattleGuiTime((int)(pkt.decisionNanos / 1000000000L));
                TurnBasedMinecraftMod.proxy.setBattleGuiBattleChanged();
    		});
    		ctx.get().setPacketHandled(true);
    	}
    }
}
