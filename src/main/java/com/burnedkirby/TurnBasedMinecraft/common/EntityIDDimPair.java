package com.burnedkirby.TurnBasedMinecraft.common;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;

public class EntityIDDimPair {
    public int id;
    public RegistryKey<World> dim;

    EntityIDDimPair() {
        id = 0;
//        dim = Minecraft.getInstance().world.dimension();
        dim = null;
    }

    EntityIDDimPair(int id, RegistryKey<World> dim) {
        this.id = id;
        this.dim = dim;
    }

    EntityIDDimPair(Entity entity) {
        id = entity.getId();
        dim = entity.level.dimension();
    }

    public Entity getEntity() {
        return TurnBasedMinecraftMod.proxy.getEntity(id, dim);
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
