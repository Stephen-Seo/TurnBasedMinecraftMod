package com.burnedkirby.TurnBasedMinecraft.common;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

public class OtherModHandler {
    public OtherModHandler() {
    }

    public void postInit() {
        // Check if CustomNPCs is available, and handle player damage events if it is.
        for (int i = 0; i < 1; ++i) {
            Class<?> customNPCsAPI = null;
            try {
                customNPCsAPI = Class.forName("noppes.npcs.scripted.NpcAPI");
            } catch (ClassNotFoundException e) {
                TurnBasedMinecraftMod.logger.info("NpcAPI not found, not handling it.");
            }
            if (customNPCsAPI == null) {
                break;
            }

            Class<?> customNPCsPlayerHurtEvent = null;
            try {
                customNPCsPlayerHurtEvent = Class.forName("noppes.npcs.scripted.event.PlayerEvent.DamagedEvent");
            } catch (ClassNotFoundException e) {
                TurnBasedMinecraftMod.logger.info("CustomNPCs Player Hurt Event class not found, not handling it.");
            }
            if (customNPCsPlayerHurtEvent == null) {
                break;
            }

            Class<?> customNPCsIDamageSource = null;
            try {
                customNPCsIDamageSource = Class.forName("noppes.npcs.scripted.interfaces.IDamageSource");
            } catch (ClassNotFoundException e) {
                TurnBasedMinecraftMod.logger.info("CustomNPCs IDamageSource not found, not handling it.");
            }
            if (customNPCsIDamageSource == null) {
                break;
            }

            Class<?> customNPCsIEntity = null;
            try {
                customNPCsIEntity = Class.forName("noppes.npcs.scripted.interfaces.entity.IEntity");
            } catch (ClassNotFoundException e) {
                TurnBasedMinecraftMod.logger.info("CustomNPCs IEntity not found, not handling it.");
            }
            if (customNPCsIEntity == null) {
                break;
            }

            TurnBasedMinecraftMod.logger.info("NpcAPI found, setting up player-not-getting-hurt workaround...");

            Method getNPCsEventBusMethod = null;
            try {
                getNPCsEventBusMethod = customNPCsAPI.getMethod("events");
            } catch (NoSuchMethodException e) {
                TurnBasedMinecraftMod.logger.warn("NpcAPI.events() could not be found!");
            }
            if (getNPCsEventBusMethod == null) {
                break;
            }

            IEventBus customNPCsEventBus = null;
            try {
                customNPCsEventBus = (IEventBus) getNPCsEventBusMethod.invoke(customNPCsAPI);
            } catch (InvocationTargetException e) {
                TurnBasedMinecraftMod.logger.warn("Failed to invoke NpcAPI.events(), InvocationTargetException!");
            } catch (IllegalAccessException e) {
                TurnBasedMinecraftMod.logger.warn("Failed to invoke NpcAPI.events(), IllegalAccessException!");
            }
            if (customNPCsEventBus == null) {
                break;
            }

            final Class<?> finalCustomNPCsPlayerHurtEvent = customNPCsPlayerHurtEvent;
            final Class<?> finalCustomNPCsIDamageSource = customNPCsIDamageSource;
            final Class<?> finalCustomNPCsIEntity = customNPCsIEntity;

            customNPCsEventBus.addListener(EventPriority.LOWEST, true, (event) -> {
                if (finalCustomNPCsPlayerHurtEvent.isInstance(event)
                        && TurnBasedMinecraftMod.proxy.getAttackingEntity() != null) {
                    Field damageSourceField;
                    try {
                        damageSourceField = finalCustomNPCsPlayerHurtEvent.getField("damageSource");
                    } catch (NoSuchFieldException e) {
                        TurnBasedMinecraftMod.logger.error("CustomNPCs PlayerHurtEvent does not have \".damageSource\"!");
                        return;
                    }

                    Object damageSourceObject;
                    try {
                        damageSourceObject = damageSourceField.get(event);
                    } catch (IllegalAccessException e) {
                        TurnBasedMinecraftMod.logger.error("CustomNPCs PlayerHurtEvent failed to get \".damageSource\"!");
                        return;
                    }

                    if (!finalCustomNPCsIDamageSource.isInstance(damageSourceObject)) {
                        TurnBasedMinecraftMod.logger.error("CustomNPCs PlayerHurtEvent damageSource is not IDamageSource!");
                        return;
                    }

                    Method trueSourceMethod;
                    try {
                        trueSourceMethod = finalCustomNPCsIDamageSource.getMethod("getTrueSource");
                    } catch (NoSuchMethodException e) {
                        TurnBasedMinecraftMod.logger.error("CustomNPCs IDamageSource does not have \".getTrueSource()\"!");
                        return;
                    }

                    Object iEntityObject;
                    try {
                        iEntityObject = trueSourceMethod.invoke(damageSourceObject);
                    } catch (IllegalAccessException e) {
                        TurnBasedMinecraftMod.logger.error("Failed to get CustomNPCs IEntity from IDamageSource, IllegalAccessException!");
                        return;
                    } catch (InvocationTargetException e) {
                        TurnBasedMinecraftMod.logger.error("Failed to get CustomNPCs IEntity from IDamageSource, InvocationTargetException!");
                        return;
                    }

                    Method getEntityIDMethod;
                    try {
                        getEntityIDMethod = finalCustomNPCsIEntity.getMethod("getEntityId");
                    } catch (NoSuchMethodException e) {
                        TurnBasedMinecraftMod.logger.error("Failed to get CustomNPCs \".getEntityId()\"!");
                        return;
                    }

                    Integer entityId;
                    try {
                        entityId = (Integer)getEntityIDMethod.invoke(iEntityObject);
                    } catch (InvocationTargetException e) {
                        TurnBasedMinecraftMod.logger.error("Failed to get CustomNPCs IEntity ID, InvocationTargetException!");
                        return;
                    } catch (IllegalAccessException e) {
                        TurnBasedMinecraftMod.logger.error("Failed to get CustomNPCs IEntity ID, IllegalAccessException!");
                        return;
                    } catch (ClassCastException e) {
                        TurnBasedMinecraftMod.logger.error("Failed to get CustomNPCs IEntity ID, ClassCastException!");
                        return;
                    }

                    if (entityId != TurnBasedMinecraftMod.proxy.getAttackingEntity().getId()) {
                        return;
                    }

                    Method getCanceledMethod;
                    try {
                        getCanceledMethod = finalCustomNPCsPlayerHurtEvent.getMethod("setCanceled", Boolean.class);
                    } catch (NoSuchMethodException e) {
                        TurnBasedMinecraftMod.logger.error("CustomNPCs PlayerHurtEvent does not have setCanceled(...)!");
                        return;
                    }

                    try {
                        getCanceledMethod.invoke(event, false);
                    } catch (IllegalAccessException e) {
                        TurnBasedMinecraftMod.logger.error("Failed to un-cancel Player hurt event, IllegalAccessException!");
                    } catch (InvocationTargetException e) {
                        TurnBasedMinecraftMod.logger.error("Failed to un-cancel Player hurt event, InvocationTargetException!");
                    }
                }
            });
            TurnBasedMinecraftMod.logger.info("Enabled NpcAPI handling of Player damaged event");
        }
    }
}
