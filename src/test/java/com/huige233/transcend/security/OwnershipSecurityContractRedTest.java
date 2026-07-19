package com.huige233.transcend.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.huige233.transcend.block.circle.MagicCircleCoreBlock;
import com.huige233.transcend.network.C2SCircleAction;
import com.huige233.transcend.network.C2SCircleSettingChange;
import com.huige233.transcend.network.C2STestDummySettingsPack;
import com.huige233.transcend.network.C2SWandCardEdit;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

class OwnershipSecurityContractRedTest {
    private static final BlockPos OPEN_TARGET = new BlockPos(10, 64, 10);
    private static final BlockPos FORGED_TARGET = new BlockPos(11, 64, 10);

    @Test
    void circlePlacementOverrideAssignsServerSideCreator() throws NoSuchMethodException {
        Method placement = MagicCircleCoreBlock.class.getDeclaredMethod("setPlacedBy",
                Level.class, BlockPos.class, BlockState.class, LivingEntity.class, ItemStack.class);
        ProductionBytecodeTestAccess.assertCreatorAssignment(placement);
    }

    @Test
    void circleActionValidationRejectsForgedPositionRangeAndMalformedAction() {
        Class<?>[] signature = {BlockPos.class, BlockPos.class, double.class, int.class};
        assertTrue(validate(C2SCircleAction.class, signature, OPEN_TARGET, OPEN_TARGET, 64.0, 0));
        assertFalse(validate(C2SCircleAction.class, signature, FORGED_TARGET, OPEN_TARGET, 1.0, 0),
                "forged BlockPos must be denied even while nearby");
        assertFalse(validate(C2SCircleAction.class, signature, OPEN_TARGET, OPEN_TARGET, 64.01, 0),
                "out-of-range circle action must be denied");
        assertFalse(validate(C2SCircleAction.class, signature, OPEN_TARGET, OPEN_TARGET, 1.0, -1),
                "negative action ordinal must be rejected without decoding failure");
        assertFalse(validate(C2SCircleAction.class, signature, OPEN_TARGET, OPEN_TARGET, 1.0,
                C2SCircleAction.ActionType.values().length), "oversized action ordinal must be rejected");
    }

    @Test
    void circleSettingValidationRejectsForgedPositionRangeAndMalformedKey() {
        Class<?>[] signature = {BlockPos.class, BlockPos.class, double.class, String.class};
        assertTrue(validate(C2SCircleSettingChange.class, signature,
                OPEN_TARGET, OPEN_TARGET, 64.0, "radius"));
        assertFalse(validate(C2SCircleSettingChange.class, signature,
                FORGED_TARGET, OPEN_TARGET, 1.0, "radius"));
        assertFalse(validate(C2SCircleSettingChange.class, signature,
                OPEN_TARGET, OPEN_TARGET, 64.01, "radius"));
        assertFalse(validate(C2SCircleSettingChange.class, signature,
                OPEN_TARGET, OPEN_TARGET, 1.0, ""));
        assertFalse(validate(C2SCircleSettingChange.class, signature,
                OPEN_TARGET, OPEN_TARGET, 1.0, "x".repeat(65)));
    }

    @Test
    void dummyHandlerValidationRejectsForgedEntityRangeAndMalformedAction() {
        Class<?>[] signature = {int.class, int.class, double.class, int.class};
        assertTrue(validate(C2STestDummySettingsPack.class, signature, 41, 41, 10.0, 0));
        assertFalse(validate(C2STestDummySettingsPack.class, signature, 42, 41, 1.0, 0),
                "forged entity ID must not select a different dummy");
        assertFalse(validate(C2STestDummySettingsPack.class, signature, 41, 41, 10.01, 0));
        assertFalse(validate(C2STestDummySettingsPack.class, signature, 41, 41, 1.0, -1));
        assertFalse(validate(C2STestDummySettingsPack.class, signature, 41, 41, 1.0, 5));
    }

    @Test
    void circleActionHandlerDelegatesValidationAndUsesContextSender() {
        ProductionBytecodeTestAccess.assertHandlerDelegates(C2SCircleAction.class,
                "activate", "deactivate", "handlePreviewGhost");
    }

    @Test
    void circleSettingHandlerDelegatesValidationAndUsesContextSender() {
        ProductionBytecodeTestAccess.assertHandlerDelegates(C2SCircleSettingChange.class,
                "setSettingValue");
    }

    @Test
    void dummySettingsHandlerDelegatesValidationAndUsesContextSender() {
        ProductionBytecodeTestAccess.assertHandlerDelegates(C2STestDummySettingsPack.class,
                "resetData", "toggleAnnounce", "setBaseValue", "setResistanceLevel", "discard");
    }

    @Test
    void wandCardEditRequiresProductionMeditationSessionAuthorization() {
        ProductionBytecodeTestAccess.assertCallsProductionMeditationSessionValidator(C2SWandCardEdit.class);
    }

    @Test
    void dummyPersistenceOverridesDelegateToSuperclassAndProductionCodec() {
        ProductionBytecodeTestAccess.assertDummyPersistenceDelegatesToSuperAndCodec(
                com.huige233.transcend.entity.TestDummy.class);
    }

    @Test
    void ordinaryDummyDamageContractDoesNotRequireAuthorizationSeam() throws Exception {
        Method hurt = com.huige233.transcend.entity.TestDummy.class.getMethod("hurt",
                net.minecraft.world.damagesource.DamageSource.class, float.class);
        assertTrue(java.lang.reflect.Modifier.isPublic(hurt.getModifiers()),
                "ordinary TestDummy damage must remain public");
    }

    private static boolean validate(Class<?> packetType, Class<?>[] signature, Object... arguments) {
        return ProductionOwnershipTestAccess.validate(packetType, signature, arguments);
    }
}
