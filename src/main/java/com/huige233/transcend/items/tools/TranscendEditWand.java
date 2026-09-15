package com.huige233.transcend.items.tools;

import com.huige233.transcend.ModRarities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/** 在潜行使用时扫描附近存活生物并按距离排序，打开因果编辑器的实体选择界面。 */
public class TranscendEditWand extends Item {

    public static final int SCAN_RADIUS = 32;
    public static final int MAX_ENTRIES = 60;

    public TranscendEditWand() {
        super(new Properties().rarity(ModRarities.COSMIC).stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                  @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("msg.transcend.editor.shift_hold")
                                .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (level.isClientSide) {
            List<Entry> found = scan(level, player);
            String[] names = new String[found.size()];
            int[] ids = new int[found.size()];
            for (int i = 0; i < found.size(); i++) {
                names[i] = found.get(i).name;
                ids[i] = found.get(i).id;
            }
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.huige233.transcend.client.TranscendEditScreen.openEntities(names, ids));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    
    private static List<Entry> scan(Level level, Player player) {
        List<Entry> out = new ArrayList<>();
        if (level == null || player == null) return out;
        AABB bb = player.getBoundingBox().inflate(SCAN_RADIUS);
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, bb,
                e -> e != player && e.isAlive() && !(e instanceof ArmorStand));
        found.sort(Comparator.comparingDouble(player::distanceToSqr));
        int n = 0;
        for (LivingEntity e : found) {
            if (n >= MAX_ENTRIES) break;
            String name;
            try {
                name = e.getName().getString();
            } catch (Throwable t) {
                name = e.getType().toString();
            }
            out.add(new Entry(e.getId(), name));
            n++;
        }
        return out;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.transcend.transcend_editor_device")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.transcend.editor.usage")
                .withStyle(ChatFormatting.GRAY));
    }

    /** 保存因果编辑器扫描结果中实体的运行时编号与显示名称。 */
    private static final class Entry {
        final int id;
        final String name;
        Entry(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}