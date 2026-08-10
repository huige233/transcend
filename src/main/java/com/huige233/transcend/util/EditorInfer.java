package com.huige233.transcend.util;

import com.huige233.transcend.Config;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 运行时"疑似"推断：把反射证据逐层标注到方法/字段行上，无证据时返回空串。
 * 分层：反射硬事实（声明类/泛型/volatile/transient/当前值）&gt; 同名重载族 &gt; ASM 读写图（开关开）&gt; 静态表启发式。
 */
public final class EditorInfer {

    private static final Map<String, String> METHOD_CN = new HashMap<>();
    /** 字段名(小写)子串 → 含义，宽容匹配的启发式兜底。 */
    private static final String[][] FIELD_PATTERNS = {
            {"health", "生命值"}, {"hp", "生命值"},
            {"mana", "魔力"}, {"mp", "魔力"},
            {"target", "攻击目标"},
            {"attackdamage", "攻击伤害"},
            {"armor", "护甲"}, {"armour", "护甲"},
            {"speed", "移动速度"},
            {"experience", "经验"}, {"exp", "经验"},
            {"airsupply", "氧气"}, {"oxygen", "氧气"},
            {"fireticks", "着火刻"}, {"onfire", "着火"}, {"burning", "着火"},
            {"invulnerable", "无敌帧"},
            {"navigation", "寻路器"}, {"pathfinder", "寻路器"},
            {"phase", "阶段"}, {"stage", "阶段"}, {"tier", "阶数"},
            {"aggro", "仇恨"}, {"anger", "仇恨"}, {"hurtby", "仇恨"},
            {"frozen", "冻结"}, {"glowing", "发光"}, {"silent", "静音"},
            {"immune", "免疫"}, {"cooldown", "冷却"}, {"cd", "冷却"},
            {"spell", "技能"}, {"ability", "技能"}, {"skill", "技能"},
            {"shield", "护盾"}, {"barrier", "屏障"},
            {"soul", "灵魂"}, {"spirit", "灵魂"},
            {"loot", "掉落"}, {"dropped", "掉落"},
            {"inventory", "物品栏"}, {"items", "物品栏"},
            {"leash", "拴绳"},
    };

    private EditorInfer() {}

