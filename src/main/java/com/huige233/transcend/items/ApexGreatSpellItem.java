package com.huige233.transcend.items;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.balance.BalanceConfig;
import com.huige233.transcend.block.circle.MagicCircleCoreBlockEntity;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.spell.MagicCrystalHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/** 至高大术物品。 */
public class ApexGreatSpellItem extends Item {

    public enum ApexType {

        SOLAR_COLLAPSE("solar_collapse", 1.0F, 0.85F, 0.15F),

        BLOOD_PACT("blood_pact", 0.9F, 0.05F, 0.05F),

        COSMIC_ANCHOR("cosmic_anchor", 0.20F, 0.40F, 0.95F),

        VOID_UNMAKING("void_unmaking", 0.55F, 0.10F, 0.65F);

        public final String id;
        public final float r, g, b;

        ApexType(String id, float r, float g, float b) {
            this.id = id;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static final String TAG_CHARGE = "transcend_apex_charge";
    private static final int REQUIRED_STAGE = 4;

    private static int channelTicks() { return BalanceConfig.get().apex.channel_ticks; }
    private static int cooldownTicks() { return BalanceConfig.get().apex.cooldown_ticks; }
    private static int circleSearchRadius() { return BalanceConfig.get().apex.circle_search_radius; }

    private final ApexType type;

    public ApexGreatSpellItem(ApexType type) {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
        this.type = type;
    }

    public ApexType getType() {
        return type;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {

            PlayerAscensionData data = AscensionCapability.get(player);
            if (data.getRitualTier() < REQUIRED_STAGE && !player.getAbilities().instabuild) {
                player.displayClientMessage(
                        Component.translatable("apex.transcend.stage_gate", REQUIRED_STAGE)
                                .withStyle(ChatFormatting.DARK_RED), true);
                return InteractionResultHolder.fail(stack);
            }

            if (!hasNearbyPrimordialCircle(player, level) && !player.getAbilities().instabuild) {
                player.displayClientMessage(
                        Component.translatable("apex.transcend.circle_gate")
                                .withStyle(ChatFormatting.DARK_RED), true);
                return InteractionResultHolder.fail(stack);
            }

            if (!hasAllAspects(player) && !player.getAbilities().instabuild) {
                player.displayClientMessage(
                        Component.translatable("apex.transcend.crystal_gate")
                                .withStyle(ChatFormatting.DARK_RED), true);
                return InteractionResultHolder.fail(stack);
            }

            stack.getOrCreateTag().putInt(TAG_CHARGE, 0);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        CompoundTag tag = stack.getOrCreateTag();
        int progress = tag.getInt(TAG_CHARGE) + 1;
        tag.putInt(TAG_CHARGE, progress);

        emitChannelParticles(serverLevel, player, type, progress);
        if (progress % 20 == 0) {
            float pitch = 0.5F + (float) progress / channelTicks();
            serverLevel.playSound(null, player.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5F, pitch);
        }

        if (progress >= channelTicks()) {
            if (!player.getAbilities().instabuild) {

                if (!consumeAllAspects(player)) {

                    player.displayClientMessage(
                            Component.translatable("apex.transcend.crystal_lost")
                                    .withStyle(ChatFormatting.RED), true);
                    tag.remove(TAG_CHARGE);
                    player.releaseUsingItem();
                    return;
                }
                stack.shrink(1);
            }
            tag.remove(TAG_CHARGE);
            executeApex(serverLevel, player, type);
            player.releaseUsingItem();
            player.getCooldowns().addCooldown(this, cooldownTicks());
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeCharged) {

        if (!level.isClientSide && stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(TAG_CHARGE)) {
                int progress = tag.getInt(TAG_CHARGE);
                tag.remove(TAG_CHARGE);
                if (progress > 0 && entity instanceof Player p) {
                    p.displayClientMessage(
                            Component.translatable("apex.transcend.channel_broken", progress, channelTicks())
                                    .withStyle(ChatFormatting.GRAY), true);
                }
            }
        }
    }

    private static boolean hasNearbyPrimordialCircle(Player player, Level level) {
        BlockPos playerPos = player.blockPosition();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        int chunkRadius = 2;
        long maxDistSq = (long) circleSearchRadius() * circleSearchRadius();

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
                    if (!(e.getValue() instanceof MagicCircleCoreBlockEntity core)) continue;
                    if (!core.isActive() || !core.isStructureValid()) continue;
                    CircleTier tier = core.getDetectedTier();
                    if (tier == null || tier != CircleTier.PRIMORDIAL) continue;
                    if (e.getKey().distSqr(playerPos) <= maxDistSq) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasAllAspects(Player player) {
        boolean a = false, b = false, c = false, t = false;
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty() || !(s.getItem() instanceof TypedManaCrystal tmc)) continue;
            switch (tmc.getAspect()) {
                case AETHER -> a = true;
                case BLOOD -> b = true;
                case COSMIC -> c = true;
                case TAINTED -> t = true;
            }
            if (a && b && c && t) return true;
        }
        return a && b && c && t;
    }

    private static boolean consumeAllAspects(Player player) {
        boolean[] consumed = new boolean[TypedManaCrystal.ManaAspect.values().length];
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty() || !(s.getItem() instanceof TypedManaCrystal tmc)) continue;
            int idx = tmc.getAspect().ordinal();
            if (!consumed[idx]) {
                s.shrink(1);
                consumed[idx] = true;
            }
        }
        for (boolean ok : consumed) if (!ok) return false;
        return true;
    }

