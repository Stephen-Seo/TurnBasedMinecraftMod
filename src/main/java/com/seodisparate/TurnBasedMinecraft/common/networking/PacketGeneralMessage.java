package com.seodisparate.TurnBasedMinecraft.common.networking;

import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketGeneralMessage implements IMessage
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

    @Override
    public void fromBytes(ByteBuf buf)
    {
        message = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeUTF8String(buf, message);
    }

    public static class HandlerGeneralMessage implements IMessageHandler<PacketGeneralMessage, IMessage>
    {
        @Override
        public IMessage onMessage(PacketGeneralMessage message, MessageContext ctx)
        {
            TurnBasedMinecraftMod.proxy.displayString(message.message);
            return null;
        }
    }
}
