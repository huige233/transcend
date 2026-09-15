package com.huige233.transcend.items.tech;

import com.huige233.transcend.ModRarities;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

   
                        
                                    
   
/** 将枪械模块类型封装为可装配物品，并按模块数据显示名称、槽位和属性变化。 */
public class GunModuleItem extends Item {

    public static final String NBT_MODULE_ID = "gun_module";

    public GunModuleItem() {
        this(new Properties().rarity(ModRarities.COSMIC).stacksTo(16));
    }

    
    protected GunModuleItem(Properties properties) {
        super(properties);
    }

    public static GunModule getModule(ItemStack stack) {
        if (stack.getItem() instanceof SiriusModuleItem) return GunModule.MUZZLE_SIRIUS;
        CompoundTag tag = stack.getTag();
        String id = tag == null ? "" : tag.getString(NBT_MODULE_ID);
        return GunModule.byId(id);
    }

    public static ItemStack create(GunModule module) {
        if (module == GunModule.MUZZLE_SIRIUS) {
            return new ItemStack(com.huige233.transcend.init.ModItems.sirius_module.get());
        }
        ItemStack stack = new ItemStack(com.huige233.transcend.init.ModItems.gun_module.get());
        stack.getOrCreateTag().putString(NBT_MODULE_ID, module.id);
        return stack;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        GunModule module = getModule(stack);
        return module == null ? super.getName(stack) : module.displayName();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        GunModule module = getModule(stack);
        if (module == null) {
            tooltip.add(Component.translatable("tooltip.transcend.gun_module.blank")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable("gunmodule.transcend." + module.id + ".desc")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("gunmodule.transcend.slot." + module.slot.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));
        if (module.damageMult != 1.0F) {
            tooltip.add(Component.translatable("tooltip.transcend.gun_module.damage",
                    String.format(Locale.ROOT, "%+.0f%%", (module.damageMult - 1) * 100)).withStyle(ChatFormatting.RED));
        }
        if (module.cooldownMult != 1.0F) {
            tooltip.add(Component.translatable("tooltip.transcend.gun_module.cooldown",
                    String.format(Locale.ROOT, "%+.0f%%", (module.cooldownMult - 1) * 100)).withStyle(ChatFormatting.YELLOW));
        }
        if (module.speedMult != 1.0F) {
            tooltip.add(Component.translatable("tooltip.transcend.gun_module.speed",
                    String.format(Locale.ROOT, "%+.0f%%", (module.speedMult - 1) * 100)).withStyle(ChatFormatting.GREEN));
        }
        tooltip.add(Component.translatable("tooltip.transcend.gun_module.install").withStyle(ChatFormatting.DARK_GRAY));
    }
}
