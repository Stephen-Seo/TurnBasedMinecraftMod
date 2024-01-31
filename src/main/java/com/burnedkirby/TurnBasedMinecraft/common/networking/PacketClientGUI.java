package com.burnedkirby.TurnBasedMinecraft.common.networking;

import com.burnedkirby.TurnBasedMinecraft.client.ClientConfig;
import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class PacketClientGUI implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(TurnBasedMinecraftMod.MODID, "network_packetclientgui");

    int reserved;

    public PacketClientGUI() {
        reserved = 0;
    }

    public PacketClientGUI(FriendlyByteBuf buf) {
        reserved = buf.readInt();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(0);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static class PayloadHandler {
        private static final PayloadHandler INSTANCE = new PayloadHandler();

        public static PayloadHandler getInstance() { return INSTANCE; }

        public void handleData(final PacketClientGUI pkt, final PlayPayloadContext ctx) {
            ctx.workHandler().submitAsync(() -> {
                if (FMLEnvironment.dist.isClient()) {
                    Minecraft.getInstance().setScreen(new ClientConfig.CliConfGui());
                }
            }).exceptionally(e -> {
                ctx.packetHandler().disconnect(Component.literal("Exception handling PacketClientGUI! " + e.getMessage()));
                return null;
            });
        }
    }
}
