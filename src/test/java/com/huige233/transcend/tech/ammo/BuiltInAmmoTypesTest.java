package com.huige233.transcend.tech.ammo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** 验证内置弹药的稳定标识能解析到对应类型，并拒绝不存在的弹药标识。 */
class BuiltInAmmoTypesTest {
    @Test
    void resolvesTheStableBuiltInAmmoIds() {
        assertSame(BuiltInAmmoTypes.charge(), BuiltInAmmoTypes.byId(BuiltInAmmoTypes.CHARGE_ID));
        assertEquals(BuiltInAmmoTypes.ENERGY_ID, BuiltInAmmoTypes.byId(BuiltInAmmoTypes.ENERGY_ID).id());
        assertEquals(BuiltInAmmoTypes.SPELL_ID, BuiltInAmmoTypes.byId(BuiltInAmmoTypes.SPELL_ID).id());
        assertNull(BuiltInAmmoTypes.byId("removed_ammo"));
    }
}
