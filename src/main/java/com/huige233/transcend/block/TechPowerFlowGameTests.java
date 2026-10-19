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
