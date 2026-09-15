package com.huige233.transcend.items.tech;

import com.huige233.transcend.init.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import java.util.List;


/** 使用时向目标机器注入高密度能量，并在成功充能后消耗物品。 */
public final class PhantomEnergyBlockItem extends Item {
    public static final int ENERGY = 1_000_000_000;
    public PhantomEnergyBlockItem() { super(new Properties().stacksTo(16).fireResistant()); }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        BlockEntity entity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (entity == null) return InteractionResult.PASS;
        int accepted;
        if (entity instanceof com.huige233.transcend.tech.energy.DirectEnergyReceiver direct) {
            accepted = direct.receiveExternalEnergy(ENERGY);
        } else {
            Direction side = context.getClickedFace();
            IEnergyStorage storage = entity.getCapability(ForgeCapabilities.ENERGY, side).orElse(null);
            if (storage == null || !storage.canReceive()) {
                storage = entity.getCapability(ForgeCapabilities.ENERGY, null).orElse(null);
            }
            if (storage == null || !storage.canReceive()) return InteractionResult.PASS;
            accepted = storage.receiveEnergy(ENERGY, false);
        }
        if (accepted <= 0) return InteractionResult.PASS;
        if (!context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        context.getPlayer().displayClientMessage(Component.translatable("msg.transcend.phantom_energy_block.used", accepted), true);
        return InteractionResult.CONSUME;
    }

    @Override public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.phantom_energy_block.energy", ENERGY));
    }
}
