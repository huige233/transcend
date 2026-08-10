package com.huige233.transcend.client;

import com.huige233.transcend.ascension.MageClass;
import com.huige233.transcend.ascension.tree.TreeDefinition;
import com.huige233.transcend.ascension.tree.TreeType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 客户端天赋树数据缓存。 */
public class ClientTreeCache {

    private static TreeDefinition ascensionTree;
    private static final Map<MageClass, TreeDefinition> talentTrees = new EnumMap<>(MageClass.class);

    public static void load(List<TreeDefinition> trees) {
        TreeDefinition newAscensionTree = null;
        Map<MageClass, TreeDefinition> newTalentTrees = new EnumMap<>(MageClass.class);
        for (TreeDefinition tree : trees) {
            if (tree.getTreeType() == TreeType.ASCENSION) {
                newAscensionTree = tree;
            } else {
                newTalentTrees.put(tree.getMageClass(), tree);
            }
        }

        com.huige233.transcend.ascension.tree.TreeRegistry reg =
                com.huige233.transcend.ascension.tree.TreeRegistry.getInstance();
        try {
            reg.replaceAll(trees);
        } catch (IllegalArgumentException e) {
            org.apache.logging.log4j.LogManager.getLogger().error("Rejected invalid tree sync", e);
            return;
        }
        ascensionTree = newAscensionTree;
        talentTrees.clear();
        talentTrees.putAll(newTalentTrees);
        reg.auditLangCompleteness();
    }

    public static TreeDefinition getAscensionTree() { return ascensionTree; }
    public static TreeDefinition getTalentTree(MageClass mc) { return talentTrees.get(mc); }
    public static boolean isLoaded() { return ascensionTree != null; }
}
