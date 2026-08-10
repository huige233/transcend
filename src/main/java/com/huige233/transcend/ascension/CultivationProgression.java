package com.huige233.transcend.ascension;

/** 修炼进度计算工具类。 */
public final class CultivationProgression {
    private CultivationProgression() {}

    public record State(CultivationStage stage, long xp, long peakXp) {
        public State {
            stage = stage == null ? CultivationStage.EARLY : stage;
            xp = Math.max(0L, xp);
            peakXp = Math.max(xp, peakXp);
        }
    }

    public static long xpRequired(int realmRank, CultivationStage stage) {
        long rank = Math.max(1, realmRank);
        long rankSquared = rank * rank;
        return switch (stage == null ? CultivationStage.EARLY : stage) {
            case EARLY -> 500L * rankSquared;
            case MIDDLE -> 750L * rankSquared;
            case LATE -> 1000L * rankSquared;
        };
    }

    public static long killXp(float targetMaxHealth, boolean isBoss) {
        long base = Math.max(1L, Math.min(100L, (long) Math.ceil(targetMaxHealth / 10.0F)));
        return base * (isBoss ? 5L : 1L);
    }

    public static State restore(int realmRank, CultivationStage stage, long xp, long peakXp) {
        CultivationStage restoredStage = stage == null ? CultivationStage.EARLY : stage;
        long restoredXp = Math.max(0L, xp);
        long required = xpRequired(realmRank, restoredStage);
        if (restoredXp < required || restoredStage == CultivationStage.LATE) {
            restoredXp = Math.min(restoredXp, required);
            return new State(restoredStage, restoredXp,
                    Math.min(required, Math.max(restoredXp, peakXp)));
        }

        return addXp(realmRank, new State(restoredStage, 0L, 0L), restoredXp);
    }

    public static State addXp(int realmRank, State current, long amount) {
        if (amount <= 0L) return current;

        CultivationStage stage = current.stage();
        long xp = current.xp();
        long peak = current.peakXp();
        long remaining = amount;
        while (remaining > 0L) {
            long required = xpRequired(realmRank, stage);
            long room = required - xp;
            if (stage == CultivationStage.LATE) {
                long gained = Math.min(room, remaining);
                xp += gained;
                peak = Math.max(peak, xp);
                break;
            }
            if (remaining < room) {
                xp += remaining;
                peak = Math.max(peak, xp);
                break;
            }

            remaining -= room;
            stage = stage.next();
            xp = 0L;
            peak = 0L;
        }
        return new State(stage, xp, peak);
    }

    public static boolean isReadyForTribulation(int realmRank, State state) {
        return state.stage() == CultivationStage.LATE
                && state.xp() >= xpRequired(realmRank, CultivationStage.LATE);
    }

    public static State applyPeakXpLoss(State current, float fraction) {
        float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
        long loss = (long) Math.floor(current.peakXp() * clamped);
        return new State(current.stage(), Math.max(0L, current.xp() - loss), current.peakXp());
    }
}
