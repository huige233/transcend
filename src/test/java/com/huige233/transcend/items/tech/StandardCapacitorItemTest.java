package com.huige233.transcend.items.tech;

import com.huige233.transcend.tech.shield.CapacitorEnergy;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证标准电容的有限电量可被耗尽且不会隐式回充，空存档也不会生成电量。 */
class StandardCapacitorItemTest {
    @Test
    void finiteChargeCanBeExhaustedWithoutImplicitRecharge() {
        CompoundTag tag = new CompoundTag();
        CapacitorEnergy.set(tag, CapacitorEnergy.CAPACITY);
        assertEquals(200.0F, CapacitorEnergy.stored(tag));
        assertEquals(75.0F, CapacitorEnergy.drain(tag, 75.0F));
        assertEquals(125.0F, CapacitorEnergy.stored(tag));
        assertEquals(125.0F, CapacitorEnergy.drain(tag, 250.0F));
        assertEquals(0.0F, CapacitorEnergy.stored(tag));
    }

    @Test
    void emptyTagDoesNotCreateCharge() {
        assertEquals(0.0F, CapacitorEnergy.stored(new CompoundTag()));
    }
}
