package com.huige233.transcend.tech.ammo;

import com.huige233.transcend.tech.attribute.AttributeContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证充能、能量和法术弹药在伤害、速度、穿透及溅射能力上的差异。 */
class BuiltInAmmoCombatProfileTest {
    @Test
    void builtInAmmoProfilesHaveDistinctCombatAndVisualRoles() {
        AttributeContainer attributes = new AttributeContainer();
        AmmoType charge = BuiltInAmmoTypes.charge();
        AmmoType energy = BuiltInAmmoTypes.byId(BuiltInAmmoTypes.ENERGY_ID);
        AmmoType spell = BuiltInAmmoTypes.byId(BuiltInAmmoTypes.SPELL_ID);

        assertEquals(0, charge.behavior(attributes).pierce);
        assertEquals(0.0F, charge.behavior(attributes).splashRadius);
        assertTrue(energy.speedMultiplier() > charge.speedMultiplier());
        assertTrue(energy.damageMultiplier() > charge.damageMultiplier());
        assertTrue(energy.behavior(attributes).pierce > 0);
        assertTrue(spell.damageMultiplier() > charge.damageMultiplier());
        assertTrue(spell.behavior(attributes).splashRadius > 0.0F);
    }
}
