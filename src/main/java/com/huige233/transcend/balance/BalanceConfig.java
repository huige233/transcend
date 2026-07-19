package com.huige233.transcend.balance;

import com.google.gson.JsonObject;

public final class BalanceConfig {

    private static final BalanceConfig INSTANCE = new BalanceConfig();

    public static BalanceConfig get() { return INSTANCE; }

    public static final class ApexBalance {

        public int channel_ticks = 100;
        public int cooldown_ticks = 400;
        public int circle_search_radius = 12;

        public float solar_damage = 60f;
        public double solar_radius = 12.0;
        public int solar_fire_seconds = 4;

        public int blood_duration_ticks = 600;
        public int blood_strength_amp = 1;
        public int blood_resistance_amp = 0;
        public int blood_speed_amp = 1;
        public int blood_regen_amp = 1;
        public int blood_absorption_amp = 3;

        public double cosmic_radius = 10.0;
        public int cosmic_duration_ticks = 300;
        public int cosmic_slow_amp = 2;
        public int cosmic_weak_amp = 1;
        public int cosmic_levitation_amp = 0;
        public int cosmic_dig_slow_amp = 2;

        public double void_radius = 16.0;
        public float void_damage_percent = 0.25f;
        public int void_wither_duration = 100;
        public int void_wither_amp = 0;
        public int void_levitation_duration = 60;
        public int void_levitation_amp = 1;
    }

    public static final class RingBalance {

        public int aether_effect_duration = 40;
        public int aether_regen_amp = 0;
        public int aether_luck_amp = 0;

        public float blood_hp_threshold = 0.4f;
        public int blood_effect_duration = 40;
        public int blood_strength_amp = 0;
        public int blood_absorption_amp = 0;

        public int cosmic_night_vision_duration = 220;
        public int cosmic_dolphin_duration = 40;

        public int tainted_interval_ticks = 30;
        public double tainted_radius = 4.0;
        public float tainted_damage = 0.5f;
        public int tainted_mana_gain = 1;
    }

    public static final class SwordBalance {
        public int base_damage = 4;
        public float attack_speed = -2.4f;

        public float aether_heal_amount = 1.0f;

        public float blood_heal_amount = 1.0f;
        public float blood_bonus_damage = 1.0f;

        public int cosmic_slow_duration = 40;
        public int cosmic_slow_amp = 0;
        public int cosmic_levitation_duration = 10;
        public int cosmic_levitation_amp = 0;

        public int tainted_wither_duration = 60;
        public int tainted_wither_amp = 0;
        public int tainted_mana_gain = 2;
    }

    public static final class ManuscriptBalance {
        public int xp_reward = 30;
    }

    public static final class FamiliarBalance {

        public double max_health = 10.0;
        public double attack_damage = 3.0;
        public double movement_speed = 0.35;
        public double follow_range = 32.0;
        public int behavior_interval = 30;
        public int despawn_timer = 600;

        public double aether_pickup_radius = 4.0;

        public double blood_search_radius = 8.0;
        public float blood_attack_damage = 2.0f;

        public int cosmic_night_vision_duration = 40;
        public int cosmic_slow_falling_duration = 40;

        public double tainted_search_radius = 6.0;
        public float tainted_damage = 0.5f;
        public int tainted_mana_gain = 2;

        public int task_radius_default = 8;
        public int task_radius_max = 12;
        public int task_radius_step = 2;

        public int task_transport_per_tick = 1;
        public double cosmic_collect_radius_mult = 1.5;
        public int tainted_harvest_mana_cost = 1;
        public int guard_return_threshold_mult = 2;
    }

    public final ApexBalance apex = new ApexBalance();
    public final RingBalance ring = new RingBalance();
    public final SwordBalance sword = new SwordBalance();
    public final ManuscriptBalance manuscript = new ManuscriptBalance();
    public final FamiliarBalance familiar = new FamiliarBalance();
    public final ScrollBalance scroll = new ScrollBalance();
    public final ForgeBalance forge = new ForgeBalance();

    public static final class ForgeBalance {

        public boolean ward_shield_enabled = true;

        public float ward_shield_per_socket = 1.0f;

        public int ward_shield_regen_interval = 100;

        public float ward_shield_regen_amount = 1.0f;

        public float global_attack_scale = 1.0f;

        public float global_defense_scale = 1.0f;
    }

    public static final class ScrollBalance {

