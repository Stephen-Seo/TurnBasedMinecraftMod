package com.seodisparate.TurnBasedMinecraft.common;

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
}