    /** 方法行追加提示："含义" 与/或 "同名重载 ×N"。 */
    public static String methodHint(String name, int sameNameCount) {
        StringBuilder sb = new StringBuilder();
        String cn = METHOD_CN.get(name);
        if (cn != null) sb.append(cn);
        if (sameNameCount > 1) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("同名重载 ×").append(sameNameCount);
        }
        return sb.length() == 0 ? "" : " // " + sb;
    }

    /** 字段类型标签（含泛型参数，比 getSimpleName 信息量大）。 */
    public static String fieldType(Field f) {
        return typeLabel(f.getGenericType());
    }

    /** 字段行追加提示："声明于 X" + volatile/transient + ASM 读写方法（开关开时）+ 字段名疑似含义。 */
    public static String fieldHint(Field f) {
        StringBuilder sb = new StringBuilder();
        sb.append("声明于 ").append(f.getDeclaringClass().getSimpleName());
        int mods = f.getModifiers();
        if (Modifier.isVolatile(mods)) sb.append(" · volatile 同步");
        if (Modifier.isTransient(mods)) sb.append(" · transient 不落盘");
        if (Config.editorAsmScan) {
            EditorAsmReader.FieldAccess acc = EditorAsmReader.access(f);
            if (!acc.isEmpty()) sb.append(" · ⟵/⟶").append(readWriteLabel(acc));
        }
        String guess = meaningForField(f.getName());
        if (guess != null) sb.append(" · 疑似:").append(guess);
        return " // " + sb;
    }

    /** ASM 读写者摘要："⟵setHealth ⟶getHealth,getMaxHealth"。 */
    private static String readWriteLabel(EditorAsmReader.FieldAccess acc) {
        StringBuilder sb = new StringBuilder();
        if (!acc.reads.isEmpty()) sb.append("⟶").append(String.join(",", acc.reads));
        if (!acc.writes.isEmpty()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append("⟵").append(String.join(",", acc.writes));
        }
        String s = sb.toString();
        return s.length() > 48 ? s.substring(0, 48) + "…" : s;
    }

    private static String meaningForField(String name) {
        String low = name.toLowerCase(Locale.ROOT);
        for (String[] p : FIELD_PATTERNS) {
            if (low.contains(p[0])) return p[1];
        }
        return null;
    }

    private static String typeLabel(Type t) {
        if (t instanceof Class<?> c) return c.getSimpleName();
        if (t instanceof ParameterizedType pt) {
            StringBuilder sb = new StringBuilder(typeLabel(pt.getRawType()));
            Type[] args = pt.getActualTypeArguments();
            sb.append('<');
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(typeLabel(args[i]));
            }
            return sb.append('>').toString();
        }
        if (t instanceof WildcardType) return "?";
        if (t instanceof TypeVariable<?> tv) return tv.getName();
        return t.getTypeName();
    }

    static {
        // lang editor.desc.* 已覆盖的方法不进表，避免渲染首尾重复
        METHOD_CN.put("moveTo", "移动到坐标");
        METHOD_CN.put("setNoGravity", "无重力");
        METHOD_CN.put("setSilent", "静音");
        METHOD_CN.put("clearFire", "灭火");
        METHOD_CN.put("setRemainingFireTicks", "着火刻");
        METHOD_CN.put("setTicksFrozen", "冰冻刻");
        METHOD_CN.put("knockback", "击退");
        METHOD_CN.put("playSound", "播放音效");
        METHOD_CN.put("setPersistenceRequired", "强制不消失");
        METHOD_CN.put("setKillCredit", "设置击杀者");
        METHOD_CN.put("addTag", "添加标签");
        METHOD_CN.put("removeTag", "移除标签");
        METHOD_CN.put("getTarget", "攻击目标");
        METHOD_CN.put("getLastHurtByMob", "最近伤害它的生物");
        METHOD_CN.put("setLastHurtByMob", "设置伤害者");
        METHOD_CN.put("getLastHurtMob", "最近攻击的生物");
        METHOD_CN.put("getVehicle", "所乘实体");
        METHOD_CN.put("getUUID", "获取UUID");
        METHOD_CN.put("getStringUUID", "获取UUID串");
        METHOD_CN.put("getEncodeId", "NBT 注册名");
        METHOD_CN.put("getLookAngle", "视线方向");
        METHOD_CN.put("getEyePosition", "眼部坐标");
        METHOD_CN.put("hasLineOfSight", "是否目视目标");
        METHOD_CN.put("getMainHandItem", "主手物品");
        METHOD_CN.put("getOffhandItem", "副手物品");
        METHOD_CN.put("swing", "挥手动画");
        METHOD_CN.put("startRiding", "骑乘");
        METHOD_CN.put("stopRiding", "下乘");
        METHOD_CN.put("isPassenger", "是否乘骑中");
        METHOD_CN.put("setCustomNameVisible", "是否显示名字");
        METHOD_CN.put("setGlowing", "发光");
        METHOD_CN.put("getAttribute", "获取属性");
        METHOD_CN.put("getAttributeValue", "属性当前值");
        METHOD_CN.put("getMaxHealth", "最大生命");
        METHOD_CN.put("removeEffect", "移除药水效果");
        METHOD_CN.put("removeEffects", "移除全部药水");
        METHOD_CN.put("getActiveEffects", "已生效药水");
        METHOD_CN.put("getActiveEffectsMap", "药水 Map");
        METHOD_CN.put("canSee", "能否看见");
        METHOD_CN.put("isInWater", "是否水中");
        METHOD_CN.put("isOnGround", "是否着地");
        METHOD_CN.put("getNavigation", "寻路器");
        METHOD_CN.put("getBrain", "Brain 行为");
        METHOD_CN.put("getSensing", "感知器");
        METHOD_CN.put("baseTick", "基础每tick");
        METHOD_CN.put("aiStep", "AI 步进");
        METHOD_CN.put("setYRot", "朝向偏航");
        METHOD_CN.put("setXRot", "朝向俯仰");
        METHOD_CN.put("lerpTo", "平滑移动");
        METHOD_CN.put("blockPosition", "所在方块位");
        METHOD_CN.put("getOnPos", "脚下方块位");
        METHOD_CN.put("distanceTo", "到目标距离");
        METHOD_CN.put("distanceToSqr", "距离平方");
        METHOD_CN.put("getBoundingBox", "碰撞箱");
        METHOD_CN.put("isDeadOrDying", "已死/濒死");
    }
}