package com.huige233.transcend.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;


/** 从服务端实体索引、区段、更新列表和跟踪连接中移除目标，并清理首领栏及离开回调。 */
public final class TranscendEntityPurge {

    private static final String SRG_ENTITY_MANAGER = "f_143244_";
    private static final String SRG_ENTITY_TICK_LIST = "f_143243_";
    private static final String SRG_PLAYERS = "f_8546_";
    private static final String SRG_IS_UPDATING_NAVS = "f_200893_";
    private static final String SRG_NAVIGATING_MOBS = "f_143246_";
    private static final String SRG_DRAGON_PARTS = "f_143247_";

    private static final String SRG_LEVEL_CALLBACK = "f_146801_";

    private static final String SRG_SECTION_STORAGE = "f_157495_";
    private static final String SRG_ENTITY_LOOKUP = "f_157494_";
    private static final String SRG_KNOWN_UUIDS = "f_157491_";

    private static final String SRG_BY_UUID = "f_156808_";
    private static final String SRG_BY_ID = "f_156807_";

    private static final String SRG_SECTION_STORAGE_MAP = "f_156827_";

    private static final String SRG_TICK_ACTIVE = "f_156903_";

    private static final String SRG_CHUNK_MAP = "f_8325_";
    private static final String SRG_ENTITY_MAP = "f_140150_";

    private static final String SRG_SEEN_BY = "f_140475_";
    private static final String SRG_SERVER_ENTITY = "f_140471_";

    private static final String SRG_SERVER_ENTITY_ENTITY = "f_8510_";

    private static final String SRG_BY_CLASS = "f_13527_";

    private TranscendEntityPurge() {
    }

    public static void purgeFromLevel(Entity entity, boolean fireLeaveCallbacks) {
        if (entity == null || entity.level() == null || entity.level().isClientSide) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        try {

            if (entity.getRemovalReason() == null) {
                TranscendUnsafe.putMcObject(entity, Entity.class,
                        TranscendUnsafe.SRG_REMOVAL_REASON, Entity.RemovalReason.KILLED);
            }

            try {
                if (entity.getRemovalReason() != null && entity.getRemovalReason().shouldDestroy()) {
                    entity.ejectPassengers();
                }
            } catch (Throwable ignored) {
            }

            try {
                List<Entity> passengers = List.copyOf(entity.getPassengers());
                for (Entity p : passengers) {
                    purgeFromLevel(p, fireLeaveCallbacks);
                }
            } catch (Throwable ignored) {
            }

            deleteFromPersistentManager(entity, level, fireLeaveCallbacks);
            clearBossBars(entity);
        } catch (Throwable ignored) {
        }
    }

