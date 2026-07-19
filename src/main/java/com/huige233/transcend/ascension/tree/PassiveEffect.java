package com.huige233.transcend.ascension.tree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public sealed interface PassiveEffect {

    float DODGE_CAP = 0.15F;
    float DAMAGE_REDUCTION_CAP = 0.30F;
    float FREE_CAST_CAP = 0.20F;
    float KILL_XP_MULTIPLIER_CAP = 2.50F;

    JsonObject toJson();

    default void onKill(ServerPlayer player, LivingEntity target, boolean isBoss) {}
    default void onHurt(LivingHurtEvent event, ServerPlayer player, int playerStage) {}

    default void onAttack(LivingHurtEvent event, ServerPlayer attacker, LivingEntity target) {}

    static float aggregateDodgeChance(float statChance, List<PassiveEffect> passives) {
        float chance = finitePositive(statChance);
        for (PassiveEffect effect : passives) {
            if (effect instanceof Dodge dodge) chance += finitePositive(dodge.chance());
        }
        return Math.min(DODGE_CAP, chance);
    }

    static float aggregateDamageReduction(List<PassiveEffect> passives) {
        float reduction = 0.0F;
        for (PassiveEffect effect : passives) {
            if (effect instanceof DamageReduction damageReduction) {
                reduction += finitePositive(damageReduction.value());
            }
        }
        return Math.min(DAMAGE_REDUCTION_CAP, reduction);
    }

    static float strongestExecuteBonus(List<PassiveEffect> passives, float targetHealthFraction) {
        if (!Float.isFinite(targetHealthFraction)) return 0.0F;
        float bonus = 0.0F;
        for (PassiveEffect effect : passives) {
            if (effect instanceof ExecuteThreshold execute
                    && targetHealthFraction <= execute.threshold()) {
                bonus = Math.max(bonus, finitePositive(execute.bonusDamage()));
            }
        }
        return bonus;
    }

    static float aggregateFreeCastChance(List<PassiveEffect> passives) {
        float chance = 0.0F;
        for (PassiveEffect effect : passives) {
            if (effect instanceof ManaFreeCast freeCast) chance += finitePositive(freeCast.chance());
        }
        return Math.min(FREE_CAST_CAP, chance);
    }

    static float aggregateKillXpMultiplier(float statBonus, List<PassiveEffect> passives) {
        float bonus = finitePositive(statBonus);
        for (PassiveEffect effect : passives) {
            if (effect instanceof XPMultiplier xp && Float.isFinite(xp.value())) {
                bonus += Math.max(0.0F, xp.value() - 1.0F);
            }
        }
        return Math.min(KILL_XP_MULTIPLIER_CAP, 1.0F + bonus);
    }

    static float aggregateDamageHealingRate(float lifesteal, float spellVamp, boolean elementalSpell) {
        float rate = finitePositive(lifesteal);
        if (elementalSpell) rate += finitePositive(spellVamp);
        return Math.min(0.10F, rate);
    }

    static UndyingPolicy aggregateUndying(List<PassiveEffect> passives) {
        float chance = 0.0F;
        int cooldown = 0;
        for (PassiveEffect effect : passives) {
            if (effect instanceof Undying undying) {
                chance = Math.max(chance, finitePositive(undying.chance()));
                cooldown = Math.max(cooldown, Math.max(0, undying.cooldown()));
            }
        }
        return new UndyingPolicy(Math.min(1.0F, chance), cooldown);
    }

    private static float finitePositive(float value) {
        return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
    }

    record UndyingPolicy(float chance, int cooldown) {
        public boolean isPresent() {
            return chance > 0.0F;
        }
    }

    static PassiveEffect fromJson(JsonObject json) {
        String type = json.get("type").getAsString();
        return switch (type) {
            case "on_kill" -> OnKillEffect.parse(json);
            case "damage_reduction" -> new DamageReduction(json.get("value").getAsFloat());
            case "chance_absorb" -> new ChanceAbsorb(
                    json.get("chance").getAsFloat(),
                    json.get("reduction").getAsFloat());
            case "chance_freeze_attacker" -> new ChanceFreezeAttacker(
                    json.get("chance").getAsFloat(),
                    json.get("duration").getAsInt(),
                    json.get("amplifier").getAsInt());
            case "chance_apply_effect" -> new ChanceApplyEffect(
                    json.get("chance").getAsFloat(),
                    json.get("target").getAsString(),
                    new ResourceLocation(json.get("effect").getAsString()),
                    json.get("duration").getAsInt(),
                    json.get("amplifier").getAsInt());
            case "chance_reflect" -> new ChanceReflect(
                    json.get("chance").getAsFloat(),
                    json.get("reflect_fraction").getAsFloat());
            case "health_threshold_cross" -> HealthThresholdCross.parse(json);
            case "divine_aegis" -> new DivineAegis(
                    json.get("chance").getAsFloat(),
                    json.get("min_stage").getAsInt());
            case "xp_multiplier" -> new XPMultiplier(json.get("value").getAsFloat());
            case "execute_threshold" -> new ExecuteThreshold(
                    json.get("threshold").getAsFloat(),
                    json.get("bonus_damage").getAsFloat());
            case "mana_free_cast" -> new ManaFreeCast(json.get("chance").getAsFloat());
            case "dodge" -> new Dodge(json.get("chance").getAsFloat());
            case "undying" -> new Undying(
                    json.get("chance").getAsFloat(),
                    json.get("cooldown").getAsInt());
            default -> throw new IllegalArgumentException("Unknown passive effect type: " + type);
        };
    }

    static List<PassiveEffect> listFromJson(JsonArray array) {
        List<PassiveEffect> list = new ArrayList<>();
        for (JsonElement el : array) {
            list.add(fromJson(el.getAsJsonObject()));
        }
        return list;
    }

    static void validate(PassiveEffect effect, String nodeId) {
        JsonObject json = effect.toJson();
        if (!json.has("chance")) return;
        float chance = json.get("chance").getAsFloat();
        if (!Float.isFinite(chance) || chance < 0.0F || chance > 1.0F) {
            throw new IllegalArgumentException("Node '" + nodeId
                    + "' has passive chance outside 0..1: " + chance);
        }
    }

    record OnKillEffect(String condition, String action, float value) implements PassiveEffect {
        static OnKillEffect parse(JsonObject json) {
            return new OnKillEffect(
                    json.get("condition").getAsString(),
                    json.get("action").getAsString(),
                    json.get("value").getAsFloat());
        }

        @Override
        public void onKill(ServerPlayer player, LivingEntity target, boolean isBoss) {
            boolean match = switch (condition) {
                case "is_monster" -> target instanceof Monster;
                case "is_boss" -> isBoss;
                case "any" -> true;
                default -> false;
            };
            if (!match) return;
            switch (action) {
                case "heal" -> player.heal(value);
                case "feed" -> player.getFoodData().eat((int) value, value * 0.1f);
            }
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "on_kill");
            json.addProperty("condition", condition);
            json.addProperty("action", action);
            json.addProperty("value", value);
            return json;
        }
    }

    record DamageReduction(float value) implements PassiveEffect {
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "damage_reduction");
            json.addProperty("value", value);
            return json;
        }
    }

    record ChanceAbsorb(float chance, float reduction) implements PassiveEffect {
        @Override
        public void onHurt(LivingHurtEvent event, ServerPlayer player, int playerStage) {
            if (player.getRandom().nextFloat() < chance) {
                event.setAmount(event.getAmount() * (1.0f - reduction));
            }
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "chance_absorb");
            json.addProperty("chance", chance);
            json.addProperty("reduction", reduction);
            return json;
        }
    }

    record ChanceFreezeAttacker(float chance, int duration, int amplifier) implements PassiveEffect {
        @Override
        public void onHurt(LivingHurtEvent event, ServerPlayer player, int playerStage) {
            if (player.getRandom().nextFloat() < chance
                    && event.getSource().getEntity() instanceof LivingEntity attacker) {
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier));
            }
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "chance_freeze_attacker");
            json.addProperty("chance", chance);
            json.addProperty("duration", duration);
            json.addProperty("amplifier", amplifier);
            return json;
        }
    }

    record ChanceApplyEffect(float chance, String target, ResourceLocation effect,
                             int duration, int amplifier) implements PassiveEffect {
        @Override
        public void onHurt(LivingHurtEvent event, ServerPlayer player, int playerStage) {
            if (player.getRandom().nextFloat() < chance) {
                MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effect);
                if (mobEffect == null) return;
                LivingEntity applyTarget = "attacker".equals(target)
                        && event.getSource().getEntity() instanceof LivingEntity atk
                        ? atk : player;
                applyTarget.addEffect(new MobEffectInstance(mobEffect, duration, amplifier));
            }
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "chance_apply_effect");
            json.addProperty("chance", chance);
            json.addProperty("target", target);
            json.addProperty("effect", effect.toString());
            json.addProperty("duration", duration);
            json.addProperty("amplifier", amplifier);
            return json;
        }
    }

    record ChanceReflect(float chance, float reflectFraction) implements PassiveEffect {
        @Override
        public void onHurt(LivingHurtEvent event, ServerPlayer player, int playerStage) {
            if (player.getRandom().nextFloat() < chance
                    && event.getSource().getEntity() instanceof LivingEntity attacker) {
                attacker.hurt(player.damageSources().magic(), event.getAmount() * reflectFraction);
            }
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "chance_reflect");
            json.addProperty("chance", chance);
            json.addProperty("reflect_fraction", reflectFraction);
            return json;
        }
    }

    record HealthThresholdCross(float threshold, List<EffectAction> effects) implements PassiveEffect {
        static HealthThresholdCross parse(JsonObject json) {
            List<EffectAction> actions = new ArrayList<>();
            for (JsonElement el : json.getAsJsonArray("effects")) {
                JsonObject ej = el.getAsJsonObject();
                actions.add(new EffectAction(
                        new ResourceLocation(ej.get("effect").getAsString()),
                        ej.get("duration").getAsInt(),
                        ej.get("amplifier").getAsInt()));
            }
            return new HealthThresholdCross(json.get("threshold").getAsFloat(), actions);
        }

        @Override
        public void onHurt(LivingHurtEvent event, ServerPlayer player, int playerStage) {
            float hp = player.getHealth();
            float maxHp = player.getMaxHealth();
            float thresholdHp = maxHp * threshold;
            if (hp > thresholdHp && hp - event.getAmount() < thresholdHp) {
                for (EffectAction ea : effects) {
                    MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(ea.effect);
                    if (mobEffect != null) {
                        player.addEffect(new MobEffectInstance(mobEffect, ea.duration, ea.amplifier));
                    }
                }
            }
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "health_threshold_cross");
            json.addProperty("threshold", threshold);
            JsonArray arr = new JsonArray();
            for (EffectAction ea : effects) {
                JsonObject ej = new JsonObject();
                ej.addProperty("effect", ea.effect.toString());
                ej.addProperty("duration", ea.duration);
                ej.addProperty("amplifier", ea.amplifier);
                arr.add(ej);
            }
            json.add("effects", arr);
            return json;
        }
    }

    record EffectAction(ResourceLocation effect, int duration, int amplifier) {}

    record DivineAegis(float chance, int minStage) implements PassiveEffect {
        @Override
        public void onHurt(LivingHurtEvent event, ServerPlayer player, int playerStage) {
            if (playerStage >= minStage && player.getRandom().nextFloat() < chance) {
                event.setCanceled(true);
            }
        }

        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "divine_aegis");
            json.addProperty("chance", chance);
            json.addProperty("min_stage", minStage);
            return json;
        }
    }

    record XPMultiplier(float value) implements PassiveEffect {
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "xp_multiplier");
            json.addProperty("value", value);
            return json;
        }
    }

    record ExecuteThreshold(float threshold, float bonusDamage) implements PassiveEffect {
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "execute_threshold");
            json.addProperty("threshold", threshold);
            json.addProperty("bonus_damage", bonusDamage);
            return json;
        }
    }

    record ManaFreeCast(float chance) implements PassiveEffect {
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "mana_free_cast");
            json.addProperty("chance", chance);
            return json;
        }
    }

    record Dodge(float chance) implements PassiveEffect {
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "dodge");
            json.addProperty("chance", chance);
            return json;
        }
    }

    record Undying(float chance, int cooldown) implements PassiveEffect {
        public JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "undying");
            json.addProperty("chance", chance);
            json.addProperty("cooldown", cooldown);
            return json;
        }
    }
}
