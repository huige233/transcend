package com.huige233.transcend.util;

import com.huige233.transcend.handle.EditorNetwork;
import com.huige233.transcend.network.S2CEntityScanPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 超越编辑的服务端反射执行（不含因果剥离/super redirect）：
 * 按名称+参数类型匹配地调用方法 / 改字段 / 强制/重置返回值 / 冻结目标，并向客户端下发扫描结果。
 */
public final class TranscendEditService {

    private static final ConcurrentHashMap<String, Object> FORCED_RETURNS = new ConcurrentHashMap<>();

    /** 查询某实体某方法的强制返回值；returnType 可为简单类型名或完整类名。匹配不到返回 null。 */
    public static Object getForcedReturn(String entityUUID, String methodName, String returnType) {
        Object v = FORCED_RETURNS.get(entityUUID + "::" + methodName + "::" + returnType);
        if (v != null) return v;
        String prefix = entityUUID + "::" + methodName + "::";
        for (String k : FORCED_RETURNS.keySet()) {
            if (k.startsWith(prefix)) return FORCED_RETURNS.get(k);
        }
        return null;
    }

    /** 扫描实体全部方法/字段（含推断证据与实体信息行），通过 S2CEntityScanPacket 下发给玩家。 */
    public static void scan(ServerPlayer player, LivingEntity living) {
        List<String> methods = new ArrayList<>();
        java.util.Map<String, Integer> nameCount = new java.util.HashMap<>();
        for (Class<?> c = living.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) || m.isSynthetic() || m.isBridge()) continue;
                nameCount.merge(m.getName(), 1, Integer::sum);
            }
        }
        String dataId = living.getEncodeId();
        String typeDesc = "";
        try {
            typeDesc = living.getType().getDescriptionId();
        } catch (Exception ignored) {}
        String typeReg;
        try {
            typeReg = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        } catch (Exception ignored) {
            typeReg = "unknown";
        }
        methods.add("§7▸ 实体 " + typeReg + "  dataId=" + dataId + "  desc=" + typeDesc
                + "  name=" + (living.getCustomName() != null ? living.getCustomName().getString() : ""));
        for (Class<?> clazz = living.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers()) || m.isSynthetic() || m.isBridge()) continue;
                methods.add(sig(m) + EditorInfer.methodHint(m.getName(), nameCount.getOrDefault(m.getName(), 1)));
            }
        }
        List<String> fields = new ArrayList<>();
        for (Class<?> clazz = living.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field f : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(living);
                    String val = v == null ? "null" : v.toString();
                    if (val.length() > 40) val = val.substring(0, 40) + "...";
                    fields.add(f.getName() + ":" + EditorInfer.fieldType(f) + "=" + val + EditorInfer.fieldHint(f));
                } catch (Throwable t) {
                    fields.add(f.getName() + ":" + EditorInfer.fieldType(f) + "=error" + EditorInfer.fieldHint(f));
                }
            }
        }
        EditorNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CEntityScanPacket(living.getId(), methods, fields));
    }

    private static String sig(Method m) {
        StringBuilder sb = new StringBuilder(m.getName()).append("(");
        Class<?>[] p = m.getParameterTypes();
        for (int i = 0; i < p.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(p[i].getSimpleName());
        }
        return sb.append("):").append(m.getReturnType().getSimpleName()).toString();
    }

    /** 调用实体方法：按名称+参数类型找方法，解析参数并反射执行，汇报结果。 */
    public static void invokeMethod(ServerPlayer player, LivingEntity living,
                                    String name, String paramTypesStr, String paramValuesStr) {
        try {
            String[] typeNames = split(paramTypesStr);
            Method target = findMethod(living, name, typeNames);
            if (target == null) {
                msg(player, "方法未找到: " + name, ChatFormatting.RED);
                return;
            }
            target.setAccessible(true);
            String[] values = split(paramValuesStr);
            Class<?>[] types = target.getParameterTypes();
            Object[] args = new Object[types.length];
            for (int i = 0; i < args.length && i < values.length; i++) {
                args[i] = parseSingle(types[i], values[i], player, living);
            }
            Object result = target.invoke(living, args);
            String r = result == null ? "void" : result.toString();
            if (r.length() > 200) r = r.substring(0, 200) + "...";
            msg(player, "调用 " + target.getName() + "(" + paramValuesStr.trim() + ") 成功 -> " + r, ChatFormatting.GREEN);
        } catch (Exception e) {
            msg(player, "调用失败: " + e.getMessage(), ChatFormatting.RED);
        }
    }

    /** 修改实体字段：按名称跨继承链找字段，解析值并写入。 */
    public static void setField(ServerPlayer player, LivingEntity living, String name, String valueStr) {
        try {
            Field f = findField(living, name);
            if (f == null) {
                msg(player, "字段未找到: " + name, ChatFormatting.RED);
                return;
            }
            f.setAccessible(true);
            Object old = null;
            try { old = f.get(living); } catch (Exception ignored) {}
            Object value = parseSingle(f.getType(), valueStr, player, living);
            f.set(living, value);
            Object nv;
            try { nv = f.get(living); } catch (Exception e) { nv = null; }
            msg(player, "字段已修改: " + name + " = " + nv + (old != null ? " (原值: " + old + ")" : ""), ChatFormatting.GREEN);
        } catch (Exception e) {
            msg(player, "设置字段失败: " + e.getMessage(), ChatFormatting.RED);
        }
    }

    /** 为实体方法写入强制返回覆盖值。 */
    public static void forceReturn(ServerPlayer player, LivingEntity living,
                                   String name, String returnType, String valueStr) {
        String key = living.getStringUUID() + "::" + name + "::" + returnType;
        Object value = null;
        if (valueStr != null && !valueStr.isEmpty() && !valueStr.equals("null")) {
            try {
                Class<?> type = Class.forName(returnType);
                value = parseSingle(type, valueStr, player, living);
            } catch (Exception e) {
                value = valueStr;
            }
        }
        FORCED_RETURNS.put(key, value);
        msg(player, "已强制返回值: " + name + " -> " + value, ChatFormatting.GREEN);
    }

    /** 移除实体的强制返回覆盖。 */
    public static void resetReturn(ServerPlayer player, LivingEntity living,
                                   String name, String returnType) {
        FORCED_RETURNS.remove(living.getStringUUID() + "::" + name + "::" + returnType);
        msg(player, "已重置返回值: " + name, ChatFormatting.YELLOW);
    }

    /** 冻结目标实体（暂停 AI/位移/仇恨），frozen=true 冻结；状态记入 persistentData。 */
    public static void setFrozen(ServerPlayer player, LivingEntity living, boolean frozen) {
        if (living == null || living.level().isClientSide) return;
        net.minecraft.nbt.CompoundTag tag = living.getPersistentData();
        String KEY = "transcend_editor_frozen";
        if (frozen) {
            tag.putBoolean(KEY, true);
            if (living instanceof net.minecraft.world.entity.Mob mob) {
                mob.setNoAi(true);
                mob.setTarget(null);
            } else {
                living.setNoGravity(true);
            }
            try { living.setDeltaMovement(0, 0, 0); } catch (Throwable ignored) {}
        } else {
            tag.putBoolean(KEY, false);
            if (living instanceof net.minecraft.world.entity.Mob mob) {
                mob.setNoAi(false);
            } else {
                living.setNoGravity(false);
            }
        }
        msg(player, (frozen ? "已暂停" : "已恢复") + " 目标活动", ChatFormatting.GOLD);
    }

    /** 在继承链中按名称+参数类型匹配方法。 */
    private static Method findMethod(LivingEntity living, String name, String[] typeNames) {
        for (Class<?> clazz = living.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;
                Class<?>[] pt = m.getParameterTypes();
                if (pt.length != typeNames.length) continue;
                boolean ok = true;
                for (int i = 0; i < pt.length; i++) {
                    if (!typeMatches(pt[i], typeNames[i])) { ok = false; break; }
                }
                if (ok) return m;
            }
        }
        return null;
    }

    /** 在继承链中按名称找字段。 */
    private static Field findField(LivingEntity living, String name) {
        for (Class<?> c = living.getClass(); c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    /** 逗号切分参数串，支持泛型尖括号深度计数、不误切泛型内逗号。 */
    private static String[] split(String s) {
        if (s == null || s.isEmpty()) return new String[0];
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            if (c == ',' && depth == 0) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else cur.append(c);
        }
        String last = cur.toString().trim();
        if (!last.isEmpty()) out.add(last);
        return out.toArray(new String[0]);
    }

    /** 类型名匹配：完整名/简单名/规范名等价，兼容原始类型与 String。 */
    private static boolean typeMatches(Class<?> actual, String typeName) {
        if (actual.getName().equals(typeName)) return true;
        if (actual.getSimpleName().equals(typeName)) return true;
        if (actual.getCanonicalName() != null && actual.getCanonicalName().equals(typeName)) return true;
        switch (typeName) {
            case "int" : return actual == Integer.TYPE;
            case "long" : return actual == Long.TYPE;
            case "float" : return actual == Float.TYPE;
            case "double" : return actual == Double.TYPE;
            case "boolean" : return actual == Boolean.TYPE;
            case "byte" : return actual == Byte.TYPE;
            case "short" : return actual == Short.TYPE;
            case "char" : return actual == Character.TYPE;
            case "String" : return actual == String.class;
        }
        return false;
    }

    /** 带上下文解析参数值：@损坏来源/@药水/@实体/@世界/@坐标/@枚举 等快捷占位符，其余落到基本类型解析。 */
    private static Object parseSingle(Class<?> type, String value, ServerPlayer player, LivingEntity living) {
        if (value != null) {
            String v = value.trim();
            if (v.startsWith("@")) {
                if (living != null && net.minecraft.world.damagesource.DamageSource.class.isAssignableFrom(type)) {
                    net.minecraft.world.damagesource.DamageSources ds = living.damageSources();
                    switch (v) {
                        case "@magic": return ds.magic();
                        case "@void": return ds.fellOutOfWorld();
                        case "@fall": return ds.fall();
                        case "@lava": return ds.lava();
                        case "@generic": return ds.generic();
                        case "@wither": return ds.wither();
                        case "@lightning": return ds.lightningBolt();
                        case "@freeze": return ds.freeze();
                        case "@drown": return ds.drown();
                        case "@starve": return ds.starve();
                        case "@kill": return ds.genericKill();
                        case "@fire": return ds.inFire();
                        default: {
                            try {
                                net.minecraft.resources.ResourceLocation rl =
                                        new net.minecraft.resources.ResourceLocation(v.substring(1));
                                net.minecraft.core.Registry<net.minecraft.world.damagesource.DamageType> reg =
                                        living.level().registryAccess()
                                                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
                                net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> key =
                                        net.minecraft.resources.ResourceKey.create(
                                                net.minecraft.core.registries.Registries.DAMAGE_TYPE, rl);
                                return new net.minecraft.world.damagesource.DamageSource(reg.getHolderOrThrow(key));
                            } catch (Exception e) {
                                return ds.generic();
                            }
                        }
                    }
                }
                // 药水效果："@<id>" 或 "@<id>;时长;等级"
                if (net.minecraft.world.effect.MobEffect.class.isAssignableFrom(type)
                        || net.minecraft.world.effect.MobEffectInstance.class.isAssignableFrom(type)) {
                    try {
                        String[] parts = v.substring(1).split(";");
                        net.minecraft.resources.ResourceLocation rl =
                                new net.minecraft.resources.ResourceLocation(parts[0].trim());
                        net.minecraft.world.effect.MobEffect effect =
                                net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(rl);
                        if (effect != null) {
                            if (net.minecraft.world.effect.MobEffect.class.isAssignableFrom(type)) return effect;
                            int duration = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 600;
                            int amplifier = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;
                            return new net.minecraft.world.effect.MobEffectInstance(effect, duration, amplifier);
                        }
                    } catch (Exception ignored) {}
                    return null;
                }
                if (net.minecraft.world.entity.Entity.class.isAssignableFrom(type)) {
                    if (v.equals("@self")) return player;
                    if (v.equals("@target")) return living;
                }
                if (living != null && v.equals("@level")
                        && net.minecraft.world.level.Level.class.isAssignableFrom(type)) {
                    return living.level();
                }
                if (living != null && v.equals("@pos")) {
                    if (net.minecraft.core.BlockPos.class.isAssignableFrom(type)) return living.blockPosition();
                    if (net.minecraft.world.phys.Vec3.class.isAssignableFrom(type)) return living.position();
                }
                // 通用枚举：去掉 @ 后忽略大小写匹配枚举常量名
                if (type.isEnum()) {
                    String want = v.substring(1);
                    for (Object ec : type.getEnumConstants()) {
                        if (ec instanceof Enum<?> en && en.name().equalsIgnoreCase(want)) return ec;
                    }
                }
            }
        }
        return parseSingle(type, value);
    }

    /** 基础类型解析：数字/布尔/字符/String；其余原样返回。 */
    private static Object parseSingle(Class<?> type, String value) {
        if (value == null || value.isEmpty() || value.equals("null")) return null;
        if (type == String.class) return value;
        if (type == Integer.TYPE || type == Integer.class) return Integer.parseInt(value.trim());
        if (type == Long.TYPE || type == Long.class) return Long.parseLong(value.trim());
        if (type == Float.TYPE || type == Float.class) return Float.valueOf(Float.parseFloat(value.trim()));
        if (type == Double.TYPE || type == Double.class) return Double.parseDouble(value.trim());
        if (type == Boolean.TYPE || type == Boolean.class) return Boolean.parseBoolean(value.trim());
        if (type == Byte.TYPE || type == Byte.class) return Byte.parseByte(value.trim());
        if (type == Short.TYPE || type == Short.class) return Short.parseShort(value.trim());
        if (type == Character.TYPE || type == Character.class) return value.charAt(0);
        return value;
    }

    private static void msg(ServerPlayer player, String str, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(str).withStyle(color));
    }
}