package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.function.Supplier;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketGeneralMessage
{
    String message;

    public String getMessage() {
        return message;
    }
    
    public PacketGeneralMessage()
    {
        message = new String();
    }
    
    public PacketGeneralMessage(String message)
    {
        this.message = message;
    }
    
    public static void encode(PacketGeneralMessage pkt, FriendlyByteBuf buf) {
    	buf.writeUtf(pkt.message);
    }
    
    public static PacketGeneralMessage decode(FriendlyByteBuf buf) {
    	return new PacketGeneralMessage(buf.readUtf());
    }
    
    public static void handle(final PacketGeneralMessage pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx));
        });
        ctx.get().setPacketHandled(true);
    }
}
