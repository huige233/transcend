package com.huige233.transcend.items.tools;

import com.huige233.transcend.balance.BalanceConfig;
import com.huige233.transcend.client.magic.MagicCrystalHelper;
import com.huige233.transcend.items.TypedManaCrystal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Round 29: Aspect Sword — 4 aspect 主题武器，与 4 ring / 4 familiar / 4 sink 对位。
 *
 * <p>所有 sword tier=NETHERITE，基础伤害 +5 / 攻速 -2.4。
 * 通过 {@link #hurtEnemy} 重写应用 aspect 专属副效果：
 * <ul>
 *   <li>AETHER — 治疗剑：攻击玩家自身回 25% damage</li>
 *   <li>BLOOD — 嗜血剑：攻击 target 立即 WITHER + lifesteal +2 HP</li>
 *   <li>COSMIC — 寰宇剑：攻击 target 60 tick SLOWNESS II + LEVITATION 0</li>
 *   <li>TAINTED — 腐蚀剑：攻击 target 80 tick WITHER I + 玩家 +5 mana</li>
 * </ul>
 */
public class AspectSwordItem extends SwordItem {

    public enum AspectSwordType {
        AETHER(TypedManaCrystal.ManaAspect.AETHER, "aether", ChatFormatting.GOLD),
        BLOOD(TypedManaCrystal.ManaAspect.BLOOD, "blood", ChatFormatting.RED),
        COSMIC(TypedManaCrystal.ManaAspect.COSMIC, "cosmic", ChatFormatting.BLUE),
        TAINTED(TypedManaCrystal.ManaAspect.TAINTED, "tainted", ChatFormatting.LIGHT_PURPLE);

        public final TypedManaCrystal.ManaAspect aspect;
        public final String id;
        public final ChatFormatting color;

        AspectSwordType(TypedManaCrystal.ManaAspect aspect, String id, ChatFormatting color) {
            this.aspect = aspect;
            this.id = id;
            this.color = color;
        }
    }

    private final AspectSwordType type;

    public AspectSwordItem(AspectSwordType type) {
        super(Tiers.NETHERITE,
                BalanceConfig.get().sword.base_damage,
                BalanceConfig.get().sword.attack_speed,
                new Properties().rarity(Rarity.EPIC).fireResistant());
        this.type = type;
    }

    public AspectSwordType getSwordType() {
        return type;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hit = super.hurtEnemy(stack, target, attacker);
        if (!hit || target.level().isClientSide) return hit;

        switch (type) {
            case AETHER -> applyAetherEffect(target, attacker);
            case BLOOD -> applyBloodEffect(target, attacker);
            case COSMIC -> applyCosmicEffect(target, attacker);
            case TAINTED -> applyTaintedEffect(target, attacker);
        }
        return hit;
    }

    private static void applyAetherEffect(LivingEntity target, LivingEntity attacker) {
        // 治疗给 attacker
        attacker.heal(BalanceConfig.get().sword.aether_heal_amount);
    }

    private static void applyBloodEffect(LivingEntity target, LivingEntity attacker) {
        BalanceConfig.SwordBalance s = BalanceConfig.get().sword;
        // Lifesteal
        attacker.heal(s.blood_heal_amount);
        // 额外魔法伤害
        target.hurt(target.level().damageSources().magic(), s.blood_bonus_damage);
    }

    private static void applyCosmicEffect(LivingEntity target, LivingEntity attacker) {
        BalanceConfig.SwordBalance s = BalanceConfig.get().sword;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, s.cosmic_slow_duration, s.cosmic_slow_amp, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, s.cosmic_levitation_duration, s.cosmic_levitation_amp, false, true));
    }

    private static void applyTaintedEffect(LivingEntity target, LivingEntity attacker) {
        BalanceConfig.SwordBalance s = BalanceConfig.get().sword;
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, s.tainted_wither_duration, s.tainted_wither_amp, false, true));
        if (attacker instanceof Player player) {
            int curr = MagicCrystalHelper.getInnateMana(player);
            int max = MagicCrystalHelper.getInnateMaxMana(player);
            MagicCrystalHelper.setInnateMana(player, Math.min(max, curr + s.tainted_mana_gain));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        String key = "sword.transcend." + type.id;
        tooltip.add(Component.translatable(key + ".desc").withStyle(type.color));
        tooltip.add(Component.translatable(key + ".effect").withStyle(ChatFormatting.GRAY));
    }
}