        public int solar_judgement_cost = 2400;
        public int leyline_eruption_cost = 3000;
        public int leyline_eruption_mana_restore = 100;
        public int chronal_stillness_cost = 3600;
        public int sovereign_aegis_cost = 2400;
        public int sovereign_aegis_duration = 300;
        public int thousand_league_return_cost = 1200;
        public int void_exile_mandate_cost = 5200;
        public int storm_king_writ_cost = 3600;
        public int worldmender_edict_cost = 4500;
        public int worldmender_mana_restore = 300;
        public int eclipse_veil_cost = 3200;
        public int avatar_fall_cost = 12000;
        public int unbroken_arsenal_cost = 2400;
        public int ordered_vault_cost = 400;
        public int oreblood_revelation_cost = 600;
        public int oreblood_duration = 1200;
        public int paper_legion_cost = 2400;
        public int paper_legion_lifetime_ticks = 1500;
        public int unremembered_fog_cost = 3200;
        public int inverted_heaven_cost = 3000;
        public int eighteenfold_dragon_cost = 4800;
        public int leyline_resync_cost = 1500;
        public int leyline_resync_center_restore = 400;
        public int leyline_resync_neighbor_restore = 150;
        public int forbidden_hollow_quarry_cost = 6000;
        public int forbidden_black_sun_cost = 12000;

        public int magic_wound_duration = 600;
        public int magic_wound_amplifier = 0;
    }

    public void applyJson(JsonObject root) {
        if (root.has("apex")) applyApex(root.getAsJsonObject("apex"));
        if (root.has("ring")) applyRing(root.getAsJsonObject("ring"));
        if (root.has("sword")) applySword(root.getAsJsonObject("sword"));
        if (root.has("manuscript")) applyManuscript(root.getAsJsonObject("manuscript"));
        if (root.has("familiar")) applyFamiliar(root.getAsJsonObject("familiar"));
        if (root.has("scroll")) applyScroll(root.getAsJsonObject("scroll"));
        if (root.has("forge")) applyForge(root.getAsJsonObject("forge"));
    }

    private void applyApex(JsonObject j) {
        apex.channel_ticks = readInt(j, "channel_ticks", apex.channel_ticks, 20, 400);
        apex.cooldown_ticks = readInt(j, "cooldown_ticks", apex.cooldown_ticks, 0, 6000);
        apex.circle_search_radius = readInt(j, "circle_search_radius", apex.circle_search_radius, 4, 32);
        apex.solar_damage = readFloat(j, "solar_damage", apex.solar_damage, 0f, 200f);
        apex.solar_radius = readDouble(j, "solar_radius", apex.solar_radius, 1.0, 32.0);
        apex.solar_fire_seconds = readInt(j, "solar_fire_seconds", apex.solar_fire_seconds, 0, 16);
        apex.blood_duration_ticks = readInt(j, "blood_duration_ticks", apex.blood_duration_ticks, 20, 1200);
        apex.blood_strength_amp = readInt(j, "blood_strength_amp", apex.blood_strength_amp, 0, 5);
        apex.blood_resistance_amp = readInt(j, "blood_resistance_amp", apex.blood_resistance_amp, 0, 4);
        apex.blood_speed_amp = readInt(j, "blood_speed_amp", apex.blood_speed_amp, 0, 4);
        apex.blood_regen_amp = readInt(j, "blood_regen_amp", apex.blood_regen_amp, 0, 4);
        apex.blood_absorption_amp = readInt(j, "blood_absorption_amp", apex.blood_absorption_amp, 0, 9);
        apex.cosmic_radius = readDouble(j, "cosmic_radius", apex.cosmic_radius, 1.0, 32.0);
        apex.cosmic_duration_ticks = readInt(j, "cosmic_duration_ticks", apex.cosmic_duration_ticks, 20, 1200);
        apex.cosmic_slow_amp = readInt(j, "cosmic_slow_amp", apex.cosmic_slow_amp, 0, 5);
        apex.cosmic_weak_amp = readInt(j, "cosmic_weak_amp", apex.cosmic_weak_amp, 0, 5);
        apex.cosmic_levitation_amp = readInt(j, "cosmic_levitation_amp", apex.cosmic_levitation_amp, 0, 5);
        apex.cosmic_dig_slow_amp = readInt(j, "cosmic_dig_slow_amp", apex.cosmic_dig_slow_amp, 0, 5);
        apex.void_radius = readDouble(j, "void_radius", apex.void_radius, 1.0, 32.0);
        apex.void_damage_percent = readFloat(j, "void_damage_percent", apex.void_damage_percent, 0f, 0.5f);
        apex.void_wither_duration = readInt(j, "void_wither_duration", apex.void_wither_duration, 0, 400);
        apex.void_wither_amp = readInt(j, "void_wither_amp", apex.void_wither_amp, 0, 3);
        apex.void_levitation_duration = readInt(j, "void_levitation_duration", apex.void_levitation_duration, 0, 200);
        apex.void_levitation_amp = readInt(j, "void_levitation_amp", apex.void_levitation_amp, 0, 3);
    }

