package com.seodisparate.TurnBasedMinecraft.common.networking;

import com.seodisparate.TurnBasedMinecraft.common.Battle;
import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;
import com.seodisparate.TurnBasedMinecraft.common.Battle.Decision;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketBattleDecision implements IMessage
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

    @Override
    public void fromBytes(ByteBuf buf)
    {
        battleID = buf.readInt();
        decision = Decision.valueOf(buf.readInt());
        targetIDOrItemID = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(battleID);
        buf.writeInt(decision.getValue());
        buf.writeInt(targetIDOrItemID);
    }

    public static class HandleBattleDecision implements IMessageHandler<PacketBattleDecision, IMessage>
    {
        @Override
        public IMessage onMessage(PacketBattleDecision message, MessageContext ctx)
        {
            Battle b = TurnBasedMinecraftMod.getBattleManager().getBattleByID(message.battleID);
            if(b != null)
            {
                EntityPlayerMP player = ctx.getServerHandler().player;
                b.setDecision(player.getEntityId(), message.decision, message.targetIDOrItemID);
            }
            return null;
        }
    }
}
