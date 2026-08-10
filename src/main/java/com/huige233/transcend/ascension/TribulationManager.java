package com.huige233.transcend.ascension;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.spell.SpellDamageMath;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 天劫(渡劫)流程管理工具类。 */
public final class TribulationManager {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private TribulationManager() {}

    public static boolean start(ServerPlayer player) {
        return start(player, false);
    }

    public static boolean startForTesting(ServerPlayer player) {
        return start(player, true);
    }

    private static boolean start(ServerPlayer player, boolean bypassReadiness) {
        PlayerAscensionData data = AscensionCapability.get(player);
        CultivationRealm current = data.getCultivationRealm();
        if (SESSIONS.containsKey(player.getUUID()) || data.isTribulationActive()
                || data.hasCultivationDeviation() || current == CultivationRealm.CHAOS
                || (!bypassReadiness && !data.isReadyForTribulation())) {
            return false;
        }

        CultivationRealm target = current.next();
        Vec3 center = player.position();
        String dimension = player.level().dimension().location().toString();
        data.beginTribulation(target, dimension, center.x, center.y, center.z);
        data.setAuraGuardEnabled(false);

        Session session = new Session(player.getUUID(), player.level().dimension(), center, target);
        SESSIONS.put(player.getUUID(), session);
        spawnCloud((ServerLevel) player.level(), session);
        scheduleRound(player, data, session);
        AscensionHandler.syncToClient(player, data);
        player.sendSystemMessage(Component.translatable("msg.transcend.tribulation.started",
                Component.translatable(target.getDisplayKey())).withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    public static boolean cancel(ServerPlayer player) {
        PlayerAscensionData data = AscensionCapability.get(player);
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null && !data.isTribulationActive()) return false;
        removeCloud(player.server, session);
        data.clearTribulation();
        AscensionHandler.syncToClient(player, data);
        player.sendSystemMessage(Component.translatable("msg.transcend.tribulation.cancelled")
                .withStyle(ChatFormatting.YELLOW));
        return true;
    }

    public static Component status(ServerPlayer player) {
        PlayerAscensionData data = AscensionCapability.get(player);
        if (!data.isTribulationActive()) {
            return Component.translatable("msg.transcend.tribulation.status.inactive",
                    data.getCultivationDeviationTicks() / 20);
        }
        return Component.translatable("msg.transcend.tribulation.status.active",
                Component.translatable(data.getTribulationTarget().getDisplayKey()),
                data.getTribulationElapsedTicks(), TribulationRules.SESSION_TICKS,
                data.getTribulationRounds(), TribulationRules.TOTAL_ROUNDS);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) return;

        for (Session session : new ArrayList<>(SESSIONS.values())) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(session.playerId);
            if (player == null) continue;
            PlayerAscensionData data = AscensionCapability.get(player);

            if (!data.isTribulationActive()) {
                removeCloud(event.getServer(), session);
                SESSIONS.remove(session.playerId, session);
                continue;
            }
            if (!player.isAlive()) {
                fail(player, data, session, "death");
                SESSIONS.remove(session.playerId, session);
                continue;
            }
            if (player.level().dimension() != session.dimension) {
                fail(player, data, session, "dimension");
                SESSIONS.remove(session.playerId, session);
                continue;
            }
            if (player.position().distanceToSqr(session.center)
                    > TribulationRules.ARENA_RADIUS * TribulationRules.ARENA_RADIUS) {
                fail(player, data, session, "left_arena");
                SESSIONS.remove(session.playerId, session);
                continue;
            }

            session.elapsedTicks++;
            processImpacts(player, data, session);
            if (!data.isTribulationActive()) {
                SESSIONS.remove(session.playerId, session);
                continue;
            }
            if (session.elapsedTicks < TribulationRules.SESSION_TICKS
                    && session.elapsedTicks % TribulationRules.ROUND_INTERVAL_TICKS == 0) {
                scheduleRound(player, data, session);
            }
            data.updateTribulationProgress(session.elapsedTicks, session.rounds);
            ensureCloud((ServerLevel) player.level(), session);

            if (session.elapsedTicks >= TribulationRules.SESSION_TICKS) {
                succeed(player, data, session);
                SESSIONS.remove(session.playerId, session);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.tickCultivationDeviation()) {
            AscensionHandler.applyPersistentStats(player, data);
            AscensionHandler.syncToClient(player, data);
            player.sendSystemMessage(Component.translatable("msg.transcend.tribulation.deviation_cleared")
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) failActive(player, "death");
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) failActive(player, "logout");
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) failActive(player, "dimension");
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerAscensionData data = AscensionCapability.get(player);
        if (data.isTribulationActive()) {

            Session staleSession = SESSIONS.remove(player.getUUID());
            fail(player, data, staleSession, "interrupted");
        }
    }

    private static void failActive(ServerPlayer player, String reason) {
        PlayerAscensionData data = AscensionCapability.get(player);
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null || data.isTribulationActive()) fail(player, data, session, reason);
    }

    private static void succeed(ServerPlayer player, PlayerAscensionData data, Session session) {
        removeCloud(player.server, session);
        data.setCultivationProgress(session.target, CultivationStage.EARLY, 0L);
        data.addTalentPoints(2);
        data.clearTribulation();
        AscensionHandler.applyPersistentStats(player, data);
        AscensionHandler.syncToClient(player, data);
        player.sendSystemMessage(Component.translatable("msg.transcend.tribulation.success",
                Component.translatable(session.target.getDisplayKey())).withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void fail(ServerPlayer player, PlayerAscensionData data, Session session, String reason) {
        removeCloud(player.server, session);
        CultivationRealm target = session != null ? session.target : data.getTribulationTarget();
        float penalty = TribulationRules.peakXpPenaltyFraction(player.getRandom().nextFloat());
        data.losePeakCultivationXP(penalty);
        data.applyCultivationDeviation(TribulationRules.deviationDurationTicks(target.getRank()));
        data.clearTribulation();
        AscensionHandler.applyPersistentStats(player, data);
        AscensionHandler.syncToClient(player, data);
        player.sendSystemMessage(Component.translatable("msg.transcend.tribulation.failed",
                Component.translatable("msg.transcend.tribulation.reason." + reason),
                Math.round(penalty * 100.0F), data.getCultivationDeviationTicks() / 20)
                .withStyle(ChatFormatting.RED));
    }

    private static void scheduleRound(ServerPlayer player, PlayerAscensionData data, Session session) {
        if (session.rounds >= TribulationRules.TOTAL_ROUNDS) return;
        StrikeElement strike = chooseElement(data.getMastery(), session.target.getRank(), player);
        SpellElement element = strike.element();
        double angle = player.getRandom().nextDouble() * Math.PI * 2.0D;
        double radius = player.getRandom().nextDouble() * 4.5D;
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * radius);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * radius);
        double y = findWarningY((ServerLevel) player.level(), x, player.blockPosition().getY(), z);
        Vec3 impact = new Vec3(x + 0.5D, y, z + 0.5D);
        session.impacts.add(new PendingImpact(session.elapsedTicks + TribulationRules.WARNING_TICKS,
                impact, element, strike.chaos()));
        session.rounds++;
        data.updateTribulationProgress(session.elapsedTicks, session.rounds);

        Vector3f color = new Vector3f(element.getParticleR(), element.getParticleG(), element.getParticleB());
        ((ServerLevel) player.level()).sendParticles(new DustParticleOptions(color, 1.5F),
                impact.x, impact.y + 0.1D, impact.z, 30, 1.4D, 0.05D, 1.4D, 0.0D);
        player.level().playSound(null, BlockPos.containing(impact), SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.HOSTILE, 1.0F, 0.6F);
        player.displayClientMessage(Component.translatable("msg.transcend.tribulation.warning",
                Component.translatable(element.getDisplayKey())).withStyle(ChatFormatting.RED), true);
    }

    private static void processImpacts(ServerPlayer player, PlayerAscensionData data, Session session) {
        Iterator<PendingImpact> iterator = session.impacts.iterator();
        while (iterator.hasNext()) {
            PendingImpact pending = iterator.next();
            if (pending.dueTick > session.elapsedTicks) continue;
            iterator.remove();
            ServerLevel level = (ServerLevel) player.level();
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(pending.position);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pending.position.x, pending.position.y + 0.2D,
                    pending.position.z, 35, 0.8D, 1.2D, 0.8D, 0.15D);

            if (player.position().distanceToSqr(pending.position) <= 12.25D) {
                float resistance = (pending.chaos ? SpellDamageMath.CHAOS_BASE_RESISTANCE : SpellDamageMath.BASE_RESISTANCE)
                        + data.getElementResistanceBonus(pending.element);
                float baseDamage = 5.0F + session.target.getRank() * 1.5F;
                float damage = SpellDamageMath.applyElementalResistance(baseDamage, resistance);
                player.hurt(level.damageSources().lightningBolt(), damage);
            }
        }
    }

    private static StrikeElement chooseElement(ElementMastery mastery, int targetRank, ServerPlayer player) {
        SpellElement[] five = {SpellElement.METAL, SpellElement.WOOD, SpellElement.WATER,
                SpellElement.FIRE, SpellElement.EARTH};
        if (targetRank >= 7) {
            boolean chaos = player.getRandom().nextInt(4) == 0;
            return new StrikeElement(five[player.getRandom().nextInt(five.length)], chaos);
        }
        if (mastery != null && mastery.isSpecific()) {
            SpellElement counter = switch (mastery.element) {
                case METAL -> SpellElement.FIRE;
                case WOOD -> SpellElement.METAL;
                case WATER -> SpellElement.EARTH;
                case FIRE -> SpellElement.WATER;
                case EARTH -> SpellElement.WOOD;
                case CHAOS -> five[player.getRandom().nextInt(five.length)];
            };
            return new StrikeElement(counter, false);
        }
        return new StrikeElement(five[player.getRandom().nextInt(five.length)], false);
    }

    private static double findWarningY(ServerLevel level, int x, int startY, int z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, startY, z);
        for (int i = 0; i < 24 && cursor.getY() > level.getMinBuildHeight(); i++, cursor.move(0, -1, 0)) {
            if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) return cursor.getY() + 1.05D;
        }
        return startY + 0.05D;
    }

    private static void spawnCloud(ServerLevel level, Session session) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, session.center.x, session.center.y + 12.0D, session.center.z);
        cloud.setRadius(6.0F);
        cloud.setDuration(TribulationRules.SESSION_TICKS + 100);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick(0.0F);
        cloud.setParticle(ParticleTypes.CLOUD);
        cloud.getPersistentData().putBoolean("transcend_tribulation_cloud", true);
        level.addFreshEntity(cloud);
        session.cloudId = cloud.getUUID();
    }

    private static void ensureCloud(ServerLevel level, Session session) {
        Entity cloud = session.cloudId == null ? null : level.getEntity(session.cloudId);
        if (cloud == null || !cloud.isAlive()) spawnCloud(level, session);
    }

    private static void removeCloud(net.minecraft.server.MinecraftServer server, Session session) {
        if (session == null || session.cloudId == null) return;
        ServerLevel level = server.getLevel(session.dimension);
        if (level == null) return;
        Entity cloud = level.getEntity(session.cloudId);
        if (cloud != null) cloud.discard();
    }

    private static final class Session {
        private final UUID playerId;
        private final ResourceKey<Level> dimension;
        private final Vec3 center;
        private final CultivationRealm target;
        private final List<PendingImpact> impacts = new ArrayList<>();
        private UUID cloudId;
        private int elapsedTicks;
        private int rounds;

        private Session(UUID playerId, ResourceKey<Level> dimension, Vec3 center, CultivationRealm target) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.center = center;
            this.target = target;
        }
    }

    private record StrikeElement(SpellElement element, boolean chaos) {}

    private record PendingImpact(int dueTick, Vec3 position, SpellElement element, boolean chaos) {}
}