    private void applyRing(JsonObject j) {
        ring.aether_effect_duration = readInt(j, "aether_effect_duration", ring.aether_effect_duration, 20, 300);
        ring.aether_regen_amp = readInt(j, "aether_regen_amp", ring.aether_regen_amp, 0, 2);
        ring.aether_luck_amp = readInt(j, "aether_luck_amp", ring.aether_luck_amp, 0, 2);
        ring.blood_hp_threshold = readFloat(j, "blood_hp_threshold", ring.blood_hp_threshold, 0.1f, 0.9f);
        ring.blood_effect_duration = readInt(j, "blood_effect_duration", ring.blood_effect_duration, 20, 300);
        ring.blood_strength_amp = readInt(j, "blood_strength_amp", ring.blood_strength_amp, 0, 2);
        ring.blood_absorption_amp = readInt(j, "blood_absorption_amp", ring.blood_absorption_amp, 0, 2);
        ring.cosmic_night_vision_duration = readInt(j, "cosmic_night_vision_duration", ring.cosmic_night_vision_duration, 60, 600);
        ring.cosmic_dolphin_duration = readInt(j, "cosmic_dolphin_duration", ring.cosmic_dolphin_duration, 20, 300);
        ring.tainted_interval_ticks = readInt(j, "tainted_interval_ticks", ring.tainted_interval_ticks, 5, 200);
        ring.tainted_radius = readDouble(j, "tainted_radius", ring.tainted_radius, 1.0, 16.0);
        ring.tainted_damage = readFloat(j, "tainted_damage", ring.tainted_damage, 0f, 4f);
        ring.tainted_mana_gain = readInt(j, "tainted_mana_gain", ring.tainted_mana_gain, 0, 5);
    }

    private void applySword(JsonObject j) {
        sword.base_damage = readInt(j, "base_damage", sword.base_damage, 1, 8);
        sword.attack_speed = readFloat(j, "attack_speed", sword.attack_speed, -3.0f, -1.0f);
        sword.aether_heal_amount = readFloat(j, "aether_heal_amount", sword.aether_heal_amount, 0f, 4f);
        sword.blood_heal_amount = readFloat(j, "blood_heal_amount", sword.blood_heal_amount, 0f, 4f);
        sword.blood_bonus_damage = readFloat(j, "blood_bonus_damage", sword.blood_bonus_damage, 0f, 4f);
        sword.cosmic_slow_duration = readInt(j, "cosmic_slow_duration", sword.cosmic_slow_duration, 0, 200);
        sword.cosmic_slow_amp = readInt(j, "cosmic_slow_amp", sword.cosmic_slow_amp, 0, 3);
        sword.cosmic_levitation_duration = readInt(j, "cosmic_levitation_duration", sword.cosmic_levitation_duration, 0, 60);
        sword.cosmic_levitation_amp = readInt(j, "cosmic_levitation_amp", sword.cosmic_levitation_amp, 0, 2);
        sword.tainted_wither_duration = readInt(j, "tainted_wither_duration", sword.tainted_wither_duration, 0, 200);
        sword.tainted_wither_amp = readInt(j, "tainted_wither_amp", sword.tainted_wither_amp, 0, 2);
        sword.tainted_mana_gain = readInt(j, "tainted_mana_gain", sword.tainted_mana_gain, 0, 10);
    }

    private void applyManuscript(JsonObject j) {
        manuscript.xp_reward = readInt(j, "xp_reward", manuscript.xp_reward, 0, 200);
    }

