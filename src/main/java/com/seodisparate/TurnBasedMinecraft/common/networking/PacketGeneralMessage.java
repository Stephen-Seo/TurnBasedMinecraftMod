package com.seodisparate.TurnBasedMinecraft.common.networking;

import java.util.function.Supplier;

import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class PacketGeneralMessage
{
    String message;
    
    public PacketGeneralMessage()
    {
        message = new String();
    }
    
    public PacketGeneralMessage(String message)
    {
        this.message = message;
    }
    
    public static void encode(PacketGeneralMessage pkt, PacketBuffer buf) {
    	buf.writeString(pkt.message);
    }
    
    public static PacketGeneralMessage decode(PacketBuffer buf) {
    	return new PacketGeneralMessage(buf.readString());
    }
    
    public static class Handler {
    	public static void handle(final PacketGeneralMessage pkt, Supplier<NetworkEvent.Context> ctx) {
    		ctx.get().enqueueWork(() -> {
    			TurnBasedMinecraftMod.proxy.displayString(pkt.message);
    		});
    		ctx.get().setPacketHandled(true);
    	}
    }
}
