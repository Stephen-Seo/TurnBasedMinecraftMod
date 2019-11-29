package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class Utility
{
    public static float yawDirection(double posX, double posZ, double targetX, double targetZ)
    {
        double radians = Math.atan2(targetZ - posZ, targetX - posX);
        radians = (radians - Math.PI / 2.0);
        if(radians < 0.0)
        {
            radians += Math.PI * 2.0;
        }
        return (float)(radians * 180.0 / Math.PI);
    }
    
    public static float pitchDirection(double posX, double posY, double posZ, double targetX, double targetY, double targetZ)
    {
        double diffX = targetX - posX;
        double diffY = targetY - posY;
        double diffZ = targetZ - posZ;
        double distance = Math.sqrt(diffX * diffX + diffZ * diffZ);
        if(Math.abs(diffY) < 0.1)
        {
            return 0;
        }
        else
        {
            return (float)(-Math.atan(diffY / distance) * 180.0 / Math.PI);
        }
    }
    
    public static boolean doesPlayerHaveArrows(PlayerEntity player)
    {
        for(int i = 0; i < player.inventory.getSizeInventory(); ++i)
        {
            if(player.inventory.getStackInSlot(i).getItem() instanceof ArrowItem)
            {
                return true;
            }
        }
        return false;
    }
    
    public static double distanceBetweenEntities(Entity a, Entity b)
    {
        return Math.sqrt(Math.pow(a.posX - b.posX, 2.0) + Math.pow(a.posY - b.posY, 2.0) + Math.pow(a.posZ - b.posZ, 2.0));
    }
}
