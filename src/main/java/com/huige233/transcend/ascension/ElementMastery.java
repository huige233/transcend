package com.huige233.transcend.ascension;

import com.huige233.transcend.spell.SpellElement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 元素专精系统
 *
 * 玩家在完成飞升仪式后可选择一种专精路线：
 *   - NONE       : 尚未选择
 *   - OMNI       : 全系法师 — 所有元素均+15%伤害，无法进一步专精
 *   - 18个元素专精 : 专精元素获得大幅强化，非专精元素仅+5%
 *
 * 专精伤害加成分级：
 *   Tier 0 (未选) : 0%
 *   Tier 1 (全系) : +15%（所有元素）
 *   Tier 2 (专精) : +60%（专精元素）/ +5%（其他元素）
 *
 * 专精额外被动（对应专精元素的独特效果）存储在 AscensionStatBlock 里，
 * 由 AscensionHandler 在事件中检查并应用。
 */
public enum ElementMastery {

    NONE("none", null, ChatFormatting.GRAY,
            0f, 0f,
            "mastery.transcend.none", "mastery.transcend.none.desc"),

    OMNI("omni", null, ChatFormatting.WHITE,
            0.40f, 0.40f,
            "mastery.transcend.omni", "mastery.transcend.omni.desc"),

    FIRE("fire",   SpellElement.FIRE,    ChatFormatting.RED,
            0.75f, -0.30f,
            "mastery.transcend.fire",   "mastery.transcend.fire.desc"),
    ICE("ice",    SpellElement.ICE,     ChatFormatting.AQUA,
            0.75f, -0.30f,
            "mastery.transcend.ice",    "mastery.transcend.ice.desc"),
    THUNDER("thunder", SpellElement.THUNDER, ChatFormatting.YELLOW,
            0.75f, -0.30f,
            "mastery.transcend.thunder","mastery.transcend.thunder.desc"),
    WIND("wind",  SpellElement.WIND,    ChatFormatting.WHITE,
            0.75f, -0.30f,
            "mastery.transcend.wind",   "mastery.transcend.wind.desc"),
    EARTH("earth", SpellElement.EARTH,  ChatFormatting.GREEN,
            0.75f, -0.30f,
            "mastery.transcend.earth",  "mastery.transcend.earth.desc"),
    VOID("void",  SpellElement.VOID,    ChatFormatting.DARK_PURPLE,
            0.75f, -0.30f,
            "mastery.transcend.void",   "mastery.transcend.void.desc"),
    HOLY("holy",  SpellElement.HOLY,    ChatFormatting.GOLD,
            0.75f, -0.30f,
            "mastery.transcend.holy",   "mastery.transcend.holy.desc"),
    BLOOD("blood", SpellElement.BLOOD,  ChatFormatting.DARK_RED,
            0.75f, -0.30f,
            "mastery.transcend.blood",  "mastery.transcend.blood.desc"),
    DARK("dark",  SpellElement.DARK,    ChatFormatting.DARK_GRAY,
            0.75f, -0.30f,
            "mastery.transcend.dark",   "mastery.transcend.dark.desc"),
    LIGHT("light", SpellElement.LIGHT,  ChatFormatting.WHITE,
            0.75f, -0.30f,
            "mastery.transcend.light",  "mastery.transcend.light.desc"),
    POISON("poison", SpellElement.POISON, ChatFormatting.DARK_GREEN,
            0.75f, -0.30f,
            "mastery.transcend.poison", "mastery.transcend.poison.desc"),
    TIME("time",  SpellElement.TIME,    ChatFormatting.LIGHT_PURPLE,
            0.75f, -0.30f,
            "mastery.transcend.time",   "mastery.transcend.time.desc"),
    SPACE("space", SpellElement.SPACE,  ChatFormatting.BLUE,
            0.75f, -0.30f,
            "mastery.transcend.space",  "mastery.transcend.space.desc"),
    NATURE("nature", SpellElement.NATURE, ChatFormatting.GREEN,
            0.75f, -0.30f,
            "mastery.transcend.nature", "mastery.transcend.nature.desc"),
    CHAOS("chaos", SpellElement.CHAOS,  ChatFormatting.LIGHT_PURPLE,
            0.75f, -0.30f,
            "mastery.transcend.chaos",  "mastery.transcend.chaos.desc"),
    ACID("acid",  SpellElement.ACID,    ChatFormatting.GREEN,
            0.75f, -0.30f,
            "mastery.transcend.acid",   "mastery.transcend.acid.desc"),
    SONIC("sonic", SpellElement.SONIC,  ChatFormatting.WHITE,
            0.75f, -0.30f,
            "mastery.transcend.sonic",  "mastery.transcend.sonic.desc"),
    ELDRITCH("eldritch", SpellElement.ELDRITCH, ChatFormatting.DARK_PURPLE,
            0.75f, -0.30f,
            "mastery.transcend.eldritch","mastery.transcend.eldritch.desc");

    /** 专精ID（与 SpellElement.id 一一对应，NONE/OMNI 除外） */
    public final String id;
    /** 绑定的元素（NONE/OMNI 为 null） */
    public final SpellElement element;
    public final ChatFormatting color;
    /** 专精元素伤害倍率加成 */
    public final float masteredBonus;
    /** 非专精元素伤害倍率加成 */
    public final float otherBonus;
    public final String nameKey;
    public final String descKey;

    ElementMastery(String id, SpellElement element, ChatFormatting color,
                   float masteredBonus, float otherBonus,
                   String nameKey, String descKey) {
        this.id = id;
        this.element = element;
        this.color = color;
        this.masteredBonus = masteredBonus;
        this.otherBonus = otherBonus;
        this.nameKey = nameKey;
        this.descKey = descKey;
    }

    // ─── 查询 ──────────────────────────────────────────────────────────────

    public boolean isSelected() { return this != NONE; }
    public boolean isOmni()     { return this == OMNI; }
    public boolean isSpecific() { return this != NONE && this != OMNI; }

    /**
     * 计算对给定元素的伤害倍率加成
     */
    public float getDamageBonus(SpellElement castElement) {
        if (this == NONE) return 0f;
        if (this == OMNI) return masteredBonus; // 全系平等加成
        if (castElement == this.element) return masteredBonus;
        return otherBonus;
    }

    /**
     * 计算魔力消耗折扣（专精元素减少15%，全系减少5%，其他不变）
     */
    public float getManaCostReduction(SpellElement castElement) {
        if (this == NONE) return 0f;
        if (this == OMNI) return 0.10f;
        if (castElement == this.element) return 0.25f;
        return 0f;
    }

    public Component getDisplayName() {
        return Component.translatable(nameKey).withStyle(color);
    }

    public Component getDescription() {
        return Component.translatable(descKey).withStyle(ChatFormatting.GRAY);
    }

    public static ElementMastery getById(String id) {
        if (id == null || id.isEmpty()) return NONE;
        for (ElementMastery m : values()) {
            if (m.id.equals(id)) return m;
        }
        return NONE;
    }

    /** 从 SpellElement 找到对应专精 */
    public static ElementMastery fromElement(SpellElement el) {
        for (ElementMastery m : values()) {
            if (m.element == el) return m;
        }
        return NONE;
    }
}
