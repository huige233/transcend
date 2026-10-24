package com.huige233.transcend.block;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.*;
import com.huige233.transcend.tech.api.TechCapabilities;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
import java.math.BigInteger;

@GameTestHolder(Transcend.MODID)
@PrefixGameTestTemplate(false)
public final class TechPowerFlowGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void revivedStorageCapabilityStillMarksChanges(GameTestHelper helper) {
        class TrackedStorage extends LongStorageBlockEntity {
            int changes;
            TrackedStorage() { super(BlockPos.ZERO, ModBlocks.LONG_STORAGE_QUANTUM.get().defaultBlockState(), 1000); }
            @Override public void setChanged() { changes++; }
        }
        var storage = new TrackedStorage();
        var old = storage.getCapability(TechCapabilities.TECH_ENERGY, Direction.EAST);
        check(old.isPresent(), "initial capability exists");
        storage.invalidateCaps();
        check(!old.isPresent(), "old reference invalidated");
        storage.reviveCaps();
        var energy = storage.getCapability(TechCapabilities.TECH_ENERGY, Direction.EAST).orElseThrow(AssertionError::new);
        int before = storage.changes;
        check(energy.receive(100, true) == 100 && storage.changes == before, "simulation does not dirty storage");
        check(energy.receive(100, false) == 100 && storage.changes > before, "revived input marks save dirty");
        before = storage.changes;
        check(energy.extract(40, false) == 40 && storage.changes > before, "revived output marks save dirty");
        check(storage.saveWithoutMetadata().getLong("Energy") == 60, "saved energy reflects transfers");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void universeVisualSyncDoesNotOverwriteMachineInventory(GameTestHelper helper) {
        var state = ModBlocks.MINI_UNIVERSE_GENERATOR.get().defaultBlockState();
        var server = new MiniUniverseGeneratorBlockEntity(BlockPos.ZERO, state);
        CompoundTag saved = new CompoundTag();
        saved.putString("Phase", "GENERATING");
        saved.putString("Remaining", MiniUniverseGeneratorBlockEntity.OUTPUT.divide(BigInteger.TEN).toString());
        server.load(saved);
        var client = new MiniUniverseGeneratorBlockEntity(BlockPos.ZERO, state);
        client.items().insertItem(0, new ItemStack(ModItems.mini_singularity.get()), false);
        CompoundTag update = server.getUpdateTag();
        check(update.getAllKeys().size() == 1 && update.contains("VisualUsage"), "visual packet excludes inventories and energy pools");
        client.handleUpdateTag(update);
        check(Math.abs(client.generationUsage() - .9f) < .001f, "chunk update restores evolution progress");
        check(client.items().getStackInSlot(0).getCount() == 1, "visual update cannot clear inventory");
        server.load(new CompoundTag());
        client.onDataPacket(null, server.getUpdatePacket());
        check(client.generationUsage() == 0, "block entity packet resets visual progress");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void microUniverseSharesExportBudgetAcrossLongFeAndFaces(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.MINI_UNIVERSE_GENERATOR.get());
        var generator = (MiniUniverseGeneratorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)));
        generator.items().insertItem(0, new ItemStack(ModItems.mini_singularity.get()), false);
        check(generator.receiveExternalEnergy(Integer.MAX_VALUE) == Integer.MAX_VALUE, "startup energy supplied");
        MiniUniverseGeneratorBlockEntity.serverTick(helper.getLevel(), generator.getBlockPos(), generator.getBlockState(), generator);
        check(generator.active(), "real startup enters generating phase");
        var longPort = generator.getCapability(TechCapabilities.TECH_ENERGY, Direction.EAST).orElseThrow(AssertionError::new);
        var otherLong = generator.getCapability(TechCapabilities.TECH_ENERGY, Direction.WEST).orElseThrow(AssertionError::new);
        var fe = generator.getCapability(ForgeCapabilities.ENERGY, Direction.NORTH).orElseThrow(AssertionError::new);
        BigInteger total = generator.remainingAmount().add(generator.generatedAmount()).add(BigInteger.valueOf(generator.outputEnergy()));
        check(longPort.extract(Long.MAX_VALUE, true) == MiniUniverseGeneratorBlockEntity.LONG_OUTPUT_RATE, "simulation offers a trillion RF");
        check(generator.remainingAmount().add(generator.generatedAmount()).add(BigInteger.valueOf(generator.outputEnergy())).equals(total), "simulation cannot consume energy");
        check(longPort.extract(Long.MAX_VALUE, false) == MiniUniverseGeneratorBlockEntity.LONG_OUTPUT_RATE, "single transfer exceeds integer FE ceiling");
        check(otherLong.extract(Long.MAX_VALUE, false) == 0 && fe.extractEnergy(Integer.MAX_VALUE, false) == 0, "faces and FE cannot bypass shared budget");
        check(total.subtract(generator.remainingAmount().add(generator.generatedAmount()).add(BigInteger.valueOf(generator.outputEnergy())))
                .equals(BigInteger.valueOf(MiniUniverseGeneratorBlockEntity.LONG_OUTPUT_RATE)), "total energy deducted exactly once");
        helper.runAfterDelay(1, () -> {
            check(longPort.extract(Long.MAX_VALUE, false) == MiniUniverseGeneratorBlockEntity.LONG_OUTPUT_RATE, "budget resets next tick");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void longStorageActivelySuppliesResearchBeyondIntegerRange(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.LONG_STORAGE_QUANTUM.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.RESEARCH_STATION.get());
        var storage = (LongStorageBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)));
        var station = (ResearchStationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 1)));
        station.plugins().insertItem(0, new ItemStack(ModItems.research_cosmic_simulator.get()), false);
        CompoundTag energy = new CompoundTag(); energy.putLong("Energy", 400_000_000_000L); storage.load(energy);
        LongStorageBlockEntity.tick(helper.getLevel(), storage);
        check(storage.stored() == 0 && station.energyStored() == 400_000_000_000L, "active transfer preserves the full long amount");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void microUniverseActivelySuppliesNativeStorageWithoutDuplicatingEnergy(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.MINI_UNIVERSE_GENERATOR.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.LONG_STORAGE_QUANTUM.get());
        var generator = (MiniUniverseGeneratorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)));
        var storage = (LongStorageBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 1)));
        generator.items().insertItem(0, new ItemStack(ModItems.mini_singularity.get()), false);
        generator.receiveExternalEnergy(Integer.MAX_VALUE);
        MiniUniverseGeneratorBlockEntity.serverTick(helper.getLevel(), generator.getBlockPos(), generator.getBlockState(), generator);
        check(storage.stored() == MiniUniverseGeneratorBlockEntity.LONG_OUTPUT_RATE, "FE buffered push and native push share the exact rate");
        BigInteger exported = MiniUniverseGeneratorBlockEntity.OUTPUT.subtract(generator.remainingAmount())
                .subtract(generator.generatedAmount())
                .subtract(BigInteger.valueOf(generator.outputEnergy()));
        check(exported.equals(BigInteger.valueOf(storage.stored())), "remaining generator energy plus receiver storage is conserved");
        helper.succeed();
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
