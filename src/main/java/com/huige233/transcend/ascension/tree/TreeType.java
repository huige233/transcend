package com.huige233.transcend.ascension.tree;

public enum TreeType {
    ASCENSION("ascension"),
    TALENT("talent");

    public final String id;

    TreeType(String id) {
        this.id = id;
    }

    public static TreeType getById(String id) {
        for (TreeType t : values()) {
            if (t.id.equals(id)) return t;
        }
        return null;
    }
}
