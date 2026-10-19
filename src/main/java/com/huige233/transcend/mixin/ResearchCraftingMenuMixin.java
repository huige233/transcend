package com.huige233.transcend.mixin;

import com.huige233.transcend.tech.research.ResearchUnlocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shift 取物直接移动堆栈，不经过 Slot.mayPickup，必须在移动之前单独检查。 */
@Mixin({CraftingMenu.class, InventoryMenu.class})
public abstract class ResearchCraftingMenuMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void transcend$researchQuickMove(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!player.level().isClientSide && index == 0
                && !ResearchUnlocks.canManufacture(player, ((AbstractContainerMenu) (Object) this).getSlot(0).getItem()))
            cir.setReturnValue(ItemStack.EMPTY);
    }
}
