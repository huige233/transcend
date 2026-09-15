package com.huige233.transcend.menu;

import com.huige233.transcend.block.AssemblyBlockEntity;
import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModMenus;
import com.huige233.transcend.items.tech.GunConfig;
import com.huige233.transcend.items.tech.GunModule;
import com.huige233.transcend.items.tech.GunModuleItem;
import com.huige233.transcend.items.tech.ParticleGun;
import com.huige233.transcend.items.tech.PhaseShield;
import com.huige233.transcend.items.tech.ShieldModule;
import com.huige233.transcend.items.tech.ShieldModuleItem;
import com.huige233.transcend.items.tech.PhaseShieldModules;
import com.huige233.transcend.tech.assembly.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Comparator;
import java.util.Optional;


/** 管理装配台槽位和状态同步，并在服务端校验制造、模块拆装、充能与批量控制操作。 */
public class AssemblyMenu extends AbstractContainerMenu {
    public static final int BUTTON_PRESS = 0;
    public static final int BUTTON_GRIND = 1;
    public static final int BUTTON_CAST = 2;
    public static final int BUTTON_FABRICATE = 3;
    public static final int BUTTON_RECYCLE = 4;
    public static final int BUTTON_INSTALL = 5;
    public static final int BUTTON_REMOVE_AMMO = 6;
    public static final int BUTTON_REMOVE_BARREL = 7;
    public static final int BUTTON_REMOVE_MUZZLE = 8;
    public static final int BUTTON_CHARGE = 9;
    public static final int BUTTON_BATCH_LESS = 10;
    public static final int BUTTON_BATCH_MORE = 11;
    public static final int BUTTON_CANCEL = 12;
    private static final int DATA_COUNT = 18;
    private static final String INSTALLED = "AssemblyInstalledModules";
    private final Player owner;
    private final AssemblyBlockEntity bench;
    private final ItemStackHandler items;
    private final ContainerData data;

