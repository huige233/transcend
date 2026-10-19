package com.huige233.transcend.block;

import com.huige233.transcend.Transcend;

import com.huige233.transcend.block.ResearchStationBlockEntity;
import com.huige233.transcend.block.LongStorageBlockEntity;
import com.huige233.transcend.block.AssemblyBlockEntity;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.items.tech.ShieldModule;
import com.huige233.transcend.items.tech.ShieldModuleItem;
import com.huige233.transcend.tech.api.TechCapabilities;
import com.huige233.transcend.tech.assembly.*;
import com.huige233.transcend.tech.research.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.math.BigInteger;

@net.minecraftforge.gametest.GameTestHolder(Transcend.MODID)
@net.minecraftforge.gametest.PrefixGameTestTemplate(false)
public final class ResearchStationGameTests {
    private ResearchStationGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void researchCapabilitySurvivesSaveAndDeathClone(GameTestHelper helper) {
        Player original = helper.makeMockPlayer();
        ResearchProgress progress = original.getCapability(ResearchCapabilities.PROGRESS).orElseThrow(AssertionError::new);
        check(progress.start("energy_basics"), "start research");
        progress.addPoints(BigInteger.valueOf(17)); progress.tick();
        CompoundTag saved = original.serializeNBT();
        check(saved.getCompound("ForgeCaps").contains("transcend:research_progress"), "Forge serializes research capability");
        Player restored = helper.makeMockPlayer(); restored.deserializeNBT(saved);
        ResearchProgress loaded = restored.getCapability(ResearchCapabilities.PROGRESS).orElseThrow(AssertionError::new);
        check(loaded.paid().equals(BigInteger.valueOf(17)) && loaded.ticks() == 1, "reload preserves paid points and work");
        var reference = original.getCapability(ResearchCapabilities.PROGRESS);
        original.invalidateCaps(); check(!reference.isPresent(), "removed capability reference invalidates");
        Player clone = helper.makeMockPlayer();
        ResearchEvents.clone(new PlayerEvent.Clone(clone, original, true));
        ResearchProgress copied = clone.getCapability(ResearchCapabilities.PROGRESS).orElseThrow(AssertionError::new);
        check("energy_basics".equals(copied.active()) && copied.paid().equals(BigInteger.valueOf(17)), "death clone preserves paid research");
        check(!original.getCapability(ResearchCapabilities.PROGRESS).isPresent(), "old player remains invalidated");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void allEnergyPortsSharePerTickBudget(GameTestHelper helper) {
        ResearchStationBlockEntity station = station(helper, 0);
        var fe = station.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
        var tech = station.getCapability(TechCapabilities.TECH_ENERGY).orElseThrow(AssertionError::new);
        check(fe.receiveEnergy(400_000, false) == 400_000, "initial FE transfer");
        check(tech.receive(Long.MAX_VALUE, true) == 600_000, "simulation observes shared budget");
        check(tech.receive(Long.MAX_VALUE, false) == 600_000, "remaining long transfer");
        check(fe.receiveEnergy(Integer.MAX_VALUE, false) == 0, "cannot bypass rate with second port");
        helper.runAfterDelay(1, () -> {
            check(tech.receive(Long.MAX_VALUE, false) == ResearchStationBlockEntity.BASE_INPUT_RATE, "budget resets next tick");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void pluginsEnableLongTransfersAndAutomatedInputSlots(GameTestHelper helper) {
        ResearchStationBlockEntity station = station(helper, 0);
        station.plugins().insertItem(0, new ItemStack(ModItems.research_cosmic_simulator.get()), false);
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.LONG_STORAGE_QUANTUM.get());
        var storage = (LongStorageBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 1)));
        CompoundTag seed = new CompoundTag(); seed.putLong("Energy", 20_000_000_000L); storage.load(seed);
        ResearchStationBlockEntity.tick(helper.getLevel(), station);
        check(station.energyStored() == 20_000_000_000L && storage.stored() == 0, "pull exceeds FE integer ceiling without duplicating energy");
        var items = station.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        check(items.getSlots() == 6, "both material and all plugin slots exposed");
        check(items.insertItem(0, new ItemStack(ModItems.blank_research_component.get(), 16), false).isEmpty(), "automation supplies blanks");
        check(items.insertItem(1, new ItemStack(ModItems.research_assist_unit.get()), false).isEmpty(), "automation supplies assists");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void bulkConversionIsFiniteAndPersists(GameTestHelper helper) {
        ResearchStationBlockEntity station = station(helper, 500 * ResearchEnergyAccount.RF_PER_POINT);
        station.plugins().insertItem(0, new ItemStack(ModItems.research_processor.get()), false);
        station.inputs().insertItem(0, new ItemStack(ModItems.blank_research_component.get(), 3), false);
        station.inputs().insertItem(1, new ItemStack(ModItems.research_assist_unit.get(), 2), false);
        check(station.preparePoints(true) == 2, "bulk limited by materials");
        check(station.researchPoints().equals(BigInteger.valueOf(200)), "processor batches produce 100 points each");
        check(station.energyStored() == 300 * ResearchEnergyAccount.RF_PER_POINT, "RF charged exactly");
        check(station.preparePoints(true) == 0, "cannot repeat without assists");
        ResearchStationBlockEntity restored = new ResearchStationBlockEntity(station.getBlockPos(), station.getBlockState());
        restored.load(station.saveWithFullMetadata());
        check(restored.researchPoints().equals(station.researchPoints()) && restored.energyStored() == station.energyStored(), "save retains accounting");
        check(restored.inputs().getStackInSlot(0).getCount() == 1, "remaining consumables preserved");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void manufactureRequiresActualSpecializedResearch(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ASSEMBLY.get());
        var bench = (AssemblyBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)));
        var recipe = (AssemblyRecipe) helper.getLevel().getRecipeManager().byKey(Transcend.rl("shield_module_b_magic")).orElseThrow();
        bench.items.setStackInSlot(2, new ItemStack(ModItems.tech_part_capacitor_core.get(), 4));
        bench.items.setStackInSlot(3, new ItemStack(net.minecraft.world.item.Items.AMETHYST_SHARD, 8));
        bench.receiveExternalEnergy(AssemblyBlockEntity.CAPACITY);
        CompoundTag skills = new CompoundTag(); skills.putInt("KnowledgeTier", 7); skills.putInt("fabricate", AssemblyRules.MAX_XP);
        player.getPersistentData().put("TranscendAssembly", skills);
        ListTag completed = new ListTag();
        ResearchRegistry.all().stream().filter(ResearchNode::core).forEach(n -> completed.add(StringTag.valueOf(n.id())));
        CompoundTag state = new CompoundTag(); state.put("Completed", completed);
        var progress = player.getCapability(ResearchCapabilities.PROGRESS).orElseThrow(AssertionError::new);
        progress.load(state);
        check(AssemblyOperations.readiness(bench, player, recipe) == 8, "mainline alone cannot fabricate specialization");
        check(!bench.startJob(player, recipe), "server job creation enforces research");
        check(bench.items.getStackInSlot(2).getCount() == 4, "rejected manufacture consumes no materials");
        completed.add(StringTag.valueOf("shield_combat_magic")); progress.load(state);
        check(AssemblyOperations.readiness(bench, player, recipe) == 1, "completed specialization enables real manufacture");
        check(ResearchUnlocks.canManufacture(player, ShieldModuleItem.create(ShieldModule.B_MAGIC)), "unlock mapping agrees with manufacture");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void automatedResearchUsesResourcesAndGrantsKnowledge(GameTestHelper helper) {
        ResearchStationBlockEntity station = station(helper, 2_000 * ResearchEnergyAccount.RF_PER_POINT);
        station.inputs().insertItem(0, new ItemStack(ModItems.blank_research_component.get(), 16), false);
        station.inputs().insertItem(1, new ItemStack(ModItems.research_assist_unit.get(), 16), false);
        check(station.preparePoints(), "initial manual points");
        station.plugins().insertItem(0, new ItemStack(ModItems.research_processor.get()), false);
        station.plugins().insertItem(1, new ItemStack(ModItems.research_processor.get()), false);
        check(station.powerMultiplier() == 19, "multiple plugins contribute");
        station.setAutoPrepare(true);
        var player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "research-test"));
        check(station.startResearch(player, "knowledge_basics"), "manual start after preparing points");
        helper.onEachTick(() -> station.tickResearch(player));
        helper.runAfterDelay(30, () -> {
                var progress = player.getCapability(ResearchCapabilities.PROGRESS).orElseThrow(AssertionError::new);
                check(progress.completed().contains("knowledge_basics"), "plugins accelerate work to completion");
                check(AssemblyKnowledge.tier(player) == 1, "knowledge outcome is applied");
                check(station.energyStored() == 0 && station.researchPoints().signum() == 0, "exact study cost, no free points or overfunding");
                check(station.inputs().getStackInSlot(0).getCount() == 4, "automatic supply consumes materials and limits final batch");
                check(station.researcher() == null, "completed station releases researcher");
                helper.succeed();
        });
    }

    private static ResearchStationBlockEntity station(GameTestHelper helper, long energy) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.RESEARCH_STATION.get());
        var station = (ResearchStationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        CompoundTag seed = new CompoundTag(); seed.putLong("Energy", energy); station.load(seed);
        return station;
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
