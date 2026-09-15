package com.huige233.transcend.tech.gun;

import com.huige233.transcend.tech.attribute.AttributeContainer;
import com.huige233.transcend.tech.attribute.TechAttribute;

import java.util.EnumSet;
import java.util.Set;


/** 描述武器平台支持的模块槽位、弹匣容量、热容量和基础属性，不承担射击执行。 */
public record GunType(String id, Set<ModuleSlot> supportedSlots, double magazineCapacity,
                      double heatCapacity, AttributeContainer baseAttributes) {
    public GunType {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Gun type id is required");
        supportedSlots = supportedSlots == null || supportedSlots.isEmpty()
                ? Set.of() : Set.copyOf(EnumSet.copyOf(supportedSlots));
        magazineCapacity = Math.max(1.0D, magazineCapacity);
        heatCapacity = Math.max(0.001D, heatCapacity);
        baseAttributes = baseAttributes == null ? new AttributeContainer() : baseAttributes;
    }

    public static GunType particle() {
        return new GunType("particle", EnumSet.of(ModuleSlot.AMMO, ModuleSlot.BARREL,
                ModuleSlot.MUZZLE, ModuleSlot.BATTERY, ModuleSlot.UTILITY),
                12.0D, 100.0D, new AttributeContainer());
    }

    public boolean supports(ModuleSlot slot) {
        return slot != null && supportedSlots.contains(slot);
    }
}
