package com.huige233.transcend.ascension.tree;

import com.huige233.transcend.ascension.AscensionStatBlock;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.AscensionHandler;
import com.huige233.transcend.ascension.MageClass;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2CTreeSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

/** 天赋树注册表。 */
public class TreeRegistry {

    private static final TreeRegistry INSTANCE = new TreeRegistry();
    private static final Set<String> ELEMENT_IDS = Set.of("metal", "wood", "water", "fire", "earth", "chaos");

    private TreeDefinition ascensionTree;
    private final Map<MageClass, TreeDefinition> talentTrees = new EnumMap<>(MageClass.class);
    private final Map<String, NodeDefinition> allNodes = new HashMap<>();
    private final Map<String, TreeDefinition> nodeOwners = new HashMap<>();

    private static final AscensionTreeLoader ASC_LOADER = new AscensionTreeLoader();
    private static final TalentTreeLoader TALENT_LOADER = new TalentTreeLoader();

    public static TreeRegistry getInstance() { return INSTANCE; }

    public synchronized void clear() {
        ascensionTree = null;
        talentTrees.clear();
        allNodes.clear();
        nodeOwners.clear();
    }

    public synchronized void replaceAscensionTrees(Collection<TreeDefinition> trees) {
        applyValidatedState(trees, talentTrees.values());
    }

    public synchronized void replaceTalentTrees(Collection<TreeDefinition> trees) {
        applyValidatedState(ascensionTree == null ? List.of() : List.of(ascensionTree), trees);
    }

    public synchronized void replaceAll(Collection<TreeDefinition> trees) {
        List<TreeDefinition> ascension = new ArrayList<>();
        List<TreeDefinition> talents = new ArrayList<>();
        for (TreeDefinition tree : trees) {
            (tree.getTreeType() == TreeType.ASCENSION ? ascension : talents).add(tree);
        }
        applyValidatedState(ascension, talents);
    }

    private void applyValidatedState(Collection<TreeDefinition> ascensionTrees,
                                     Collection<TreeDefinition> newTalentTrees) {
        if (ascensionTrees.size() > 1) {
            throw new IllegalArgumentException("Multiple universal ascension trees are not allowed");
        }

        TreeDefinition newAscensionTree = null;
        Map<MageClass, TreeDefinition> talents = new EnumMap<>(MageClass.class);
        Map<String, NodeDefinition> nodes = new HashMap<>();
        Map<String, TreeDefinition> owners = new HashMap<>();

        for (TreeDefinition tree : ascensionTrees) {
            if (tree.getTreeType() != TreeType.ASCENSION) {
                throw new IllegalArgumentException("Non-ascension tree in ascension registry: " + tree.getId());
            }
            newAscensionTree = tree;
            indexNodes(tree, nodes, owners);
        }
        for (TreeDefinition tree : newTalentTrees) {
            if (tree.getTreeType() != TreeType.TALENT) {
                throw new IllegalArgumentException("Non-talent tree in talent registry: " + tree.getId());
            }
            if (tree.getMageClass() == MageClass.NONE) {
                throw new IllegalArgumentException("Talent tree has unknown or NONE mage_class: " + tree.getId());
            }
            TreeDefinition previous = talents.putIfAbsent(tree.getMageClass(), tree);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate talent tree ownership for " + tree.getMageClass().id
                        + ": " + previous.getId() + " and " + tree.getId());
            }
            indexNodes(tree, nodes, owners);
        }

        ascensionTree = newAscensionTree;
        talentTrees.clear();
        talentTrees.putAll(talents);
        allNodes.clear();
        allNodes.putAll(nodes);
        nodeOwners.clear();
        nodeOwners.putAll(owners);
    }

    private static void indexNodes(TreeDefinition tree, Map<String, NodeDefinition> nodes,
                                   Map<String, TreeDefinition> owners) {
        tree.validate();
        for (NodeDefinition node : tree.getNodes().values()) {
            for (String elementId : node.getAllElementScaling().keySet()) {
                if (!ELEMENT_IDS.contains(elementId)) {
                    throw new IllegalArgumentException("Node '" + node.getId() + "' in " + tree.getId()
                            + " uses non-canonical element_scaling key '" + elementId + "'");
                }
            }
            TreeDefinition previousOwner = owners.putIfAbsent(node.getId(), tree);
            if (previousOwner != null) {
                throw new IllegalArgumentException("Duplicate node ID '" + node.getId() + "' in "
                        + previousOwner.getId() + " and " + tree.getId());
            }
            nodes.put(node.getId(), node);
        }
    }

    public synchronized NodeDefinition getNode(String id) {
        return allNodes.get(id);
    }

    public synchronized TreeDefinition getNodeOwner(String id) {
        return nodeOwners.get(id);
    }

    public synchronized TreeDefinition getAscensionTree() {
        return ascensionTree;
    }

    public synchronized TreeDefinition getTalentTree(MageClass mc) {
        return talentTrees.get(mc);
    }

    public synchronized Collection<TreeDefinition> getAllTrees() {
        List<TreeDefinition> all = new ArrayList<>();
        if (ascensionTree != null) all.add(ascensionTree);
        all.addAll(talentTrees.values());
        return all;
    }

    public static final float NODE_STAT_GLOBAL_MULT = 1.0f;

    public synchronized AscensionStatBlock computeNodeStats(Set<String> unlockedNodeIds, com.huige233.transcend.ascension.MageClass mageClass) {
        AscensionStatBlock block = new AscensionStatBlock();
        for (String nodeId : unlockedNodeIds) {
            NodeDefinition node = allNodes.get(nodeId);
            if (node == null || !isNodeAuthorized(nodeId, mageClass)) continue;
            for (Map.Entry<StatType, Float> entry : node.getStatBonuses().entrySet()) {
                entry.getKey().applyTo(block, entry.getValue() * NODE_STAT_GLOBAL_MULT);
            }
            if (mageClass != null && mageClass.isSelected()) {
                Map<String, Map<StatType, Float>> allScaling = node.getAllElementScaling();
                if (allScaling != null && !allScaling.isEmpty()) {
                    if (mageClass == MageClass.OMNISCIENT) {
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

    public synchronized List<PassiveEffect> getActivePassives(Set<String> unlockedNodeIds, MageClass mageClass) {
        List<PassiveEffect> effects = new ArrayList<>();
        for (String nodeId : unlockedNodeIds.stream().sorted().toList()) {
            NodeDefinition node = allNodes.get(nodeId);
            if (node == null || !isNodeAuthorized(nodeId, mageClass)) continue;
            effects.addAll(node.getPassiveEffects());
        }
        return effects;
    }

    public synchronized boolean isNodeAuthorized(String nodeId, MageClass mageClass) {
        TreeDefinition owner = nodeOwners.get(nodeId);
        if (owner == null) return false;
        return owner.getTreeType() == TreeType.ASCENSION
                || mageClass != null && mageClass.isSelected() && owner.getMageClass() == mageClass;
    }

    public static void resyncOnlinePlayersAfterReload() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.execute(() -> {
            Collection<TreeDefinition> trees = INSTANCE.getAllTrees();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                var data = AscensionCapability.get(player);
                AscensionHandler.applyPersistentStats(player, data);
                AscensionHandler.syncToClient(player, data);
                NetworkHandler.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new S2CTreeSync(trees));
            }
        });
    }

    public synchronized boolean isLoaded() {
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

    public synchronized void auditLangCompleteness() {
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
