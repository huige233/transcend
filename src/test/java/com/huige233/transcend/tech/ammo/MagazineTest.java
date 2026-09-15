package com.huige233.transcend.tech.ammo;

import com.huige233.transcend.tech.attribute.AttributeContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证弹匣精确扣费、单一弹种装填、失败不改状态，以及清空后移除过期弹种存档。 */
class MagazineTest {
    @Test
    void reloadConsumesExactAmountAndUsesOneAmmoType() {
        TestResource resource = new TestResource(20L, true, true);
        Magazine magazine = new Magazine();

        assertTrue(magazine.tryReload(null, null, new TestAmmo("energy", resource), 12, 10L));
        assertEquals(10L, resource.consumed);
        assertEquals(12, magazine.charges());
        assertEquals("energy", magazine.loadedAmmoId());

        assertTrue(magazine.tryReload(null, null, new TestAmmo("charge", resource), 5, 5L));
        assertEquals(15L, resource.consumed);
        assertEquals(5, magazine.charges());
        assertEquals("charge", magazine.loadedAmmoId());
    }

    @Test
    void unsuccessfulReloadLeavesExistingMagazineUntouched() {
        TestResource enough = new TestResource(20L, true, true);
        Magazine magazine = new Magazine();
        assertTrue(magazine.tryReload(null, null, new TestAmmo("energy", enough), 4, 10L));

        TestResource partial = new TestResource(20L, true, false);
        assertFalse(magazine.tryReload(null, null, new TestAmmo("spell", partial), 12, 10L));
        assertEquals("energy", magazine.loadedAmmoId());
        assertEquals(4, magazine.charges());
    }

    @Test
    void emptyMagazineDoesNotPersistStaleAmmoId() {
        Magazine magazine = new Magazine();
        TestResource resource = new TestResource(5L, true, true);
        assertTrue(magazine.tryReload(null, null, new TestAmmo("energy", resource), 1, 1L));
        assertTrue(magazine.tryConsumeRound());

        CompoundTag data = new CompoundTag();
        data.putString(Magazine.NBT_AMMO_ID, "stale");
        magazine.save(data);
        assertFalse(data.contains(Magazine.NBT_AMMO_ID));

        data.putInt(Magazine.NBT_CHARGES, -2);
        data.putString(Magazine.NBT_AMMO_ID, "invalid");
        magazine.load(data);
        assertTrue(magazine.isEmpty());
        assertNull(magazine.loadedAmmoId());
    }

    /** 为弹匣测试提供绑定指定标识和资源的最小弹药类型实现。 */
    private record TestAmmo(String id, AmmoResource resource) implements AmmoType {
        @Override public String displayName() { return id; }
        @Override public DamageSource createDamageSource(Level level, @Nullable net.minecraft.world.entity.Entity owner) {
            throw new UnsupportedOperationException();
        }
        @Override public BoltVisual visual() { return new BoltVisual(Component.literal(id), 0, null, 1.0F, false); }
        @Override public BoltBehavior behavior(AttributeContainer attrs) { return BoltBehavior.direct(); }
        @Override public long reloadCost() { return 1L; }
        @Override public int reloadCapacity() { return 1; }
    }

    /** 模拟可用余额及精确扣费成败，并累计资源消耗以验证弹匣装填事务。 */
    private static final class TestResource implements AmmoResource {
        private long available;
        private final boolean availableFlag;
        private final boolean exact;
        private long consumed;

        private TestResource(long available, boolean availableFlag, boolean exact) {
            this.available = available;
            this.availableFlag = availableFlag;
            this.exact = exact;
        }

        @Override public String id() { return "test"; }
        @Override public Component displayName() { return Component.literal("test"); }
        @Override public long available(Player holder, ItemStack gun) { return available; }
        @Override public long consume(Player holder, ItemStack gun, long cost) {
            if (!exact || available < cost) return 0L;
            available -= cost;
            consumed += cost;
            return cost;
        }
        @Override public boolean isAvailable(Player holder) { return availableFlag; }
    }
}
