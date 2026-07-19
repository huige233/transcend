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

        revealLore(sp, type);

        if (!alreadyRead) {
            stack.getOrCreateTag().putBoolean(TAG_READ, true);

            sp.giveExperiencePoints(xpReward());

            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.3F);
            sl.playSound(null, sp.blockPosition(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2F, 1.0F);

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

    private static void revealLore(ServerPlayer player, ManuscriptType type) {
        String prefix = "manuscript.transcend." + type.id;

        player.sendSystemMessage(Component.literal("§e§l══════════════════════════════════"));
        player.sendSystemMessage(
                Component.translatable(prefix + ".title")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("§e§l══════════════════════════════════"));

        for (int i = 1; i <= type.lineCount; i++) {
            player.sendSystemMessage(
                    Component.translatable(prefix + ".line" + i)
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
        }

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