    private void applyFamiliar(JsonObject j) {
        familiar.max_health = readDouble(j, "max_health", familiar.max_health, 1.0, 40.0);
        familiar.attack_damage = readDouble(j, "attack_damage", familiar.attack_damage, 0.0, 12.0);
        familiar.movement_speed = readDouble(j, "movement_speed", familiar.movement_speed, 0.05, 0.6);
        familiar.follow_range = readDouble(j, "follow_range", familiar.follow_range, 8.0, 64.0);
        familiar.behavior_interval = readInt(j, "behavior_interval", familiar.behavior_interval, 5, 200);
        familiar.despawn_timer = readInt(j, "despawn_timer", familiar.despawn_timer, 100, 6000);
        familiar.aether_pickup_radius = readDouble(j, "aether_pickup_radius", familiar.aether_pickup_radius, 1.0, 16.0);
        familiar.blood_search_radius = readDouble(j, "blood_search_radius", familiar.blood_search_radius, 1.0, 32.0);
        familiar.blood_attack_damage = readFloat(j, "blood_attack_damage", familiar.blood_attack_damage, 0f, 12f);
        familiar.cosmic_night_vision_duration = readInt(j, "cosmic_night_vision_duration", familiar.cosmic_night_vision_duration, 20, 600);
        familiar.cosmic_slow_falling_duration = readInt(j, "cosmic_slow_falling_duration", familiar.cosmic_slow_falling_duration, 20, 400);
        familiar.tainted_search_radius = readDouble(j, "tainted_search_radius", familiar.tainted_search_radius, 1.0, 16.0);
        familiar.tainted_damage = readFloat(j, "tainted_damage", familiar.tainted_damage, 0f, 4f);
        familiar.tainted_mana_gain = readInt(j, "tainted_mana_gain", familiar.tainted_mana_gain, 0, 10);

        familiar.task_radius_default = readInt(j, "task_radius_default", familiar.task_radius_default, 4, 12);
        familiar.task_radius_max = readInt(j, "task_radius_max", familiar.task_radius_max, 4, 12);
        familiar.task_radius_step = readInt(j, "task_radius_step", familiar.task_radius_step, 1, 4);
        familiar.task_transport_per_tick = readInt(j, "task_transport_per_tick", familiar.task_transport_per_tick, 1, 8);
        familiar.cosmic_collect_radius_mult = readDouble(j, "cosmic_collect_radius_mult", familiar.cosmic_collect_radius_mult, 1.0, 2.0);
        familiar.tainted_harvest_mana_cost = readInt(j, "tainted_harvest_mana_cost", familiar.tainted_harvest_mana_cost, 0, 5);
        familiar.guard_return_threshold_mult = readInt(j, "guard_return_threshold_mult", familiar.guard_return_threshold_mult, 1, 4);
    }

