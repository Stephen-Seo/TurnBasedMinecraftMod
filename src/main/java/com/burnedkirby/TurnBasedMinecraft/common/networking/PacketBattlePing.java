package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketBattlePing {
    private int battleID;
    private int decisionSeconds;

    public PacketBattlePing() {
        battleID = 0;
        decisionSeconds = 1;
    }

    public PacketBattlePing(int battleID, int decisionSeconds) {
        this.battleID = battleID;
        this.decisionSeconds = decisionSeconds;
    }

    public static class Encoder implements BiConsumer<PacketBattlePing, RegistryFriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketBattlePing pkt, RegistryFriendlyByteBuf buf) {
            buf.writeInt(pkt.battleID);
            buf.writeInt(pkt.decisionSeconds);
        }
    }

    public static class Decoder implements Function<RegistryFriendlyByteBuf, PacketBattlePing> {
        public Decoder() {}

        @Override
        public PacketBattlePing apply(RegistryFriendlyByteBuf buf) {
            return new PacketBattlePing(buf.readInt(), buf.readInt());
        }
    }

    public static class Consumer implements BiConsumer<PacketBattlePing, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketBattlePing pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                if (TurnBasedMinecraftMod.proxy.getLocalBattle() == null) {
                    TurnBasedMinecraftMod.proxy.createLocalBattle(pkt.battleID);
                }
                TurnBasedMinecraftMod.proxy.setBattleGuiAsGui();
                TurnBasedMinecraftMod.proxy.setBattleGuiBattleChanged();
                TurnBasedMinecraftMod.proxy.setBattleGuiTime(pkt.decisionSeconds);
            });
            ctx.setPacketHandled(true);
        }
    }
}