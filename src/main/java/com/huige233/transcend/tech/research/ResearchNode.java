package com.huige233.transcend.tech.research;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;


/** 定义研究节点的时代、前置节点、研究点费用、耗时和解锁标识，并校验定义有效性。 */
public record ResearchNode(String id, ResearchTier tier, List<String> prerequisites,
                           BigInteger cost, long durationTicks, List<String> unlocks) {
    public ResearchNode {
        Objects.requireNonNull(id); Objects.requireNonNull(tier); Objects.requireNonNull(prerequisites);
        Objects.requireNonNull(cost); Objects.requireNonNull(unlocks);
        if (id.isBlank() || cost.signum() < 0 || durationTicks < 1) throw new IllegalArgumentException("Invalid research node");
        prerequisites = List.copyOf(prerequisites); unlocks = List.copyOf(unlocks);
    }
}