    private void applyScroll(JsonObject j) {
        scroll.solar_judgement_cost = readInt(j, "solar_judgement_cost", scroll.solar_judgement_cost, 100, 20000);
        scroll.leyline_eruption_cost = readInt(j, "leyline_eruption_cost", scroll.leyline_eruption_cost, 100, 20000);
        scroll.leyline_eruption_mana_restore = readInt(j, "leyline_eruption_mana_restore", scroll.leyline_eruption_mana_restore, 0, 1000);
        scroll.chronal_stillness_cost = readInt(j, "chronal_stillness_cost", scroll.chronal_stillness_cost, 100, 20000);
        scroll.sovereign_aegis_cost = readInt(j, "sovereign_aegis_cost", scroll.sovereign_aegis_cost, 100, 20000);
        scroll.sovereign_aegis_duration = readInt(j, "sovereign_aegis_duration", scroll.sovereign_aegis_duration, 20, 2400);
        scroll.thousand_league_return_cost = readInt(j, "thousand_league_return_cost", scroll.thousand_league_return_cost, 100, 20000);
        scroll.void_exile_mandate_cost = readInt(j, "void_exile_mandate_cost", scroll.void_exile_mandate_cost, 100, 20000);
        scroll.storm_king_writ_cost = readInt(j, "storm_king_writ_cost", scroll.storm_king_writ_cost, 100, 20000);
        scroll.worldmender_edict_cost = readInt(j, "worldmender_edict_cost", scroll.worldmender_edict_cost, 100, 20000);
        scroll.worldmender_mana_restore = readInt(j, "worldmender_mana_restore", scroll.worldmender_mana_restore, 0, 2000);
        scroll.eclipse_veil_cost = readInt(j, "eclipse_veil_cost", scroll.eclipse_veil_cost, 100, 20000);
        scroll.avatar_fall_cost = readInt(j, "avatar_fall_cost", scroll.avatar_fall_cost, 100, 30000);
        scroll.unbroken_arsenal_cost = readInt(j, "unbroken_arsenal_cost", scroll.unbroken_arsenal_cost, 100, 20000);
        scroll.ordered_vault_cost = readInt(j, "ordered_vault_cost", scroll.ordered_vault_cost, 100, 20000);
        scroll.oreblood_revelation_cost = readInt(j, "oreblood_revelation_cost", scroll.oreblood_revelation_cost, 100, 20000);
        scroll.oreblood_duration = readInt(j, "oreblood_duration", scroll.oreblood_duration, 20, 6000);
        scroll.paper_legion_cost = readInt(j, "paper_legion_cost", scroll.paper_legion_cost, 100, 20000);
        scroll.paper_legion_lifetime_ticks = readInt(j, "paper_legion_lifetime_ticks", scroll.paper_legion_lifetime_ticks, 100, 24000);
        scroll.unremembered_fog_cost = readInt(j, "unremembered_fog_cost", scroll.unremembered_fog_cost, 100, 20000);
        scroll.inverted_heaven_cost = readInt(j, "inverted_heaven_cost", scroll.inverted_heaven_cost, 100, 20000);
        scroll.eighteenfold_dragon_cost = readInt(j, "eighteenfold_dragon_cost", scroll.eighteenfold_dragon_cost, 100, 20000);
        scroll.leyline_resync_cost = readInt(j, "leyline_resync_cost", scroll.leyline_resync_cost, 100, 20000);
        scroll.leyline_resync_center_restore = readInt(j, "leyline_resync_center_restore", scroll.leyline_resync_center_restore, 0, 2000);
        scroll.leyline_resync_neighbor_restore = readInt(j, "leyline_resync_neighbor_restore", scroll.leyline_resync_neighbor_restore, 0, 2000);
        scroll.forbidden_hollow_quarry_cost = readInt(j, "forbidden_hollow_quarry_cost", scroll.forbidden_hollow_quarry_cost, 100, 30000);
        scroll.forbidden_black_sun_cost = readInt(j, "forbidden_black_sun_cost", scroll.forbidden_black_sun_cost, 100, 30000);
        scroll.magic_wound_duration = readInt(j, "magic_wound_duration", scroll.magic_wound_duration, 20, 6000);
        scroll.magic_wound_amplifier = readInt(j, "magic_wound_amplifier", scroll.magic_wound_amplifier, 0, 3);
    }

    private static int readInt(JsonObject j, String key, int def, int min, int max) {
        if (!j.has(key)) return def;
        try { return Math.max(min, Math.min(max, j.get(key).getAsInt())); } catch (Exception e) { return def; }
    }
    private static float readFloat(JsonObject j, String key, float def, float min, float max) {
        if (!j.has(key)) return def;
        try { return Math.max(min, Math.min(max, j.get(key).getAsFloat())); } catch (Exception e) { return def; }
    }
    private static double readDouble(JsonObject j, String key, double def, double min, double max) {
        if (!j.has(key)) return def;
        try { return Math.max(min, Math.min(max, j.get(key).getAsDouble())); } catch (Exception e) { return def; }
    }
    private static boolean readBool(JsonObject j, String key, boolean def) {
        if (!j.has(key)) return def;
        try { return j.get(key).getAsBoolean(); } catch (Exception e) { return def; }
    }

    private void applyForge(JsonObject j) {
        forge.ward_shield_enabled        = readBool(j, "ward_shield_enabled", forge.ward_shield_enabled);
        forge.ward_shield_per_socket     = readFloat(j, "ward_shield_per_socket", forge.ward_shield_per_socket, 0.0f, 5.0f);
        forge.ward_shield_regen_interval = readInt(j, "ward_shield_regen_interval", forge.ward_shield_regen_interval, 20, 1200);
        forge.ward_shield_regen_amount   = readFloat(j, "ward_shield_regen_amount", forge.ward_shield_regen_amount, 0.0f, 4.0f);
        forge.global_attack_scale        = readFloat(j, "global_attack_scale", forge.global_attack_scale, 0.0f, 2.0f);
        forge.global_defense_scale       = readFloat(j, "global_defense_scale", forge.global_defense_scale, 0.0f, 2.0f);
    }
}
