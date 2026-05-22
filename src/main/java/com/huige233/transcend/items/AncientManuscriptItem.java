package com.huige233.transcend.items;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Round 30: 古代手稿 — 探索发现的 lore 解锁物品。
 *
 * <p>使用 → 阅读 lore 内容（聊天框 7 行格式化文本） + 一次性 50 XP 奖励 + 视觉粒子 + 持久 Read 标记。
 * 已阅读的手稿物品在 tooltip 显示"已阅"标记，但保留在背包作为收藏。
 *
 * <p>6 种 ManuscriptType — 每种对应 mod 不同维度的世界观背景：
 * <ul>
 *   <li>WORLD_ORIGIN — 创世神话</li>
 *   <li>MANA_THEORY — 法力本质论</li>
 *   <li>ASPECT_LORE — 四相位起源</li>
 *   <li>BOSS_LORE — 守望者的诞生</li>
 *   <li>ASCENSION_LORE — 飞升之道</li>
 *   <li>AETHER_LORE — 以太领域</li>
 * </ul>
 */
public class AncientManuscriptItem extends Item {

    public enum ManuscriptType {
        WORLD_ORIGIN("world_origin", 7),
        MANA_THEORY("mana_theory", 7),
        ASPECT_LORE("aspect_lore", 7),
        BOSS_LORE("boss_lore", 7),
        ASCENSION_LORE("ascension_lore", 7),
        AETHER_LORE("aether_lore", 7);

        public final String id;
        public final int lineCount;

        ManuscriptType(String id, int lineCount) {
            this.id = id;
            this.lineCount = lineCount;
        }
    }

    private static final String TAG_READ = "transcend_read";

    /** Round 35: 数据驱动 — 从 BalanceConfig 读取，默认 30（早期为 50）。 */
    private static int xpReward() {
        return com.huige233.transcend.balance.BalanceConfig.get().manuscript.xp_reward;
    }

    private final ManuscriptType type;

    public AncientManuscriptItem(ManuscriptType type) {
        super(new Properties().stacksTo(4).rarity(Rarity.RARE).fireResistant());
        this.type = type;
    }

    public ManuscriptType getManuscriptType() {
        return type;
    }

    public static boolean isRead(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_READ);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.pass(stack);
        }

        boolean alreadyRead = isRead(stack);
        // 即使已读，玩家也可重读（不消耗，不给 XP）
        revealLore(sp, type);

        if (!alreadyRead) {
            stack.getOrCreateTag().putBoolean(TAG_READ, true);
            // 一次性 XP 奖励
            sp.giveExperiencePoints(xpReward());
            // 视觉 + 音效（仅初次）
            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.3F);
            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2F, 1.0F);
            // 玩家头顶粒子
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    sp.getX(), sp.getY() + 2.0, sp.getZ(),
                    30, 0.5, 0.5, 0.5, 0.5);
            sp.displayClientMessage(
                    Component.translatable("manuscript.transcend.first_read")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        } else {
            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6F, 1.0F);
        }

        return InteractionResultHolder.consume(stack);
    }

    /** 在聊天框打印 7 行格式化 lore 文本 */
    private static void revealLore(ServerPlayer player, ManuscriptType type) {
        String prefix = "manuscript.transcend." + type.id;

        // 标题装饰行
        player.sendSystemMessage(Component.literal("§e§l══════════════════════════════════"));
        player.sendSystemMessage(
                Component.translatable(prefix + ".title")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("§e§l══════════════════════════════════"));

        // lore 内容 7 行
        for (int i = 1; i <= type.lineCount; i++) {
            player.sendSystemMessage(
                    Component.translatable(prefix + ".line" + i)
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
        }

        // 尾注
        player.sendSystemMessage(Component.literal("§7§o— " + type.id + " manuscript —"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !isRead(stack);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return isRead(stack) ? Rarity.COMMON : Rarity.RARE;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        String prefix = "manuscript.transcend." + type.id;
        tooltip.add(Component.translatable(prefix + ".title")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(prefix + ".tooltip")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.empty());
        if (isRead(stack)) {
            tooltip.add(Component.translatable("manuscript.transcend.status.read")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.translatable("manuscript.transcend.status.unread")
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        }
        tooltip.add(Component.translatable("manuscript.transcend.tip.use")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
