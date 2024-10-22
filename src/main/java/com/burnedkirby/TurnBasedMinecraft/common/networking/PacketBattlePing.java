package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketBattlePing {
    private int battleID;

    public PacketBattlePing() {
        battleID = 0;
    }

    public PacketBattlePing(int battleID) {
        this.battleID = battleID;
    }

    public static class Encoder implements BiConsumer<PacketBattlePing, RegistryFriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketBattlePing pkt, RegistryFriendlyByteBuf buf) {
            buf.writeInt(pkt.battleID);
        }
    }

    public static class Decoder implements Function<RegistryFriendlyByteBuf, PacketBattlePing> {
        public Decoder() {}

        @Override
        public PacketBattlePing apply(RegistryFriendlyByteBuf buf) {
            return new PacketBattlePing(buf.readInt());
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
            });
            ctx.setPacketHandled(true);
        }
    }
}