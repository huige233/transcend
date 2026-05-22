package com.huige233.transcend.items.curio;

import com.huige233.transcend.balance.BalanceConfig;
import com.huige233.transcend.client.magic.MagicCrystalHelper;
import com.huige233.transcend.items.TypedManaCrystal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Round 28: Aspect Ring 饰品 — 与 TypedManaCrystal 对应 4 aspect 各 1 个 curio。
 *
 * <p>装备 curio 槽（slot=ring）后被动给主人 buff：
 * <ul>
 *   <li>{@link AspectRingType#AETHER} — REGENERATION 1 + LUCK 1（生命回复 + 幸运）</li>
 *   <li>{@link AspectRingType#BLOOD} — HP < 50% 时 STRENGTH 1 + ABSORPTION 1（绝境强化）</li>
 *   <li>{@link AspectRingType#COSMIC} — NIGHT_VISION + DOLPHINS_GRACE（夜视 + 水中加速）</li>
 *   <li>{@link AspectRingType#TAINTED} — 每 20 tick 抽 6 格内敌怪 1 HP → 玩家 +1 mana</li>
 * </ul>
 *
 * <p>所有 ring 都 fireResistant + RARE rarity。
 */
public class AspectRingItem extends Item implements ICurioItem {

    public enum AspectRingType {
        AETHER(TypedManaCrystal.ManaAspect.AETHER, ChatFormatting.GOLD),
        BLOOD(TypedManaCrystal.ManaAspect.BLOOD, ChatFormatting.RED),
        COSMIC(TypedManaCrystal.ManaAspect.COSMIC, ChatFormatting.BLUE),
        TAINTED(TypedManaCrystal.ManaAspect.TAINTED, ChatFormatting.LIGHT_PURPLE);

        public final TypedManaCrystal.ManaAspect aspect;
        public final ChatFormatting color;

        AspectRingType(TypedManaCrystal.ManaAspect aspect, ChatFormatting color) {
            this.aspect = aspect;
            this.color = color;
        }
    }

    private final AspectRingType type;

    public AspectRingItem(AspectRingType type) {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
        this.type = type;
    }

    public AspectRingType getRingType() {
        return type;
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        LivingEntity entity = ctx.entity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        switch (type) {
            case AETHER -> applyAether(player);
            case BLOOD -> applyBlood(player);
            case COSMIC -> applyCosmic(player);
            case TAINTED -> applyTainted(player);
        }
    }

    private static void applyAether(Player player) {
        BalanceConfig.RingBalance r = BalanceConfig.get().ring;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, r.aether_effect_duration, r.aether_regen_amp, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, r.aether_effect_duration, r.aether_luck_amp, true, false));
    }

    private static void applyBlood(Player player) {
        BalanceConfig.RingBalance r = BalanceConfig.get().ring;
        if (player.getHealth() < player.getMaxHealth() * r.blood_hp_threshold) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, r.blood_effect_duration, r.blood_strength_amp, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, r.blood_effect_duration, r.blood_absorption_amp, true, false));
        }
    }

    private static void applyCosmic(Player player) {
        BalanceConfig.RingBalance r = BalanceConfig.get().ring;
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, r.cosmic_night_vision_duration, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, r.cosmic_dolphin_duration, 0, true, false));
    }

    private static void applyTainted(Player player) {
        BalanceConfig.RingBalance r = BalanceConfig.get().ring;
        if (player.level().getGameTime() % r.tainted_interval_ticks != 0) return;
        if (!(player.level() instanceof ServerLevel sl)) return;

        AABB search = player.getBoundingBox().inflate(r.tainted_radius);
        var enemies = sl.getEntitiesOfClass(LivingEntity.class, search,
                e -> e != player && e.isAlive() && e instanceof Enemy);
        if (enemies.isEmpty()) return;

        LivingEntity victim = enemies.get(0);
        if (victim.getHealth() <= 1.5F) return; // 留 1 HP
        victim.hurt(sl.damageSources().magic(), r.tainted_damage);

        int curr = MagicCrystalHelper.getInnateMana(player);
        int max = MagicCrystalHelper.getInnateMaxMana(player);
        MagicCrystalHelper.setInnateMana(player, Math.min(max, curr + r.tainted_mana_gain));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        String key = "ring.transcend." + type.name().toLowerCase();
        tooltip.add(Component.translatable(key + ".desc").withStyle(type.color));
        tooltip.add(Component.translatable(key + ".effect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("ring.transcend.curio_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
