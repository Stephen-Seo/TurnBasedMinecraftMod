package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketBattleInfo
{
    private int battleID;
    private Collection<Integer> sideA;
    private Collection<Integer> sideB;
    private long decisionNanos;

    private long maxDecisionNanos;
    private boolean turnTimerEnabled;
    
    public PacketBattleInfo()
    {
        battleID = 0;
        sideA = new ArrayList<Integer>();
        sideB = new ArrayList<Integer>();
        decisionNanos = TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos();
        maxDecisionNanos = decisionNanos;
        turnTimerEnabled = false;
    }

    public PacketBattleInfo(int battleID, Collection<Integer> sideA, Collection<Integer> sideB, long decisionNanos, long maxDecisionNanos, boolean turnTimerEnabled)
    {
        this.battleID = battleID;
        this.sideA = sideA;
        this.sideB = sideB;
        this.decisionNanos = decisionNanos;
        this.maxDecisionNanos = maxDecisionNanos;
        this.turnTimerEnabled = turnTimerEnabled;
    }

    public static class Encoder implements BiConsumer<PacketBattleInfo, RegistryFriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketBattleInfo msg, RegistryFriendlyByteBuf buf) {
            buf.writeInt(msg.battleID);
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

    public static class Decoder implements Function<RegistryFriendlyByteBuf, PacketBattleInfo> {
        public Decoder() {}

        @Override
        public PacketBattleInfo apply(RegistryFriendlyByteBuf buf) {
            int battleID = buf.readInt();
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
            return new PacketBattleInfo(battleID, sideA, sideB, decisionNanos, maxDecisionNanos, turnTimerEnabled);
        }
    }

    public static class Consumer implements BiConsumer<PacketBattleInfo, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketBattleInfo pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                if(TurnBasedMinecraftMod.proxy.getLocalBattle() == null)
                {
                    TurnBasedMinecraftMod.proxy.createLocalBattle(pkt.battleID);
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
                TurnBasedMinecraftMod.proxy.setBattleGuiAsGui();
                TurnBasedMinecraftMod.proxy.setBattleGuiTime((int)(pkt.decisionNanos / 1000000000L));
                TurnBasedMinecraftMod.proxy.setBattleGuiBattleChanged();
                TurnBasedMinecraftMod.proxy.setBattleGuiTurnTimerEnabled(pkt.turnTimerEnabled);
                TurnBasedMinecraftMod.proxy.setBattleGuiTurnTimerMax((int)(pkt.maxDecisionNanos / 1000000000L));
            });
            ctx.setPacketHandled(true);
        }
    }
}
