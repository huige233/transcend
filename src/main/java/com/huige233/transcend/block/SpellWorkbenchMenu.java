package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlocks;
import com.huige233.transcend.init.ModMenus;
import com.huige233.transcend.items.SpellBaseItem;
import com.huige233.transcend.items.SpellCarrierItem;
import com.huige233.transcend.items.SpellEffectItem;
import com.huige233.transcend.items.SpellElementItem;
import com.huige233.transcend.items.SpellScrollItem;
import com.huige233.transcend.items.SpellUpgradeStone;
import com.huige233.transcend.items.RuneItem;
import com.huige233.transcend.items.TranscendWand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class SpellWorkbenchMenu extends AbstractContainerMenu {

    private final SpellWorkbenchBlockEntity blockEntity;
    private final ContainerLevelAccess access;

    // Client constructor
    public SpellWorkbenchMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, getBlockEntity(playerInv, extraData));
    }

    // Server constructor
    public SpellWorkbenchMenu(int containerId, Inventory playerInv, SpellWorkbenchBlockEntity blockEntity) {
        super(ModMenus.SPELL_WORKBENCH_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        IItemHandler handler = blockEntity.getItemHandler();

        // Slot 0: Carrier / Scroll / Wand slot
        addSlot(new SlotItemHandler(handler, 0, 27, 18) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof SpellCarrierItem
                        || stack.getItem() instanceof SpellScrollItem
                        || stack.getItem() instanceof TranscendWand;
            }
        });

        // Slot 1: Element / Upgrade Stone slot
        addSlot(new SlotItemHandler(handler, 1, 27, 36) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof SpellElementItem
                        || stack.getItem() instanceof SpellUpgradeStone
                        || stack.getItem() instanceof RuneItem;
            }
        });

        // Slot 2: Effect slot (SpellEffectItem only, optional)
        addSlot(new SlotItemHandler(handler, 2, 27, 54) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof SpellEffectItem;
            }
        });

        // Slot 3: Base slot (SpellBaseItem only, optional)
        addSlot(new SlotItemHandler(handler, 3, 63, 36) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof SpellBaseItem;
            }
        });

        // Slot 4: Output slot (read-only - can take but not place)
        addSlot(new SlotItemHandler(handler, 4, 121, 36) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        // Player inventory (9x3, starting at y=84)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar (starting at y=142)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    private static SpellWorkbenchBlockEntity getBlockEntity(Inventory playerInv, FriendlyByteBuf data) {
        Level level = playerInv.player.level();
        BlockEntity be = level.getBlockEntity(data.readBlockPos());
        if (be instanceof SpellWorkbenchBlockEntity spellBE) {
            return spellBE;
        }
        throw new IllegalStateException("Block entity is not SpellWorkbenchBlockEntity at " + be);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            returnStack = slotStack.copy();

            // Workbench slots: 0-4, Player inv: 5-31, Hotbar: 32-40
            if (pIndex < 5) {
                // Move from workbench to player inventory
                if (!this.moveItemStackTo(slotStack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to workbench
                if (slotStack.getItem() instanceof SpellCarrierItem) {
                    // Try carrier slot (0)
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.getItem() instanceof SpellElementItem) {
                    // Try element slot (1)
                    if (!this.moveItemStackTo(slotStack, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.getItem() instanceof SpellEffectItem) {
                    // Try effect slot (2)
                    if (!this.moveItemStackTo(slotStack, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.getItem() instanceof SpellBaseItem) {
                    // Try base slot (3)
                    if (!this.moveItemStackTo(slotStack, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (pIndex < 32) {
                    // Move from main inventory to hotbar
                    if (!this.moveItemStackTo(slotStack, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Move from hotbar to main inventory
                    if (!this.moveItemStackTo(slotStack, 5, 32, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == returnStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, slotStack);
        }

        return returnStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(this.access, pPlayer, ModBlocks.SPELL_WORKBENCH.get());
    }
}
