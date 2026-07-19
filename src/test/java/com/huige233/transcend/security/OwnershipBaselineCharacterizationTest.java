package com.huige233.transcend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.entity.TestDummy;
import com.huige233.transcend.network.C2SCircleAction;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Modifier;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class OwnershipBaselineCharacterizationTest {
    private static final UUID OWNER = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void actualCircleLoadsOldNbtAndPersistsOwnerAcrossReload() {
        CompoundTag oldNbt = new CompoundTag();
        assertNull(MagicCircleCoreBlockEntity.loadOwnerFromNbt(oldNbt),
                "old circle NBT must not invent an owner");

        CompoundTag saved = new CompoundTag();
        MagicCircleCoreBlockEntity.saveOwnerToNbt(saved, OWNER);
        assertEquals(OWNER, saved.getUUID("Owner"));
        assertEquals(OWNER, MagicCircleCoreBlockEntity.loadOwnerFromNbt(saved),
                "actual circle owner codec must survive save/reload");
    }

    @Test
    void actualCircleActionPacketCodecPreservesServerTargetAndAction() {
        BlockPos position = new BlockPos(7, 65, -9);
        C2SCircleAction packet = new C2SCircleAction(position, C2SCircleAction.ActionType.DEACTIVATE, 3);
        FriendlyByteBuf bytes = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(bytes);

        C2SCircleAction decoded = new C2SCircleAction(bytes);
        assertEquals(position, ProductionOwnershipTestAccess.field(decoded, "corePos"));
        assertEquals(C2SCircleAction.ActionType.DEACTIVATE,
                ProductionOwnershipTestAccess.field(decoded, "action"));
        assertEquals(3, ProductionOwnershipTestAccess.field(decoded, "param"));
    }

    @Test
    void ordinaryDummyDamageEntryPointRemainsPublic() throws NoSuchMethodException {
        int modifiers = TestDummy.class.getMethod("hurt",
                net.minecraft.world.damagesource.DamageSource.class, float.class).getModifiers();
        assertTrue(Modifier.isPublic(modifiers), "ordinary dummy damage must remain publicly callable");
    }
}
