package com.huige233.transcend.items;

import com.huige233.transcend.entity.familiar.TranscendFamiliar;
import com.huige233.transcend.init.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Round 20: 契约召唤物品 — 召唤特定 aspect 的 TranscendFamiliar 并绑定到玩家。
 *
 * <p>每个 FamiliarPactItem 绑定一种 {@link TranscendFamiliar.FamiliarType}。
 * 右键使用时：
 * <ul>
 *   <li>在玩家位置生成对应 type 的 familiar</li>
 *   <li>绑定 owner UUID</li>
 *   <li>消耗本契约物品（stack size -1）</li>
 *   <li>玩家附近若已有同 type familiar → 拒绝召唤</li>
 * </ul>
 *
 * <p>典型获取路径：通过合成（typed crystal + spell scroll + ghast_tear）。
 */
public class FamiliarPactItem extends Item {

    private final TranscendFamiliar.FamiliarType type;

    public FamiliarPactItem(TranscendFamiliar.FamiliarType type) {
        super(new Properties().stacksTo(4).rarity(Rarity.RARE));
        this.type = type;
    }

    public TranscendFamiliar.FamiliarType getFamiliarType() {
        return type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.fail(stack);
        }

        // 检查附近 24 格内是否已有同 type 同主人 familiar
        var existing = sl.getEntitiesOfClass(TranscendFamiliar.class,
                player.getBoundingBox().inflate(24.0),
                f -> f.isAlive() && f.getFamiliarType() == type &&
                        f.getOwnerUUID().map(u -> u.equals(player.getUUID())).orElse(false));
        if (!existing.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("familiar.transcend.already_summoned")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.fail(stack);
        }

        // 召唤
        TranscendFamiliar familiar = ModEntities.FAMILIAR.get().create(sl);
        if (familiar == null) return InteractionResultHolder.fail(stack);
        familiar.setFamiliarType(type);
        familiar.setOwner(player);
        familiar.setPos(player.getX() + 0.5, player.getY() + 0.5, player.getZ() + 0.5);
        sl.addFreshEntity(familiar);

        sl.playSound(null, player.blockPosition(),
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.2F, 1.4F);

        player.displayClientMessage(
                Component.translatable("familiar.transcend." + type.name().toLowerCase() + ".summoned")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        String key = "familiar.transcend." + type.name().toLowerCase();
        tooltip.add(Component.translatable(key + ".desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(key + ".behavior")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("familiar.transcend.bind_warning")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
