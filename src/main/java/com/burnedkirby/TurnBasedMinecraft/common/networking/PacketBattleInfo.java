package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketBattleInfo
{
    private Collection<Integer> sideA;
    private Collection<Integer> sideB;
    private long decisionNanos;

    private long maxDecisionNanos;
    private boolean turnTimerEnabled;
    
    public PacketBattleInfo()
    {
        sideA = new ArrayList<Integer>();
        sideB = new ArrayList<Integer>();
        decisionNanos = TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos();
        maxDecisionNanos = decisionNanos;
        turnTimerEnabled = false;
    }

    public PacketBattleInfo(Collection<Integer> sideA, Collection<Integer> sideB, long decisionNanos, long maxDecisionNanos, boolean turnTimerEnabled)
    {
        this.sideA = sideA;
        this.sideB = sideB;
        this.decisionNanos = decisionNanos;
        this.maxDecisionNanos = maxDecisionNanos;
        this.turnTimerEnabled = turnTimerEnabled;
    }

    public static class Encoder implements BiConsumer<PacketBattleInfo, FriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketBattleInfo msg, FriendlyByteBuf buf) {
            buf.writeInt(msg.sideA.size());
            buf.writeInt(msg.sideB.size());
            for(Integer id : msg.sideA) {
                buf.writeInt(id);
            }
            for(Integer id : msg.sideB) {
                buf.writeInt(id);
            }
            buf.writeLong(msg.decisionNanos);
            buf.writeLong(msg.maxDecisionNanos);
            buf.writeBoolean(msg.turnTimerEnabled);
        }
    }

    public static class Decoder implements Function<FriendlyByteBuf, PacketBattleInfo> {
        public Decoder() {}

        @Override
        public PacketBattleInfo apply(FriendlyByteBuf buf) {
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
            long maxDecisionNanos = buf.readLong();
            boolean turnTimerEnabled = buf.readBoolean();
            return new PacketBattleInfo(sideA, sideB, decisionNanos, maxDecisionNanos, turnTimerEnabled);
        }
    }

    public static class Consumer implements BiConsumer<PacketBattleInfo, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketBattleInfo pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
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
                TurnBasedMinecraftMod.proxy.setBattleGuiTurnTimerMax((int)(pkt.maxDecisionNanos / 1000000000L));
            });
            ctx.setPacketHandled(true);
        }
    }
}
