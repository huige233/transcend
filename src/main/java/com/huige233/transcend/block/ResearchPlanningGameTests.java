package com.huige233.transcend.block;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.menu.ResearchStationMenu;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(Transcend.MODID)
@PrefixGameTestTemplate(false)
public final class ResearchPlanningGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void researchStatusExplainsStallsAndRecognizesAutomaticTailBatch(GameTestHelper helper) {
        var station = station(helper);
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "research-status"));
        var menu = new ResearchStationMenu(0, player.getInventory(), station);
        CompoundTag progress = new CompoundTag();
        progress.putString("Active", "energy_basics");
        progress.putString("Paid", "999");
        snapshot(menu, progress, "0", 0, player.getUUID(), false);
        status(menu, "waiting_points");
        snapshot(menu, progress, "0", 0, player.getUUID(), true);
        status(menu, "waiting_materials");
        station.inputs().setStackInSlot(0, new ItemStack(ModItems.blank_research_component.get()));
        station.inputs().setStackInSlot(1, new ItemStack(ModItems.research_assist_unit.get()));
        status(menu, "waiting_energy");
        // Saved fractional RF is not usable for the whole-point conversion; exactly 100,000 is needed.
        snapshot(menu, progress, "0", 99_999, player.getUUID(), true);
        status(menu, "waiting_energy");
        snapshot(menu, progress, "0", 100_000, player.getUUID(), true);
        status(menu, "in_progress");
        check(!menu.canPrepare(), "manual preparation still requires a full ten-point batch");
        snapshot(menu, progress, "1", 0, player.getUUID(), false);
        status(menu, "in_progress");
        progress.putString("Paid", "1000");
        snapshot(menu, progress, "0", 0, player.getUUID(), false);
        status(menu, "in_progress");
        snapshot(menu, progress, "0", 0, null, false);
        status(menu, "paused");
        snapshot(menu, progress, "0", 0, UUID.randomUUID(), false);
        status(menu, "action.busy");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void researchSnapshotRefreshesCachedUnlocksAndBalances(GameTestHelper helper) {
        var station = station(helper);
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "research-cache"));
        var menu = new ResearchStationMenu(0, player.getInventory(), station);
        CompoundTag progress = new CompoundTag();
        snapshot(menu, progress, "1", 0, null, false);
        check(menu.canStart("energy_basics") && !menu.isAvailable("energy_network"), "initial availability reflects era gate");
        ListTag done = new ListTag();
        for (String id : new String[] {"energy_basics", "materials_basics", "automation_basics", "combat_basics", "knowledge_basics"}) done.add(StringTag.valueOf(id));
        progress.put("Completed", done);
        snapshot(menu, progress, "1", 0, null, false);
        check(menu.canStart("energy_network") && !menu.isAvailable("energy_basics"), "new snapshot refreshes completion and unlock caches");
        snapshot(menu, progress, "invalid", 0, null, false);
        check(menu.isAvailable("energy_network") && !menu.canStart("energy_network"), "invalid points cannot enable start");
        progress.putString("Active", "energy_network");
        snapshot(menu, progress, "0", 0, null, false);
        check(menu.canStart("energy_network") && !menu.isAvailable("automation"), "paused topic can resume; other starts wait");
        snapshot(menu, new CompoundTag(), "0", 0, null, false);
        check(menu.completedResearch().isEmpty() && !menu.isAvailable("energy_network"), "reset snapshot cannot leave stale unlocks");
        helper.succeed();
    }

    private static ResearchStationBlockEntity station(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.RESEARCH_STATION.get());
        return (ResearchStationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    private static void snapshot(ResearchStationMenu menu, CompoundTag progress, String points, long energy, UUID researcher, boolean auto) {
        menu.setClientState(new ResearchStationMenu.CompoundState(progress.copy(), energy, ResearchStationBlockEntity.BASE_INPUT_RATE,
                points, 1, researcher, menu.station().getBlockPos(), auto));
    }

    private static void status(ResearchStationMenu menu, String expected) {
        var contents = (TranslatableContents) menu.status("energy_basics").getContents();
        check(contents.getKey().equals("gui.transcend.research." + expected), "expected " + expected + ", got " + contents.getKey());
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
