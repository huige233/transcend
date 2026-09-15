package com.huige233.transcend.items.tech;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 验证枪械模块标识、模型索引和槽位契约，以及散射、穿透和爆炸模块的战斗参数。 */
class GunModuleTest {
    @Test
    void allCraftableModulesHaveStableIdsAndSlots() {
        assertEquals(10, java.util.Arrays.stream(GunModule.values()).filter(m -> !m.id.isEmpty()).count());
        for (GunModule module : GunModule.values()) {
            if (!module.id.isEmpty()) assertEquals(module, GunModule.byId(module.id));
            assertTrue(module.modelIndex() >= 0);
        }
    }

    @Test
    void specialModulesExposeTheirCombatEffects() {
        assertEquals(3, GunModule.AMMO_SCATTER.projectileCount());
        assertEquals(2, GunModule.AMMO_PIERCING.entityPierceBonus());
        assertEquals(2, GunModule.AMMO_PIERCING.penetrationBonus());
        assertEquals(2.5F, GunModule.AMMO_EXPLOSIVE.splashRadius());
        assertEquals(1, GunModule.AMMO_STD.projectileCount());
    }

    @Test
    void defaultsAreNotRemovableAndEmptyIdsRemainInvalid() {
        assertNull(GunModule.byId(""));
        assertNull(GunModule.byId("unknown"));
        assertEquals(GunModule.MUZZLE_SIRIUS, GunModule.byId("sirius"));
        assertEquals(10, GunModule.MUZZLE_SIRIUS.modelIndex());
    }
}
