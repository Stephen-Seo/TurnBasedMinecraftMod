package com.seodisparate.TurnBasedMinecraft.common.networking;

import java.util.HashMap;
import java.util.Map;

import com.seodisparate.TurnBasedMinecraft.TurnBasedMinecraftMod;
import com.seodisparate.TurnBasedMinecraft.client.BattleGui;
import com.seodisparate.TurnBasedMinecraft.common.Battle;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.text.TextComponentString;
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
        TURN_END(12);
        
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
            Entity fromEntity = Minecraft.getMinecraft().world.getEntityByID(message.entityIDFrom);
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
            else if(TurnBasedMinecraftMod.currentBattle != null)
            {
                fromEntity = TurnBasedMinecraftMod.currentBattle.getCombatantEntity(message.entityIDFrom);
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
            Entity toEntity = Minecraft.getMinecraft().world.getEntityByID(message.entityIDTo);
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
            else if(TurnBasedMinecraftMod.currentBattle != null)
            {
                toEntity = TurnBasedMinecraftMod.currentBattle.getCombatantEntity(message.entityIDTo);
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
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                    from + " entered battle!"));
                if(TurnBasedMinecraftMod.currentBattle == null || TurnBasedMinecraftMod.currentBattle.getId() != message.amount)
                {
                    TurnBasedMinecraftMod.currentBattle = new Battle(message.amount, null, null, false);
                }
                if(TurnBasedMinecraftMod.currentBattleGui == null)
                {
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        TurnBasedMinecraftMod.currentBattleGui = new BattleGui();
                        Minecraft.getMinecraft().displayGuiScreen(TurnBasedMinecraftMod.currentBattleGui);
                    });
                }
                break;
            case FLEE:
                if(message.amount != 0)
                {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                        from + " fled battle!"));
                }
                else
                {
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                        from + " tried to flee battle but failed!"));
                }
                break;
            case DIED:
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                    from + " died in battle!"));
                break;
            case ENDED:
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                        "Battle has ended!"));
                TurnBasedMinecraftMod.currentBattle = null;
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    TurnBasedMinecraftMod.currentBattleGui = null;
                    Minecraft.getMinecraft().setIngameFocus();
                });
                break;
            case ATTACK:
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                    from + " attacked " + to + " and dealt " + message.amount + " damage!"));
                break;
            case DEFEND:
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                    from + " blocked " + to + "'s attack!"));
                break;
            case DEFENSE_DAMAGE:
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                    from + " retaliated from " + to + "'s attack and dealt " + message.amount + " damage!"));
                break;
            case MISS:
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                    from + " attacked " + to + " but missed!"));
                break;
            case DEFENDING:
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                    from + " is defending!"));
                break;
            case DID_NOTHING:
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                    from + " did nothing!"));
                break;
            case USED_ITEM:
                switch(UsedItemAction.valueOf(message.amount))
                {
                case USED_NOTHING:
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                            from + " tried to use nothing!"));
                    break;
                case USED_INVALID:
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                            from + " tried to consume " + message.custom + " and failed!"));
                    break;
                case USED_FOOD:
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                            from + " ate a " + message.custom + "!"));
                    break;
                case USED_POTION:
                    Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                            from + " drank a " + message.custom + "!"));
                    break;
                }
                break;
            case TURN_BEGIN:
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                        "The turn begins!"));
                if(TurnBasedMinecraftMod.currentBattleGui != null)
                {
                    TurnBasedMinecraftMod.currentBattleGui.turnBegin();
                }
                break;
            case TURN_END:
                Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
                        "The turn ended!"));
                if(TurnBasedMinecraftMod.currentBattleGui != null)
                {
                    TurnBasedMinecraftMod.currentBattleGui.turnEnd();
                }
                break;
            }
            return null;
        }
    }
}
