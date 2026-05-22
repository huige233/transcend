package com.huige233.transcend.block;

import com.huige233.transcend.init.ModBlockEntities;
import com.huige233.transcend.items.*;
import com.huige233.transcend.spell.SpellCarrier;
import com.huige233.transcend.spell.SpellEffect;
import com.huige233.transcend.spell.SpellElement;
import com.huige233.transcend.spell.WandRune;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpellWorkbenchBlockEntity extends BlockEntity implements MenuProvider {

    public static final int MAX_UPGRADE_LEVEL = 10;

    private boolean crafting = false;

    private final ItemStackHandler itemHandler = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot != 4 && !crafting) {
                updateOutput();
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.getItem() instanceof SpellCarrierItem
                        || stack.getItem() instanceof SpellScrollItem
                        || stack.getItem() instanceof TranscendWand;
                case 1 -> stack.getItem() instanceof SpellElementItem
                        || stack.getItem() instanceof SpellUpgradeStone
                        || stack.getItem() instanceof RuneItem;
                case 2 -> stack.getItem() instanceof SpellEffectItem;
                case 3 -> stack.getItem() instanceof SpellBaseItem;
                case 4 -> false;
                default -> false;
            };
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 4 && !simulate && !getStackInSlot(4).isEmpty()) {
                ItemStack result = super.extractItem(slot, amount, false);
                consumeInputs();
                return result;
            }
            return super.extractItem(slot, amount, simulate);
        }
    };

    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

    public SpellWorkbenchBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SPELL_WORKBENCH_BE.get(), pPos, pBlockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.transcend.spell_workbench");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new SpellWorkbenchMenu(pContainerId, pPlayerInventory, this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    private void updateOutput() {
        ItemStack slot0 = itemHandler.getStackInSlot(0);
        ItemStack slot1 = itemHandler.getStackInSlot(1);

        if (slot0.getItem() instanceof SpellScrollItem && slot1.getItem() instanceof SpellUpgradeStone) {
            upgradeScroll(slot0);
            return;
        }

        if (slot0.getItem() instanceof TranscendWand && slot1.getItem() instanceof SpellUpgradeStone) {
            upgradeWand(slot0);
            return;
        }

        if (slot0.getItem() instanceof TranscendWand && slot1.getItem() instanceof RuneItem runeItem) {
            inscribeRune(slot0, runeItem.getRune());
            return;
        }

        if (slot0.getItem() instanceof SpellCarrierItem) {
            craftSpell();
            return;
        }

        itemHandler.setStackInSlot(4, ItemStack.EMPTY);
    }

    private void craftSpell() {
        ItemStack carrierStack = itemHandler.getStackInSlot(0);
        if (carrierStack.isEmpty() || !(carrierStack.getItem() instanceof SpellCarrierItem carrierItem)) {
            itemHandler.setStackInSlot(4, ItemStack.EMPTY);
            return;
        }

        ItemStack elementStack = itemHandler.getStackInSlot(1);
        if (elementStack.isEmpty() || !(elementStack.getItem() instanceof SpellElementItem elementItem)) {
            itemHandler.setStackInSlot(4, ItemStack.EMPTY);
            return;
        }

        ItemStack baseStack = itemHandler.getStackInSlot(3);
        if (baseStack.isEmpty() || !(baseStack.getItem() instanceof SpellBaseItem base)) {
            itemHandler.setStackInSlot(4, ItemStack.EMPTY);
            return;
        }

        SpellCarrier carrier = carrierItem.getCarrier();
        SpellElement element = elementItem.getElement();

        SpellEffect effect = null;
        ItemStack effectStack = itemHandler.getStackInSlot(2);
        if (!effectStack.isEmpty() && effectStack.getItem() instanceof SpellEffectItem effectItem) {
            effect = effectItem.getEffect();
        }

        float basePower = base.getPowerMultiplier();
        float baseCooldown = base.getCooldownMultiplier();

        ItemStack result = SpellScrollItem.createScroll(carrier, element, effect, basePower, baseCooldown);
        itemHandler.setStackInSlot(4, result);
    }

    private void upgradeScroll(ItemStack scrollStack) {
        int currentLevel = scrollStack.getOrCreateTag().getInt("upgrade_level");
        if (currentLevel >= MAX_UPGRADE_LEVEL) {
            itemHandler.setStackInSlot(4, ItemStack.EMPTY);
            return;
        }

        ItemStack upgraded = scrollStack.copy();
        CompoundTag tag = upgraded.getOrCreateTag();
        int newLevel = currentLevel + 1;
        tag.putInt("upgrade_level", newLevel);

        float oldPower = tag.getFloat("base_power");
        tag.putFloat("base_power", oldPower + 0.15F);

        float oldCooldown = tag.getFloat("base_cooldown");
        tag.putFloat("base_cooldown", Math.max(0.3F, oldCooldown - 0.05F));

        itemHandler.setStackInSlot(4, upgraded);
    }

    private void upgradeWand(ItemStack wandStack) {
        int currentLevel = wandStack.getOrCreateTag().getInt("wand_upgrade_level");
        if (currentLevel >= MAX_UPGRADE_LEVEL) {
            itemHandler.setStackInSlot(4, ItemStack.EMPTY);
            return;
        }

        ItemStack upgraded = wandStack.copy();
        CompoundTag tag = upgraded.getOrCreateTag();
        int newLevel = currentLevel + 1;
        tag.putInt("wand_upgrade_level", newLevel);

        itemHandler.setStackInSlot(4, upgraded);
    }

    private void inscribeRune(ItemStack wandStack, WandRune rune) {
        CompoundTag tag = wandStack.getOrCreateTag();
        String existing = tag.getString("wand_rune");
        if (rune.id.equals(existing)) {
            itemHandler.setStackInSlot(4, ItemStack.EMPTY);
            return;
        }

        ItemStack inscribed = wandStack.copy();
        inscribed.getOrCreateTag().putString("wand_rune", rune.id);
        itemHandler.setStackInSlot(4, inscribed);
    }

    private void consumeInputs() {
        crafting = true;
        ItemStack slot0 = itemHandler.getStackInSlot(0);

        if (slot0.getItem() instanceof SpellScrollItem || slot0.getItem() instanceof TranscendWand) {
            itemHandler.setStackInSlot(0, ItemStack.EMPTY);
            ItemStack stone = itemHandler.getStackInSlot(1);
            if (!stone.isEmpty()) stone.shrink(1);
        } else {
            for (int i = 0; i < 4; i++) {
                ItemStack s = itemHandler.getStackInSlot(i);
                if (!s.isEmpty()) {
                    s.shrink(1);
                }
            }
        }
        updateOutput();
        crafting = false;
    }
}
