package com.huige233.transcend.tech.combat;

import com.huige233.transcend.entity.projectile.ParticleBolt;
import com.huige233.transcend.items.armor.TranscendArmor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;


/** 依据粒子弹穿甲强度和目标护甲等级，仅调整普通护甲层的有效护甲值。 */
public final class ArmorPenetration {
    private ArmorPenetration() {}

    public static float effectiveArmor(LivingEntity target, DamageSource source, float armor) {
        if (target.level().isClientSide || !(source.getDirectEntity() instanceof ParticleBolt bolt)) return armor;
        int tier = PenetrationPolicy.ORDINARY_ARMOR_TIER;
        for (ItemStack stack : target.getArmorSlots()) {
            if (stack.getItem() instanceof TranscendArmor) {
                tier = PenetrationPolicy.TRANSCEND_ARMOR_TIER;
                break;
            }
        }
        return PenetrationPolicy.effectiveArmor(bolt.penetration(), tier, armor);
    }
}
