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

/**
 * 字节码读写图：不改任何代码，只对实体的类字节做只读分析。
 * 目标是把字段名/含义的"猜"替换成可验证证据 —— 某个字段被哪些方法读写。
 * 一个只在 setHealth / getHealth 里被触碰的 float 字段，就是血量本尊，
 * 这不是语义猜测，是调用图证据。
 *
 * 默认关闭：Config.editorAsmScan 开启后才跑（首次对复杂实体类会付 ~毫秒级解析成本）。
 *
 * 结构：类内部名 → 字段名 → {读方法名集} / {写方法名集}。
 */
public final class EditorAsmReader {

    private static final Map<String, ClassInfo> CACHE = new ConcurrentHashMap<>();

    private EditorAsmReader() {}

    /** 读取字段在其声明类里的读写者集合。开关关闭、解析失败或无读写者时返回 NONE。 */
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

    /** 解析单个类字节：只记录声明于此类的字段的读写方法名。 */
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

    /** 一个字段的读写者集合。 */
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

    private static final class ClassInfo {
        final Map<String, Set<String>> readsByField = new HashMap<>();
        final Map<String, Set<String>> writesByField = new HashMap<>();
    }
}