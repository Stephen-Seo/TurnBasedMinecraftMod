package com.seodisparate.TurnBasedMinecraft.common;

import com.seodisparate.TurnBasedMinecraft.common.networking.PacketEditingMessage;
import com.seodisparate.TurnBasedMinecraft.common.networking.PacketGeneralMessage;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class CommandTBMEdit extends CommandBase
{
    public static final String usage = "/tbm-edit (Invoke without parameters to start edit)";
    private Config config;

    public CommandTBMEdit(Config config)
    {
        this.config = config;
    }

    @Override
    public String getName()
    {
        return "tbm-edit";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return usage;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayer senderEntity = null;
        EditingInfo  editingInfo = null;
        try
        {
            senderEntity = (EntityPlayer) sender.getCommandSenderEntity();
        } catch (ClassCastException e)
        {
            // if sender is not EntityPlayer, ignore
            return;
        }
        editingInfo = TurnBasedMinecraftMod.proxy.getEditingInfo(senderEntity.getEntityId());
        if(args.length == 0)
        {
            if(editingInfo != null && !editingInfo.isPendingEntitySelection)
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
            }
            else if(editingInfo != null)
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY), (EntityPlayerMP) senderEntity);
            }
            else
            {
                TurnBasedMinecraftMod.proxy.setEditingPlayer(senderEntity);
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY), (EntityPlayerMP) senderEntity);
                TurnBasedMinecraftMod.logger.info("Begin editing TBM Entity for player \"" + senderEntity.getName() + "\"");
            }
        }
        else if(args.length == 1)
        {
            if(editingInfo != null && !editingInfo.isPendingEntitySelection)
            {
                if(args[0].toLowerCase().equals("finish"))
                {
                    config.editEntityEntry(editingInfo.entityInfo);
                    TurnBasedMinecraftMod.proxy.removeEditingInfo(senderEntity.getEntityId());
                    TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Entity info saved in config and loaded."), (EntityPlayerMP) senderEntity);
                }
                else if(args[0].toLowerCase().equals("cancel"))
                {
                    TurnBasedMinecraftMod.proxy.removeEditingInfo(senderEntity.getEntityId());
                    TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Cancelled editing entity."), (EntityPlayerMP)senderEntity);
                }
                else if(args[0].toLowerCase().equals("edit"))
                {
                    TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                }
                else
                {
                    TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit <arg>\"."), (EntityPlayerMP)senderEntity);
                }
            }
            else if(editingInfo != null)
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY), (EntityPlayerMP) senderEntity);
            }
            else
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Cannot edit entity without starting editing process (use \"/tbm-edit\")."), (EntityPlayerMP)senderEntity);
            }
        }
        else if(args.length == 2)
        {
            if(editingInfo != null && !editingInfo.isPendingEntitySelection)
            {
                if(args[0].toLowerCase().equals("edit"))
                {
                    switch(args[1])
                    {
                    case "ignoreBattle":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_IGNORE_BATTLE), (EntityPlayerMP) senderEntity);
                        break;
                    case "attackPower":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_POWER), (EntityPlayerMP) senderEntity);
                        break;
                    case "attackProbability":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_PROBABILITY), (EntityPlayerMP) senderEntity);
                        break;
                    case "attackVariance":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_VARIANCE), (EntityPlayerMP) senderEntity);
                        break;
                    case "attackEffect":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_EFFECT), (EntityPlayerMP) senderEntity);
                        break;
                    case "attackEffectProbability":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_ATTACK_EFFECT_PROBABILITY), (EntityPlayerMP) senderEntity);
                        break;
                    case "defenseDamage":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DEFENSE_DAMAGE), (EntityPlayerMP) senderEntity);
                        break;
                    case "defenseDamageProbability":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DEFENSE_DAMAGE_PROBABILITY), (EntityPlayerMP) senderEntity);
                        break;
                    case "evasion":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_EVASION), (EntityPlayerMP) senderEntity);
                        break;
                    case "speed":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_SPEED), (EntityPlayerMP) senderEntity);
                        break;
                    case "category":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_CATEGORY), (EntityPlayerMP) senderEntity);
                        break;
                    case "decisionAttack":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DECISION_ATTACK), (EntityPlayerMP) senderEntity);
                        break;
                    case "decisionDefend":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DECISION_DEFEND), (EntityPlayerMP) senderEntity);
                        break;
                    case "decisionFlee":
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.EDIT_DECISION_FLEE), (EntityPlayerMP) senderEntity);
                        break;
                    default:
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit <arg>\""), (EntityPlayerMP) senderEntity);
                        break;
                    }
                }
                else
                {
                    TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid arguments for \"/tbm-edit <arg> <arg>\"."), (EntityPlayerMP)senderEntity);
                }
            }
            else if(editingInfo != null)
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY), (EntityPlayerMP)senderEntity);
            }
            else
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Cannot edit entity without starting editing process (use \"/tbm-edit\")."), (EntityPlayerMP)senderEntity);
            }
        }
        else if(args.length == 3)
        {
            if(editingInfo != null && !editingInfo.isPendingEntitySelection)
            {
                if(args[0].toLowerCase().equals("edit"))
                {
                    switch(args[1])
                    {
                    case "ignoreBattle":
                        if(args[2].toLowerCase().equals("true"))
                        {
                            editingInfo.entityInfo.ignoreBattle = true;
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        else if(args[2].toLowerCase().equals("false"))
                        {
                            editingInfo.entityInfo.ignoreBattle = false;
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        else
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit ignoreBattle <boolean>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "attackPower":
                        try
                        {
                            editingInfo.entityInfo.attackPower = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.attackPower < 0)
                            {
                                editingInfo.entityInfo.attackPower = 0;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit attackPower <integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "attackProbability":
                        try
                        {
                            editingInfo.entityInfo.attackProbability = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.attackProbability < 0)
                            {
                                editingInfo.entityInfo.attackProbability = 0;
                            }
                            else if(editingInfo.entityInfo.attackProbability > 100)
                            {
                                editingInfo.entityInfo.attackProbability = 100;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit attackProbability <percentage-integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "attackVariance":
                        try
                        {
                            editingInfo.entityInfo.attackVariance = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.attackVariance < 0)
                            {
                                editingInfo.entityInfo.attackVariance = 0;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit attackVariance <integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "attackEffect":
                        editingInfo.entityInfo.attackEffect = EntityInfo.Effect.fromString(args[2]);
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        break;
                    case "attackEffectProbability":
                        try
                        {
                            editingInfo.entityInfo.attackEffectProbability = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.attackEffectProbability < 0)
                            {
                                editingInfo.entityInfo.attackEffectProbability = 0;
                            }
                            else if(editingInfo.entityInfo.attackEffectProbability > 100)
                            {
                                editingInfo.entityInfo.attackEffectProbability = 100;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit attackEffectProbability <percentage-integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "defenseDamage":
                        try
                        {
                            editingInfo.entityInfo.defenseDamage = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.defenseDamage < 0)
                            {
                                editingInfo.entityInfo.defenseDamage = 0;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit defenseDamage <integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "defenseDamageProbability":
                        try
                        {
                            editingInfo.entityInfo.defenseDamageProbability = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.defenseDamageProbability < 0)
                            {
                                editingInfo.entityInfo.defenseDamageProbability = 0;
                            }
                            else if(editingInfo.entityInfo.defenseDamageProbability > 100)
                            {
                                editingInfo.entityInfo.defenseDamageProbability = 100;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit defenseDamageProbability <percentage-integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "evasion":
                        try
                        {
                            editingInfo.entityInfo.evasion = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.evasion < 0)
                            {
                                editingInfo.entityInfo.evasion = 0;
                            }
                            else if(editingInfo.entityInfo.evasion > 100)
                            {
                                editingInfo.entityInfo.evasion = 100;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit evasion <percentage-integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "speed":
                        try
                        {
                            editingInfo.entityInfo.speed = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.speed < 0)
                            {
                                editingInfo.entityInfo.speed = 0;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit speed <integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "category":
                        editingInfo.entityInfo.category = args[2];
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        break;
                    case "decisionAttack":
                        try
                        {
                            editingInfo.entityInfo.decisionAttack = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.decisionAttack < 0)
                            {
                                editingInfo.entityInfo.decisionAttack = 0;
                            }
                            else if(editingInfo.entityInfo.decisionAttack > 100)
                            {
                                editingInfo.entityInfo.decisionAttack = 100;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit decisionAttack <percentage-integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "decisionDefend":
                        try
                        {
                            editingInfo.entityInfo.decisionDefend = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.decisionDefend < 0)
                            {
                                editingInfo.entityInfo.decisionDefend = 0;
                            }
                            else if(editingInfo.entityInfo.decisionDefend > 100)
                            {
                                editingInfo.entityInfo.decisionDefend = 100;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit decisionDefend <percentage-integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    case "decisionFlee":
                        try
                        {
                            editingInfo.entityInfo.decisionFlee = Integer.parseInt(args[2]);
                            if(editingInfo.entityInfo.decisionFlee < 0)
                            {
                                editingInfo.entityInfo.decisionFlee = 0;
                            }
                            else if(editingInfo.entityInfo.decisionFlee > 100)
                            {
                                editingInfo.entityInfo.decisionFlee = 100;
                            }
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.PICK_EDIT, editingInfo.entityInfo), (EntityPlayerMP)senderEntity);
                        }
                        catch (NumberFormatException e)
                        {
                            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid argument for \"/tbm-edit edit decisionFlee <percentage-integer>\""), (EntityPlayerMP)senderEntity);
                        }
                        break;
                    default:
                        TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid second argument for \"/tbm-edit edit <arg> <arg>\""), (EntityPlayerMP)senderEntity);
                        break;
                    }
                }
                else
                {
                    TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid arguments for \"/tbm-edit <arg> <arg> <arg>\"."), (EntityPlayerMP)senderEntity);
                }
            }
            else if(editingInfo != null)
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketEditingMessage(PacketEditingMessage.Type.ATTACK_ENTITY), (EntityPlayerMP)senderEntity);
            }
            else
            {
                TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Cannot edit entity without starting editing process (use \"/tbm-edit\")."), (EntityPlayerMP)senderEntity);
            }
        }
        else
        {
            TurnBasedMinecraftMod.NWINSTANCE.sendTo(new PacketGeneralMessage("Invalid arguments for \"/tbm-edit\"."), (EntityPlayerMP)senderEntity);
        }
    }
}
