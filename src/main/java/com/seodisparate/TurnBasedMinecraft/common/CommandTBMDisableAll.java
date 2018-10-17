package com.seodisparate.TurnBasedMinecraft.common;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketGeneralMessage;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class CommandTBMDisableAll extends CommandBase
{
    public static final String usage = "/tbm-disable-all (Disables turn-based-battle for everyone";
    private Config config;
    
    public CommandTBMDisableAll(Config config)
    {
        this.config = config;
    }

    @Override
    public String getName()
    {
        return "tbm-disable-all";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return usage;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        config.setBattleDisabledForAll(true);
        for(Entity player : server.getPlayerList().getPlayers())
        {
            config.addBattleIgnoringPlayer(player.getEntityId());
        }
        EntityPlayerMP senderPlayer = null;
        try {
            senderPlayer = getCommandSenderAsPlayer(sender);
        } catch(Throwable t)
        {
            // Ignore case when sender EntityPlayer cannot be found
        }
        if(senderPlayer != null)
        {
            TurnBasedMinecraftMod.logger.info(senderPlayer.getName() + " disabled turn-based-combat for everyone");
            TurnBasedMinecraftMod.NWINSTANCE.sendToAll(new PacketGeneralMessage(senderPlayer.getName() + " disabled turn-based-battle for everyone"));
        }
        else
        {
            TurnBasedMinecraftMod.logger.info("An admin disabled turn-based-combat for everyone");
            TurnBasedMinecraftMod.NWINSTANCE.sendToAll(new PacketGeneralMessage("An admin disabled turn-based-battle for everyone"));
        }
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

}
