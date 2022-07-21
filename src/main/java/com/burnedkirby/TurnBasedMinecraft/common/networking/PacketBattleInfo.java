package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketBattleInfo
{
    private Collection<Integer> sideA;
    private Collection<Integer> sideB;
    private long decisionNanos;
    private boolean turnTimerEnabled;
    
    public PacketBattleInfo()
    {
        sideA = new ArrayList<Integer>();
        sideB = new ArrayList<Integer>();
        decisionNanos = TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos();
        turnTimerEnabled = false;
    }

    public PacketBattleInfo(Collection<Integer> sideA, Collection<Integer> sideB, long decisionNanos, boolean turnTimerEnabled)
    {
        this.sideA = sideA;
        this.sideB = sideB;
        this.decisionNanos = decisionNanos;
        this.turnTimerEnabled = turnTimerEnabled;
    }

    public static void encode(PacketBattleInfo msg, FriendlyByteBuf buf) {
    	buf.writeInt(msg.sideA.size());
    	buf.writeInt(msg.sideB.size());
    	for(Integer id : msg.sideA) {
    		buf.writeInt(id);
    	}
    	for(Integer id : msg.sideB) {
    		buf.writeInt(id);
    	}
    	buf.writeLong(msg.decisionNanos);
        buf.writeBoolean(msg.turnTimerEnabled);
    }

    public static PacketBattleInfo decode(FriendlyByteBuf buf) {
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
        boolean turnTimerEnabled = buf.readBoolean();
    	return new PacketBattleInfo(sideA, sideB, decisionNanos, turnTimerEnabled);
    }
    
    public static void handle(final PacketBattleInfo pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if(TurnBasedMinecraftMod.proxy.getLocalBattle() == null)
            {
                return;
            }
            TurnBasedMinecraftMod.proxy.getLocalBattle().clearCombatants();
            for(Integer id : pkt.sideA)
            {
                Entity e = Minecraft.getInstance().level.getEntity(id);
                if(e != null)
                {
                    TurnBasedMinecraftMod.proxy.getLocalBattle().addCombatantToSideA(e);
                }
            }
            for(Integer id : pkt.sideB)
            {
                Entity e = Minecraft.getInstance().level.getEntity(id);
                if(e != null)
                {
                    TurnBasedMinecraftMod.proxy.getLocalBattle().addCombatantToSideB(e);
                }
            }
            TurnBasedMinecraftMod.proxy.setBattleGuiTime((int)(pkt.decisionNanos / 1000000000L));
            TurnBasedMinecraftMod.proxy.setBattleGuiBattleChanged();
            TurnBasedMinecraftMod.proxy.setBattleGuiTurnTimerEnabled(pkt.turnTimerEnabled);
        });
        ctx.get().setPacketHandled(true);
    }
}
