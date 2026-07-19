package com.huige233.transcend.items;

import com.mojang.authlib.GameProfile;
import com.huige233.transcend.spell.MagicCrystalHelper;
import com.huige233.transcend.spell.SpellAspect;
import com.huige233.transcend.spell.SpellDamageService;
import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscendWandAuthoritativePaymentTest {
    private static final ChunkPos CAST_POS = new ChunkPos(4, -3);

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
    void chunkAspectMultiplierUsesExistingWholeManaTruncation() {
        assertEquals(14, ChunkManaSavedData.applyManaCostMultiplier(17, 0.85F));
        assertEquals(19, ChunkManaSavedData.applyManaCostMultiplier(17, 1.15F));
        assertEquals(17, ChunkManaSavedData.applyManaCostMultiplier(17, 1.0F));
        assertEquals(1, ChunkManaSavedData.applyManaCostMultiplier(1, 0.85F));
        assertEquals(0, ChunkManaSavedData.applyManaCostMultiplier(0, 1.15F));
    }

    @Test
    void malformedChunkMultipliersAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ChunkManaSavedData.applyManaCostMultiplier(17, Float.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ChunkManaSavedData.applyManaCostMultiplier(17, 0.0F));
    }

    @ParameterizedTest(name = "real wand boundary denies insufficient {0} cost atomically")
    @MethodSource("insufficientRelationships")
    void realWandBoundaryLeavesConcretePlayerWandAndChunkStateUnchanged(
            String relationship, SpellAspect chunkAspect, int available, int expectedCost) throws Exception {
        Fixture fixture = fixture(chunkAspect, available, false);
        Snapshot before = fixture.snapshot();

        SpellCastPayment.Result result = TranscendWand.castConfiguredSpellPaymentBoundary(
                fixture.player, fixture.wand, fixture.chunkMana, CAST_POS,
                SpellAspect.BLAZE, 17, false);

        assertFalse(result.paid(), relationship);
        assertEquals(expectedCost, result.manaCost(), relationship);
        assertEquals(before, fixture.snapshot(), relationship + " denial mutated concrete cast state");
    }

    @Test
    void realWandBoundaryDeductsActualInnateManaExactlyOnce() throws Exception {
        Fixture fixture = fixture(SpellAspect.BLAZE, 14, false);

        SpellCastPayment.Result result = TranscendWand.castConfiguredSpellPaymentBoundary(
                fixture.player, fixture.wand, fixture.chunkMana, CAST_POS,
                SpellAspect.BLAZE, 17, false, () -> {
                    fixture.player.maxManaReads++;
                    return 10_000;
                });

        assertTrue(result.paid());
        assertEquals(14, result.manaCost());
        assertEquals(0, MagicCrystalHelper.getInnateMana(fixture.player));
        assertEquals(1, fixture.player.maxManaReads,
                "one real innate deduction must perform one clamped write");
        assertEquals(100.0F, fixture.chunkMana.getTintProgress(CAST_POS));
        assertEquals(9, fixture.wand.getTag().getInt("slot_cd_0"));
        assertEquals(41, fixture.wand.getTag().getInt("spell_xp_probe"));
    }

    @Test
    void realAscensionFreeBoundarySkipsMalformedManaWithoutMutation() throws Exception {
        Fixture fixture = fixture(SpellAspect.FROST, -1, false);

        SpellCastPayment.Result result = TranscendWand.castConfiguredSpellPaymentBoundary(
                fixture.player, fixture.wand, fixture.chunkMana, CAST_POS,
                SpellAspect.BLAZE, 17, true);

        assertTrue(result.paid());
        assertEquals(0, result.manaCost());
        assertEquals(-1, MagicCrystalHelper.getInnateMana(fixture.player));
        assertEquals(0, fixture.player.maxManaReads);
        assertFalse(fixture.wand.getTag().getBoolean("transcend_echo_pending"));
    }

    @Test
    void realEchoFreeBoundaryIsOverflowSafeAndConsumesPendingAfterSuccess() throws Exception {
        Fixture fixture = fixture(SpellAspect.FROST, -1, true);

        SpellCastPayment.Result result = TranscendWand.castConfiguredSpellPaymentBoundary(
                fixture.player, fixture.wand, fixture.chunkMana, CAST_POS,
                SpellAspect.BLAZE, Integer.MAX_VALUE, false);

        assertTrue(result.paid());
        assertEquals(0, result.manaCost());
        assertEquals(-1, MagicCrystalHelper.getInnateMana(fixture.player));
        assertEquals(0, fixture.player.maxManaReads);
        assertFalse(fixture.wand.getTag().getBoolean("transcend_echo_pending"));
    }

    @Test
    void realBoundarySaturatesOverflowAndRejectsMalformedPaidMana() throws Exception {
        Fixture fixture = fixture(SpellAspect.FROST, -1, false);
        Snapshot before = fixture.snapshot();

        SpellCastPayment.Result result = TranscendWand.castConfiguredSpellPaymentBoundary(
                fixture.player, fixture.wand, fixture.chunkMana, CAST_POS,
                SpellAspect.BLAZE, Integer.MAX_VALUE, false);

        assertFalse(result.paid());
        assertEquals(Integer.MAX_VALUE, result.manaCost());
        assertEquals(before, fixture.snapshot());
    }

    private static Stream<Arguments> insufficientRelationships() {
        return Stream.of(
                Arguments.of("same", SpellAspect.BLAZE, 13, 14),
                Arguments.of("opposite", SpellAspect.FROST, 18, 19),
                Arguments.of("unrelated", SpellAspect.VERDANT, 16, 17));
    }

    private static Fixture fixture(SpellAspect chunkAspect, int innateMana, boolean echoPending)
            throws Exception {
        TestServerPlayer player = (TestServerPlayer) unsafe().allocateInstance(TestServerPlayer.class);
        player.data = new CompoundTag();
        player.data.putInt(MagicCrystalHelper.INNATE_MANA_TAG, innateMana);
        player.data.putString("transcend_element_mark", "water");
        player.data.putInt("transcend_element_mark_ticks", 77);
        player.data.putLong("task7_cast_counter_probe", 12L);
        player.data.putInt(SpellDamageService.CANONICAL_MARK_TAG, 5);
        player.inventory = new Inventory(player);

        ItemStack wand = new ItemStack(Items.STICK);
        CompoundTag wandTag = wand.getOrCreateTag();
        wandTag.putBoolean("transcend_echo_pending", echoPending);
        wandTag.putInt("slot_cd_0", 9);
        wandTag.putInt("spell_xp_probe", 41);
        ListTag slots = new ListTag();
        CompoundTag slot = new CompoundTag();
        slot.putString("carrier", "orb");
        slot.putString("element", "fire");
        slot.putInt("spell_xp", 8);
        slots.add(slot);
        wandTag.put("wand_slots", slots);

        return new Fixture(player, wand, establishedChunk(chunkAspect));
    }

    private static ChunkManaSavedData establishedChunk(SpellAspect aspect) {
        CompoundTag root = new CompoundTag();
        ListTag aspects = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putLong("Pos", CAST_POS.toLong());
        entry.putString("Aspect", aspect.name());
        entry.putFloat("Tint", 100.0F);
        aspects.add(entry);
        root.put("Aspects", aspects);
        return ChunkManaSavedData.load(root);
    }

    private record Fixture(TestServerPlayer player, ItemStack wand, ChunkManaSavedData chunkMana) {
        Snapshot snapshot() {
            return new Snapshot(MagicCrystalHelper.getInnateMana(player), player.inventory.getContainerSize(),
                    player.data.copy(), wand.getOrCreateTag().copy(), chunkMana.save(new CompoundTag()),
                    player.maxManaReads);
        }
    }

    private record Snapshot(int innateMana, int inventorySize, CompoundTag playerData,
                            CompoundTag wandData, CompoundTag chunkData, int maxManaReads) {
    }

    private static final class TestServerPlayer extends ServerPlayer {
        private Inventory inventory;
        private CompoundTag data;
        private int maxManaReads;

        private TestServerPlayer() {
            super((MinecraftServer) null, (ServerLevel) null,
                    new GameProfile(UUID.randomUUID(), "task7"));
            throw new UnsupportedOperationException("allocate with Unsafe");
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        @Override
        public CompoundTag getPersistentData() {
            return data;
        }

        @Override
        public boolean isCreative() {
            return false;
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public double getAttributeValue(Attribute attribute) {
            maxManaReads++;
            return 10_000.0D;
        }
    }

    private static sun.misc.Unsafe unsafe() throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (sun.misc.Unsafe) field.get(null);
    }
}
