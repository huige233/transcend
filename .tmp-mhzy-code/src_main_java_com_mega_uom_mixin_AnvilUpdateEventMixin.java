package com.mega.uom.mixin;

import com.mega.uom.util.itf.event.AnvilUpdateEventInterface;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraftforge.event.AnvilUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AnvilUpdateEvent.class, remap = false)
public class AnvilUpdateEventMixin implements AnvilUpdateEventInterface {
    @Unique
    private AnvilMenu uom$menu;

    @Override
    public AnvilMenu uom$getMenu() {
        return uom$menu;
    }

    @Override
    public void uom$setMenu(AnvilMenu menu) {
        this.uom$menu = menu;
    }
}
