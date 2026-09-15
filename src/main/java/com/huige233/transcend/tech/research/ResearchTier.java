package com.huige233.transcend.tech.research;


/** 定义 T0 至 T8 的研究时代顺序，供跨时代解锁条件比较使用。 */
public enum ResearchTier {
    T0(0), T1(1), T2(2), T3(3), T4(4), T5(5), T6(6), T7(7), T8(8);
    private final int index;
    ResearchTier(int index) { this.index = index; }
    public int index() { return index; }
    public boolean isAfter(ResearchTier other) { return index > other.index; }
}