    private static void deleteFromPersistentManager(Entity entity, ServerLevel level, boolean fireLeave) {
        Object manager = getMc(level, ServerLevel.class, SRG_ENTITY_MANAGER);
        if (manager == null) {

            removeFromTickList(entity, level);
            serverLevelOnTrackingEnd(entity, level, fireLeave);
            return;
        }

        Object sectionStorage = getField(manager, SRG_SECTION_STORAGE);
        Object section = null;
        if (sectionStorage != null) {
            try {
                long sectionKey = SectionPos.asLong(entity.blockPosition());
                Method getSection = findMethod(sectionStorage.getClass(), "m_156895_", long.class);
                if (getSection == null) {

                    getSection = findMethodByParamCount(sectionStorage.getClass(), 1, long.class);
                }
                if (getSection != null) {
                    section = getSection.invoke(sectionStorage, sectionKey);
                }
            } catch (Throwable ignored) {
            }
        }

        if (section != null) {
            Object multiMap = getField(section, SRG_SECTION_STORAGE_MAP);
            if (multiMap instanceof ClassInstanceMultiMap<?> cimm) {
                removeEntityFromSection(entity, cimm);
            } else if (multiMap != null) {
                removeEntityFromSectionReflect(entity, multiMap);
            }
        }

        removeFromTickList(entity, level);
        serverLevelOnTrackingEnd(entity, level, fireLeave);

        Object lookup = getField(manager, SRG_ENTITY_LOOKUP);
        if (lookup != null) {
            Object byUuid = getField(lookup, SRG_BY_UUID);
            if (byUuid instanceof Map<?, ?> map) {
                try {
                    map.remove(entity.getUUID());
                } catch (Throwable ignored) {
                }
            }
            Object byId = getField(lookup, SRG_BY_ID);
            if (byId instanceof Int2ObjectMap<?> intMap) {
                try {
                    intMap.remove(entity.getId());
                } catch (Throwable ignored) {
                }
            }
        }

        try {
            level.getScoreboard().entityRemoved(entity);
        } catch (Throwable ignored) {
        }

        Object knownUuids = getField(manager, SRG_KNOWN_UUIDS);
        if (knownUuids instanceof Set<?> set) {
            try {
                set.remove(entity.getUUID());
            } catch (Throwable ignored) {
            }
        }

        try {
            Field cb = TranscendUnsafe.mcField(Entity.class, SRG_LEVEL_CALLBACK);
            if (cb != null && TranscendUnsafe.UNSAFE != null) {
                TranscendUnsafe.UNSAFE.putObject(entity, TranscendUnsafe.UNSAFE.objectFieldOffset(cb),
                        EntityInLevelCallback.NULL);
            } else {
                entity.setLevelCallback(EntityInLevelCallback.NULL);
            }
        } catch (Throwable ignored) {
            try {
                entity.setLevelCallback(EntityInLevelCallback.NULL);
            } catch (Throwable ignored2) {
            }
        }

        if (section != null) {
            try {
                long sectionKey = SectionPos.asLong(entity.blockPosition());
                Method stopTracking = findMethod(manager.getClass(), "m_157509_", long.class, section.getClass());
                if (stopTracking == null) {
                    for (Method m : manager.getClass().getDeclaredMethods()) {
                        if (m.getParameterCount() == 2
                                && m.getParameterTypes()[0] == long.class
                                && m.getParameterTypes()[1].isInstance(section)) {
                            stopTracking = m;
                            break;
                        }
                    }
                }
                if (stopTracking != null) {
                    stopTracking.setAccessible(true);
                    stopTracking.invoke(manager, sectionKey, section);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void removeFromTickList(Entity entity, ServerLevel level) {
        Object tickList = getMc(level, ServerLevel.class, SRG_ENTITY_TICK_LIST);
        if (tickList == null) return;
        try {

            Method ensure = findMethod(tickList.getClass(), "m_156907_");
            if (ensure == null) {
                ensure = findMethod(tickList.getClass(), "ensureActiveIsNotIterated");
            }
            if (ensure != null) {
                ensure.setAccessible(true);
                ensure.invoke(tickList);
            }
        } catch (Throwable ignored) {
        }
        Object active = getField(tickList, SRG_TICK_ACTIVE);
        if (active instanceof Int2ObjectMap<?> intMap) {
            try {
                intMap.remove(entity.getId());
            } catch (Throwable ignored) {
            }
        }
    }

    private static void serverLevelOnTrackingEnd(Entity entity, ServerLevel level, boolean fireLeave) {
        try {
            ServerChunkCache cache = level.getChunkSource();
            Object chunkMap = getMc(cache, ServerChunkCache.class, SRG_CHUNK_MAP);
            if (chunkMap == null) {

                try {
                    chunkMap = cache.chunkMap;
                } catch (Throwable ignored) {
                }
            }
            if (chunkMap != null) {
                chunkMapRemoveEntity(entity, chunkMap, fireLeave);
            }
        } catch (Throwable ignored) {
        }

        if (entity instanceof ServerPlayer sp) {
            Object players = getMc(level, ServerLevel.class, SRG_PLAYERS);
            if (players instanceof List<?> list) {
                try {
                    list.remove(sp);
                } catch (Throwable ignored) {
                }
            }
            try {
                level.updateSleepingPlayerList();
            } catch (Throwable ignored) {
            }
        }

        if (entity instanceof Mob mob) {
            Object navSet = getMc(level, ServerLevel.class, SRG_NAVIGATING_MOBS);
            if (navSet instanceof Set<?> set) {
                try {
                    set.remove(mob);
                } catch (Throwable ignored) {
                }
            }
        }

        if (entity.isMultipartEntity()) {
            Object partMap = getMc(level, ServerLevel.class, SRG_DRAGON_PARTS);
            PartEntity<?>[] parts = entity.getParts();
            if (partMap instanceof Int2ObjectMap<?> intMap && parts != null) {
                for (PartEntity<?> part : parts) {
                    if (part == null) continue;
                    try {
                        intMap.remove(part.getId());
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        try {
            entity.invalidateCaps();
        } catch (Throwable ignored) {
        }

        if (fireLeave) {
            try {
                entity.onRemovedFromWorld();
            } catch (Throwable ignored) {
            }
            try {
                MinecraftForge.EVENT_BUS.post(new EntityLeaveLevelEvent(entity, level));
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void chunkMapRemoveEntity(Entity entity, Object chunkMap, boolean fireLeave) {
        if (entity instanceof ServerPlayer player) {
            try {
                Method updatePlayerStatus = findMethod(chunkMap.getClass(), "m_140192_",
                        ServerPlayer.class, boolean.class);
                if (updatePlayerStatus == null) {
                    updatePlayerStatus = findMethod(chunkMap.getClass(), "updatePlayerStatus",
                            ServerPlayer.class, boolean.class);
                }
                if (updatePlayerStatus != null) {
                    updatePlayerStatus.setAccessible(true);
                    updatePlayerStatus.invoke(chunkMap, player, false);
                }
            } catch (Throwable ignored) {
            }

            Object entityMap = getField(chunkMap, SRG_ENTITY_MAP);
            if (entityMap instanceof Int2ObjectMap map) {
                try {
                    for (Object tracked : map.values()) {
                        invokeRemovePlayer(tracked, player);
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        Object entityMap = getField(chunkMap, SRG_ENTITY_MAP);
        Object tracked = null;
        if (entityMap instanceof Int2ObjectMap map) {
            try {
                tracked = map.remove(entity.getId());
            } catch (Throwable ignored) {
            }
        }
        if (tracked != null) {
            broadcastRemove(tracked, fireLeave);
        }
        clearBossBars(entity);
    }

    private static void invokeRemovePlayer(Object trackedEntity, ServerPlayer player) {
        try {
            Method m = findMethod(trackedEntity.getClass(), "m_140485_", ServerPlayer.class);
            if (m == null) {
                m = findMethod(trackedEntity.getClass(), "removePlayer", ServerPlayer.class);
            }
            if (m != null) {
                m.setAccessible(true);
                m.invoke(trackedEntity, player);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void broadcastRemove(Object trackedEntity, boolean useServerEntityRemove) {
        Object seenBy = getField(trackedEntity, SRG_SEEN_BY);
        Object serverEntity = getField(trackedEntity, SRG_SERVER_ENTITY);
        if (!(seenBy instanceof Set<?> connections)) {
            return;
        }

        Entity tracked = null;
        if (serverEntity != null) {
            Object e = getField(serverEntity, SRG_SERVER_ENTITY_ENTITY);
            if (e instanceof Entity ent) {
                tracked = ent;
            }
        }

        for (Object connObj : List.copyOf(connections)) {
            if (!(connObj instanceof ServerPlayerConnection connection)) {
                continue;
            }
            try {
                if (useServerEntityRemove && serverEntity != null) {
                    Method removePairing = findMethod(serverEntity.getClass(), "m_8534_", ServerPlayer.class);
                    if (removePairing == null) {
                        removePairing = findMethod(serverEntity.getClass(), "removePairing", ServerPlayer.class);
                    }
                    if (removePairing != null) {
                        removePairing.setAccessible(true);
                        removePairing.invoke(serverEntity, connection.getPlayer());
                        continue;
                    }
                }
                if (tracked != null) {
                    connection.send(new ClientboundRemoveEntitiesPacket(tracked.getId()));
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void removeEntityFromSection(Entity entity, ClassInstanceMultiMap multiMap) {
        try {

            multiMap.remove(entity);
            return;
        } catch (Throwable ignored) {
        }
        removeEntityFromSectionReflect(entity, multiMap);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void removeEntityFromSectionReflect(Entity entity, Object multiMap) {
        Object byClass = getField(multiMap, SRG_BY_CLASS);
        if (!(byClass instanceof Map map)) {
            return;
        }
        try {
            for (Object entryObj : map.entrySet()) {
                if (!(entryObj instanceof Map.Entry entry)) continue;
                Object key = entry.getKey();
                if (!(key instanceof Class<?> cls) || !cls.isInstance(entity)) continue;
                Object value = entry.getValue();
                if (value instanceof List list) {
                    list.remove(entity);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void clearBossBars(Entity entity) {
        if (entity == null) return;
        try {
            for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    try {
                        f.setAccessible(true);
                        Object v = f.get(entity);
                        if (v instanceof ServerBossEvent be) {
                            be.removeAllPlayers();
                            be.setVisible(false);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object getMc(Object target, Class<?> owner, String srg) {
        if (target == null) return null;
        Field f = TranscendUnsafe.mcField(owner, srg);
        if (f == null) return null;
        try {
            if (TranscendUnsafe.UNSAFE != null) {
                return TranscendUnsafe.UNSAFE.getObject(target, TranscendUnsafe.UNSAFE.objectFieldOffset(f));
            }
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object getField(Object target, String srgOrName) {
        if (target == null) return null;

        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            Field f = TranscendUnsafe.mcField(c, srgOrName);
            if (f != null) {
                try {
                    if (TranscendUnsafe.UNSAFE != null) {
                        return TranscendUnsafe.UNSAFE.getObject(target, TranscendUnsafe.UNSAFE.objectFieldOffset(f));
                    }
                    f.setAccessible(true);
                    return f.get(target);
                } catch (Throwable ignored) {
                }
            }
            try {
                Field raw = c.getDeclaredField(srgOrName);
                raw.setAccessible(true);
                if (TranscendUnsafe.UNSAFE != null) {
                    return TranscendUnsafe.UNSAFE.getObject(target, TranscendUnsafe.UNSAFE.objectFieldOffset(raw));
                }
                return raw.get(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> owner, String name, Class<?>... params) {
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
            }
            try {
                return ObfuscationReflectionHelper.findMethod(c, name, params);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Method findMethodByParamCount(Class<?> owner, int count, Class<?> firstParam) {
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() == count && m.getParameterTypes()[0] == firstParam) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }
}
