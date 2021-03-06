package com.burnedkirby.TurnBasedMinecraft.common.networking;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.burnedkirby.TurnBasedMinecraft.common.TurnBasedMinecraftMod;

import com.burnedkirby.TurnBasedMinecraft.common.Utility;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

public class PacketBattleMessage
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
        BOW_NO_AMMO(18),
        CREEPER_WAIT(19),
        CREEPER_WAIT_FINAL(20),
        CREEPER_EXPLODE(21);
        
        private int value;
        private static Map<Integer, MessageType> map = new HashMap<Integer, MessageType>();
        
        MessageType(int value)
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
        
        UsedItemAction(int value)
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
    RegistryKey<World> dimension;
    
    public PacketBattleMessage() { custom = new String(); }
    
    public PacketBattleMessage(MessageType messageType, int entityIDFrom, int entityIDTo, RegistryKey<World> dimension, int amount)
    {
        this.messageType = messageType;
        this.entityIDFrom = entityIDFrom;
        this.entityIDTo = entityIDTo;
        this.dimension = dimension;
        this.amount = amount;
        custom = new String();
    }
    
    public PacketBattleMessage(MessageType messageType, int entityIDFrom, int entityIDTo, RegistryKey<World> dimension, int amount, String custom)
    {
        this.messageType = messageType;
        this.entityIDFrom = entityIDFrom;
        this.entityIDTo = entityIDTo;
        this.dimension = dimension;
        this.amount = amount;
        this.custom = custom;
    }
    
    public static void encode(PacketBattleMessage pkt, PacketBuffer buf) {
        buf.writeInt(pkt.messageType.getValue());
        buf.writeInt(pkt.entityIDFrom);
        buf.writeInt(pkt.entityIDTo);
        buf.writeUtf(Utility.serializeDimension(pkt.dimension));
        buf.writeInt(pkt.amount);
        buf.writeUtf(pkt.custom);
    }
    
    public static PacketBattleMessage decode(PacketBuffer buf) {
    	return new PacketBattleMessage(
    		MessageType.valueOf(
    			buf.readInt()),
			buf.readInt(),
			buf.readInt(),
			Utility.deserializeDimension(buf.readUtf()),
			buf.readInt(),
			buf.readUtf());
    }
    
    public static class Handler {
    	public static void handle(final PacketBattleMessage pkt, Supplier<NetworkEvent.Context> ctx) {
    		ctx.get().enqueueWork(() -> {
                Entity fromEntity = TurnBasedMinecraftMod.proxy.getEntity(pkt.entityIDFrom, pkt.dimension);
                String from = "Unknown";
                if(fromEntity != null)
                {
                	from = fromEntity.getDisplayName().getString();
                }
                else if(TurnBasedMinecraftMod.proxy.getLocalBattle() != null)
                {
                    fromEntity = TurnBasedMinecraftMod.proxy.getLocalBattle().getCombatantEntity(pkt.entityIDFrom);
                    if(fromEntity != null)
                    {
                    	from = fromEntity.getDisplayName().getString();
                    }
                }
                Entity toEntity = TurnBasedMinecraftMod.proxy.getEntity(pkt.entityIDTo, pkt.dimension);
                String to = "Unknown";
                if(toEntity != null)
                {
                	to = toEntity.getDisplayName().getString();
                }
                else if(TurnBasedMinecraftMod.proxy.getLocalBattle() != null)
                {
                    toEntity = TurnBasedMinecraftMod.proxy.getLocalBattle().getCombatantEntity(pkt.entityIDTo);
                    if(toEntity != null)
                    {
                        to = toEntity.getDisplayName().getString();
                    }
                }
                
                switch(pkt.messageType)
                {
                case ENTERED:
                    TurnBasedMinecraftMod.proxy.displayString(from + " entered battle!");
                    if(TurnBasedMinecraftMod.proxy.getLocalBattle() == null || TurnBasedMinecraftMod.proxy.getLocalBattle().getId() != pkt.amount)
                    {
                        TurnBasedMinecraftMod.proxy.createLocalBattle(pkt.amount);
                    }
                    TurnBasedMinecraftMod.proxy.battleStarted();
                    TurnBasedMinecraftMod.proxy.typeEnteredBattle(pkt.custom);
                    break;
                case FLEE:
                    if(pkt.amount != 0)
                    {
                        TurnBasedMinecraftMod.proxy.displayString(from + " fled battle!");
                        TurnBasedMinecraftMod.proxy.typeLeftBattle(pkt.custom);
                    }
                    else
                    {
                        TurnBasedMinecraftMod.proxy.displayString(from + " tried to flee battle but failed!");
                    }
                    break;
                case DIED:
                    TurnBasedMinecraftMod.proxy.displayString(from + " died in battle!");
                    TurnBasedMinecraftMod.proxy.typeLeftBattle(pkt.custom);
                    break;
                case ENDED:
                    TurnBasedMinecraftMod.proxy.displayString("Battle has ended!");
                    TurnBasedMinecraftMod.proxy.battleEnded();
                    break;
                case ATTACK:
                    TurnBasedMinecraftMod.proxy.displayString(from + " attacked " + to + " and dealt " + pkt.amount + " damage!");
                    break;
                case DEFEND:
                    TurnBasedMinecraftMod.proxy.displayString(from + " blocked " + to + "'s attack!");
                    break;
                case DEFENSE_DAMAGE:
                    TurnBasedMinecraftMod.proxy.displayString(from + " retaliated from " + to + "'s attack and dealt " + pkt.amount + " damage!");
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
                    switch(UsedItemAction.valueOf(pkt.amount))
                    {
                    case USED_NOTHING:
                        TurnBasedMinecraftMod.proxy.displayString(from + " tried to use nothing!");
                        break;
                    case USED_INVALID:
                        if(pkt.custom.length() > 0)
                        {
                            TurnBasedMinecraftMod.proxy.displayString(from + " tried to consume " + pkt.custom + " and failed!");
                        }
                        else
                        {
                            TurnBasedMinecraftMod.proxy.displayString(from + " tried to consume an invalid item and failed!");
                        }
                        break;
                    case USED_FOOD:
                        TurnBasedMinecraftMod.proxy.displayString(from + " ate a " + pkt.custom + "!");
                        break;
                    case USED_POTION:
                        TurnBasedMinecraftMod.proxy.displayString(from + " drank a " + pkt.custom + "!");
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
                        if(pkt.amount == 0)
                        {
                            TurnBasedMinecraftMod.proxy.displayString("The turn ended!");
                        }
                        else
                        {
                            TurnBasedMinecraftMod.proxy.displayString("The turn ended (abnormally due to internal error)!");
                        }
                    }
                    TurnBasedMinecraftMod.proxy.battleGuiTurnEnd();
                    break;
                case SWITCHED_ITEM:
                    if(pkt.amount != 0)
                    {
                        TurnBasedMinecraftMod.proxy.displayString(from + " switched to a different item!");
                    }
                    else
                    {
                        TurnBasedMinecraftMod.proxy.displayString(from + " switched to a different item but failed because it was invalid!");
                    }
                    break;
                case WAS_AFFECTED:
                    TurnBasedMinecraftMod.proxy.displayString(to + " was " + pkt.custom + " by " + from + "!");
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
                case CREEPER_WAIT: {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent message = new StringTextComponent(from + " is charging up!");
                    message.setStyle(message.getStyle().withColor(Color.fromRgb(0xFFFFFF00)));
                    prefix.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                }
                    break;
                case CREEPER_WAIT_FINAL: {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent message = new StringTextComponent(from + " is about to explode!");
                    message.setStyle(message.getStyle().withColor(Color.fromRgb(0xFFFF5050)));
                    prefix.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                }
                    break;
                case CREEPER_EXPLODE: {
                    StringTextComponent prefix = new StringTextComponent("TBM: ");
                    prefix.setStyle(prefix.getStyle().withColor(Color.fromRgb(0xFF00FF00)).withBold(true));
                    StringTextComponent message = new StringTextComponent(from + " exploded!");
                    message.setStyle(message.getStyle().withColor(Color.fromRgb(0xFFFF0000)));
                    prefix.getSiblings().add(message);
                    TurnBasedMinecraftMod.proxy.displayTextComponent(prefix);
                }
                    break;
                }
    		});
    		ctx.get().setPacketHandled(true);
    	}
    }
}