    private static void executeApex(ServerLevel level, ServerPlayer caster, ApexType type) {
        BlockPos pos = caster.blockPosition();

        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 3.0F, 0.4F);
        level.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3, 0.5, 0.5, 0.5, 0.0);

        switch (type) {
            case SOLAR_COLLAPSE -> applySolarCollapse(level, caster);
            case BLOOD_PACT -> applyBloodPact(level, caster);
            case COSMIC_ANCHOR -> applyCosmicAnchor(level, caster);
            case VOID_UNMAKING -> applyVoidUnmaking(level, caster);
        }

        caster.displayClientMessage(
                Component.translatable("apex.transcend." + type.id + ".released")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
    }

    private static void applySolarCollapse(ServerLevel level, ServerPlayer caster) {
        BalanceConfig.ApexBalance a = BalanceConfig.get().apex;
        AABB box = new AABB(caster.blockPosition()).inflate(a.solar_radius);
        var targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && (e instanceof Enemy || (e instanceof Mob m && !m.isAlliedTo(caster))));
        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().magic(), a.solar_damage);
            e.setSecondsOnFire(a.solar_fire_seconds);
        }

        for (int i = 0; i < 200; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = level.random.nextDouble() * a.solar_radius;
            double x = caster.getX() + Math.cos(angle) * dist;
            double z = caster.getZ() + Math.sin(angle) * dist;
            double y = caster.getY() + 20.0;
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 5, 0.5, 0.5, 0.5, 0.2);
        }
    }

    private static void applyBloodPact(ServerLevel level, ServerPlayer caster) {
        BalanceConfig.ApexBalance a = BalanceConfig.get().apex;
        caster.setHealth(1.0F);
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, a.blood_duration_ticks, a.blood_strength_amp, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, a.blood_duration_ticks, a.blood_resistance_amp, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, a.blood_duration_ticks, a.blood_speed_amp, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, a.blood_duration_ticks, a.blood_regen_amp, false, true));
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, a.blood_duration_ticks, a.blood_absorption_amp, false, true));

        for (int i = 0; i < 100; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double r = 1.0 + level.random.nextDouble() * 2.0;
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.9F, 0.05F, 0.1F), 2.0F),
                    caster.getX() + Math.cos(angle) * r,
                    caster.getY() + level.random.nextDouble() * 2.5,
                    caster.getZ() + Math.sin(angle) * r,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static void applyCosmicAnchor(ServerLevel level, ServerPlayer caster) {
        BalanceConfig.ApexBalance a = BalanceConfig.get().apex;
        AABB box = new AABB(caster.blockPosition()).inflate(a.cosmic_radius);
        var targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && (e instanceof Enemy || (e instanceof Mob m && !m.isAlliedTo(caster))));
        for (LivingEntity e : targets) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, a.cosmic_duration_ticks, a.cosmic_slow_amp, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, a.cosmic_duration_ticks, a.cosmic_weak_amp, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, a.cosmic_duration_ticks, a.cosmic_levitation_amp, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, a.cosmic_duration_ticks, a.cosmic_dig_slow_amp, false, true));
        }

        for (int i = 0; i < 300; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2;
            double phi = level.random.nextDouble() * Math.PI;
            double r = a.cosmic_radius;
            double x = caster.getX() + Math.sin(phi) * Math.cos(theta) * r;
            double y = caster.getY() + Math.cos(phi) * r;
            double z = caster.getZ() + Math.sin(phi) * Math.sin(theta) * r;
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.2F, 0.4F, 0.95F), 2.0F),
                    x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void applyVoidUnmaking(ServerLevel level, ServerPlayer caster) {
        BalanceConfig.ApexBalance a = BalanceConfig.get().apex;
        AABB box = new AABB(caster.blockPosition()).inflate(a.void_radius);
        var targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && (e instanceof Enemy || (e instanceof Mob m && !m.isAlliedTo(caster))));
        for (LivingEntity e : targets) {
            float damage = e.getMaxHealth() * a.void_damage_percent;
            e.hurt(level.damageSources().magic(), damage);
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, a.void_levitation_duration, a.void_levitation_amp, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, a.void_wither_duration, a.void_wither_amp, false, true));
        }

        for (int i = 0; i < 400; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = level.random.nextDouble() * a.void_radius;
            double y = caster.getY() + level.random.nextDouble() * 30.0;
            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.55F, 0.1F, 0.65F), 2.5F),
                    caster.getX() + Math.cos(angle) * dist,
                    y,
                    caster.getZ() + Math.sin(angle) * dist,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void emitChannelParticles(ServerLevel level, ServerPlayer player,
                                              ApexType type, int progress) {
        float t = (float) progress / channelTicks();

        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f(type.r, type.g, type.b), 1.8F);
        long gt = level.getGameTime();
        int count = 12;
        double baseRadius = 3.0 - 2.5 * t;
        double height = 2.5 + 1.5 * (float) Math.sin(gt * 0.15);

        for (int i = 0; i < count; i++) {
            double angle = (gt * 0.05 + i * (Math.PI * 2 / count)) % (Math.PI * 2);
            double x = player.getX() + Math.cos(angle) * baseRadius;
            double y = player.getY() + 0.5 + (height * t);
            double z = player.getZ() + Math.sin(angle) * baseRadius;
            level.sendParticles(dust, x, y, z, 1, 0.02, 0.02, 0.02, 0.0);
        }

        if (t > 0.75F && progress % 3 == 0) {
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 2.2, player.getZ(),
                    3, 0.3, 0.1, 0.3, 0.05);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("apex.transcend." + type.id + ".desc")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("apex.transcend.gate.stage", REQUIRED_STAGE)
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("apex.transcend.gate.circle")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("apex.transcend.gate.crystals")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("apex.transcend.channel_time", channelTicks() / 20)
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("apex.transcend.cost_warning")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
