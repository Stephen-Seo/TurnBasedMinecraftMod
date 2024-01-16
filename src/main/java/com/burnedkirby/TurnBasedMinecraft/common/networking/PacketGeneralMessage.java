package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class PacketGeneralMessage implements CustomPacketPayload
{
    public static final ResourceLocation ID = new ResourceLocation(TurnBasedMinecraftMod.MODID, "network_packetgeneralmessage");

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

    public PacketGeneralMessage(final FriendlyByteBuf buf) {
        this.message = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(message);
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

        public void handleData(final PacketGeneralMessage pkt, final PlayPayloadContext ctx) {
            ctx.workHandler().submitAsync(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    TurnBasedMinecraftMod.proxy.handlePacket(pkt, ctx);
                }
            }).exceptionally(e -> {
                ctx.packetHandler().disconnect(Component.literal("Exception handling PacketGeneralMessage! " + e.getMessage()));
                return null;
            });
        }
    }
}
