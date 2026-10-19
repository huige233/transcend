package com.huige233.transcend.tech.research;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.block.AssemblyBlockEntity;
import com.huige233.transcend.tech.assembly.AssemblyOperations;
import com.huige233.transcend.tech.assembly.AssemblyRecipe;
import com.huige233.transcend.tech.assembly.AssemblyRules;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.*;

@GameTestHolder(Transcend.MODID)
@PrefixGameTestTemplate(false)
public final class ResearchManufacturingGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void workbenchRequiresSpecializationBeforeProducingOrConsuming(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        CraftingMenu menu = workbench(helper, player);
        complete(player, true);
        fillStorageRecipe(menu, 2);
        check(menu.getSlot(0).getItem().isEmpty(), "mainline research cannot replace storage specialization");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        check(menu.getCarried().isEmpty() && player.getInventory().countItem(ModItems.long_storage_basic.get()) == 0, "no locked product granted");
        check(menu.getSlot(1).getItem().getCount() == 2, "locked attempt consumes nothing");
        complete(player, true, "energy_capacitor_design");
        menu.slotsChanged(menu.getSlot(1).container);
        check(menu.getSlot(0).getItem().is(ModItems.long_storage_basic.get()), "specialization enables result");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        check(menu.getCarried().is(ModItems.long_storage_basic.get()), "normal pickup works after research");
        check(menu.getSlot(1).getItem().getCount() == 1, "successful crafting consumes exactly one recipe");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void inventoryCraftingAndBulkCraftingEnforceTheSameResearch(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        InventoryMenu menu = player.inventoryMenu; player.containerMenu = menu;
        menu.getSlot(1).set(new ItemStack(ModItems.tech_part_gear_refined.get(), 3));
        menu.getSlot(2).set(new ItemStack(com.huige233.transcend.tech.assembly.TechMaterials.item("tech_plate_crude"), 3));
        menu.slotsChanged(menu.getSlot(1).container);
        check(menu.getSlot(0).getItem().isEmpty(), "2x2 result gated on server");
        check(menu.quickMoveStack(player, 0).isEmpty(), "locked direct Shift transfer rejected");
        check(menu.getSlot(1).getItem().getCount() == 3, "2x2 rejection preserves materials");
        complete(player, true);
        menu.slotsChanged(menu.getSlot(1).container);
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        check(player.getInventory().countItem(ModItems.tech_part_gear.get()) == 3, "bulk crafting grants exactly three unlocked items");
        check(menu.getSlot(1).getItem().isEmpty(), "bulk input consumed exactly once per craft");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void staleResultsCannotEscapeThroughPickupHotbarDropOrShift(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (AbstractContainerMenu menu : List.of(workbench(helper, player), player.inventoryMenu)) {
            player.containerMenu = menu;
            for (ClickType click : List.of(ClickType.PICKUP, ClickType.SWAP, ClickType.THROW, ClickType.QUICK_MOVE)) {
                menu.getSlot(0).set(new ItemStack(ModItems.long_storage_basic.get()));
                menu.clicked(0, 0, click, player);
                check(menu.getSlot(0).getItem().is(ModItems.long_storage_basic.get()), "stale result preserved and denied: " + click);
                check(menu.getCarried().isEmpty() && player.getInventory().countItem(ModItems.long_storage_basic.get()) == 0, "no stale result escaped: " + click);
            }
            check(menu.quickMoveStack(player, 0).isEmpty(), "direct Shift call also denied");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void recipeBookPlacementCannotBypassResearch(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        CraftingMenu menu = workbench(helper, player);
        var recipe = helper.getLevel().getRecipeManager().byKey(Transcend.rl("long_storage_basic")).orElseThrow();
        player.awardRecipes(List.of(recipe));
        player.getInventory().add(new ItemStack(Items.IRON_INGOT, 4));
        player.getInventory().add(new ItemStack(Items.REDSTONE_BLOCK));
        player.getInventory().add(new ItemStack(ModItems.tech_energy_cell.get(), 4));
        menu.handlePlacement(false, recipe, player);
        menu.slotsChanged(menu.getSlot(1).container);
        check(menu.getSlot(1).getItem().is(Items.IRON_INGOT), "recipe book really placed inputs");
        check(menu.getSlot(0).getItem().isEmpty(), "recipe book cannot create a locked output");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        check(menu.getSlot(1).getItem().getCount() == 1, "blocked recipe book craft consumes no material");
        complete(player, true, "energy_capacitor_design");
        menu.slotsChanged(menu.getSlot(1).container);
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        check(player.getInventory().countItem(ModItems.long_storage_basic.get()) == 1, "placed recipe becomes craftable after research");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void bootstrapCraftingRemainsOpenAndMaterialAssemblyRequiresResearch(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        check(ResearchUnlocks.canManufacture(player, new ItemStack(ModItems.research_station.get())), "station has no research prerequisite");
        check(ResearchUnlocks.canManufacture(player, new ItemStack(ModItems.tech_part_circuit_printed.get())), "bootstrap printed circuits remain open");
        check(ResearchUnlocks.canManufacture(player, new ItemStack(ModItems.blank_research_component.get())), "bootstrap research consumables remain open");
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ASSEMBLY.get());
        var bench = (AssemblyBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 1)));
        var recipe = (AssemblyRecipe) helper.getLevel().getRecipeManager().byKey(Transcend.rl("tech_part_capacitor")).orElseThrow();
        CompoundTag skills = new CompoundTag(); skills.putInt("KnowledgeTier", 7); skills.putInt("cast", AssemblyRules.MAX_XP);
        player.getPersistentData().put("TranscendAssembly", skills);
        bench.items.setStackInSlot(2, new ItemStack(ModItems.tech_part_capacitor_core.get(), 2));
        bench.items.setStackInSlot(3, new ItemStack(com.huige233.transcend.tech.assembly.TechMaterials.item("tech_plate_alloy"), 2));
        bench.receiveExternalEnergy(AssemblyBlockEntity.CAPACITY);
        complete(player, true);
        check(AssemblyOperations.readiness(bench, player, recipe) == 8 && !bench.startJob(player, recipe), "advanced materials require specialization even with max knowledge");
        check(bench.items.getStackInSlot(2).getCount() == 2, "no deduction on research rejection");
        complete(player, true, "precision_components_materials");
        check(AssemblyOperations.readiness(bench, player, recipe) == 1, "same unified rule unlocks assembly");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "research-craft"));
    }
    private static CraftingMenu workbench(GameTestHelper helper, ServerPlayer player) {
        BlockPos pos = new BlockPos(1, 1, 1); helper.setBlock(pos, Blocks.CRAFTING_TABLE);
        CraftingMenu menu = new CraftingMenu(1, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(pos)));
        player.containerMenu = menu; return menu;
    }
    private static void fillStorageRecipe(CraftingMenu menu, int count) {
        for (int i = 1; i <= 9; i++) menu.getSlot(i).set(new ItemStack(i == 5 ? Items.REDSTONE_BLOCK
                : i % 2 == 1 ? Items.IRON_INGOT : ModItems.tech_energy_cell.get(), count));
        menu.slotsChanged(menu.getSlot(1).container);
    }
    private static void complete(ServerPlayer player, boolean mainline, String... specializations) {
        ListTag list = new ListTag();
        if (mainline) ResearchRegistry.all().stream().filter(ResearchNode::core).forEach(node -> list.add(StringTag.valueOf(node.id())));
        for (String id : specializations) list.add(StringTag.valueOf(id));
        CompoundTag state = new CompoundTag(); state.put("Completed", list);
        player.getCapability(ResearchCapabilities.PROGRESS).orElseThrow(AssertionError::new).load(state);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
