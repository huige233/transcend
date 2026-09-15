package com.huige233.transcend.tech.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 验证天狼星四级穿透和固定百分比伤害预算的弹丸分摊，并防止普通射击获得专属加成。 */
class ShotPenetrationTest {
    @Test
    void siriusGrantsTierFourAndExactlyTenPercent() {
        PenetrationProfile result = ShotPenetration.perProjectile(PenetrationProfile.none(), true, 1);
        assertEquals(4, result.armorStrength());
        assertEquals(4, result.shieldStrength());
        assertEquals(4, result.bossStrength());
        assertEquals(100.0F, PenetrationPolicy.directPercentDamage(result, 3, 1000.0F));
        assertEquals(0.0F, PenetrationPolicy.directPercentDamage(result, 5, 1000.0F));
        assertEquals(0.0F, PenetrationPolicy.directPercentDamage(result, 0, 1000.0F));
    }

    @Test
    void scatterAndAttributesCannotMultiplySiriusBudget() {
        PenetrationProfile base = new PenetrationProfile(6, 6, 6, 20.0F);
        for (int count : new int[]{1, 3, 16, 64}) {
            PenetrationProfile result = ShotPenetration.perProjectile(base, true, count);
            assertEquals(10.0F, result.maxHealthDamagePercent() * count, 0.00001F);
            assertEquals(6, result.bossStrength());
        }
    }

    @Test
    void ordinaryShotsDoNotAcquireSiriusPenetrationOrPercent() {
        assertEquals(PenetrationProfile.none(), ShotPenetration.perProjectile(PenetrationProfile.none(), false, 3));
        PenetrationProfile generic = new PenetrationProfile(1, 2, 3, 18.0F);
        assertEquals(new PenetrationProfile(1, 2, 3, 6.0F), ShotPenetration.perProjectile(generic, false, 3));
        assertEquals(generic, ShotPenetration.perProjectile(generic, false, 0));
    }
}
