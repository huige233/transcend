package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SkyMantleExecutor implements CircleFunctionExecutor {

    private static final int SLOW_FALLING_DURATION_TICKS = 120;

    private static final double CM_PER_FLYER_PER_MIN = 6.0;

    private static final double TICKS_PER_MIN = 60.0;

    private final Set<UUID> grantedFlyers = new HashSet<>();

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        grantedFlyers.clear();
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        List<Player> inRadius = getPlayersInRadius(ctx);
        Set<UUID> currentInRadius = new HashSet<>();
        for (Player p : inRadius) {
            currentInRadius.add(p.getUUID());
        }

        List<UUID> toRevoke = new ArrayList<>();
        for (UUID uuid : grantedFlyers) {
            if (!currentInRadius.contains(uuid)) {
                toRevoke.add(uuid);
            }
        }
        for (UUID uuid : toRevoke) {
            Player player = level.getPlayerByUUID(uuid);
            revokeFlight(player);
            grantedFlyers.remove(uuid);
        }

        for (Player player : inRadius) {
            if (player.isCreative() || player.isSpectator()) {

                continue;
            }
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                if (player instanceof ServerPlayer sp) {
                    sp.onUpdateAbilities();
                }
            }
            grantedFlyers.add(player.getUUID());
        }

        int flyerCount = grantedFlyers.size();
        if (flyerCount > 0) {
            double cmPerTickCall = (CM_PER_FLYER_PER_MIN * flyerCount) / TICKS_PER_MIN;
            int whole = (int) Math.floor(cmPerTickCall);
            double frac = cmPerTickCall - whole;
            if (level.getRandom().nextDouble() < frac) {
                whole += 1;
            }
            if (whole > 0) {

                if (!ctx.consumeMana(whole)) {
                    revokeAll(level);
                }
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            grantedFlyers.clear();
            return;
        }
        revokeAll(level);
    }

    private void revokeAll(ServerLevel level) {
        for (UUID uuid : grantedFlyers) {
            Player player = level.getPlayerByUUID(uuid);
            revokeFlight(player);
        }
        grantedFlyers.clear();
    }

    private void revokeFlight(Player player) {
        if (player == null) {
            return;
        }
        if (player.isCreative() || player.isSpectator()) {

            return;
        }
        if (player.getAbilities().mayfly) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            if (player instanceof ServerPlayer sp) {
                sp.onUpdateAbilities();
            }
        }
        player.addEffect(new MobEffectInstance(
                MobEffects.SLOW_FALLING,
                SLOW_FALLING_DURATION_TICKS,
                0,
                true,
                false,
                true
        ));
    }

    private List<Player> getPlayersInRadius(CircleFunctionContext ctx) {
        double r = ctx.getBaseRadius();
        BlockPos pos = ctx.getCorePos();
        AABB area = new AABB(
                pos.getX() - r, pos.getY() - 2, pos.getZ() - r,
                pos.getX() + r, pos.getY() + 4, pos.getZ() + r
        );
        return ctx.getLevel().getEntitiesOfClass(Player.class, area);
    }
}
