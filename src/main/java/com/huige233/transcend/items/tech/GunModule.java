package com.huige233.transcend.items.tech;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;


/** 定义粒子枪模块的稳定标识、所属槽位、属性倍率和弹体附加行为。 */
public enum GunModule {
    
    AMMO_STD("ammo_std", Slot.AMMO, 1.0F, 1.0F, 1.0F),
    AMMO_SCATTER("ammo_scatter", Slot.AMMO, 0.55F, 1.6F, 1.2F),
    AMMO_PIERCING("ammo_piercing", Slot.AMMO, 1.4F, 1.2F, 1.0F),
    AMMO_EXPLOSIVE("ammo_explosive", Slot.AMMO, 1.2F, 1.4F, 1.5F),

    BARREL_STD("barrel_std", Slot.BARREL, 1.0F, 1.0F, 1.0F),
    BARREL_LONG("barrel_long", Slot.BARREL, 1.6F, 1.4F, 1.5F),
    BARREL_ACCEL("barrel_accel", Slot.BARREL, 1.3F, 0.6F, 1.2F),

    MUZZLE_NONE("", Slot.MUZZLE, 1.0F, 1.0F, 1.0F),
    MUZZLE_SILENCER("muzzle_silencer", Slot.MUZZLE, 0.9F, 1.0F, 1.0F),
    MUZZLE_SCOPE("muzzle_scope", Slot.MUZZLE, 1.0F, 1.0F, 1.0F),
    MUZZLE_SIRIUS("sirius", Slot.MUZZLE, 1.0F, 1.0F, 1.0F);

    /** 区分粒子枪的弹头、枪管与枪口安装槽位。 */
    public enum Slot { AMMO, BARREL, MUZZLE }

    public final String id;
    public final Slot slot;
    public final float damageMult;
    
    public final float cooldownMult;
    public final float speedMult;

    GunModule(String id, Slot slot, float damageMult, float cooldownMult, float speedMult) {
        this.id = id;
        this.slot = slot;
        this.damageMult = damageMult;
        this.cooldownMult = cooldownMult;
        this.speedMult = speedMult;
    }

    public MutableComponent displayName() {
        return Component.translatable("gunmodule.transcend." + (id.isEmpty() ? "none" : id));
    }

    
    public int modelIndex() {
        return switch (this) {
            case MUZZLE_NONE -> 0;
            case AMMO_STD -> 1;
            case AMMO_SCATTER -> 2;
            case AMMO_PIERCING -> 3;
            case AMMO_EXPLOSIVE -> 4;
            case BARREL_STD -> 5;
            case BARREL_LONG -> 6;
            case BARREL_ACCEL -> 7;
            case MUZZLE_SILENCER -> 8;
            case MUZZLE_SCOPE -> 9;
            case MUZZLE_SIRIUS -> 10;
        };
    }

    public int projectileCount() { return this == AMMO_SCATTER ? 3 : 1; }
    public int entityPierceBonus() { return this == AMMO_PIERCING ? 2 : 0; }
    public int penetrationBonus() { return this == AMMO_PIERCING ? 2 : 0; }
    public float splashRadius() { return this == AMMO_EXPLOSIVE ? 2.5F : 0.0F; }

    @Nullable
    public static GunModule byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (GunModule module : values()) {
            if (module.id.equals(id)) return module;
        }
        return null;
    }

    public static GunModule defaultFor(Slot slot) {
        return switch (slot) {
            case AMMO -> AMMO_STD;
            case BARREL -> BARREL_STD;
            case MUZZLE -> MUZZLE_NONE;
        };
    }
}
