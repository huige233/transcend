package com.mega.uom.event.item;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import org.jetbrains.annotations.NotNull;

@Cancelable
public class ItemCraftingEvent extends PlayerEvent {
    @NotNull
    private ItemStack crafting;
    private Container craftMatrix;

    public ItemCraftingEvent(Player player, @NotNull ItemStack crafting, Container craftMatrix) {
        super(player);
        this.crafting = crafting;
        this.craftMatrix = craftMatrix;
    }

    @NotNull
    public ItemStack getCrafting() {
        return this.crafting;
    }

    public void setCrafting(ItemStack crafting) {
        this.crafting = crafting;
    }

    public Container getInventory() {
        return this.craftMatrix;
    }

    public void setCraftMatrix(Container craftMatrix) {
        this.craftMatrix = craftMatrix;
    }

    public void removeCrafting() {
        ItemStack crafting = this.crafting;
        crafting.setCount(0);
    }

    @Cancelable
    public static class Quick extends ItemCraftingEvent {

        public Quick(Player player, @NotNull ItemStack crafting, Container craftMatrix) {
            super(player, crafting, craftMatrix);
        }
    }

    @Cancelable
    public static class Take extends ItemCraftingEvent {

        public Take(Player player, @NotNull ItemStack crafting, Container craftMatrix) {
            super(player, crafting, craftMatrix);
        }
    }
}
