package com.huige233.transcend.tech.research;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared prerequisite navigation and funding estimates; never changes research progress. */
public final class ResearchPlanning {
    private static final Map<String, List<ResearchNode>> REQUIREMENTS = buildRequirements();
    private static final BigInteger RF_PER_POINT = BigInteger.valueOf(ResearchEnergyAccount.RF_PER_POINT);

    private ResearchPlanning() { }

    private static Map<String, List<ResearchNode>> buildRequirements() {
        Map<String, List<ResearchNode>> result = new HashMap<>();
        for (ResearchNode node : ResearchRegistry.all()) {
            result.put(node.id(), ResearchRegistry.all().stream()
                    .filter(n -> n.core() && n.tier().index() < node.tier().index()
                            || node.prerequisites().contains(n.id()))
                    .sorted(Comparator.comparingInt((ResearchNode n) -> n.tier().index()).thenComparing(ResearchNode::id))
                    .toList());
        }
        return Map.copyOf(result);
    }

    public static List<ResearchNode> prerequisites(String id) {
        return id == null ? List.of() : REQUIREMENTS.getOrDefault(id, List.of());
    }

    public static List<ResearchNode> missingPrerequisites(Set<String> completed, String id) {
        return prerequisites(id).stream().filter(n -> !completed.contains(n.id())).toList();
    }

    /** Prefer finishing the existing topic, otherwise find an unlocked dependency of the target. */
    public static ResearchNode nextPrerequisite(Set<String> completed, String id, String active) {
        if (ResearchRegistry.get(id) == null || completed.contains(id)) return null;
        ResearchNode running = ResearchRegistry.get(active);
        if (running != null && !completed.contains(active)) return running;
        return nextPrerequisite(completed, id, new HashSet<>());
    }

    private static ResearchNode nextPrerequisite(Set<String> completed, String id, Set<String> visited) {
        if (!visited.add(id)) return null;
        for (ResearchNode node : missingPrerequisites(completed, id)) {
            if (ResearchService.isUnlocked(completed, node.id())) return node;
            ResearchNode next = nextPrerequisite(completed, node.id(), visited);
            if (next != null) return next;
        }
        return null;
    }

    public static BigInteger nonNegative(String value) {
        try { return new BigInteger(value).max(BigInteger.ZERO); }
        catch (NumberFormatException | NullPointerException ignored) { return BigInteger.ZERO; }
    }

    public static Funding funding(BigInteger cost, BigInteger paid, BigInteger balance, long stored, long pointsPerBatch, boolean started) {
        BigInteger missing = cost.subtract(paid.max(BigInteger.ZERO)).subtract(balance.max(BigInteger.ZERO)).max(BigInteger.ZERO);
        BigInteger batch = BigInteger.valueOf(Math.max(1, pointsPerBatch));
        BigInteger pairs = missing.add(batch).subtract(BigInteger.ONE).divide(batch);
        BigInteger energy = BigInteger.valueOf(Math.max(0, stored));
        // A new topic needs prepared points before it can start automatic conversion.
        BigInteger automaticPoints = !started && balance.signum() <= 0 && missing.signum() > 0 ? missing.max(batch) : missing;
        // Conversion consumes whole-point RF amounts; the saved conversion remainder is retained.
        return new Funding(missing, pairs,
                automaticPoints.multiply(RF_PER_POINT).subtract(energy).max(BigInteger.ZERO),
                pairs.multiply(batch).multiply(RF_PER_POINT).subtract(energy).max(BigInteger.ZERO));
    }

    public record Funding(BigInteger missingPoints, BigInteger inputPairs,
                          BigInteger automaticExternalRf, BigInteger manualExternalRf) { }
}
