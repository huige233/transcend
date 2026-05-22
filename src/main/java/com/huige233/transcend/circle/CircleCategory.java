package com.huige233.transcend.circle;

/**
 * 魔法阵功能分类枚举。
 * <p>
 * 用于在 UI、配方书、教程系统中对庞大的功能集合进行归类与筛选。
 * 每个分类拥有自己的本地化键，便于翻译展示。
 */
public enum CircleCategory {
    /** 魔力物流：与魔力的产出、传输、储存、放大相关。 */
    MANA_LOGISTICS("mana_logistics"),
    /** 玩家增益：为玩家施加各类正面状态效果。 */
    PLAYER_BUFF("player_buff"),
    /** 世界交互：影响天气、时间、声音、光照等世界状态。 */
    WORLD_INTERACTION("world_interaction"),
    /** 高级功能：跨维度、元素融合、共鸣等复杂高耗能功能。 */
    ADVANCED("advanced"),
    /** 农业相关：作物、矿物、生物刷新等生产性功能。 */
    FARMING("farming"),
    /** 防御相关：屏障、警报、陷阱等保护性功能。 */
    DEFENSE("defense"),
    /** 社交相关：盟约、旗帜、共享资源等多人协作功能。 */
    SOCIAL("social"),
    /** 探索相关：地图视野、生物群系感知等辅助探索功能。 */
    EXPLORATION("exploration"),
    /** 制作相关：原地附魔、合成、回复等工坊辅助功能。 */
    CRAFTING("crafting"),
    /** 美学相关：特效、光影、极光等装饰性功能。 */
    AESTHETIC("aesthetic"),
    /** 危险功能：高风险、高副作用的强力功能。 */
    DANGEROUS("dangerous");

    /** 字符串 ID，用于本地化键以及配置/数据驱动查找。 */
    private final String id;

    CircleCategory(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * 获取本地化键，用于在界面、提示信息中显示该分类的名称。
     *
     * @return 本地化键，例如 {@code "circle.transcend.category.mana_logistics"}
     */
    public String getTranslationKey() {
        return "circle.transcend.category." + id;
    }
}
