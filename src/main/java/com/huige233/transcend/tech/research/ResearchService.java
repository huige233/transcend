package com.huige233.transcend.tech.research;

import java.math.BigInteger;
import java.util.*;


/** 根据前置节点和低时代研究完成情况判断解锁资格，并计算剩余研究点及完成集合。 */
public final class ResearchService {
    private ResearchService() {}
    public static boolean isUnlocked(Set<String> completed, String id) {
        ResearchNode node = ResearchRegistry.get(id);
        if (node == null || completed == null) return false;
        for (ResearchNode earlier : ResearchRegistry.all()) {
            if (earlier.tier().index() < node.tier().index() && !completed.contains(earlier.id())) return false;
        }
        return completed.containsAll(node.prerequisites());
    }
    public static boolean canStart(Set<String> completed, String id, String active) { return active == null && !completed.contains(id) && isUnlocked(completed, id); }
    public static BigInteger remaining(BigInteger paid, String id) { ResearchNode n=ResearchRegistry.get(id); return n==null?BigInteger.ZERO:n.cost().subtract(paid==null?BigInteger.ZERO:paid.max(BigInteger.ZERO)).max(BigInteger.ZERO); }
    public static Set<String> complete(Set<String> completed, String id) { if (!isUnlocked(completed,id)) throw new IllegalStateException("Research prerequisites incomplete: "+id); Set<String> r=new HashSet<>(completed); r.add(id); return r; }
}
