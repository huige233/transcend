package com.huige233.transcend.tech.shield;

import com.huige233.transcend.entity.TestDummy.DamageCategory;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证护盾层按类别与安装顺序吸收伤害、跳过被穿透层，并正确计算溢出和恢复存档状态。 */
class ShieldClusterTest {

    @Test
    void directionalLayerUsesThreeToOneBaselineForMatchingCategory() {
        ShieldCluster cluster = new ShieldCluster();
        cluster.add(new ShieldInstance("test", 10.0F, 10.0F, 0.0F,
                ShieldDefenseMode.directional(DamageCategory.MELEE.index), 3.0F));

        ShieldResolution result = cluster.resolve(new ShieldDamageContext(DamageCategory.MELEE, 12.0F, false));

        assertEquals(12.0F, result.absorbedDamage(), 0.0001F);
        assertEquals(4.0F, result.shieldEnergySpent(), 0.0001F);
        assertEquals(6.0F, cluster.energy(), 0.0001F);
    }

    @Test
    void unmatchedDirectionalLayerDoesNotDrain() {
        ShieldCluster cluster = new ShieldCluster();
        cluster.add(new ShieldInstance("test", 10.0F, 10.0F, 0.0F,
                ShieldDefenseMode.directional(DamageCategory.MELEE.index), 3.0F));

        ShieldResolution result = cluster.resolve(new ShieldDamageContext(DamageCategory.FIRE, 5.0F, false));

        assertFalse(result.absorbedAny());
        assertEquals(5.0F, result.remainingDamage(), 0.0001F);
        assertEquals(10.0F, cluster.energy(), 0.0001F);
    }

    @Test
    void clusterUsesLayersInInstallationOrderAndReturnsOverflow() {
        ShieldCluster cluster = new ShieldCluster();
        cluster.add(new ShieldInstance("first", 2.0F, 2.0F, 0.0F, ShieldDefenseMode.omni(), 1.0F));
        cluster.add(new ShieldInstance("second", 4.0F, 4.0F, 0.0F, ShieldDefenseMode.omni(), 1.0F));

        ShieldResolution result = cluster.resolve(new ShieldDamageContext(DamageCategory.MAGIC, 8.0F, false));

        assertEquals(6.0F, result.absorbedDamage(), 0.0001F);
        assertEquals(2.0F, result.remainingDamage(), 0.0001F);
        assertEquals(2, result.participatingLayers());
        assertEquals(2, result.depletedLayers());
    }

    @Test
    void bypassedDamageCannotConsumeShieldEnergy() {
        ShieldCluster cluster = new ShieldCluster();
        cluster.add(new ShieldInstance("test", 10.0F, 10.0F, 0.0F, ShieldDefenseMode.omni(), 1.0F));

        ShieldResolution result = cluster.resolve(new ShieldDamageContext(DamageCategory.ENVIRONMENT, 5.0F, true));

        assertFalse(result.absorbedAny());
        assertEquals(10.0F, cluster.energy(), 0.0001F);
    }

    @Test
    void penetrationSkipsLowerTierWithoutDraining() {
        ShieldCluster cluster = new ShieldCluster();
        cluster.add(new ShieldInstance("tier_one", 10, 10, 0, ShieldDefenseMode.omni(), 1));
        cluster.add(new ShieldInstance("tier_two", 10, 10, 0, ShieldDefenseMode.omni(), 1));
        
        cluster.clear();
        cluster.add(new ShieldInstance("tier_one", 1, 10, 10, 0, ShieldDefenseMode.omni(), 1));
        cluster.add(new ShieldInstance("tier_two", 2, 10, 10, 0, ShieldDefenseMode.omni(), 1));

        ShieldResolution result = cluster.resolve(new ShieldDamageContext(DamageCategory.MAGIC, 5, false, 1));

        assertEquals(5.0F, result.absorbedDamage(), 0.0001F);
        assertEquals(10.0F, cluster.layers().get(0).energy(), 0.0001F);
        assertEquals(5.0F, cluster.layers().get(1).energy(), 0.0001F);
        assertEquals(1, result.penetratedLayers());
    }
    @Test
    void persistedClusterRestoresLayerState() {
        ShieldCluster original = new ShieldCluster();
        original.add(new ShieldInstance("directional", 20.0F, 7.5F, 0.5F,
                ShieldDefenseMode.directional(DamageCategory.PROJECTILE.index), 3.0F));
        CompoundTag tag = new CompoundTag();
        original.save(tag);

        ShieldCluster restored = ShieldCluster.load(tag);

        assertEquals(1, restored.layers().size());
        assertEquals(7.5F, restored.energy(), 0.0001F);
        assertEquals(DamageCategory.PROJECTILE.index, restored.layers().get(0).defenseMode().categoryIndex());
    }
}
