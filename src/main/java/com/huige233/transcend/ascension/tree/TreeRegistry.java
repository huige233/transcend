package com.huige233.transcend.ascension.tree;

import com.huige233.transcend.ascension.AscensionStatBlock;
import com.huige233.transcend.ascension.MageClass;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2CTreeSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class TreeRegistry {

    private static final TreeRegistry INSTANCE = new TreeRegistry();

    private TreeDefinition ascensionTree;
    private final Map<MageClass, TreeDefinition> talentTrees = new EnumMap<>(MageClass.class);
    private final Map<String, NodeDefinition> allNodes = new HashMap<>();

    private static final AscensionTreeLoader ASC_LOADER = new AscensionTreeLoader();
    private static final TalentTreeLoader TALENT_LOADER = new TalentTreeLoader();

    public static TreeRegistry getInstance() { return INSTANCE; }

    public void clear() {
        ascensionTree = null;
        talentTrees.clear();
        allNodes.clear();
    }

    public void register(TreeDefinition tree) {
        if (tree.getTreeType() == TreeType.ASCENSION) {
            ascensionTree = tree;
        } else {
            talentTrees.put(tree.getMageClass(), tree);
        }
        for (NodeDefinition node : tree.getNodes().values()) {
            allNodes.put(node.getId(), node);
        }
    }

    public NodeDefinition getNode(String id) {
        return allNodes.get(id);
    }

    public TreeDefinition getAscensionTree() {
        return ascensionTree;
    }

    public TreeDefinition getTalentTree(MageClass mc) {
        return talentTrees.get(mc);
    }

    public Collection<TreeDefinition> getAllTrees() {
        List<TreeDefinition> all = new ArrayList<>();
        if (ascensionTree != null) all.add(ascensionTree);
        all.addAll(talentTrees.values());
        return all;
    }

    /**
     * Global multiplier applied to all node stat_bonuses (data-driven JSON values).
     * v4: 降到 1.2 — fromLevel / ritual / mastery 已大幅强化,避免节点贡献过爆。
     */
    public static final float NODE_STAT_GLOBAL_MULT = 1.2f;

    public AscensionStatBlock computeNodeStats(Set<String> unlockedNodeIds, com.huige233.transcend.ascension.MageClass mageClass) {
        AscensionStatBlock block = new AscensionStatBlock();
        for (String nodeId : unlockedNodeIds) {
            NodeDefinition node = allNodes.get(nodeId);
            if (node == null) continue;
            for (Map.Entry<StatType, Float> entry : node.getStatBonuses().entrySet()) {
                entry.getKey().applyTo(block, entry.getValue() * NODE_STAT_GLOBAL_MULT);
            }
            if (mageClass != null && mageClass.isSelected()) {
                Map<String, Map<StatType, Float>> allScaling = node.getAllElementScaling();
                if (allScaling != null && !allScaling.isEmpty()) {
                    if ("omni".equals(mageClass.primaryElement)) {
                        Map<StatType, Float> averaged = new java.util.LinkedHashMap<>();
                        for (Map<StatType, Float> s : allScaling.values()) {
                            for (Map.Entry<StatType, Float> e : s.entrySet()) {
                                averaged.merge(e.getKey(), e.getValue(), Float::sum);
                            }
                        }
                        int count = allScaling.size();
                        for (Map.Entry<StatType, Float> e : averaged.entrySet()) {
                            e.getKey().applyTo(block, (e.getValue() / count) * NODE_STAT_GLOBAL_MULT);
                        }
                    } else {
                        Map<StatType, Float> scaling = allScaling.get(mageClass.primaryElement);
                        if (scaling != null) {
                            for (Map.Entry<StatType, Float> entry : scaling.entrySet()) {
                                entry.getKey().applyTo(block, entry.getValue() * NODE_STAT_GLOBAL_MULT);
                            }
                        }
                    }
                }
            }
        }
        return block;
    }

    public List<PassiveEffect> getActivePassives(Set<String> unlockedNodeIds) {
        List<PassiveEffect> effects = new ArrayList<>();
        for (String nodeId : unlockedNodeIds) {
            NodeDefinition node = allNodes.get(nodeId);
            if (node == null) continue;
            effects.addAll(node.getPassiveEffects());
        }
        return effects;
    }

    public boolean isLoaded() {
        return ascensionTree != null;
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ASC_LOADER);
        event.addListener(TALENT_LOADER);
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && INSTANCE.isLoaded()) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new S2CTreeSync(INSTANCE.getAllTrees()));
        }
    }

    /**
     * 启动期 sanity check — 校验所有节点的 name_key/desc_key 在 client lang 表里都有值。
     * 缺失 = 玩家会看到空白节点 (omnimancer v13 bug 同款)。
     *
     * <p>仅客户端调用 (lang 表服务端拿不到)。每次资源 reload 后跑一次。
     */
    public void auditLangCompleteness() {
        org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("TranscendTreeAudit");
        int missing = 0;
        java.util.List<String> details = new java.util.ArrayList<>();
        for (NodeDefinition node : allNodes.values()) {
            String nameKey = node.getNameKey();
            String descKey = node.getDescKey();
            if (!net.minecraft.client.resources.language.I18n.exists(nameKey)) {
                missing++;
                details.add("name " + nameKey);
            }
            if (!net.minecraft.client.resources.language.I18n.exists(descKey)) {
                missing++;
                details.add("desc " + descKey);
            }
        }
        if (missing > 0) {
            log.warn("[Transcend] Tree lang audit: {} missing keys. Affected nodes will show empty text.", missing);
            for (String d : details) log.warn("  - {}", d);
        } else {
            log.info("[Transcend] Tree lang audit: all {} nodes have complete name+desc lang values.", allNodes.size());
        }
    }
}
