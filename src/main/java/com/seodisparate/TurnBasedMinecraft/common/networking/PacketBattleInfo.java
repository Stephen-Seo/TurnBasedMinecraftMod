package com.seodisparate.TurnBasedMinecraft.common.networking;

import java.util.ArrayList;
import java.util.Collection;

import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketBattleInfo implements IMessage
{
    private Collection<Integer> sideA;
    private Collection<Integer> sideB;
    private long decisionNanos;
    
    public PacketBattleInfo()
    {
        sideA = new ArrayList<Integer>();
        sideB = new ArrayList<Integer>();
        decisionNanos = TurnBasedMinecraftMod.proxy.getConfig().getDecisionDurationNanos();
    }
    
    public PacketBattleInfo(Collection<Integer> sideA, Collection<Integer> sideB, long decisionNanos)
    {
        this.sideA = sideA;
        this.sideB = sideB;
        this.decisionNanos = decisionNanos;
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
        decisionNanos = buf.readLong();
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
        buf.writeLong(decisionNanos);
    }

    public static class HandlerBattleInfo implements IMessageHandler<PacketBattleInfo, IMessage>
    {
        @Override
        public IMessage onMessage(PacketBattleInfo message, MessageContext ctx)
        {
            if(TurnBasedMinecraftMod.proxy.getLocalBattle() == null)
            {
                return null;
            }
            TurnBasedMinecraftMod.proxy.getLocalBattle().clearCombatants();
            for(Integer id : message.sideA)
            {
                Entity e = Minecraft.getMinecraft().world.getEntityByID(id);
                if(e != null)
                {
                    TurnBasedMinecraftMod.proxy.getLocalBattle().addCombatantToSideA(e);
                }
            }
            for(Integer id : message.sideB)
            {
                Entity e = Minecraft.getMinecraft().world.getEntityByID(id);
                if(e != null)
                {
                    TurnBasedMinecraftMod.proxy.getLocalBattle().addCombatantToSideB(e);
                }
            }
            TurnBasedMinecraftMod.proxy.setBattleGuiTime((int)(message.decisionNanos / 1000000000L));
            TurnBasedMinecraftMod.proxy.setBattleGuiBattleChanged();
            return null;
        }
    }
}
