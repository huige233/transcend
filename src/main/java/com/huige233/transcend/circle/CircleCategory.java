package com.huige233.transcend.circle;

/** 法阵功能类别枚举。 */
public enum CircleCategory {

    MANA_LOGISTICS("mana_logistics"),

    PLAYER_BUFF("player_buff"),

    WORLD_INTERACTION("world_interaction"),

    ADVANCED("advanced"),

    FARMING("farming"),

    DEFENSE("defense"),

    SOCIAL("social"),

    EXPLORATION("exploration"),

    CRAFTING("crafting"),

    AESTHETIC("aesthetic"),

    DANGEROUS("dangerous");

    private final String id;

    CircleCategory(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return "circle.transcend.category." + id;
    }
}
