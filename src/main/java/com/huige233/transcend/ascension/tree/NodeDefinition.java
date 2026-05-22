package com.huige233.transcend.ascension.tree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.*;

public class NodeDefinition {

    private final String id;
    private final int tier;
    private final int cost;
    private final ChatFormatting color;
    private final String nameKey;
    private final String descKey;
    private final List<String> parents;
    private final int posX;
    private final int posY;
    private final Map<StatType, Float> statBonuses;
    private final List<PassiveEffect> passiveEffects;
    private final Map<String, Map<StatType, Float>> elementScaling;

    private String treeId;

    public NodeDefinition(String id, int tier, int cost, ChatFormatting color,
                          String nameKey, String descKey, List<String> parents,
                          int posX, int posY,
                          Map<StatType, Float> statBonuses,
                          List<PassiveEffect> passiveEffects,
                          Map<String, Map<StatType, Float>> elementScaling) {
        this.id = id;
        this.tier = tier;
        this.cost = cost;
        this.color = color;
        this.nameKey = nameKey;
        this.descKey = descKey;
        this.parents = List.copyOf(parents);
        this.posX = posX;
        this.posY = posY;
        this.statBonuses = Map.copyOf(statBonuses);
        this.passiveEffects = List.copyOf(passiveEffects);
        this.elementScaling = elementScaling;
    }

    public String getId() { return id; }
    public int getTier() { return tier; }
    public int getCost() { return cost; }
    public ChatFormatting getColor() { return color; }
    public String getNameKey() { return nameKey; }
    public String getDescKey() { return descKey; }
    public List<String> getParents() { return parents; }
    public int getPosX() { return posX; }
    public int getPosY() { return posY; }
    public Map<StatType, Float> getStatBonuses() { return statBonuses; }
    public List<PassiveEffect> getPassiveEffects() { return passiveEffects; }
    public Map<String, Map<StatType, Float>> getAllElementScaling() { return elementScaling; }
    public Map<StatType, Float> getElementScaling(String masteryId) {
        return elementScaling != null ? elementScaling.get(masteryId) : null;
    }

    public String getTreeId() { return treeId; }
    public void setTreeId(String treeId) { this.treeId = treeId; }

    public boolean isTierFive() { return tier == 5; }

    public Component getDisplayName() {
        return Component.translatable(nameKey).withStyle(color);
    }

    public Component getDescription() {
        return Component.translatable(descKey).withStyle(ChatFormatting.GRAY);
    }

    public static NodeDefinition fromJson(String nodeId, JsonObject json) {
        int tier = json.get("tier").getAsInt();
        int cost = json.get("cost").getAsInt();
        ChatFormatting color = ChatFormatting.getByName(json.get("color").getAsString());
        if (color == null) color = ChatFormatting.WHITE;

        String nameKey = json.has("name_key") ? json.get("name_key").getAsString()
                : "node.transcend." + nodeId;
        String descKey = json.has("desc_key") ? json.get("desc_key").getAsString()
                : "node.transcend." + nodeId + ".desc";

        List<String> parents = new ArrayList<>();
        if (json.has("parents")) {
            for (JsonElement el : json.getAsJsonArray("parents")) {
                parents.add(el.getAsString());
            }
        }

        int posX = 0, posY = 0;
        if (json.has("position")) {
            JsonObject pos = json.getAsJsonObject("position");
            posX = pos.get("x").getAsInt();
            posY = pos.get("y").getAsInt();
        }

        Map<StatType, Float> statBonuses = new LinkedHashMap<>();
        if (json.has("stat_bonuses")) {
            JsonObject sb = json.getAsJsonObject("stat_bonuses");
            for (Map.Entry<String, JsonElement> entry : sb.entrySet()) {
                StatType type = StatType.getByKey(entry.getKey());
                if (type != null) {
                    statBonuses.put(type, entry.getValue().getAsFloat());
                }
            }
        }

        List<PassiveEffect> passiveEffects = new ArrayList<>();
        if (json.has("passive_effects")) {
            passiveEffects = PassiveEffect.listFromJson(json.getAsJsonArray("passive_effects"));
        }

        Map<String, Map<StatType, Float>> elementScaling = new LinkedHashMap<>();
        if (json.has("element_scaling")) {
            JsonObject esJson = json.getAsJsonObject("element_scaling");
            for (Map.Entry<String, JsonElement> esEntry : esJson.entrySet()) {
                String elementId = esEntry.getKey();
                Map<StatType, Float> bonuses = new LinkedHashMap<>();
                JsonObject bonusObj = esEntry.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> bEntry : bonusObj.entrySet()) {
                    StatType type = StatType.getByKey(bEntry.getKey());
                    if (type != null) bonuses.put(type, bEntry.getValue().getAsFloat());
                }
                elementScaling.put(elementId, bonuses);
            }
        }

        return new NodeDefinition(nodeId, tier, cost, color, nameKey, descKey,
                parents, posX, posY, statBonuses, passiveEffects, elementScaling);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(id);
        buf.writeInt(tier);
        buf.writeInt(cost);
        buf.writeUtf(color.getName());
        buf.writeUtf(nameKey);
        buf.writeUtf(descKey);
        buf.writeInt(parents.size());
        for (String p : parents) buf.writeUtf(p);
        buf.writeInt(posX);
        buf.writeInt(posY);
        buf.writeInt(statBonuses.size());
        for (Map.Entry<StatType, Float> entry : statBonuses.entrySet()) {
            buf.writeUtf(entry.getKey().jsonKey);
            buf.writeFloat(entry.getValue());
        }
        buf.writeInt(elementScaling.size());
        for (Map.Entry<String, Map<StatType, Float>> esEntry : elementScaling.entrySet()) {
            buf.writeUtf(esEntry.getKey());
            buf.writeInt(esEntry.getValue().size());
            for (Map.Entry<StatType, Float> entry : esEntry.getValue().entrySet()) {
                buf.writeUtf(entry.getKey().jsonKey);
                buf.writeFloat(entry.getValue());
            }
        }
    }

    public static NodeDefinition read(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        int tier = buf.readInt();
        int cost = buf.readInt();
        ChatFormatting color = ChatFormatting.getByName(buf.readUtf());
        if (color == null) color = ChatFormatting.WHITE;
        String nameKey = buf.readUtf();
        String descKey = buf.readUtf();
        int parentCount = buf.readInt();
        List<String> parents = new ArrayList<>();
        for (int i = 0; i < parentCount; i++) parents.add(buf.readUtf());
        int posX = buf.readInt();
        int posY = buf.readInt();
        int statCount = buf.readInt();
        Map<StatType, Float> stats = new LinkedHashMap<>();
        for (int i = 0; i < statCount; i++) {
            StatType type = StatType.getByKey(buf.readUtf());
            float val = buf.readFloat();
            if (type != null) stats.put(type, val);
        }
        int esCount = buf.readInt();
        Map<String, Map<StatType, Float>> elementScaling = new LinkedHashMap<>();
        for (int i = 0; i < esCount; i++) {
            String elemId = buf.readUtf();
            int bonusCount = buf.readInt();
            Map<StatType, Float> bonuses = new LinkedHashMap<>();
            for (int j = 0; j < bonusCount; j++) {
                StatType type = StatType.getByKey(buf.readUtf());
                float val = buf.readFloat();
                if (type != null) bonuses.put(type, val);
            }
            elementScaling.put(elemId, bonuses);
        }
        return new NodeDefinition(id, tier, cost, color, nameKey, descKey,
                parents, posX, posY, stats, List.of(), elementScaling);
    }
}
