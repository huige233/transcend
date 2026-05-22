package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.server.level.ServerLevel;

/**
 * 御天敕令（Weather Edict）功能执行器。
 * <p>
 * 在范围内进行天气改变。激活后先进入 60 秒"咏唱"阶段（仅计时，无即时效果），
 * 然后调用 {@link ServerLevel#setWeatherParameters(int, int, int, boolean, boolean)}
 * 锁定一段时间的天气。当激活期间，会持续刷新天气计时器以维持目标天气。
 *
 * <p>当前简化策略：
 * <ul>
 *     <li>T3：强制晴天。</li>
 *     <li>T4：强制下雨。</li>
 *     <li>T5：强制雷暴。</li>
 * </ul>
 * 后续可改为通过催化剂 / GUI 选项让玩家自由选择。
 */
public class WeatherEdictExecutor implements CircleFunctionExecutor {

    /** 咏唱阶段时长（tick），60 秒 × 20 tick。 */
    private static final int CHANNEL_DURATION_TICKS = 60 * 20;
    /** 每次刷新维持的天气持续时间（tick），略大于 tick 调用间隔即可。 */
    private static final int WEATHER_REFRESH_TICKS = 60 * 20;

    /** 咏唱计时器（按 tick 调用累加，每次累加 20）。 */
    private int channelTimer = 0;
    /** 是否已经完成咏唱并进入维持阶段。 */
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

        // 维持阶段：持续刷新天气参数以避免被自然变化推翻
        applyWeather(level, ctx.getTier());
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        channelTimer = 0;
        channelComplete = false;
        // 停用时让世界天气恢复自然变化（无需特别清理）。
    }

    /** 根据当前层级选择目标天气并强制设置。 */
    private void applyWeather(ServerLevel level, CircleTier tier) {
        switch (tier) {
            case PRIMORDIAL:
                // T5：雷暴
                level.setWeatherParameters(0, WEATHER_REFRESH_TICKS, true, true);
                break;
            case ARCHON:
                // T4：下雨
                level.setWeatherParameters(0, WEATHER_REFRESH_TICKS, true, false);
                break;
            case MASTER:
            default:
                // T3：晴天
                level.setWeatherParameters(WEATHER_REFRESH_TICKS, 0, false, false);
                break;
        }
    }
}
