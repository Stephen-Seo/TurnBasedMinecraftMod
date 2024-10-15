package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.Battle;
import com.burnedkirby.TurnBasedMinecraft.common.Battle.Decision;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.BiConsumer;
import java.util.function.Function;

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

    public static class Encoder implements BiConsumer<PacketBattleDecision, RegistryFriendlyByteBuf> {
        public Encoder() {}

        @Override
        public void accept(PacketBattleDecision pkt, RegistryFriendlyByteBuf buf) {
            buf.writeInt(pkt.battleID);
            buf.writeInt(pkt.decision.getValue());
            buf.writeInt(pkt.targetIDOrItemID);
        }
    }

    public static class Decoder implements Function<RegistryFriendlyByteBuf, PacketBattleDecision> {
        public Decoder() {}

        @Override
        public PacketBattleDecision apply(RegistryFriendlyByteBuf buf) {
            return new PacketBattleDecision(buf.readInt(), Decision.valueOf(buf.readInt()), buf.readInt());
        }
    }

    public static class Consumer implements BiConsumer<PacketBattleDecision, CustomPayloadEvent.Context> {
        public Consumer() {}

        @Override
        public void accept(PacketBattleDecision pkt, CustomPayloadEvent.Context ctx) {
            ctx.enqueueWork(() -> {
                Battle b = TurnBasedMinecraftMod.proxy.getBattleManager().getBattleByID(pkt.battleID);
                if(b != null)
                {
                    ServerPlayer player = ctx.getSender();
                    b.setDecision(player.getId(), pkt.decision, pkt.targetIDOrItemID);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
