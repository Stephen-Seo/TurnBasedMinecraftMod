package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.ArrayList;
import java.util.Collection;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class PacketBattleInfo implements CustomPacketPayload
{
    public static final ResourceLocation ID = new ResourceLocation(TurnBasedMinecraftMod.MODID, "network_packetbattleinfo");
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

    public PacketBattleInfo(final FriendlyByteBuf buf) {
        int sideACount = buf.readInt();
        int sideBCount = buf.readInt();
        this.sideA = new ArrayList<>(sideACount);
        this.sideB = new ArrayList<>(sideBCount);
        for (int i = 0; i < sideACount; ++i) {
            this.sideA.add(buf.readInt());
        }
        for (int i = 0; i < sideBCount; ++i) {
            this.sideB.add(buf.readInt());
        }
        this.decisionNanos = buf.readLong();
        this.maxDecisionNanos = buf.readLong();
        this.turnTimerEnabled = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(sideA.size());
        buf.writeInt(sideB.size());
        for(Integer id : sideA) {
            buf.writeInt(id);
        }
        for(Integer id : sideB) {
            buf.writeInt(id);
        }
        buf.writeLong(decisionNanos);
        buf.writeLong(maxDecisionNanos);
        buf.writeBoolean(turnTimerEnabled);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static class PayloadHandler {
        private static final PayloadHandler INSTANCE = new PayloadHandler();

        public static PayloadHandler getInstance() {
            return INSTANCE;
        }

        public void handleData(final PacketBattleInfo pkt, final PlayPayloadContext ctx) {
            ctx.workHandler().submitAsync(() -> {
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
            }).exceptionally(e -> {
                ctx.packetHandler().disconnect(Component.literal("Exception handling PacketBattleInfo! " + e.getMessage()));
                return null;
            });
        }
    }
}
