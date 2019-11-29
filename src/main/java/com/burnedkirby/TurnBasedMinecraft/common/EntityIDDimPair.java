package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.DimensionManager;

public class EntityIDDimPair {
    public int id;
    public DimensionType dim;

    EntityIDDimPair() {
        id = 0;
        dim = Minecraft.getInstance().world.dimension.getType();
    }

    EntityIDDimPair(int id, DimensionType dim) {
        this.id = id;
        this.dim = dim;
    }

    EntityIDDimPair(Entity entity) {
        id = entity.getEntityId();
        dim = entity.dimension;
    }

    public Entity getEntity() {
        return Utility.getEntity(id, dim);
    }

    @Override
    public int hashCode() {
        return (id + dim.toString()).hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof EntityIDDimPair) {
            EntityIDDimPair otherPair = (EntityIDDimPair) other;
            return otherPair.id == id && otherPair.dim == dim;
        }
        return false;
    }
}
