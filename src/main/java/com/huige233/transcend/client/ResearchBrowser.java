package com.huige233.transcend.client;

import com.huige233.transcend.tech.research.ResearchNode;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** Client-side catalog ordering and filtering; research rules remain on the server. */
final class ResearchBrowser {
    enum Branch { ALL, ENERGY, MATERIALS, AUTOMATION, COMBAT, KNOWLEDGE }
    private static final Set<String> MATERIALS = Set.of("materials_basics", "material_processing", "advanced_materials", "exotic_materials");
    private static final Set<String> AUTOMATION = Set.of("automation_basics", "automation", "precision_automation", "autonomous_factories");
    static final Comparator<ResearchNode> ORDER = Comparator.comparingInt((ResearchNode node) -> node.tier().index())
            .thenComparingInt(node -> branch(node).ordinal()).thenComparing(ResearchNode::id);

    private ResearchBrowser() { }

    static Branch branch(ResearchNode node) {
        String id = node.id();
        if (MATERIALS.contains(id) || id.endsWith("_materials")) return Branch.MATERIALS;
        if (AUTOMATION.contains(id) || id.endsWith("_automation")) return Branch.AUTOMATION;
        if (id.contains("combat")) return Branch.COMBAT;
        if (id.contains("knowledge")) return Branch.KNOWLEDGE;
        return Branch.ENERGY;
    }

    static List<ResearchNode> filter(Collection<ResearchNode> nodes, String query, Branch branch,
                                     Predicate<ResearchNode> state, Function<String, String> name) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return nodes.stream().filter(node -> branch == Branch.ALL || branch(node) == branch)
                .filter(state).filter(node -> needle.isEmpty() ||
                        (name.apply(node.id()) + " " + node.id() + " " + node.tier()).toLowerCase(Locale.ROOT).contains(needle))
                .sorted(ORDER).toList();
    }

    static List<ResearchNode> successors(Collection<ResearchNode> nodes, String id) {
        return nodes.stream().filter(node -> node.prerequisites().contains(id)).sorted(ORDER).toList();
    }
}
