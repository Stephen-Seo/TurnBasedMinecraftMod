package com.seodisparate.TurnBasedMinecraft.common.networking;

import java.util.HashMap;
import java.util.Map;

import com.seodisparate.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketBattleMessage implements IMessage
{
    public enum MessageType
    {
        ENTERED(0),
        FLEE(1),
        DIED(2),
        ENDED(3),
        ATTACK(4),
        DEFEND(5),
        DEFENSE_DAMAGE(6),
        MISS(7),
        DEFENDING(8),
        DID_NOTHING(9),
        USED_ITEM(10),
        TURN_BEGIN(11),
        TURN_END(12),
        SWITCHED_ITEM(13),
        WAS_AFFECTED(14),
        BECAME_CREATIVE(15),
        FIRED_ARROW(16),
        ARROW_HIT(17),
        BOW_NO_AMMO(18);
        
        private int value;
        private static Map<Integer, MessageType> map = new HashMap<Integer, MessageType>();
        
        private MessageType(int value)
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
        
        static
        {
            for(MessageType type : MessageType.values())
            {
                map.put(type.getValue(), type);
            }
        }
        
        public static MessageType valueOf(int value)
        {
            return map.get(value);
        }
    }
    
    public enum UsedItemAction
    {
        USED_NOTHING(0),
        USED_INVALID(1),
        USED_FOOD(2),
        USED_POTION(3);
        
        private int value;
        private static Map<Integer, UsedItemAction> map = new HashMap<Integer, UsedItemAction>();
        
        private UsedItemAction(int value)
        {
            this.value = value;
        }
        
        public int getValue()
        {
            return value;
        }
        
        static
        {
            for(UsedItemAction type : UsedItemAction.values())
            {
                map.put(type.getValue(), type);
            }
        }
        
        public static UsedItemAction valueOf(int value)
        {
            return map.get(value);
        }
    }
    
    MessageType messageType;
    int entityIDFrom;
    int entityIDTo;
    int amount;
    String custom;
    
    public PacketBattleMessage() { custom = new String(); }
    
    public PacketBattleMessage(MessageType messageType, int entityIDFrom, int entityIDTo, int amount)
    {
        this.messageType = messageType;
        this.entityIDFrom = entityIDFrom;
        this.entityIDTo = entityIDTo;
        this.amount = amount;
        custom = new String();
    }
    
    public PacketBattleMessage(MessageType messageType, int entityIDFrom, int entityIDTo, int amount, String custom)
    {
        this.messageType = messageType;
        this.entityIDFrom = entityIDFrom;
        this.entityIDTo = entityIDTo;
        this.amount = amount;
        this.custom = custom;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        messageType = MessageType.valueOf(buf.readInt());
        entityIDFrom = buf.readInt();
        entityIDTo = buf.readInt();
        amount = buf.readInt();
        custom = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(messageType.getValue());
        buf.writeInt(entityIDFrom);
        buf.writeInt(entityIDTo);
        buf.writeInt(amount);
        ByteBufUtils.writeUTF8String(buf, custom);
    }

    public static class HandlerBattleMessage implements IMessageHandler<PacketBattleMessage, IMessage>
    {
        @Override
        public IMessage onMessage(PacketBattleMessage message, MessageContext ctx)
        {
            Entity fromEntity = TurnBasedMinecraftMod.proxy.getEntityByID(message.entityIDFrom);
            String from = "Unknown";
            if(fromEntity != null)
            {
                if(fromEntity.hasCustomName())
                {
                    from = fromEntity.getCustomNameTag();
                }
                else if(fromEntity instanceof EntityPlayer)
                {
                    from = ScorePlayerTeam.formatPlayerName(fromEntity.getTeam(), fromEntity.getName());
                }
                else
                {
                    from = fromEntity.getName();
                }
            }
            else if(TurnBasedMinecraftMod.proxy.getLocalBattle() != null)
            {
                fromEntity = TurnBasedMinecraftMod.proxy.getLocalBattle().getCombatantEntity(message.entityIDFrom);
                if(fromEntity != null)
                {
                    if(fromEntity.hasCustomName())
                    {
                        from = fromEntity.getCustomNameTag();
                    }
                    else if(fromEntity instanceof EntityPlayer)
                    {
                        from = ScorePlayerTeam.formatPlayerName(fromEntity.getTeam(), fromEntity.getName());
                    }
                    else
                    {
                        from = fromEntity.getName();
                    }
                }
            }
            Entity toEntity = TurnBasedMinecraftMod.proxy.getEntityByID(message.entityIDTo);
            String to = "Unknown";
            if(toEntity != null)
            {
                if(toEntity.hasCustomName())
                {
                    to = toEntity.getCustomNameTag();
                }
                else if(toEntity instanceof EntityPlayer)
                {
                    to = ScorePlayerTeam.formatPlayerName(toEntity.getTeam(), toEntity.getName());
                }
                else
                {
                    to = toEntity.getName();
                }
            }
            else if(TurnBasedMinecraftMod.proxy.getLocalBattle() != null)
            {
                toEntity = TurnBasedMinecraftMod.proxy.getLocalBattle().getCombatantEntity(message.entityIDTo);
                if(toEntity != null)
                {
                    if(toEntity.hasCustomName())
                    {
                        to = toEntity.getCustomNameTag();
                    }
                    else if(toEntity instanceof EntityPlayer)
                    {
                        to = ScorePlayerTeam.formatPlayerName(toEntity.getTeam(), toEntity.getName());
                    }
                    else
                    {
                        to = toEntity.getName();
                    }
                }
            }
            
            switch(message.messageType)
            {
            case ENTERED:
                TurnBasedMinecraftMod.proxy.displayString(from + " entered battle!");
                if(TurnBasedMinecraftMod.proxy.getLocalBattle() == null || TurnBasedMinecraftMod.proxy.getLocalBattle().getId() != message.amount)
                {
                    TurnBasedMinecraftMod.proxy.createLocalBattle(message.amount);
                }
                TurnBasedMinecraftMod.proxy.battleStarted();
                TurnBasedMinecraftMod.proxy.typeEnteredBattle(message.custom);
                break;
            case FLEE:
                if(message.amount != 0)
                {
                    TurnBasedMinecraftMod.proxy.displayString(from + " fled battle!");
                    TurnBasedMinecraftMod.proxy.typeLeftBattle(message.custom);
                }
                else
                {
                    TurnBasedMinecraftMod.proxy.displayString(from + " tried to flee battle but failed!");
                }
                break;
            case DIED:
                TurnBasedMinecraftMod.proxy.displayString(from + " died in battle!");
                TurnBasedMinecraftMod.proxy.typeLeftBattle(message.custom);
                break;
            case ENDED:
                TurnBasedMinecraftMod.proxy.displayString("Battle has ended!");
                TurnBasedMinecraftMod.proxy.battleEnded();
                break;
            case ATTACK:
                TurnBasedMinecraftMod.proxy.displayString(from + " attacked " + to + " and dealt " + message.amount + " damage!");
                break;
            case DEFEND:
                TurnBasedMinecraftMod.proxy.displayString(from + " blocked " + to + "'s attack!");
                break;
            case DEFENSE_DAMAGE:
                TurnBasedMinecraftMod.proxy.displayString(from + " retaliated from " + to + "'s attack and dealt " + message.amount + " damage!");
                break;
            case MISS:
                TurnBasedMinecraftMod.proxy.displayString(from + " attacked " + to + " but missed!");
                break;
            case DEFENDING:
                TurnBasedMinecraftMod.proxy.displayString(from + " is defending!");
                break;
            case DID_NOTHING:
                TurnBasedMinecraftMod.proxy.displayString(from + " did nothing!");
                break;
            case USED_ITEM:
                switch(UsedItemAction.valueOf(message.amount))
                {
                case USED_NOTHING:
                    TurnBasedMinecraftMod.proxy.displayString(from + " tried to use nothing!");
                    break;
                case USED_INVALID:
                    if(message.custom.length() > 0)
                    {
                        TurnBasedMinecraftMod.proxy.displayString(from + " tried to consume " + message.custom + " and failed!");
                    }
                    else
                    {
                        TurnBasedMinecraftMod.proxy.displayString(from + " tried to consume an invalid item and failed!");
                    }
                    break;
                case USED_FOOD:
                    TurnBasedMinecraftMod.proxy.displayString(from + " ate a " + message.custom + "!");
                    break;
                case USED_POTION:
                    TurnBasedMinecraftMod.proxy.displayString(from + " drank a " + message.custom + "!");
                    break;
                }
                break;
            case TURN_BEGIN:
                TurnBasedMinecraftMod.proxy.displayString("The turn begins!");
                TurnBasedMinecraftMod.proxy.battleGuiTurnBegin();
                break;
            case TURN_END:
                if(TurnBasedMinecraftMod.proxy.getLocalBattle() != null)
                {
                    TurnBasedMinecraftMod.proxy.displayString("The turn ended!");
                }
                TurnBasedMinecraftMod.proxy.battleGuiTurnEnd();
                break;
            case SWITCHED_ITEM:
                if(message.amount != 0)
                {
                    TurnBasedMinecraftMod.proxy.displayString(from + " switched to a different item!");
                }
                else
                {
                    TurnBasedMinecraftMod.proxy.displayString(from + " switched to a different item but failed because it was invalid!");
                }
                break;
            case WAS_AFFECTED:
                TurnBasedMinecraftMod.proxy.displayString(to + " was " + message.custom + " by " + from + "!");
                break;
            case BECAME_CREATIVE:
                TurnBasedMinecraftMod.proxy.displayString(from + " entered creative mode and left battle!");
                break;
            case FIRED_ARROW:
                TurnBasedMinecraftMod.proxy.displayString(from + " let loose an arrow towards " + to + "!");
                break;
            case ARROW_HIT:
                TurnBasedMinecraftMod.proxy.displayString(to + " was hit by " + from + "'s arrow!");
                break;
            case BOW_NO_AMMO:
                TurnBasedMinecraftMod.proxy.displayString(from + " tried to use their bow but ran out of ammo!");
                break;
            }
            return null;
        }
    }
}
