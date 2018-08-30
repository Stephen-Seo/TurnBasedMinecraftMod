package com.seodisparate.TurnBasedMinecraft.common.networking;

import com.seodisparate.TurnBasedMinecraft.TurnBasedMinecraftMod;
import com.seodisparate.TurnBasedMinecraft.common.Battle;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketBattleEntered implements IMessage
{
    private int battleID;
    
    public PacketBattleEntered() {}
    
    public PacketBattleEntered(int battleID)
    {
        this.battleID = battleID;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        battleID = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(battleID);
    }

    public static class HandlerBattleEntered implements IMessageHandler<PacketBattleEntered, IMessage>
    {
        @Override
        public IMessage onMessage(PacketBattleEntered message, MessageContext ctx)
        {
            TurnBasedMinecraftMod.currentBattle = new Battle(message.battleID, null, null);
            return null;
        }
    }
}
