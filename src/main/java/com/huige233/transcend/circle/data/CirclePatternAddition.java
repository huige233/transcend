package com.huige233.transcend.circle.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.huige233.transcend.circle.CircleStructurePattern;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 数据驱动的法环结构追加条目。每个 JSON 文件目标一个 CircleTier，附加额外的位置要求。
 * 与 Java 默认 {@link CircleStructurePattern} 合并（追加，不替换）。
 *
 * <p>JSON Schema:
 * <pre>{@code
 * {
 *   "tier": "initiate",   // initiate | adept | master | archon | primordial
 *   "entries": [
 *     { "pos": [dx, dy, dz], "role": "RUNE", "min_tier": 1 },
 *     ...
 *   ]
 * }
 * }</pre>
 *
 * <p>注意：追加条目会作为 <strong>新的硬性要求</strong> 出现，
 * 已有玩家的此 tier 法环可能因此失效。整合包作者请谨慎使用。
 */
public record CirclePatternAddition(
        ResourceLocation id,
        CircleTier tier,
        List<CircleStructurePattern.PatternEntry> entries
) {

    public static CirclePatternAddition fromJson(ResourceLocation id, JsonObject json) {
        String tierStr = GsonHelper.getAsString(json, "tier").toUpperCase(Locale.ROOT);
        CircleTier tier;
        try {
            tier = CircleTier.valueOf(tierStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown circle tier '" + tierStr +
                    "' in " + id + " (expected one of: INITIATE, ADEPT, MASTER, ARCHON, PRIMORDIAL)");
        }

        JsonArray arr = GsonHelper.getAsJsonArray(json, "entries");
        List<CircleStructurePattern.PatternEntry> list = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            JsonObject entry = arr.get(i).getAsJsonObject();
            JsonArray pos = GsonHelper.getAsJsonArray(entry, "pos");
            if (pos.size() != 3) {
                throw new IllegalArgumentException("Entry " + i + " in " + id + " has invalid 'pos' (expected [dx,dy,dz])");
            }
            int dx = pos.get(0).getAsInt();
            int dy = pos.get(1).getAsInt();
            int dz = pos.get(2).getAsInt();
            String roleStr = GsonHelper.getAsString(entry, "role").toUpperCase(Locale.ROOT);
            CircleStructurePattern.BlockRole role;
            try {
                role = CircleStructurePattern.BlockRole.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown role '" + roleStr + "' in entry " + i + " of " + id);
            }
            int minTier = GsonHelper.getAsInt(entry, "min_tier", tier.getLevel());
            list.add(new CircleStructurePattern.PatternEntry(dx, dy, dz, role, minTier));
        }
        return new CirclePatternAddition(id, tier, List.copyOf(list));
    }
}
