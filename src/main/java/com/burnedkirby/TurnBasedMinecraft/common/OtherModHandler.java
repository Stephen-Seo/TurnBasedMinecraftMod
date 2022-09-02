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
            // Check if required classes exist
            Class<?> customNPCsAPI = null;
            try {
                customNPCsAPI = Class.forName("noppes.npcs.api.NpcAPI");
            } catch (ClassNotFoundException e) {
                TurnBasedMinecraftMod.logger.info("NpcAPI not found, not handling it.");
            }
            if (customNPCsAPI == null) {
                break;
            }

            Class<?> customNPCsPlayerHurtEvent = null;
            try {
                customNPCsPlayerHurtEvent = Class.forName("noppes.npcs.api.event.PlayerEvent$DamagedEvent");
            } catch (ClassNotFoundException e) {
                TurnBasedMinecraftMod.logger.info("CustomNPCs Player Hurt Event class not found, not handling it.");
            }
            if (customNPCsPlayerHurtEvent == null) {
                break;
            }

            Field damageSourceField = null;
            try {
                damageSourceField = customNPCsPlayerHurtEvent.getField("damageSource");
            } catch (NoSuchFieldException e) {
                TurnBasedMinecraftMod.logger.error("CustomNPCs PlayerHurtEvent does not have \".damageSource\"!");
            }
            if (damageSourceField == null) {
                break;
            }

            Class<?> customNPCsIDamageSource = null;
            try {
                customNPCsIDamageSource = Class.forName("noppes.npcs.api.IDamageSource");
            } catch (ClassNotFoundException e) {
                TurnBasedMinecraftMod.logger.info("CustomNPCs IDamageSource not found, not handling it.");
            }
            if (customNPCsIDamageSource == null) {
                break;
            }

            Method trueSourceMethod = null;
            try {
                trueSourceMethod = customNPCsIDamageSource.getMethod("getTrueSource");
            } catch (NoSuchMethodException e) {
                TurnBasedMinecraftMod.logger.error("CustomNPCs IDamageSource does not have \".getTrueSource()\"!");
            }
            if (trueSourceMethod == null) {
                break;
            }

            Class<?> customNPCsIEntity = null;
            try {
                customNPCsIEntity = Class.forName("noppes.npcs.api.entity.IEntity");
            } catch (ClassNotFoundException e) {
                TurnBasedMinecraftMod.logger.info("CustomNPCs IEntity not found, not handling it.");
            }
            if (customNPCsIEntity == null) {
                break;
            }

            Method getEntityUUIDMethod = null;
            try {
                getEntityUUIDMethod = customNPCsIEntity.getMethod("getUUID");
            } catch (NoSuchMethodException e) {
                TurnBasedMinecraftMod.logger.error("Failed to get CustomNPCs \".getEntityId()\"!");
            }
            if (getEntityUUIDMethod == null) {
                break;
            }

            Method getCanceledMethod = null;
            try {
                getCanceledMethod = customNPCsPlayerHurtEvent.getMethod("setCanceled", boolean.class);
            } catch (NoSuchMethodException e) {
                TurnBasedMinecraftMod.logger.error("CustomNPCs PlayerHurtEvent does not have setCanceled(...)!");
            }
            if (getCanceledMethod == null) {
                break;
            }

            // Check if available
            Object instance = null;
            try {
                Method instanceMethod = customNPCsAPI.getMethod("Instance");
                instance = instanceMethod.invoke(null);
                if (!customNPCsAPI.isInstance(instance)) {
                    instance = null;
                    TurnBasedMinecraftMod.logger.error("NpcAPI.Instance() is not NpcAPI!");
                }
            } catch (NoSuchMethodException e) {
                TurnBasedMinecraftMod.logger.warn("NpcAPI.Instance() does not exist!");
            } catch (InvocationTargetException e) {
                TurnBasedMinecraftMod.logger.error("Failed to call NpcAPI.Instance(), InvocationTargetException!");
            } catch (IllegalAccessException e) {
                TurnBasedMinecraftMod.logger.error("Failed to call NpcAPI.Instance(), IllegalAccessException!");
            }
            if (instance == null) {
                break;
            }

            Boolean isAvailable = false;
            try {
                Method isAvailableMethod = customNPCsAPI.getMethod("IsAvailable");
                isAvailable = (Boolean)isAvailableMethod.invoke(instance);
            } catch (NoSuchMethodException e) {
                TurnBasedMinecraftMod.logger.warn("NpcAPI.IsAvailable() does not exist!");
            } catch (InvocationTargetException e) {
                TurnBasedMinecraftMod.logger.warn("Failed to call NpcAPI.IsAvailable(), InvocationTargetException!");
            } catch (IllegalAccessException e) {
                TurnBasedMinecraftMod.logger.warn("Failed to call NpcAPI.IsAvailable(), IllegalAccessException!");
            } catch (ClassCastException e) {
                TurnBasedMinecraftMod.logger.warn("Result of NpcAPI.IsAvailable() is not a Boolean!");
            }
            if (!isAvailable) {
                TurnBasedMinecraftMod.logger.warn("NpcAPI is not available!");
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
                customNPCsEventBus = (IEventBus) getNPCsEventBusMethod.invoke(instance);
            } catch (InvocationTargetException e) {
                TurnBasedMinecraftMod.logger.warn("Failed to invoke NpcAPI.events(), InvocationTargetException!");
            } catch (IllegalAccessException e) {
                TurnBasedMinecraftMod.logger.warn("Failed to invoke NpcAPI.events(), IllegalAccessException!");
            } catch (ClassCastException e) {
                TurnBasedMinecraftMod.logger.warn("Failed to cast NpcAPI.events(), ClassCastException!");
            }
            if (customNPCsEventBus == null) {
                break;
            }

            final Class<?> finalCustomNPCsPlayerHurtEvent = customNPCsPlayerHurtEvent;
            final Field finalDamageSourceField = damageSourceField;
            final Class<?> finalCustomNPCsIDamageSource = customNPCsIDamageSource;
            final Method finalTrueSourceMethod = trueSourceMethod;
            final Class<?> finalCustomNPCsIEntity = customNPCsIEntity;
            final Method finalGetEntityUUIDMethod = getEntityUUIDMethod;
            final Method finalGetCanceledMethod = getCanceledMethod;

            customNPCsEventBus.addListener(EventPriority.LOWEST, true, (event) -> {
                if (finalCustomNPCsPlayerHurtEvent.isInstance(event)
                        && TurnBasedMinecraftMod.proxy.getAttackingEntity() != null) {
                    Object damageSourceObject;
                    try {
                        damageSourceObject = finalDamageSourceField.get(event);
                    } catch (IllegalAccessException e) {
                        TurnBasedMinecraftMod.logger.error("CustomNPCs PlayerHurtEvent failed to get \".damageSource\"!");
                        return;
                    }

                    if (!finalCustomNPCsIDamageSource.isInstance(damageSourceObject)) {
                        TurnBasedMinecraftMod.logger.error("CustomNPCs PlayerHurtEvent damageSource is not IDamageSource!");
                        return;
                    }

                    Object iEntityObject;
                    try {
                        iEntityObject = finalTrueSourceMethod.invoke(damageSourceObject);
                    } catch (IllegalAccessException e) {
                        TurnBasedMinecraftMod.logger.error("Failed to get CustomNPCs IEntity from IDamageSource, IllegalAccessException!");
                        return;
                    } catch (InvocationTargetException e) {
                        TurnBasedMinecraftMod.logger.error("Failed to get CustomNPCs IEntity from IDamageSource, InvocationTargetException!");
                        return;
                    }

                    if (!finalCustomNPCsIEntity.isInstance(iEntityObject)) {
                        TurnBasedMinecraftMod.logger.error("IDamageSource.getTrueSource() is not IEntity!");
                        return;
                    }

                    String entityUUID;
                    try {
                        entityUUID = (String)finalGetEntityUUIDMethod.invoke(iEntityObject);
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

                    if (!TurnBasedMinecraftMod.proxy.getAttackingEntity().getStringUUID().equals(entityUUID)) {
                        return;
                    }

                    try {
                        finalGetCanceledMethod.invoke(event, false);
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
