package com.huige233.transcend.ascension;

import com.huige233.transcend.spell.SpellElement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;

/** 元素抗性数据记录类。 */
public final class ElementResistanceData {
    public static final float MIN_ACQUIRED_BONUS = -0.50F;
    public static final float MAX_ACQUIRED_BONUS = 0.95F;
    private final EnumMap<SpellElement, Float> bonuses = new EnumMap<>(SpellElement.class);

    public float getBonus(SpellElement element) {
        if (element == null) return 0.0F;
        return bonuses.getOrDefault(element.canonical(), 0.0F);
    }

    public void setBonus(SpellElement element, float bonus) {
        if (element == null || !Float.isFinite(bonus)) return;
        SpellElement canonical = element.canonical();
        if (canonical == SpellElement.CHAOS) return;
        bonus = Math.max(MIN_ACQUIRED_BONUS, Math.min(MAX_ACQUIRED_BONUS, bonus));
        if (Math.abs(bonus) < 0.0001F) {
            bonuses.remove(canonical);
        } else {
            bonuses.put(canonical, bonus);
        }
    }

    public void addBonus(SpellElement element, float amount) {
        if (!Float.isFinite(amount)) return;
        setBonus(element, getBonus(element) + amount);
    }

    public void clear() {
        bonuses.clear();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (SpellElement element : SpellElement.canonicalValues()) {
            if (element == SpellElement.CHAOS) continue;
            float bonus = getBonus(element);
            if (Math.abs(bonus) >= 0.0001F) tag.putFloat(element.id, bonus);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        clear();
        for (String key : tag.getAllKeys()) {
            if (!tag.contains(key, Tag.TAG_FLOAT) && !tag.contains(key, Tag.TAG_DOUBLE)) continue;
            SpellElement element = SpellElement.getById(key);
            if (element == null) continue;
            element = element.canonical();
            float bonus = tag.getFloat(key);
            if (element != SpellElement.CHAOS && Float.isFinite(bonus)) setBonus(element, bonus);
        }
    }
}
