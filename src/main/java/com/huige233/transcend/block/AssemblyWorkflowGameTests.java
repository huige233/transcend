package com.huige233.transcend.block;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.*;
import com.huige233.transcend.tech.assembly.*;
import com.huige233.transcend.tech.research.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(Transcend.MODID)
@PrefixGameTestTemplate(false)
public final class AssemblyWorkflowGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void repeatedPartsStackAndSpendExactlyOneRecipe(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AssemblyBlockEntity bench = bench(helper);
        var recipe = recipe(helper, "tech_metal_crude");
        bench.items.setStackInSlot(2, new ItemStack(Items.IRON_INGOT, 4));
        bench.items.setStackInSlot(3, new ItemStack(Items.REDSTONE, 2));
        int before = bench.energy();
        check(AssemblyOperations.complete(bench, player, recipe), "first part completed");
        check(AssemblyOperations.complete(bench, player, recipe), "second part stacks despite recipe mastery increase");
        check(bench.items.getStackInSlot(4).getCount() == 4 && !bench.items.getStackInSlot(4).hasTag(), "raw parts have stable NBT");
        check(bench.energy() == before - recipe.energy() * 2 && bench.items.getStackInSlot(2).isEmpty(), "exact RF and material accounting");
        check(AssemblyProficiency.xp(player, "cast") == 800, "per-tier training awards practical experience");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void realMaterialChainProducesAnEntryGun(GameTestHelper helper) {
        ServerPlayer player = player(helper); AssemblyBlockEntity bench = bench(helper);
        ItemStack metal = make(helper, bench, player, "tech_metal_crude", new ItemStack(Items.IRON_INGOT, 2), new ItemStack(Items.REDSTONE));
        ItemStack plates = make(helper, bench, player, "tech_plate_crude", metal.copyWithCount(1), ItemStack.EMPTY);
        ItemStack refined = make(helper, bench, player, "tech_part_gear_refined", new ItemStack(ModItems.tech_part_gear_blank.get()), new ItemStack(ModItems.tech_dust_conductive.get()));
        refined.grow(make(helper, bench, player, "tech_part_gear_refined", new ItemStack(ModItems.tech_part_gear_blank.get()), new ItemStack(ModItems.tech_dust_conductive.get())).getCount());
        ItemStack gears = make(helper, bench, player, "tech_part_gear", refined, plates.copyWithCount(1));
        ItemStack gun = make(helper, bench, player, "particle_gun_e", gears, new ItemStack(ModItems.tech_part_circuit_printed.get(), 2));
        check(gun.is(ModItems.particle_gun.get()), "entry gun is made with T1 knowledge and real processing recipes");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void salvageIsProbabilisticAndFailureRefundsRf(GameTestHelper helper) {
        List<ItemStack> consumed = List.of(new ItemStack(TechMaterials.item("tech_metal_crude"), 8),
                new ItemStack(ModItems.tech_part_capacitor_core.get(), 2), new ItemStack(ModItems.tech_energy_cell.get(), 16),
                new ItemStack(ModItems.tech_energy_core.get(), 4), new ItemStack(ModItems.tech_energy_matrix.get(), 3));
        check(AssemblySalvage.recover(consumed, 0, RandomSource.create(1)).isEmpty(), "zero salvage setting returns nothing");
        var recovered = AssemblySalvage.recover(consumed, 1, RandomSource.create(1));
        check(recovered.stream().mapToInt(ItemStack::getCount).sum() == 11, "8 metal scraps, 2 capacitor fragments, at most one high energy shard");
        int trials = 0;
        RandomSource random = RandomSource.create(17);
        for (int i = 0; i < 1000; i++) trials += AssemblySalvage.recover(List.of(consumed.get(0).copyWithCount(1)), .15, random).size();
        check(trials > 100 && trials < 210, "single consumed parts can return scraps at approximately 15 percent");
        ServerPlayer player = player(helper); AssemblyBlockEntity bench = bench(helper);
        var failure = new AssemblyRecipe(Transcend.rl("test_failure"), "cast", Ingredient.of(TechMaterials.item("tech_metal_crude")), 2,
                Ingredient.EMPTY, 0, new ItemStack(Items.COPPER_INGOT), 1000, 1, 1, List.of());
        bench.items.setStackInSlot(2, new ItemStack(TechMaterials.item("tech_metal_crude"), 2));
        int energy = bench.energy();
        check(AssemblyOperations.complete(bench, player, failure), "failed attempt settles once");
        check(bench.energy() == energy && bench.items.getStackInSlot(2).isEmpty() && bench.items.getStackInSlot(4).isEmpty(), "failure consumes materials and refunds all RF");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void generatorsRequireSequentialPaidResearchedUpgrades(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.FIRE_GENERATOR.get());
        var generator = (FEGeneratorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)));
        check(!generator.upgrade(player, new ItemStack(TechMaterials.item("tech_metal_steel"), 64)), "cannot skip a tier");
        var progress = player.getCapability(ResearchCapabilities.PROGRESS).orElseThrow(AssertionError::new);
        CompoundTag saved = new CompoundTag(); progress.save(saved); progress.load(new CompoundTag());
        ItemStack crude = new ItemStack(TechMaterials.item("tech_metal_crude"), 8);
        check(!generator.upgrade(player, crude) && crude.getCount() == 8, "no research means no consumption"); progress.load(saved);
        check(generator.upgrade(player, crude) && crude.getCount() == 4, "upgrade pays four ingots");
        check(generator.getProduction() == 8192 && generator.getEnergy() == 0, "upgrade increases production without generating stored energy");
        var restored = (FEGeneratorBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                generator.getBlockPos(), generator.getBlockState(), generator.saveWithFullMetadata());
        check(restored != null, "registered block entity factory restores saved generator");
        check(restored.upgradeTier() == 1 && restored.getProduction() == 8192, "upgrade persists");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void researchedAutomationFinishesBatchWithClosedMenu(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        helper.getLevel().addNewPlayer(player);
        AssemblyBlockEntity bench = bench(helper);
        bench.items.setStackInSlot(2, new ItemStack(Items.IRON_INGOT, 4));
        bench.items.setStackInSlot(3, new ItemStack(Items.REDSTONE, 2));
        bench.changeBatch(1);
        check(bench.startJob(player, recipe(helper, "tech_metal_crude")), "start real batch");
        player.containerMenu = player.inventoryMenu;
        var items = bench.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        check(items.getSlots() == 4, "automation exposes only process slots");
        check(items.extractItem(0, 1, false).isEmpty(), "running input extraction rejected");
        check(items.insertItem(2, new ItemStack(Items.DIRT), false).getCount() == 1, "output insertion rejected");
        check(items.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "automation rejects unrelated materials during a job");
        helper.runAfterDelay(135, () -> {
            check(!bench.hasJob() && bench.items.getStackInSlot(4).getCount() == 4, "closed-menu real ticks finish and stack both outputs");
            check(items.extractItem(2, 64, false).getCount() == 4, "automation can extract finished goods");
            player.discard(); helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void overlappingRecipesCanBeSelectedThroughTheServerMenu(GameTestHelper helper) {
        ServerPlayer player = player(helper); AssemblyBlockEntity bench = bench(helper);
        player.setPos(bench.getBlockPos().getX() + .5, bench.getBlockPos().getY(), bench.getBlockPos().getZ() + .5);
        bench.items.setStackInSlot(2, new ItemStack(ModItems.tech_part_gear_refined.get()));
        bench.items.setStackInSlot(3, new ItemStack(Items.IRON_INGOT, 6));
        var menu = new com.huige233.transcend.menu.AssemblyMenu(2, player.getInventory(), bench);
        player.containerMenu = menu;
        check(menu.recipeChoices(3).size() == 2, "both long and standard barrel recipes really match");
        check(menu.recipe(3).orElseThrow().getId().equals(Transcend.rl("gun_module_barrel_long")), "default deterministic choice");
        int energy = bench.energy();
        check(menu.clickMenuButton(player, com.huige233.transcend.menu.AssemblyMenu.BUTTON_SELECT_FIRST + 3), "server accepts selection button");
        check(menu.recipe(3).orElseThrow().getId().equals(Transcend.rl("gun_module_barrel_std")), "selection changes actual manufacture recipe");
        check(bench.energy() == energy && bench.items.getStackInSlot(3).getCount() == 6, "selection consumes nothing");
        helper.runAfterDelay(1, () -> {
            check(menu.clickMenuButton(player, com.huige233.transcend.menu.AssemblyMenu.BUTTON_FABRICATE), "selected recipe starts");
            check(bench.saveWithFullMetadata().getCompound("Job").getString("Recipe").equals("transcend:gun_module_barrel_std"), "job pins selected recipe identity");
            helper.succeed();
        });
    }

    private static ItemStack make(GameTestHelper helper, AssemblyBlockEntity bench, ServerPlayer player, String id, ItemStack first, ItemStack second) {
        bench.items.setStackInSlot(2, first); bench.items.setStackInSlot(3, second); bench.items.setStackInSlot(4, ItemStack.EMPTY);
        check(AssemblyOperations.complete(bench, player, recipe(helper, id)), "can manufacture " + id);
        return bench.items.extractItem(4, 64, false);
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void insufficientMaterialsStillAllowStableRecipeSelection(GameTestHelper helper) {
        ServerPlayer player = player(helper); AssemblyBlockEntity bench = bench(helper);
        bench.items.setStackInSlot(2, new ItemStack(ModItems.tech_part_gear_refined.get()));
        bench.items.setStackInSlot(3, new ItemStack(Items.IRON_INGOT));
        var menu = new com.huige233.transcend.menu.AssemblyMenu(4, player.getInventory(), bench);
        check(menu.recipeChoices(3).size() == 2, "matching recipes remain visible before counts are sufficient");
        check(bench.cycleRecipe(3, 2), "can choose the standard barrel with incomplete inputs");
        var selected = menu.recipe(3).orElseThrow();
        check(selected.getId().equals(Transcend.rl("gun_module_barrel_std")), "standard barrel selected");
        check(AssemblyOperations.readiness(bench, player, selected) == 4 && !bench.startJob(player, selected), "preview does not bypass material requirements");
        int energy = bench.energy();
        bench.items.setStackInSlot(3, new ItemStack(Items.IRON_INGOT, 6));
        check(menu.recipe(3).orElseThrow().getId().equals(selected.getId()), "adding materials cannot silently switch to another recipe");
        check(bench.energy() == energy && bench.items.getStackInSlot(2).getCount() == 1, "preview and rejected job consume nothing");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void idleAssemblyChargesFromBatteryWithoutStartingAJob(GameTestHelper helper) {
        AssemblyBlockEntity bench = bench(helper); bench.consumeEnergy(bench.energy());
        ItemStack battery = new ItemStack(ModItems.portable_capacitor.get());
        var batteryEnergy = battery.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
        check(batteryEnergy.receiveEnergy(400_000, false) == 400_000, "battery primed");
        bench.items.setStackInSlot(5, battery);
        AssemblyBlockEntity.tick(helper.getLevel(), bench.getBlockPos(), bench.getBlockState(), bench);
        check(bench.energy() == 200_000 && batteryEnergy.getEnergyStored() == 200_000, "idle charging conserves energy and respects transfer rate");
        check(!bench.hasJob() && bench.items.getStackInSlot(4).isEmpty(), "automatic power does not manufacture an item");
        check(!bench.chargeFromItem(), "manual charging cannot bypass automatic charging rate in the same tick");
        check(bench.energy() == 200_000 && batteryEnergy.getEnergyStored() == 200_000, "rejected repeat leaves both accounts unchanged");
        helper.runAfterDelay(1, () -> {
            AssemblyBlockEntity.tick(helper.getLevel(), bench.getBlockPos(), bench.getBlockState(), bench);
            check(bench.energy() == 400_000 && batteryEnergy.getEnergyStored() == 0, "charging budget resets next tick");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void assemblyInventoryMigrationKeepsExistingItems(GameTestHelper helper) {
        var legacy = new net.minecraftforge.items.ItemStackHandler(5);
        legacy.setStackInSlot(0, new ItemStack(ModItems.particle_gun.get()));
        legacy.getStackInSlot(0).getOrCreateTag().putString("MigrationMarker", "keep-custom-data");
        legacy.setStackInSlot(2, new ItemStack(Items.IRON_INGOT, 7));
        legacy.setStackInSlot(3, new ItemStack(Items.REDSTONE, 3));
        legacy.setStackInSlot(4, new ItemStack(Items.DIAMOND, 2));
        CompoundTag saved = new CompoundTag();
        saved.put("Inventory", legacy.serializeNBT());
        saved.putInt("Energy", 123456);
        var restored = new AssemblyBlockEntity(BlockPos.ZERO, ModBlocks.ASSEMBLY.get().defaultBlockState());
        restored.load(saved);
        check(restored.items.getSlots() == AssemblyBlockEntity.SLOT_COUNT, "old save gains the battery slot");
        for (int slot = 0; slot < legacy.getSlots(); slot++) {
            check(ItemStack.matches(legacy.getStackInSlot(slot), restored.items.getStackInSlot(slot)), "migration preserves slot " + slot);
        }
        check(restored.items.getStackInSlot(5).isEmpty() && restored.energy() == 123456, "new slot starts empty without resetting energy");
        saved.getCompound("Inventory").putInt("Size", Integer.MAX_VALUE);
        restored.load(saved);
        check(restored.items.getSlots() == AssemblyBlockEntity.SLOT_COUNT && restored.items.getStackInSlot(2).getCount() == 7,
                "malformed inventory size cannot allocate unbounded slots or erase valid contents");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void assemblyEnergyRejectsNegativeAndRemovedTransfers(GameTestHelper helper) {
        AssemblyBlockEntity bench = bench(helper);
        var port = bench.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
        int initial = bench.energy();
        check(port.receiveEnergy(-100, false) == 0 && bench.energy() == initial, "negative receive cannot drain stored energy");
        check(port.receiveEnergy(Integer.MAX_VALUE, true) == AssemblyBlockEntity.INPUT_RATE && bench.energy() == initial,
                "simulation reports input limit without changing energy");
        ItemStack battery = new ItemStack(ModItems.portable_capacitor.get());
        var batteryEnergy = battery.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
        batteryEnergy.receiveEnergy(400_000, false);
        bench.items.setStackInSlot(5, battery);
        bench.setRemoved();
        check(port.receiveEnergy(100, false) == 0 && bench.energy() == initial, "cached port cannot charge a removed machine");
        check(!bench.chargeFromItem() && batteryEnergy.getEnergyStored() == 400_000, "removed machine cannot consume its battery");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void shiftBatchButtonsAreHandledByTheServer(GameTestHelper helper) {
        ServerPlayer player = player(helper); AssemblyBlockEntity bench = bench(helper);
        player.setPos(bench.getBlockPos().getX() + .5, bench.getBlockPos().getY(), bench.getBlockPos().getZ() + .5);
        var menu = new com.huige233.transcend.menu.AssemblyMenu(5, player.getInventory(), bench); player.containerMenu = menu;
        check(menu.clickMenuButton(player, com.huige233.transcend.menu.AssemblyMenu.BUTTON_BATCH_MORE_EIGHT), "server accepts fast batch adjustment");
        check(bench.batchSize() == 9 && !bench.hasJob(), "Shift batch changes count by eight without starting manufacture");
        helper.runAfterDelay(1, () -> {
            check(menu.clickMenuButton(player, com.huige233.transcend.menu.AssemblyMenu.BUTTON_BATCH_LESS_EIGHT), "server accepts fast batch reduction");
            check(bench.batchSize() == 1, "batch returns to one"); helper.succeed();
        });
    }
    private static ServerPlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "workflow-test"));
        ListTag completed = new ListTag(); ResearchRegistry.all().forEach(node -> completed.add(StringTag.valueOf(node.id())));
        CompoundTag progress = new CompoundTag(); progress.put("Completed", completed);
        player.getCapability(ResearchCapabilities.PROGRESS).orElseThrow(AssertionError::new).load(progress);
        CompoundTag skills = new CompoundTag(); skills.putInt("KnowledgeTier", 1); player.getPersistentData().put("TranscendAssembly", skills);
        return player;
    }
    private static AssemblyBlockEntity bench(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ASSEMBLY.get());
        var bench = (AssemblyBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)));
        bench.receiveExternalEnergy(1_000_000); return bench;
    }
    private static AssemblyRecipe recipe(GameTestHelper helper, String id) {
        return (AssemblyRecipe) helper.getLevel().getRecipeManager().byKey(Transcend.rl(id)).orElseThrow();
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
