package com.huige233.transcend.tech.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

   
                                                                                                    
                                                                                                 
                                                                                    
   
/** 检查科技物品数据的根标签、版本号和关键字段名，避免纯数据存档契约意外变化。 */
class TechItemDataTest {
    @Test
    void keepsTheInitialSchemaContractStable() {
        assertEquals("TechData", TechItemData.ROOT);
        assertEquals(2, TechItemData.SCHEMA_VERSION);
        assertEquals("Schema", TechItemData.TAG_SCHEMA);
        assertEquals("GunCharge", TechItemData.TAG_GUN_CHARGE);
        assertEquals("Magazine", TechItemData.TAG_MAGAZINE);
        assertEquals("Attributes", TechItemData.TAG_ATTRIBUTES);
    }
}
