package com.huige233.transcend.mixin;

import com.huige233.transcend.tech.research.ResearchUnlocks;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 2×2 与 3×3 共用的服务端结果生成入口；在同步与取物之前过滤，不消耗材料。 */
@Mixin(CraftingMenu.class)
public abstract class ResearchCraftingGridMixin {
    @ModifyExpressionValue(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack transcend$researchOutput(ItemStack output, AbstractContainerMenu menu, Level level,
                                                      Player player, CraftingContainer grid, ResultContainer result) {
        return ResearchUnlocks.canManufacture(player, output) ? output : ItemStack.EMPTY;
    }
}
