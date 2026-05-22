package com.huige233.transcend.ascension.tree;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.huige233.transcend.ascension.MageClass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class TreeDefinition {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ResourceLocation id;
    private final TreeType treeType;
    private final MageClass mageClass;
    private final String displayNameKey;
    private final String descriptionKey;
    private final int iconColor;
    private final Map<String, NodeDefinition> nodes;

    public TreeDefinition(ResourceLocation id, TreeType treeType, MageClass mageClass,
                          String displayNameKey, String descriptionKey, int iconColor,
                          Map<String, NodeDefinition> nodes) {
        this.id = id;
        this.treeType = treeType;
        this.mageClass = mageClass;
        this.displayNameKey = displayNameKey;
        this.descriptionKey = descriptionKey;
        this.iconColor = iconColor;
        this.nodes = Collections.unmodifiableMap(nodes);
    }

    public ResourceLocation getId() { return id; }
    public TreeType getTreeType() { return treeType; }
    public MageClass getMageClass() { return mageClass; }
    public String getDisplayNameKey() { return displayNameKey; }
    public String getDescriptionKey() { return descriptionKey; }
    public int getIconColor() { return iconColor; }
    public Map<String, NodeDefinition> getNodes() { return nodes; }

    public NodeDefinition getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public List<NodeDefinition> getNodesForTier(int tier) {
        List<NodeDefinition> list = new ArrayList<>();
        for (NodeDefinition n : nodes.values()) {
            if (n.getTier() == tier) list.add(n);
        }
        return list;
    }

    public List<NodeDefinition> getRootNodes() {
        List<NodeDefinition> list = new ArrayList<>();
        for (NodeDefinition n : nodes.values()) {
            if (n.getParents().isEmpty()) list.add(n);
        }
        return list;
    }

    public int getMaxTier() {
        int max = 0;
        for (NodeDefinition n : nodes.values()) {
            if (n.getTier() > max) max = n.getTier();
        }
        return max;
    }

    public static TreeDefinition fromJson(ResourceLocation id, JsonObject json) {
        TreeType treeType = TreeType.getById(json.get("tree_type").getAsString());
        if (treeType == null) throw new IllegalArgumentException("Unknown tree_type in " + id);

        MageClass mageClass = MageClass.getById(json.get("mage_class").getAsString());
        String displayNameKey = json.get("display_name").getAsString();
        String descriptionKey = json.get("description").getAsString();

        int iconColor = 0xFFFFFF;
        if (json.has("icon_color")) {
            String hex = json.get("icon_color").getAsString().replace("#", "");
            iconColor = Integer.parseInt(hex, 16);
        }

        Map<String, NodeDefinition> nodes = new LinkedHashMap<>();
        JsonObject nodesJson = json.getAsJsonObject("nodes");
        for (Map.Entry<String, JsonElement> entry : nodesJson.entrySet()) {
            String nodeId = entry.getKey();
            NodeDefinition node = NodeDefinition.fromJson(nodeId, entry.getValue().getAsJsonObject());
            node.setTreeId(id.toString());
            nodes.put(nodeId, node);
        }

        TreeDefinition tree = new TreeDefinition(id, treeType, mageClass,
                displayNameKey, descriptionKey, iconColor, nodes);
        tree.validate();
        return tree;
    }

    private void validate() {
        for (Map.Entry<String, NodeDefinition> entry : nodes.entrySet()) {
            NodeDefinition node = entry.getValue();
            for (String parentId : node.getParents()) {
                if (!nodes.containsKey(parentId)) {
                    LOGGER.warn("Tree {}: node '{}' references unknown parent '{}'",
                            id, node.getId(), parentId);
                }
            }
            if (!node.isTierFive() && node.getParents().size() > 1) {
                LOGGER.warn("Tree {}: non-T5 node '{}' has multiple parents (DAG only allowed for T5)",
                        id, node.getId());
            }
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeUtf(treeType.id);
        buf.writeUtf(mageClass.id);
        buf.writeUtf(displayNameKey);
        buf.writeUtf(descriptionKey);
        buf.writeInt(iconColor);
        buf.writeInt(nodes.size());
        for (NodeDefinition node : nodes.values()) {
            node.write(buf);
        }
    }

    public static TreeDefinition read(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        TreeType treeType = TreeType.getById(buf.readUtf());
        MageClass mageClass = MageClass.getById(buf.readUtf());
        String displayNameKey = buf.readUtf();
        String descriptionKey = buf.readUtf();
        int iconColor = buf.readInt();
        int nodeCount = buf.readInt();
        Map<String, NodeDefinition> nodes = new LinkedHashMap<>();
        for (int i = 0; i < nodeCount; i++) {
            NodeDefinition node = NodeDefinition.read(buf);
            node.setTreeId(id.toString());
            nodes.put(node.getId(), node);
        }
        return new TreeDefinition(id, treeType, mageClass,
                displayNameKey, descriptionKey, iconColor, nodes);
    }
}