    public AssemblyMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, null, new ItemStackHandler(6), new SimpleContainerData(DATA_COUNT));
        buffer.readBlockPos();
    }

    public AssemblyMenu(int id, Inventory inventory, AssemblyBlockEntity bench) {
        this(id, inventory, bench, bench.items, new ContainerData() {
            @Override public int get(int index) {
                if (index == 12) return AssemblyKnowledge.tier(inventory.player);
                if (index == 13) return bench.batchSize();
                if (index == 14) return bench.remaining();
                if (index == 15) return bench.progress();
                if (index == 16) return bench.duration();
                if (index == 17) return bench.status();
                int value = index < 2 ? bench.energy()
                        : AssemblyProficiency.xp(inventory.player, AssemblyRecipe.PROCESSES.get((index - 2) / 2));
                return (index % 2 == 0 ? value : value >>> 16) & 0xffff;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        });
    }

    private AssemblyMenu(int id, Inventory inventory, AssemblyBlockEntity bench,
                         ItemStackHandler items, ContainerData data) {
        super(ModMenus.ASSEMBLY.get(), id);
        this.owner = inventory.player;
        this.bench = bench;
        this.items = items;
        this.data = data;
        addDataSlots(data);
        addSlot(new SlotItemHandler(items, 0, 44, 34) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ParticleGun || stack.getItem() instanceof PhaseShield; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new SlotItemHandler(items, 1, 76, 34) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof GunModuleItem || stack.getItem() instanceof ShieldModuleItem
                    || stack.getItem() instanceof com.huige233.transcend.items.tech.SiriusModuleItem; }
        });
        addSlot(new SlotItemHandler(items, 2, 121, 34) {
            @Override public boolean mayPickup(Player player) { return !inputsLocked(); }
        });
        addSlot(new SlotItemHandler(items, 3, 145, 34) {
            @Override public boolean mayPickup(Player player) { return !inputsLocked(); }
        });
        addSlot(new SlotItemHandler(items, 4, 188, 34) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new SlotItemHandler(items, 5, 224, 34) {
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 61 + col * 18, 194 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 61 + col * 18, 252));
    }

    private boolean inputsLocked() { return bench == null ? remaining() > 0 : bench.hasJob(); }
    public int knowledgeTier() { return data.get(12); }
    public int batchSize() { return data.get(13); }
    public int remaining() { return data.get(14); }
    public int progress() { return data.get(15); }
    public int duration() { return data.get(16); }
    public int status() { return data.get(17); }
    public boolean isOperating(AssemblyBlockEntity expected, Player player) {
        return bench == expected && player == owner && stillValid(player);
    }

    public int energy() { return data.get(0) & 0xffff | (data.get(1) & 0xffff) << 16; }
    public int xp(int process) {
        int index = 2 + process * 2;
        return data.get(index) & 0xffff | (data.get(index + 1) & 0xffff) << 16;
    }

    public Optional<AssemblyRecipe> recipe(int process) {
        SimpleContainer input = new SimpleContainer(items.getStackInSlot(2), items.getStackInSlot(3));
        return owner.level().getRecipeManager().getAllRecipesFor(AssemblyRecipeRegistration.TYPE.get()).stream()
                .filter(recipe -> recipe.process().equals(AssemblyRecipe.PROCESSES.get(process)))
                .filter(recipe -> recipe.matches(input, owner.level()))
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString())).findFirst();
    }

    @Override public boolean stillValid(Player player) {
        return bench == null ? owner.level().isClientSide : player == owner && !bench.isRemoved()
                && player.level() == bench.getLevel()
                && player.level().getBlockEntity(bench.getBlockPos()) == bench
                && stillValid(ContainerLevelAccess.create(player.level(), bench.getBlockPos()), player, ModBlocks.ASSEMBLY.get());
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (bench == null || player.level().isClientSide || player != owner || player.containerMenu != this
                || !stillValid(player) || button < 0 || button > BUTTON_CANCEL || !bench.claimOperation()) return false;
        boolean result;
        if (button == BUTTON_CANCEL) result = bench.cancelJob(player);
        else if (button == BUTTON_BATCH_LESS) result = bench.changeBatch(-1);
        else if (button == BUTTON_BATCH_MORE) result = bench.changeBatch(1);
        else if (button == BUTTON_CHARGE) result = bench.chargeFromItem();
        else if (bench.hasJob()) result = false;
        else if (button < 5) result = manufacture(player, button);
        else if (button == BUTTON_INSTALL) result = install();
        else if (button == BUTTON_REMOVE_AMMO && items.getStackInSlot(0).getItem() instanceof PhaseShield)
            result = uninstallShield();
        else result = uninstall(GunModule.Slot.values()[button - 6]);
        if (!result) player.displayClientMessage(Component.translatable("assembly.transcend.unavailable"), true);
        broadcastChanges();
        return result;
    }

    private boolean canOutput(ItemStack stack) {
        ItemStack output = items.getStackInSlot(4);
        return stack.isEmpty() || (output.isEmpty() ? stack.getCount() <= stack.getMaxStackSize()
                : ItemStack.isSameItemSameTags(output, stack)
                && output.getCount() + stack.getCount() <= output.getMaxStackSize());
    }

    private void output(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack existing = items.getStackInSlot(4);
        ItemStack result = stack.copy();
        if (!existing.isEmpty()) result.grow(existing.getCount());
        items.setStackInSlot(4, result);
    }

    private boolean manufacture(Player player, int process) {
        AssemblyRecipe recipe = recipe(process).orElse(null);
        if (recipe == null) return false;
        if (!AssemblyKnowledge.canManufacture(AssemblyKnowledge.tier(player),
                AssemblyRules.level(AssemblyProficiency.xp(player, recipe.process())), recipe.tier())) {
            player.displayClientMessage(Component.translatable("assembly.transcend.requirements",
                    recipe.tier(), AssemblyKnowledge.requiredProficiency(recipe.tier())), false);
            return false;
        }
        return bench.startJob(player, recipe);
    }

    private GunModule current(ItemStack gun, GunModule.Slot slot) {
        return switch (slot) {
            case AMMO -> GunConfig.getAmmo(gun);
            case BARREL -> GunConfig.getBarrel(gun);
            case MUZZLE -> GunConfig.getMuzzle(gun);
        };
    }

    private ItemStack installed(ItemStack gun, GunModule.Slot slot) {
        CompoundTag root = gun.getTag();
        if (root != null && root.getCompound(INSTALLED).contains(slot.name())) {
            return ItemStack.of(root.getCompound(INSTALLED).getCompound(slot.name()));
        }
        GunModule module = current(gun, slot);
        return module == GunModule.defaultFor(slot) ? ItemStack.EMPTY : GunModuleItem.create(module);
    }

    private boolean install() {
        ItemStack host = items.getStackInSlot(0);
        ItemStack supplied = items.getStackInSlot(1);
        if (host.getItem() instanceof PhaseShield && supplied.getItem() instanceof ShieldModuleItem) {
            ShieldModule module = ShieldModuleItem.getModule(supplied);
            if (module == null) return false;
            CompoundTag tag = host.getOrCreateTag();
            String oldId = tag.getString("shield_module");
            if (module.id().equals(oldId)) return false;
            ItemStack old = oldId.isEmpty() ? ItemStack.EMPTY : ShieldModuleItem.create(ShieldModule.byId(oldId));
            if (!oldId.isEmpty() && old.isEmpty()) return false;
            if (!canOutput(old)) return false;
            ItemStack updated = host.copy();
            CompoundTag hostTag = updated.getOrCreateTag();
            PhaseShieldModules.install(hostTag, module, supplied.save(new CompoundTag()));
            items.extractItem(1, 1, false);
            items.setStackInSlot(0, updated);
            output(old);
            return true;
        }
        if (!(host.getItem() instanceof ParticleGun) || !(supplied.getItem() instanceof GunModuleItem)) return false;
        ItemStack gun = host;
        GunModule module = supplied.getItem() instanceof com.huige233.transcend.items.tech.SiriusModuleItem
                ? GunModule.MUZZLE_SIRIUS : GunModuleItem.getModule(supplied);
        if (module == null || module == current(gun, module.slot)) return false;
        ItemStack old = installed(gun, module.slot);
        if (!canOutput(old)) return false;
        ItemStack updated = gun.copy();
        CompoundTag modules = updated.getOrCreateTag().getCompound(INSTALLED);
        modules.put(module.slot.name(), supplied.copyWithCount(1).save(new CompoundTag()));
        updated.getOrCreateTag().put(INSTALLED, modules);
        GunConfig.set(updated, module.slot, module);
        items.extractItem(1, 1, false);
        items.setStackInSlot(0, updated);
        output(old);
        return true;
    }

    private boolean uninstallShield() {
        ItemStack shield = items.getStackInSlot(0);
        CompoundTag tag = shield.getTag();
        String id = tag == null ? "" : tag.getString(PhaseShieldModules.MODULE);
        ShieldModule module = ShieldModule.byId(id);
        if (!(shield.getItem() instanceof PhaseShield) || module == null) return false;
        ItemStack old = ShieldModuleItem.create(module);
        if (!canOutput(old)) return false;
        ItemStack updated = shield.copy();
        updated.getOrCreateTag().remove(PhaseShieldModules.MODULE);
        updated.getOrCreateTag().remove(PhaseShieldModules.INSTALLED);
        items.setStackInSlot(0, updated);
        output(old);
        return true;
    }

    private boolean uninstall(GunModule.Slot slot) {
        ItemStack gun = items.getStackInSlot(0);
        if (!(gun.getItem() instanceof ParticleGun)) return false;
        ItemStack old = installed(gun, slot);
        if (old.isEmpty() || !canOutput(old)) return false;
        ItemStack updated = gun.copy();
        GunConfig.set(updated, slot, null);
        updated.getOrCreateTag().getCompound(INSTALLED).remove(slot.name());
        items.setStackInSlot(0, updated);
        output(old);
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 6) {
            if (!moveItemStackTo(stack, 6, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof ParticleGun) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof GunModuleItem || stack.getItem() instanceof ShieldModuleItem) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).isPresent()) {
            if (!moveItemStackTo(stack, 5, 6, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 2, 4, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
