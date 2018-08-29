package com.seodisparate.TurnBasedMinecraft.common.networking;

import java.util.ArrayList;
import java.util.Collection;

import com.seodisparate.TurnBasedMinecraft.TurnBasedMinecraftMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketBattleInfo implements IMessage
{
    private Collection<Integer> sideA;
    private Collection<Integer> sideB;
    
    public PacketBattleInfo()
    {
        sideA = new ArrayList<Integer>();
        sideB = new ArrayList<Integer>();
    }
    
    public PacketBattleInfo(Collection<Integer> sideA, Collection<Integer> sideB)
    {
        this.sideA = sideA;
        this.sideB = sideB;
    }
    
    @Override
    public void fromBytes(ByteBuf buf)
    {
        int sideACount = buf.readInt();
        int sideBCount = buf.readInt();
        for(int i = 0; i < sideACount; ++i)
        {
            sideA.add(buf.readInt());
        }
        for(int i = 0; i < sideBCount; ++i)
        {
            sideB.add(buf.readInt());
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(sideA.size());
        buf.writeInt(sideB.size());
        for(Integer id : sideA)
        {
            buf.writeInt(id);
        }
        for(Integer id : sideB)
        {
            buf.writeInt(id);
        }
    }

    public static class HandlerBattleInfo implements IMessageHandler<PacketBattleInfo, IMessage>
    {
        @Override
        public IMessage onMessage(PacketBattleInfo message, MessageContext ctx)
        {
            if(TurnBasedMinecraftMod.currentBattle == null)
            {
                return null;
            }
            TurnBasedMinecraftMod.currentBattle.clearCombatants();
            for(Integer id : message.sideA)
            {
                TurnBasedMinecraftMod.currentBattle.addCombatantToSideA(Minecraft.getMinecraft().world.getEntityByID(id));
            }
            for(Integer id : message.sideB)
            {
                TurnBasedMinecraftMod.currentBattle.addCombatantToSideB(Minecraft.getMinecraft().world.getEntityByID(id));
            }
            return null;
        }
    }
}
