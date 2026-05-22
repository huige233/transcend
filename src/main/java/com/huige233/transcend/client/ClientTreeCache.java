package com.huige233.transcend.client;

import com.huige233.transcend.ascension.MageClass;
import com.huige233.transcend.ascension.tree.TreeDefinition;
import com.huige233.transcend.ascension.tree.TreeType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ClientTreeCache {

    private static TreeDefinition ascensionTree;
    private static final Map<MageClass, TreeDefinition> talentTrees = new EnumMap<>(MageClass.class);

    public static void load(List<TreeDefinition> trees) {
        ascensionTree = null;
        talentTrees.clear();
        for (TreeDefinition tree : trees) {
            if (tree.getTreeType() == TreeType.ASCENSION) {
                ascensionTree = tree;
            } else {
                talentTrees.put(tree.getMageClass(), tree);
            }
        }
        // 同步到 TreeRegistry (client-side) 并执行 lang 完整性检查
        com.huige233.transcend.ascension.tree.TreeRegistry reg =
                com.huige233.transcend.ascension.tree.TreeRegistry.getInstance();
        reg.clear();
        for (TreeDefinition tree : trees) reg.register(tree);
        reg.auditLangCompleteness();
    }

    public static TreeDefinition getAscensionTree() { return ascensionTree; }
    public static TreeDefinition getTalentTree(MageClass mc) { return talentTrees.get(mc); }
    public static boolean isLoaded() { return ascensionTree != null; }
}
