package com.seodisparate.TurnBasedMinecraft.common.networking;

import com.seodisparate.TurnBasedMinecraft.TurnBasedMinecraftMod;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketBattleExited implements IMessage
{
    @Override
    public void fromBytes(ByteBuf buf)
    {
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
    }

    public static class HandlerBattleExited implements IMessageHandler<PacketBattleExited, IMessage>
    {
        @Override
        public IMessage onMessage(PacketBattleExited message, MessageContext ctx)
        {
            TurnBasedMinecraftMod.currentBattle = null;
            return null;
        }
    }
}
