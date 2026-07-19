package com.huige233.transcend.spell;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellDamageServiceEntropyIntegrationTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError error) {
            if (!causedByMissingForgeNetworkEventConstructor(error)) throw error;
        }
    }

    private static boolean causedByMissingForgeNetworkEventConstructor(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof NoSuchMethodException
                    && cause.getMessage().contains("net.minecraftforge.network.NetworkEvent.<init>()")) return true;
        }
        return false;
    }

    @Test
    void activeEntropyUsesOneResolvedElementAcrossCompleteDealPipeline() throws Exception {
        TestLivingEntity target = targetWithMark(SpellElement.WOOD);
        AtomicInteger selections = new AtomicInteger();
        RecordingRuntime runtime = new RecordingRuntime();

        SpellDamageService.Result result = SpellDamageService.deal(
                serverLevel(), null, null, target, SpellElement.WATER, 3, 10.0F, true,
                true, () -> {
                    selections.incrementAndGet();
                    return 3;
                }, runtime);

        assertTrue(result.landed());
        assertEquals(1, selections.get());
        assertEquals(SpellElement.FIRE, result.resolvedElement());
        assertEquals(SpellElement.FIRE, runtime.resistanceElement);
        assertEquals(SpellElement.FIRE, runtime.effectElement);
        assertEquals(SpellElement.FIRE, runtime.flashElement);
        assertEquals(7.5F, result.attemptedDamage(), 0.0001F,
                "FIRE resistance adjustment and WOOD->FIRE generation must both apply");
        assertEquals(92.5F, target.health, 0.0001F);
        assertEquals("fire", target.data.getString("transcend_element_mark"));
        assertEquals(100, target.data.getInt("transcend_element_mark_ticks"));
    }

    @Test
    void inactiveEntropyKeepsCanonicalElementAndDoesNotConsumeSelector() throws Exception {
        TestLivingEntity target = targetWithMark(SpellElement.METAL);
        AtomicInteger selections = new AtomicInteger();
        RecordingRuntime runtime = new RecordingRuntime();

        SpellDamageService.Result result = SpellDamageService.deal(
                serverLevel(), null, null, target, SpellElement.WATER, 3, 10.0F, true,
                false, () -> {
                    selections.incrementAndGet();
                    return 4;
                }, runtime);

        assertTrue(result.landed());
        assertEquals(0, selections.get());
        assertEquals(SpellElement.WATER, result.resolvedElement());
        assertEquals(SpellElement.WATER, runtime.resistanceElement);
        assertEquals(SpellElement.WATER, runtime.effectElement);
        assertEquals(SpellElement.WATER, runtime.flashElement);
        assertEquals("water", target.data.getString("transcend_element_mark"));
    }

    @Test
    void chaosSelectionRemainsSeparateWhileUsingResolvedPipelineElement() throws Exception {
        TestLivingEntity target = targetWithMark(SpellElement.METAL);
        RecordingRuntime runtime = new RecordingRuntime();

        SpellDamageService.Result result = SpellDamageService.deal(
                serverLevel(), null, null, target, SpellElement.CHAOS, 3, 10.0F, true,
                false, () -> 4, runtime);

        assertTrue(result.landed());
        assertTrue(runtime.chaosResistance);
        assertEquals(SpellElement.EARTH, result.resolvedElement());
        assertEquals(SpellElement.EARTH, runtime.resistanceElement);
        assertEquals(SpellElement.EARTH, runtime.effectElement);
        assertEquals(SpellElement.EARTH, runtime.flashElement);
        assertEquals("earth", target.data.getString("transcend_element_mark"));
    }

    private static TestLivingEntity targetWithMark(SpellElement mark) throws Exception {
        TestLivingEntity target = (TestLivingEntity) unsafe().allocateInstance(TestLivingEntity.class);
        target.data = new CompoundTag();
        target.health = 100.0F;
        ElementReaction.markElement(target, mark);
        return target;
    }

    private static ServerLevel serverLevel() throws Exception {
        return (ServerLevel) unsafe().allocateInstance(ServerLevel.class);
    }

    private static final class RecordingRuntime implements SpellDamageService.DealRuntime {
        private SpellElement resistanceElement;
        private SpellElement effectElement;
        private SpellElement flashElement;
        private boolean chaosResistance;

        @Override
        public float applyResistance(LivingEntity target, LivingEntity caster, SpellElement resolved,
                                     int spellTier, float rawDamage, boolean chaos) {
            resistanceElement = resolved;
            chaosResistance = chaos;
            return resolved == SpellElement.FIRE ? rawDamage * 0.5F : rawDamage;
        }

        @Override
        public boolean hurt(ServerLevel level, LivingEntity caster, net.minecraft.world.entity.Entity directSource,
                            LivingEntity target, float damage) {
            return target.hurt(null, damage);
        }

        @Override
        public void applyEffect(LivingEntity target, LivingEntity caster, SpellElement resolved) {
            effectElement = resolved;
        }

        @Override
        public void spawnHitFlash(ServerLevel level, LivingEntity target, SpellElement resolved) {
            flashElement = resolved;
        }
    }

    private static final class TestLivingEntity extends LivingEntity {
        private CompoundTag data;
        private float health;

        private TestLivingEntity() {
            super(EntityType.ARMOR_STAND, null);
            throw new UnsupportedOperationException("allocate with Unsafe");
        }

        @Override
        public boolean isAlive() {
            return health > 0.0F;
        }

        @Override
        public CompoundTag getPersistentData() {
            return data;
        }

        @Override
        public float getHealth() {
            return health;
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            health = Math.max(0.0F, health - amount);
            return true;
        }

        @Override
        public HumanoidArm getMainArm() {
            return HumanoidArm.RIGHT;
        }

        @Override
        public Iterable<ItemStack> getArmorSlots() {
            return List.of();
        }

        @Override
        public ItemStack getItemBySlot(EquipmentSlot slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        }
    }

    private static sun.misc.Unsafe unsafe() throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }
}
