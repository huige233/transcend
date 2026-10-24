package com.huige233.transcend.block;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.tech.BlackHoleSeedItem;
import com.huige233.transcend.items.tech.MagneticConfinementContainerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Transcend.MODID)
@PrefixGameTestTemplate(false)
public final class BreederWorkflowGameTests {
    private BreederWorkflowGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void completeCycleConsumesExactMatterAndEnergy(GameTestHelper helper) {
        var breeder = breeder(helper, new BlockPos(1, 1, 1));
        supplyMatter(helper, breeder, BlackHoleSeedBreederBlockEntity.MATTER_PER_CYCLE);
        supplyEnergy(helper, breeder);
        // Let the registered block ticker perform the work, without calling tick manually.
        helper.runAfterDelay(BlackHoleSeedBreederBlockEntity.CYCLE_TICKS - 2, () -> {
            helper.assertTrue(breeder.items().getStackInSlot(2).isEmpty(), "a full cycle must finish before a singularity appears");
            helper.succeedWhen(() -> assertCompleted(helper, breeder));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void missingEnergyOrValidMatterCannotProduceFreeSingularities(GameTestHelper helper) {
        var noMatter = breeder(helper, new BlockPos(1, 1, 1));
        var noEnergy = breeder(helper, new BlockPos(2, 1, 1));
        var invalidMatter = breeder(helper, new BlockPos(1, 1, 2));
        supplyEnergy(helper, noMatter);
        supplyMatter(helper, noEnergy, BlackHoleSeedBreederBlockEntity.MATTER_PER_CYCLE);
        supplyEnergy(helper, invalidMatter);
        // Model an old inventory whose item is no longer in the data-pack tag.
        invalidMatter.items().setStackInSlot(1, new ItemStack(Items.DIRT, BlackHoleSeedBreederBlockEntity.MATTER_PER_CYCLE));
        helper.runAfterDelay(BlackHoleSeedBreederBlockEntity.CYCLE_TICKS + 10, () -> {
            for (var machine : new BlackHoleSeedBreederBlockEntity[] {noMatter, noEnergy, invalidMatter}) {
                helper.assertTrue(machine.progress() == 0 && machine.matterConsumed() == 0, "missing resources cannot advance breeding");
                helper.assertTrue(machine.items().getStackInSlot(2).isEmpty() && machine.items().getStackInSlot(3).isEmpty(),
                        "waiting for resources cannot create either output");
                helper.assertTrue(BlackHoleSeedBreederBlockEntity.isLoadedContainer(machine.items().getStackInSlot(0)),
                        "waiting preserves the loaded input container");
            }
            helper.assertTrue(noMatter.energyStored() == BlackHoleSeedBreederBlockEntity.ENERGY_CAPACITY,
                    "missing matter cannot waste energy");
            helper.assertTrue(noEnergy.energyStored() == 0
                            && noEnergy.items().getStackInSlot(1).getCount() == BlackHoleSeedBreederBlockEntity.MATTER_PER_CYCLE,
                    "missing energy cannot waste matter");
            helper.assertTrue(invalidMatter.energyStored() == BlackHoleSeedBreederBlockEntity.ENERGY_CAPACITY
                            && invalidMatter.items().getStackInSlot(1).getCount() == BlackHoleSeedBreederBlockEntity.MATTER_PER_CYCLE,
                    "invalid matter cannot be substituted for tagged ingredients");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void materialShortfallPausesAndResumesTheSameCycle(GameTestHelper helper) {
        var breeder = breeder(helper, new BlockPos(1, 1, 1));
        supplyMatter(helper, breeder, BlackHoleSeedBreederBlockEntity.MATTER_PER_CYCLE - 1);
        supplyEnergy(helper, breeder);
        helper.runAfterDelay(BlackHoleSeedBreederBlockEntity.CYCLE_TICKS + 10, () -> {
            int paidTicks = BlackHoleSeedBreederBlockEntity.CYCLE_TICKS - BlackHoleSeedBreederBlockEntity.MATTER_INTERVAL;
            helper.assertTrue(breeder.progress() == paidTicks && breeder.matterConsumed() == BlackHoleSeedBreederBlockEntity.MATTER_PER_CYCLE - 1,
                    "the last missing ingredient pauses the paid cycle at its next matter boundary");
            helper.assertTrue(breeder.energyStored() == BlackHoleSeedBreederBlockEntity.MATTER_INTERVAL * BlackHoleSeedBreederBlockEntity.FE_PER_TICK,
                    "paused work retains exactly the unpaid energy");
            helper.assertTrue(breeder.items().getStackInSlot(2).isEmpty(), "partial materials cannot yield a complete singularity");
            helper.assertTrue(breeder.items().extractItem(0, 1, false).isEmpty(), "in-progress container remains locked while paused");
            supplyMatter(helper, breeder, 1);
            helper.succeedWhen(() -> assertCompleted(helper, breeder));
        });
    }

    private static BlackHoleSeedBreederBlockEntity breeder(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.BLACK_HOLE_SEED_BREEDER.get());
        var breeder = (BlackHoleSeedBreederBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        ItemStack seed = new ItemStack(ModItems.black_hole_seed.get());
        BlackHoleSeedItem.markInactive(seed);
        ItemStack container = new ItemStack(ModItems.magnetic_confinement_container.get());
        helper.assertTrue(MagneticConfinementContainerItem.load(container, seed), "real seed loads into a confinement container");
        helper.assertTrue(breeder.items().insertItem(0, container, false).isEmpty(), "breeder accepts the loaded container");
        return breeder;
    }

    private static void supplyMatter(GameTestHelper helper, BlackHoleSeedBreederBlockEntity breeder, int count) {
        helper.assertTrue(breeder.items().insertItem(1, new ItemStack(Items.WHEAT_SEEDS, count), false).isEmpty(),
                "current data-pack matter tag permits wheat seeds");
    }

    private static void supplyEnergy(GameTestHelper helper, BlackHoleSeedBreederBlockEntity breeder) {
        helper.assertTrue(breeder.receiveExternalEnergy(BlackHoleSeedBreederBlockEntity.ENERGY_CAPACITY)
                        == BlackHoleSeedBreederBlockEntity.ENERGY_CAPACITY,
                "a full cycle receives its exact energy cost");
    }

    private static void assertCompleted(GameTestHelper helper, BlackHoleSeedBreederBlockEntity breeder) {
        ItemStack singularity = breeder.items().getStackInSlot(2);
        ItemStack returned = breeder.items().getStackInSlot(3);
        helper.assertTrue(singularity.is(ModItems.mini_singularity.get()) && singularity.getCount() == 1,
                "one paid cycle produces exactly one miniature singularity");
        helper.assertTrue(returned.is(ModItems.magnetic_confinement_container.get()) && returned.getCount() == 1
                        && !MagneticConfinementContainerItem.isLoaded(returned),
                "the cycle returns exactly one reusable empty container");
        helper.assertTrue(breeder.energyStored() == 0 && breeder.items().getStackInSlot(1).isEmpty(),
                "a singularity costs exactly 200 ticks of energy and 20 matter");
        helper.assertTrue(breeder.items().getStackInSlot(0).isEmpty() && breeder.progress() == 0 && breeder.matterConsumed() == 0,
                "completion consumes the loaded input and clears only completed cycle state");
    }
}
