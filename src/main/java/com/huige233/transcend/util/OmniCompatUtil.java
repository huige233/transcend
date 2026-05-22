package com.huige233.transcend.util;

import com.huige233.transcend.items.tools.TranscendSword;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

public final class OmniCompatUtil {

    private static final String OMNIMOBS_MODID = "omnimobs";
    private static final String OMNI_PACKAGE_PREFIX = "flashfur.omnimobs.";
    private static final String META_PROXY_CLASS = "flashfur.omnimobs.entities.metapotent_flashfur.MetapotentFlashfurEntity";
    private static final String META_LEVEL_CLASS = "flashfur.omnimobs.entities.metapotent_flashfur.MetapotentFlashfurLevel";

    private OmniCompatUtil() {
    }

    public static boolean isOmniLoaded() {
        return ModList.get().isLoaded(OMNIMOBS_MODID);
    }

    public static boolean isOmniEntity(@Nullable Entity entity) {
        return entity != null && entity.getClass().getName().startsWith(OMNI_PACKAGE_PREFIX);
    }

    public static boolean isMetapotentProxy(@Nullable Entity entity) {
        return entity != null && META_PROXY_CLASS.equals(entity.getClass().getName());
    }

    public static boolean playerHoldingTranscendSword(@Nullable Player player) {
        if (player == null) return false;
        return player.getMainHandItem().getItem() instanceof TranscendSword
                || player.getOffhandItem().getItem() instanceof TranscendSword;
    }

    public static boolean isMetapotentLikeDamage(@Nullable DamageSource source) {
        if (source == null || !isOmniLoaded()) return false;
        if (isOmniEntity(source.getEntity()) || isOmniEntity(source.getDirectEntity())) {
            return true;
        }
        String msgId = source.getMsgId();
        if (msgId == null) return false;
        String id = msgId.toLowerCase(Locale.ROOT);
        return id.contains("metapotent") || id.contains("flashfur") || id.contains("omni");
    }

    public static boolean tryNeutralizeMetapotentProxy(@Nullable Entity target) {
        if (!isOmniLoaded() || !isMetapotentProxy(target)) return false;
        try {
            Object metapotent = readField(target, "metapotentFlashfur");
            if (metapotent != null) {
                Class<?> levelClass = Class.forName(META_LEVEL_CLASS);
                Method remove = levelClass.getMethod("remove", metapotent.getClass());
                remove.invoke(null, metapotent);
                Method updateEventBus = levelClass.getMethod("updateEventBus");
                updateEventBus.invoke(null);
            }
            forceRemoveOmniEntity(target);
            return true;
        } catch (Throwable ignored) {
            forceRemoveOmniEntity(target);
            return false;
        }
    }

    private static void forceRemoveOmniEntity(Entity target) {
        if (target == null) return;
        try {
            Class<?> entityUtil = Class.forName("flashfur.omnimobs.util.EntityUtil");
            Method forceRemove = entityUtil.getMethod("forceRemove", Entity.class, Entity.RemovalReason.class);
            forceRemove.invoke(null, target, Entity.RemovalReason.KILLED);
            return;
        } catch (Throwable ignored) {
        }

        try {
            target.kill();
            target.remove(Entity.RemovalReason.KILLED);
            target.discard();
        } catch (Throwable ignored) {
        }
    }

    private static Object readField(Object instance, String fieldName) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }
}
