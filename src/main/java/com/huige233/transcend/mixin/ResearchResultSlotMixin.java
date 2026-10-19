package com.huige233.transcend.mixin;

import com.huige233.transcend.tech.research.ResearchUnlocks;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;

/** 防止旧结果或其他菜单共用 ResultSlot 时，经普通点击、数字键或丢弃取得受限产物。 */
@Mixin(ResultSlot.class)
public abstract class ResearchResultSlotMixin extends Slot {
    protected ResearchResultSlotMixin(Container container, int index, int x, int y) { super(container, index, x, y); }

    @Override
    public boolean mayPickup(Player player) {
        return super.mayPickup(player) && (player.level().isClientSide || ResearchUnlocks.canManufacture(player, getItem()));
    }
}
