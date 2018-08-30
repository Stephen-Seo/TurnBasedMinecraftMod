package com.seodisparate.TurnBasedMinecraft.common.networking;

import com.seodisparate.TurnBasedMinecraft.TurnBasedMinecraftMod;
import com.seodisparate.TurnBasedMinecraft.common.Battle;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketBattleRequestInfo implements IMessage
{
    private int battleID;
    
    public PacketBattleRequestInfo() {}
    
    public PacketBattleRequestInfo(int battleID)
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

    public static class HandlerBattleRequestInfo implements IMessageHandler<PacketBattleRequestInfo, PacketBattleInfo>
    {
        @Override
        public PacketBattleInfo onMessage(PacketBattleRequestInfo message, MessageContext ctx)
        {
            Battle b = TurnBasedMinecraftMod.getBattleManager().getBattleByID(message.battleID);
            if(b == null)
            {
                return null;
            }
            PacketBattleInfo battleInfo = new PacketBattleInfo(b.getSideAIDs(), b.getSideBIDs());
            return battleInfo;
        }
    }
}
