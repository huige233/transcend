package com.huige233.transcend.util;

import com.huige233.transcend.Config;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

   
                               
                                         
                                                    
                  
  
                                                        
  
                                     
   
/** 在配置允许时只读扫描并缓存实体类字节码，查明各实例字段的读写方法。 */
public final class EditorAsmReader {

    private static final Map<String, ClassInfo> CACHE = new ConcurrentHashMap<>();

    private EditorAsmReader() {}

    
    public static FieldAccess access(Field f) {
        if (!Config.editorAsmScan) return FieldAccess.NONE;
        ClassInfo info;
        try {
            info = CACHE.computeIfAbsent(internalName(f.getDeclaringClass()), EditorAsmReader::parse);
        } catch (Throwable t) {
            return FieldAccess.NONE;
        }
        Set<String> reads = info.readsByField.get(f.getName());
        Set<String> writes = info.writesByField.get(f.getName());
        if ((reads == null || reads.isEmpty()) && (writes == null || writes.isEmpty())) {
            return FieldAccess.NONE;
        }
        return new FieldAccess(
                reads == null ? Set.of() : new HashSet<>(reads),
                writes == null ? Set.of() : new HashSet<>(writes));
    }

    private static String internalName(Class<?> c) {
        return c.getName().replace('.', '/');
    }

    
    private static ClassInfo parse(String internalName) {
        ClassReader cr;
        try {
            InputStream in = EditorAsmReader.class.getResourceAsStream("/" + internalName + ".class");
            if (in == null) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                in = cl == null ? null : cl.getResourceAsStream(internalName + ".class");
            }
            if (in == null) return new ClassInfo();
            cr = new ClassReader(in);
        } catch (Throwable t) {
            return new ClassInfo();
        }
        ClassInfo info = new ClassInfo();
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String fname, String fdesc) {
                        boolean isOwn = owner.equals(internalName);
                        if (opcode == Opcodes.GETFIELD) {
                            if (isOwn) info.readsByField.computeIfAbsent(fname, k -> new HashSet<>()).add(name);
                        } else if (opcode == Opcodes.PUTFIELD) {
                            if (isOwn) info.writesByField.computeIfAbsent(fname, k -> new HashSet<>()).add(name);
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return info;
    }

    
    /** 封装单个字段的读取方法与写入方法集合，并提供空结果判定。 */
    public static final class FieldAccess {
        public static final FieldAccess NONE = new FieldAccess(Set.of(), Set.of());
        public final Set<String> reads;
        public final Set<String> writes;

        public FieldAccess(Set<String> reads, Set<String> writes) {
            this.reads = reads;
            this.writes = writes;
        }

        public boolean isEmpty() {
            return reads.isEmpty() && writes.isEmpty();
        }
    }

    /** 按字段名缓存单个类中实例字段的读取方法集合和写入方法集合。 */
    private static final class ClassInfo {
        final Map<String, Set<String>> readsByField = new HashMap<>();
        final Map<String, Set<String>> writesByField = new HashMap<>();
    }
}