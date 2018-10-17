package com.seodisparate.TurnBasedMinecraft.common;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketGeneralMessage;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class CommandTBMEnable extends CommandBase
{
    public static final String usage = "/tbm-enable (Enables turn-based-battle for the current player (only OP or anyone, depending on config))";
    private Config config;
    
    CommandTBMEnable(Config config)
    {
        this.config = config;
    }

    @Override
    public String getName()
    {
        return "tbm-enable";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return usage;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayerMP player = null;
        player = getCommandSenderAsPlayer(sender);
        if(!config.getIfOnlyOPsCanDisableTurnBasedForSelf() || player.getServer().isSinglePlayer() || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null)
        {
            config.removeBattleIgnoringPlayer(player.getEntityId());
            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Enabled turn-based-combat for current player"), player);
            TurnBasedMinecraftMod.logger.info("Enabled turn-based-combat for " + player.getName());
        }
        else
        {
            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Only OPs can use this command (based on config)"), player);
        }
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return true;
    }
}
