package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

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
    
    public static boolean doesPlayerHaveArrows(Player player)
    {
        for(int i = 0; i < player.getInventory().getContainerSize(); ++i)
        {
            if(player.getInventory().getItem(i).getItem() instanceof ArrowItem)
            {
                return true;
            }
        }
        return false;
    }
    
    public static double distanceBetweenEntities(Entity a, Entity b)
    {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2.0) + Math.pow(a.getY()- b.getY(), 2.0) + Math.pow(a.getZ()- b.getZ(), 2.0));
    }

    public static String serializeDimension(ResourceKey<Level> dimObject) {
        return dimObject.registry().toString();
    }

    public static ResourceKey<Level> deserializeDimension(String dimString) {
        ResourceLocation dimRes = ResourceLocation.parse(dimString);
        return ResourceKey.create(Registries.DIMENSION, dimRes);
    }

    public static boolean isItemEdible(ItemStack itemStack, @Nullable LivingEntity entity) {
        return itemStack.getFoodProperties(entity) != null;
    }
}
