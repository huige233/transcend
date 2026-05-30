package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.gear.ForgeStage;
import com.huige233.transcend.gear.GearCategory;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * R85: 经历觉醒（Experience Awakening, C 阶段）事件钩子。
 *
 * <p>无需玩家主动交互——只要装备在使用中（手持武器击杀、手持工具挖掘、穿戴护甲受击），
 * 计数器自动累加；累计达到阈值后自动升 awakening tier（最多 3）。
 *
 * <h2>三类计数源（按 {@link GearCategory} 分流）</h2>
 * <table border="1">
 *   <caption>GearCategory → 计数源 → 阈值</caption>
 *   <tr><th>Category</th><th>事件</th><th>判定条件</th><th>tier 阈值（R80）</th></tr>
 *   <tr><td>WEAPON</td><td>{@link LivingDeathEvent}</td><td>主手装备 = WEAPON 且击杀来源 = 玩家</td>
 *       <td>100 / 400 / 1000</td></tr>
 *   <tr><td>TOOL</td><td>{@link BlockEvent.BreakEvent}</td><td>主手装备 = TOOL</td>
 *       <td>1000 / 4000 / 10000</td></tr>
 *   <tr><td>ARMOR</td><td>{@link LivingHurtEvent}</td><td>受击者穿戴有该护甲</td>
 *       <td>50 / 200 / 500</td></tr>
 * </table>
 *
 * <h2>R80 不可逆门</h2>
 * 仅对已写入 CRUCIBLE 的装备累加（{@code GearForgeData.incrementExperience} 本身已校验）。
 * tier 仅可升不可降。
 *
 * <h2>反馈</h2>
 * tier 提升时（仅在 0→1 / 1→2 / 2→3）向玩家显示一条 chat overlay：
 * "§a§l✦ 觉醒 §r§a[gear name] §7Tier %d/%d"
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExperienceAwakeningHandler {

    /** 武器击杀计数。 */
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (killer.level().isClientSide) return;
        ItemStack weapon = killer.getItemInHand(InteractionHand.MAIN_HAND);
        if (!eligibleForAwakening(weapon, GearCategory.WEAPON)) return;

        GearForgeData.incrementExperience(weapon, 1, 0, 0, 0);
        tryAwaken(killer, weapon);
    }

    /** 工具破坏方块计数。 */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player breaker = event.getPlayer();
        if (breaker == null || breaker.level().isClientSide) return;
        ItemStack tool = breaker.getItemInHand(InteractionHand.MAIN_HAND);
        if (!eligibleForAwakening(tool, GearCategory.TOOL)) return;

        GearForgeData.incrementExperience(tool, 0, 0, 1, 0);
        tryAwaken(breaker, tool);
    }

    /** 护甲受击计数（每件穿戴护甲分别 +1）。 */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (event.getAmount() <= 0) return; // 0 伤害不算受击

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack armor = player.getItemBySlot(slot);
            if (!eligibleForAwakening(armor, GearCategory.ARMOR)) continue;
            GearForgeData.incrementExperience(armor, 0, 0, 0, 1);
            tryAwaken(player, armor);
        }
    }

    /**
     * 公共钩子：法术施放计数（R87 战斗整合时可调用本方法把 cast 计数喂进 weapon/wand）。
     * 目前不主动订阅；保留入口为 R87 留 hook。
     */
    public static void noteCast(Player player, ItemStack stack) {
        if (player.level().isClientSide) return;
        if (stack.isEmpty() || !GearForgeData.isStageWritten(stack, ForgeStage.CRUCIBLE)) return;
        GearForgeData.incrementExperience(stack, 0, 1, 0, 0);
        // cast 不参与 tier 升级（GearForgeData.upgradeExperienceTier 按 category 看 kills/blocks/hits）
        // 但仍累加便于 R87 tooltip / 战斗效果使用
    }

    // ─── 玩家克隆时不会丢 NBT（属于装备 ItemStack NBT，已随物品流转）──────
    // 此事件保留作为占位提醒：若以后改用玩家 capability 存储，需要 onClone 复制
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // No-op: experience 数据在装备 NBT 中，跟着 ItemStack 走
    }

    // ─── 公共工具方法 ────────────────────────────────────────────────

    private static boolean eligibleForAwakening(ItemStack stack, GearCategory expected) {
        if (stack.isEmpty()) return false;
        if (!GearForgeData.isEligibleForPipeline(stack)) return false;
        if (!GearForgeData.isStageWritten(stack, ForgeStage.CRUCIBLE)) return false;
        return GearCategory.classify(stack) == expected;
    }

    private static void tryAwaken(Player player, ItemStack stack) {
        int oldTier = GearForgeData.getExperience(stack).tier();
        int newTier = GearForgeData.upgradeExperienceTier(stack);
        if (newTier > oldTier && player instanceof ServerPlayer sp) {
            // 升 tier 通知
            sp.displayClientMessage(
                    Component.translatable("msg.transcend.experience.awakened",
                            stack.getHoverName(), newTier, GearForgeData.MAX_EXPERIENCE_TIER)
                            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
            sp.level().playSound(null, sp.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.2F);
        }
    }
}
