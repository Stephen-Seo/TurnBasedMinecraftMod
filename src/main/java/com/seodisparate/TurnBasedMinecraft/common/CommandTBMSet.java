package com.seodisparate.TurnBasedMinecraft.common;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketGeneralMessage;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class CommandTBMSet extends CommandBase
{
    public static final String usage = "/tbm-set <player> <true or false; if true turn-based-battle is enabled for that player>";
    private Config config;
    
    public CommandTBMSet(Config config)
    {
        this.config = config;
    }

    @Override
    public String getName()
    {
        return "tbm-set";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return usage;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if(args.length != 2)
        {
            throw new WrongUsageException(usage, new Object[0]);
        }
        EntityPlayerMP target = getPlayer(server, sender, args[0]);
        EntityPlayerMP senderPlayer = null;
        try {
            senderPlayer = getCommandSenderAsPlayer(sender);
        } catch(Throwable t)
        {
            // Ignore case when sender EntityPlayer cannot be found
        }
        if(args[1].toLowerCase().equals("true"))
        {
            config.removeBattleIgnoringPlayer(target.getEntityId());
            if(senderPlayer != null)
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage(senderPlayer.getName() + " enabled turn-based-combat for you"), target);
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("You enabled turn-based-combat for " + target.getName()), senderPlayer);
                TurnBasedMinecraftMod.logger.info(senderPlayer.getName() + " enabled turn-based-combat for " + target.getName());
            }
            else
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("An admin enabled turn-based-combat for you"), target);
                TurnBasedMinecraftMod.logger.info("Enabled turn-based-combat for " + target.getName());
            }
        }
        else if(args[1].toLowerCase().equals("false"))
        {
            config.addBattleIgnoringPlayer(target.getEntityId());
            if(senderPlayer != null)
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage(senderPlayer.getName() + " disabled turn-based-combat for you"), target);
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("You disabled turn-based-combat for " + target.getName()), senderPlayer);
                TurnBasedMinecraftMod.logger.info(senderPlayer.getName() + " disabled turn-based-combat for " + target.getName());
            }
            else
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("An admin disabled turn-based-combat for you"), target);
                TurnBasedMinecraftMod.logger.info("Disabled turn-based-combat for " + target.getName());
            }
        }
        else
        {
            throw new WrongUsageException(usage, new Object[0]);
        }
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }
}
