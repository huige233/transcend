package com.huige233.transcend.client;

import com.huige233.transcend.tech.research.ResearchNode;
import com.huige233.transcend.tech.research.ResearchRegistry;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ResearchBrowserTest {
    @Test void allFiveBranchesContainOneNodeForEachEra() {
        assertEquals(45, ResearchRegistry.all().size());
        for (var branch : ResearchBrowser.Branch.values()) {
            if (branch == ResearchBrowser.Branch.ALL) continue;
            var nodes = ResearchBrowser.filter(ResearchRegistry.all(), "", branch, n -> true, id -> id);
            assertEquals(9, nodes.size(), branch.name());
            for (int tier = 0; tier < 9; tier++) assertEquals(tier, nodes.get(tier).tier().index());
        }
    }

    @Test void searchSupportsLocalizedNamesIdsAndTiers() {
        var nodes = ResearchBrowser.filter(ResearchRegistry.all(), "量子", ResearchBrowser.Branch.ALL,
                n -> true, id -> id.equals("quantum_knowledge") ? "量子知识" : id);
        assertEquals(List.of("quantum_knowledge"), nodes.stream().map(ResearchNode::id).toList());
        assertEquals(5, ResearchBrowser.filter(ResearchRegistry.all(), " t8 ", ResearchBrowser.Branch.ALL, n -> true, id -> id).size());
        assertEquals(1, ResearchBrowser.filter(ResearchRegistry.all(), "ENERGY_BASICS", ResearchBrowser.Branch.ALL, n -> true, id -> id).size());
    }

    @Test void branchAndStateFiltersCombineWithoutInventingAvailability() {
        Set<String> ready = Set.of("energy_basics", "materials_basics");
        var nodes = ResearchBrowser.filter(ResearchRegistry.all(), "", ResearchBrowser.Branch.ENERGY,
                n -> ready.contains(n.id()), id -> id);
        assertEquals(List.of("energy_basics"), nodes.stream().map(ResearchNode::id).toList());
        assertTrue(ResearchBrowser.filter(ResearchRegistry.all(), "unmatched", ResearchBrowser.Branch.ALL, n -> true, id -> id).isEmpty());
    }

    @Test void successorLinksUseActualPrerequisitesIncludingCrossBranchEdges() {
        var nodes = ResearchBrowser.successors(ResearchRegistry.all(), "advanced_materials");
        assertEquals(List.of("quantum_combat", "exotic_materials"), nodes.stream().map(ResearchNode::id).toList());
        assertTrue(ResearchBrowser.successors(ResearchRegistry.all(), "ultimate_universe").isEmpty());
    }

    @Test void everyRegisteredNodeHasDescriptionsInBothLanguages() throws Exception {
        for (String language : List.of("en_us", "zh_cn")) {
            var strings = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/transcend/lang/" + language + ".json"))).getAsJsonObject();
            for (var node : ResearchRegistry.all()) {
                assertTrue(strings.has("research.transcend." + node.id()), "Missing name: " + node.id());
                String key = "research.transcend.description." + node.id();
                assertTrue(strings.has(key), key);
                assertFalse(strings.get(key).getAsString().isBlank(), key);
            }
        }
    }
}
