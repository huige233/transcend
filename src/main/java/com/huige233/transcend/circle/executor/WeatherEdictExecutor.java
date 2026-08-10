package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.server.level.ServerLevel;

/** 天气诏令法阵功能执行器。 */
public class WeatherEdictExecutor implements CircleFunctionExecutor {

    private static final int CHANNEL_DURATION_TICKS = 60 * 20;

    private static final int WEATHER_REFRESH_TICKS = 60 * 20;

    private int channelTimer = 0;

    private boolean channelComplete = false;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        channelTimer = 0;
        channelComplete = false;
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        ServerLevel level = ctx.getLevel();
        if (level == null) {
            return;
        }

        if (!channelComplete) {
            channelTimer += 20;
            if (channelTimer >= CHANNEL_DURATION_TICKS) {
                channelComplete = true;
                applyWeather(level, ctx.getTier());
            }
            return;
        }

        applyWeather(level, ctx.getTier());
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        channelTimer = 0;
        channelComplete = false;

    }

    private void applyWeather(ServerLevel level, CircleTier tier) {
        switch (tier) {
            case PRIMORDIAL:

                level.setWeatherParameters(0, WEATHER_REFRESH_TICKS, true, true);
                break;
            case ARCHON:

                level.setWeatherParameters(0, WEATHER_REFRESH_TICKS, true, false);
                break;
            case MASTER:
            default:

                level.setWeatherParameters(WEATHER_REFRESH_TICKS, 0, false, false);
                break;
        }
    }
}
