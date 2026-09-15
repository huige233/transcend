package com.huige233.transcend.util;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

   
                                                                
  
                                                      
                                                                                   
  
                                                                              
                                    
   
/** 延迟加载内嵌名称映射，供实体编辑器在 SRG 运行时成员名与可读名称之间转换。 */
public final class RuntimeMapping {

    private static final String RESOURCE = "/assets/transcend/mapping/srg2official.txt";
    private static final Map<String, String> METHOD = new ConcurrentHashMap<>();
    private static final Map<String, String> FIELD = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;
    private static volatile boolean missing = false;

    private RuntimeMapping() {}

    
    public static String method(String srg) {
        ensureLoaded();
        if (srg == null || srg.isEmpty()) return srg;
        String r = METHOD.get(srg);
        return r != null ? r : srg;
    }

    
    public static String field(String srg) {
        ensureLoaded();
        if (srg == null || srg.isEmpty()) return srg;
        String r = FIELD.get(srg);
        return r != null ? r : srg;
    }

    
    public static String toReadable(String srg) {
        ensureLoaded();
        if (srg == null || srg.isEmpty()) return srg;
        if (srg.startsWith("m_")) {
            String r = METHOD.get(srg);
            return r != null ? r : srg;
        }
        if (srg.startsWith("f_")) {
            String r = FIELD.get(srg);
            return r != null ? r : srg;
        }
        return srg;
    }

    
    @Nullable
    public static String methodToSrg(String readable) {
        ensureLoaded();
        if (readable == null || readable.isEmpty()) return null;
        
        if (readable.matches("m_\\d+_")) return readable;
        for (Map.Entry<String, String> e : METHOD.entrySet()) {
            if (e.getValue().equals(readable)) return e.getKey();
        }
        return null;
    }

    
    @Nullable
    public static String fieldToSrg(String readable) {
        ensureLoaded();
        if (readable == null || readable.isEmpty()) return null;
        if (readable.matches("f_\\d+_")) return readable;
        for (Map.Entry<String, String> e : FIELD.entrySet()) {
            if (e.getValue().equals(readable)) return e.getKey();
        }
        return null;
    }

    private static void ensureLoaded() {
        if (loaded || missing) return;
        synchronized (RuntimeMapping.class) {
            if (loaded || missing) return;
            try (InputStream in = RuntimeMapping.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    missing = true;
                    return;
                }
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String section = "";
                String line;
                int ti = 1;
                while ((line = r.readLine()) != null) {
                    if (line.startsWith("#")) {
                        section = line.startsWith("#M") ? "M" : line.startsWith("#F") ? "F" : section;
                        continue;
                    }
                    int sp = line.indexOf(' ');
                    if (sp <= 0) continue;
                    String k = line.substring(0, sp);
                    String v = line.substring(sp + 1);
                    if (section.equals("M")) METHOD.put(k, v);
                    else if (section.equals("F")) FIELD.put(k, v);
                    ti++;
                }
                loaded = true;
            } catch (IOException e) {
                missing = true;
            }
        }
    }

    
    public static boolean isLoaded() {
        ensureLoaded();
        return loaded;
    }
}