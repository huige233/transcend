package com.huige233.transcend.client;

import com.huige233.transcend.handle.EditorNetwork;
import com.huige233.transcend.network.C2SEntityEditPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 实体因果编辑器。三级界面：实体列表 → 主菜单（调用方法/改字段/强返/冻结）→ 方法/字段列表 → 输入弹窗。
 * 数据来源是上次扫描结果（服务端 TranscendEditService 下发或本地反射），渲染在 Matrix 字符雨背景上。
 */
public class TranscendEditScreen extends Screen {

    private static final int PHASE_LIST = 0;
    private static final int PHASE_MAIN = 1;
    private static final int PHASE_METHODS = 2;
    private static final int PHASE_FIELDS = 3;
    private static final int PHASE_INPUT = 4;

    private static final int ROW_H = 18;
    private static final int MENU_COLS = 2;
    private static final int MENU_OPT_W = 150;
    private static final int MENU_OPT_H = 42;
    private static final int INPUT_W = 280;

    private static final int HEADER_H = 24;
    private static final int SEARCH_H = 18;
    private static final int PAD = 12;

    private final List<String> entityNames = new ArrayList<>();
    private final List<Integer> entityIds = new ArrayList<>();
    private final List<String> methodLines = new ArrayList<>();
    private final List<String> fieldLines = new ArrayList<>();
    /** 扫描附带的实体信息行（注册名/dataId/本地化名），置顶显示，不参与交互。 */
    private String infoLine = "";

    private int phase = PHASE_LIST;
    private int entityId = -1;
    private String entityName = "";
    private boolean frozen;

    private int panelX, panelY, panelW, panelH;
    private float time, alpha;

    private EditBox search;
    private int scroll;
    private int hoveredIdx = -1;
    private int selectedIdx = -1;
    private int visible;

    private EditBox in1;
    private EditBox in2;
    private int inputAction;
    private String savedParamTypes = "";
    private String savedReturnType = "";
    private final Runnable onFreezeRelease;

    /** 无参构造：打开实体列表页。 */
    public TranscendEditScreen() {
        super(Component.translatable("gui.transcend.editor.title"));
        onFreezeRelease = null;
    }

    /** 主菜单矩形。 */
    private record Rect(int x, int y, int w, int h, int idx) {}

    /** 输入弹窗内可点击 chip 的命中区域，每帧在 renderInputOverlay 重建；browse=true 表示"全部▾"开关。 */
    private record ChipRect(int x, int y, int w, int h, String text, int paramIdx, boolean browse) {}

    private final List<ChipRect> chipRects = new ArrayList<>();

    /** 注册表侧边选择器状态：kind 0=伤害类型 1=药水效果。 */
    private boolean selectorOpen;
    private int selectorParamIdx = -1;
    private int selectorKind;
    private final List<String> selectorIds = new ArrayList<>();
    private int selectorScroll;
    private int selX, selY, selW, selH, selVisibleRows;

    /** 对目标实体打开主菜单页；onFreezeRelease 在解冻时回调。 */
    public TranscendEditScreen(int entityId, String entityName, Runnable onFreezeRelease) {
        super(Component.translatable("gui.transcend.editor.title"));
        this.entityId = entityId;
        this.entityName = entityName == null ? "?" : entityName;
        this.onFreezeRelease = onFreezeRelease;
        this.frozen = true;
        this.phase = PHASE_MAIN;
    }

    /** 用实体名/ID 列表打开实体列表页。 */
    public static void openEntities(String[] names, int[] ids) {
        TranscendEditScreen s = new TranscendEditScreen();
        if (names != null) for (String n : names) s.entityNames.add(n);
        if (ids != null) for (int id : ids) s.entityIds.add(id);
        Minecraft.getInstance().setScreen(s);
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 60, (int) (width * 0.62f));
        panelH = Math.min(height - 90, (int) (height * 0.60f));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2 - 8;

        if (search == null) {
            search = makeSearchBox();
        } else {
            removeWidget(search);
        }
        search.setHeight(SEARCH_H);
        addRenderableWidget(search);
        repositionSearch();
        applySearchVisibility();

        if (phase == PHASE_INPUT) {
            if (in1 != null) { removeWidget(in1); addRenderableWidget(in1); }
            if (in2 != null) { removeWidget(in2); addRenderableWidget(in2); }
        }
    }

    private EditBox makeSearchBox() {
        EditBox box = new EditBox(font, 0, 0, 220, SEARCH_H,
                Component.translatable("gui.transc.editor.search"));
        box.setMaxLength(64);
        box.setHint(Component.translatable("gui.transc.editor.search_hint"));
        box.setBordered(false);
        box.setTextColor(0xFF66FFEE);
        box.setTextColorUneditable(0xFF3F7A52);
        return box;
    }

    /** 把搜索框定位到面板标题条下方、占满面板宽度。 */
    private void repositionSearch() {
        if (search == null) return;
        search.setX(panelX + PAD + 16);
        search.setY(panelY + HEADER_H + 10);
        search.setWidth(panelW - PAD * 2 - 16);
        search.setHeight(SEARCH_H);
    }

    /** 搜索框只在列表/方法/字段三页显示，其余页隐藏并释放焦点。 */
    private void applySearchVisibility() {
        if (search != null) {
            boolean shown = phase == PHASE_LIST || phase == PHASE_METHODS || phase == PHASE_FIELDS;
            search.setVisible(shown);
            if (!shown) search.setFocused(false);
        }
    }

    /** 把键盘焦点授给指定输入框并解除其余输入框焦点。 */
    private void focusOnly(EditBox box) {
        if (search != null && box != search) search.setFocused(false);
        if (in1 != null && box != in1) in1.setFocused(false);
        if (in2 != null && box != in2) in2.setFocused(false);
        this.setFocused(box);
    }

    /** 列表区顶部 Y 坐标。 */
    private int listContentTop() {
        return panelY + HEADER_H + SEARCH_H + 24;
    }

    /** 实体列表行（编号前缀）。 */
    private List<String> entityList() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < entityNames.size(); i++) out.add((i + 1 + ". " + entityNames.get(i)));
        return out;
    }

    /** 当前阶段要展示的原始行列表。 */
    private List<String> visibleBase() {
        return switch (phase) {
            case PHASE_METHODS -> methodLines;
            case PHASE_FIELDS -> fieldLines;
            default -> entityList();
        };
    }

    /** 按搜索词过滤后的行列表（仅列表/方法/字段页参与过滤）。 */
    private List<String> filtered() {
        List<String> base = visibleBase();
        String q;
        if (phase == PHASE_LIST || phase == PHASE_METHODS || phase == PHASE_FIELDS) {
            q = search == null || search.getValue() == null ? "" : search.getValue().toLowerCase(Locale.ENGLISH);
        } else {
            q = "";
        }
        List<String> out = new ArrayList<>();
        for (String s : base) if (q.isEmpty() || s.toLowerCase(Locale.ENGLISH).contains(q)) out.add(s);
        return out;
    }

    /** 本地反射扫描目标实体，填装方法/字段列表与实体信息行。 */
    private void scanLocal() {
        methodLines.clear();
        fieldLines.clear();
        infoLine = "";
        net.minecraft.world.entity.Entity e = minecraft.level != null ? minecraft.level.getEntity(entityId) : null;
        if (e != null) {
            infoLine = "▸ 实体 " + net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType())
                    + " · dataId=" + e.getEncodeId()
                    + " · desc=" + Component.translatable(e.getType().getDescriptionId()).getString();
            for (String line : C2SEntityEditPacket.scanMethods(e).split("\n"))
                if (!line.isEmpty()) methodLines.add(line);
            for (String line : C2SEntityEditPacket.scanFields(e).split("\n"))
                if (!line.isEmpty()) fieldLines.add(line);
        }
    }

    /** 接收服务端扫描结果；抽取实体信息行，其余进入方法/字段列表。 */
    public void receiveScan(int targetId, java.util.List<String> m, java.util.List<String> f) {
        if (targetId != entityId) return;
        methodLines.clear();
        fieldLines.clear();
        infoLine = "";
        if (m != null) {
            for (String s : m) {
                if (s.startsWith("▸")) { infoLine = s; continue; }
                methodLines.add(s);
            }
        }
        if (f != null) fieldLines.addAll(f);
        scroll = 0;
    }

    // ---------------- 交互 ----------------
    /** 选中实体：记 ID、冻结目标、跳主菜单。 */
    private void selectEntity(int idx) {
        entityId = entityIds.get(idx);
        entityName = entityNames.get(idx);
        frozen = true;
        EditorNetwork.CHANNEL.sendToServer(new C2SEntityEditPacket(
                entityId, C2SEntityEditPacket.MODE_FREEZE, "", "", "1"));
        methodLines.clear();
        fieldLines.clear();
        selectedIdx = -1;
        scroll = 0;
        phase = PHASE_MAIN;
        applySearchVisibility();
    }

    /** 主菜单按钮分发：0/2 进方法页，1 进字段页，3 切冻结。 */
    private void handleMenu(int sel) {
        switch (sel) {
            case 0 -> switchPhase(PHASE_METHODS);
            case 1 -> switchPhase(PHASE_FIELDS);
            case 2 -> switchPhase(PHASE_METHODS);
            default -> toggleFreeze();
        }
    }

    /** 切换目标冻结/解冻，并向服务端发送对应包。 */
    private void toggleFreeze() {
        if (entityId < 0) return;
        frozen = !frozen;
        EditorNetwork.CHANNEL.sendToServer(new C2SEntityEditPacket(
                entityId, C2SEntityEditPacket.MODE_FREEZE, "", "", frozen ? "1" : "0"));
        if (!frozen && onFreezeRelease != null) onFreezeRelease.run();
    }

    /** 切换阶段；进入方法/字段页且列表为空时触发本地扫描。 */
    private void switchPhase(int to) {
        phase = to;
        scroll = 0;
        selectedIdx = -1;
        applySearchVisibility();
        if (to == PHASE_METHODS || to == PHASE_FIELDS) {
            if (methodLines.isEmpty() && fieldLines.isEmpty()) scanLocal();
        }
    }

    /** 列表点击分发：实体页选实体，方法/字段页打开对应输入弹窗。 */
    private void openAt(List<String> list, int idxInFiltered) {
        if (phase == PHASE_MAIN) return;
        if (phase == PHASE_LIST) { selectEntity(idxInFiltered); return; }
        String sig = list.get(idxInFiltered);
        if (phase == PHASE_METHODS) openMethod(sig);
        else if (phase == PHASE_FIELDS) openField(sig);
    }

    /** 打开方法调用弹窗，解析签名里的参数类型。 */
    private void openMethod(String sig) {
        if (phase == PHASE_INPUT) return;
        int p = sig.indexOf('(');
        String name = p > 0 ? sig.substring(0, p) : sig;
        savedParamTypes = "";
        if (p >= 0) {
            int q = sig.indexOf(')');
            savedParamTypes = q > p ? sig.substring(p + 1, q) : "";
        }
        inputAction = 0;
        showInput(name);
    }

    /** 打开字段修改弹窗，取字段名。 */
    private void openField(String sig) {
        if (phase == PHASE_INPUT) return;
        int c = sig.indexOf(':');
        String name = c > 0 ? sig.substring(0, c) : sig;
        inputAction = 1;
        savedParamTypes = "";
        showInput(name);
    }

    /** 打开强制返回弹窗：取所选方法的返回类型。 */
    private void openForceReturn() {
        if (phase == PHASE_INPUT) return;
        List<String> list = filtered();
        if (phase != PHASE_METHODS || selectedIdx < 0 || selectedIdx >= list.size()) return;
        String sig = list.get(selectedIdx);
        int c = sig.indexOf(':');
        savedReturnType = c >= 0 ? sig.substring(c + 1) : "";
        int p = sig.indexOf('(');
        String name = p > 0 ? sig.substring(0, p) : sig;
        inputAction = 2;
        showInput(name);
    }

    /** 挂载输入弹窗：回收旧输入框、建两个新框并聚焦第二个。 */
    private void showInput(String name) {
        closeInputWidgets();
        int cx = width / 2, cy = height / 2;
        int w = Math.min(INPUT_W, width - 80);
        int bx = cx - w / 2;
        in1 = makeInputBox(bx, cy - 33, w, name);
        in2 = makeInputBox(bx, cy + 31, w, "");
        addRenderableWidget(in1);
        addRenderableWidget(in2);
        phase = PHASE_INPUT;
        applySearchVisibility();
        focusOnly(in2);
    }

    private EditBox makeInputBox(int x, int y, int w, String value) {
        EditBox box = new EditBox(font, x, y, w, 18, Component.literal(""));
        box.setMaxLength(1024);
        box.setBordered(false);
        box.setTextColor(0xFFDDFFEE);
        box.setTextColorUneditable(0xFF6A8F6A);
        box.setValue(value);
        return box;
    }

    /** 按动作类型发出对应编辑包并关闭弹窗。 */
    private void execInput() {
        String v1 = in1 != null ? in1.getValue().trim() : "";
        String v2 = in2 != null ? in2.getValue().trim() : "";
        if (inputAction == 0) {
            EditorNetwork.CHANNEL.sendToServer(new C2SEntityEditPacket(entityId, 0, v1, savedParamTypes, v2));
        } else if (inputAction == 1) {
            EditorNetwork.CHANNEL.sendToServer(new C2SEntityEditPacket(entityId, 1, v1, "", v2));
        } else {
            EditorNetwork.CHANNEL.sendToServer(new C2SEntityEditPacket(entityId, 2, v1, savedReturnType, v2));
        }
        closeInput();
    }

    /** 关闭输入弹窗并回到方法/字段列表页。 */
    private void closeInput() {
        closeInputWidgets();
        phase = inputAction == 1 ? PHASE_FIELDS : PHASE_METHODS;
        applySearchVisibility();
        if (search != null) search.setFocused(false);
    }

    /** 返回实体列表页并聚焦搜索框。 */
    private void backToList() {
        if (search != null) search.setFocused(true);
        phase = PHASE_LIST;
        scroll = 0;
        selectedIdx = -1;
        applySearchVisibility();
    }

    /** 移除输入框控件并清空引用与侧边选择器状态。 */
    private void closeInputWidgets() {
        if (in1 != null) { removeWidget(in1); in1 = null; }
        if (in2 != null) { removeWidget(in2); in2 = null; }
        chipRects.clear();
        selectorOpen = false;
        selectorParamIdx = -1;
        selectorIds.clear();
        selectorScroll = 0;
    }

    private static boolean overBox(EditBox b, double mx, double my) {
        return mx >= b.getX() && mx <= b.getX() + b.getWidth()
                && my >= b.getY() && my <= b.getY() + b.getHeight();
    }

    /** 逗号切分简单类型名串；无泛型逗号，直接 split。 */
    private static String[] splitTypes(String s) {
        if (s == null || s.trim().isEmpty()) return new String[0];
        String[] raw = s.split(",");
        for (int i = 0; i < raw.length; i++) raw[i] = raw[i].trim();
        return raw;
    }

    /** 按参数简单类型给出预设 chip 文本；数值类型在 target 非空时附带当前坐标。 */
    private static String[] chipsFor(String simpleType, net.minecraft.world.entity.Entity target) {
        String t = simpleType == null ? "" : simpleType.trim();
        if (t.contains("DamageSource")) return new String[]{
                "@magic", "@generic", "@wither", "@lightning", "@freeze",
                "@drown", "@starve", "@fall", "@lava", "@kill"};
        switch (t) {
            case "MobEffect": case "MobEffectInstance":
                return new String[]{"@speed", "@strength", "@regeneration", "@poison", "@wither", "@glowing"};
            case "Entity": case "LivingEntity": case "Player": case "ServerPlayer":
                return new String[]{"@self", "@target"};
            case "Level": case "ServerLevel":
                return new String[]{"@level"};
            case "BlockPos": case "Vec3":
                return new String[]{"@pos"};
            case "RemovalReason":
                return new String[]{"@killed", "@discarded"};
            case "boolean": case "Boolean":
                return new String[]{"true", "false"};
            case "int": case "long": case "Integer": case "Long": {
                List<String> out = new ArrayList<>(List.of("1", "100", "9999", "1000000"));
                if (target != null) {
                    net.minecraft.core.BlockPos bp = target.blockPosition();
                    out.add(String.valueOf(bp.getX()));
                    out.add(String.valueOf(bp.getY()));
                    out.add(String.valueOf(bp.getZ()));
                }
                return out.toArray(new String[0]);
            }
            case "float": case "double": case "Float": case "Double": {
                List<String> out = new ArrayList<>(List.of("1", "100", "9999", "1000000"));
                if (target != null) {
                    net.minecraft.world.phys.Vec3 p = target.position();
                    out.add(fmt1(p.x));
                    out.add(fmt1(p.y));
                    out.add(fmt1(p.z));
                }
                return out.toArray(new String[0]);
            }
            default:
                return new String[0];
        }
    }

    private static String fmt1(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    /** 常用方法参数一键示例；teleportTo 用实体实时坐标动态生成。 */
    private static final java.util.Map<String, String> EXAMPLES = java.util.Map.ofEntries(
            java.util.Map.entry("hurt", "@magic,10000"),
            java.util.Map.entry("setHealth", "0"),
            java.util.Map.entry("heal", "100"),
            java.util.Map.entry("setSecondsOnFire", "10"),
            java.util.Map.entry("remove", "@killed"),
            java.util.Map.entry("setTarget", "@self"),
            java.util.Map.entry("setNoAi", "true"),
            java.util.Map.entry("setInvulnerable", "true"),
            java.util.Map.entry("setGlowingTag", "true"),
            java.util.Map.entry("push", "0,1,0"),
            java.util.Map.entry("setAirSupply", "0"),
            java.util.Map.entry("setDeltaMovement", "0,2,0"));

    /** 当前方法弹窗的示例参数串；无示例或取不到实体返回 null。 */
    private String exampleFor(net.minecraft.world.entity.Entity target) {
        if (inputAction != 0 || in1 == null) return null;
        String mName = in1.getValue().trim();
        if ("teleportTo".equals(mName)) {
            if (target == null) return null;
            net.minecraft.world.phys.Vec3 p = target.position();
            return fmt1(p.x) + "," + fmt1(p.y) + "," + fmt1(p.z);
        }
        return EXAMPLES.get(mName);
    }

    /** chip 行按可用宽度换行后的行数（与渲染逻辑一致）。 */
    private int chipRowsFor(String[] chips, int maxW) {
        if (chips.length == 0) return 0;
        int x = 0, rows = 1;
        for (String c : chips) {
            int w = font.width(c) + 10;
            if (x > 0 && x + w > maxW) { rows++; x = 0; }
            x += w + 6;
        }
        return rows;
    }

    /** 该参数类型是否提供侧边注册表选择器。 */
    private static boolean browsable(String simpleType) {
        String t = simpleType == null ? "" : simpleType.trim();
        return t.contains("DamageSource") || t.equals("MobEffect") || t.equals("MobEffectInstance");
    }

    /** chipsFor 结果，若可浏览则末尾追加"全部▾"开关；渲染与高度计算共用保证行数一致。 */
    private String[] effectiveChips(String t, net.minecraft.world.entity.Entity target) {
        String[] base = chipsFor(t, target);
        if (!browsable(t)) return base;
        String[] out = java.util.Arrays.copyOf(base, base.length + 1);
        out[base.length] = loc("editor.input.browse") + " ▾";
        return out;
    }

    /** 打开侧边选择器并缓存注册表 id 列表（kind 0=伤害类型 1=药水效果）。 */
    private void openSelector(int paramIdx, int kind) {
        selectorOpen = true;
        selectorParamIdx = paramIdx;
        selectorKind = kind;
        selectorScroll = 0;
        selectorIds.clear();
        if (kind == 0) {
            if (minecraft != null && minecraft.level != null) {
                for (net.minecraft.resources.ResourceLocation rl : minecraft.level.registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).keySet()) {
                    selectorIds.add(rl.toString());
                }
            }
        } else {
            for (net.minecraft.resources.ResourceLocation rl :
                    net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getKeys()) {
                selectorIds.add(rl.toString());
            }
        }
        java.util.Collections.sort(selectorIds);
    }

    /** 选择器点选：minecraft 前缀简写为 @path；MobEffectInstance 默认补 ;600;0。 */
    private void applySelector(int idx) {
        if (idx < 0 || idx >= selectorIds.size()) return;
        String id = selectorIds.get(idx);
        String token = "@" + (id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id);
        if (selectorKind == 1) {
            String[] pts = splitTypes(savedParamTypes);
            String t = selectorParamIdx >= 0 && selectorParamIdx < pts.length ? pts[selectorParamIdx] : "";
            if (t.equals("MobEffectInstance")) token += ";600;0";
        }
        int idxParam = selectorParamIdx;
        selectorOpen = false;
        fillParamValue(idxParam, token);
    }

    /** 把 chip 文本填入 in2 第 paramIdx 个参数位（逗号切分、不足补空位）；paramIdx<0 整行替换。 */
    private void fillParamValue(int paramIdx, String text) {
        if (in2 == null) return;
        if (paramIdx < 0) {
            in2.setValue(text);
            focusOnly(in2);
            return;
        }
        List<String> parts = new ArrayList<>();
        String cur = in2.getValue();
        if (cur != null && !cur.trim().isEmpty()) {
            for (String p : cur.split(",", -1)) parts.add(p.trim());
        }
        while (parts.size() <= paramIdx) parts.add("");
        parts.set(paramIdx, text);
        in2.setValue(String.join(",", parts));
        focusOnly(in2);
    }

    /** 解冻并回调 onFreezeRelease。 */
    private void doUnfreeze() {
        if (entityId < 0) return;
        if (frozen) {
            EditorNetwork.CHANNEL.sendToServer(new C2SEntityEditPacket(entityId, C2SEntityEditPacket.MODE_FREEZE, "", "", "0"));
            frozen = false;
        }
        if (onFreezeRelease != null) onFreezeRelease.run();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        if (phase == PHASE_INPUT) {
            if (selectorOpen && mx >= selX && mx <= selX + selW && my >= selY && my <= selY + selH) {
                int listTop = selY + 18;
                if (my >= listTop) {
                    int idx = selectorScroll + (int) ((my - listTop) / 13);
                    if (idx - selectorScroll < selVisibleRows) applySelector(idx);
                }
                return true;
            }
            for (ChipRect ch : chipRects) {
                if (mx >= ch.x() && mx <= ch.x() + ch.w() && my >= ch.y() && my <= ch.y() + ch.h()) {
                    if (ch.browse()) {
                        if (selectorOpen && selectorParamIdx == ch.paramIdx()) {
                            selectorOpen = false;
                        } else {
                            String[] pts = splitTypes(savedParamTypes);
                            String t = ch.paramIdx() >= 0 && ch.paramIdx() < pts.length ? pts[ch.paramIdx()] : "";
                            openSelector(ch.paramIdx(), t.contains("DamageSource") ? 0 : 1);
                        }
                    } else {
                        fillParamValue(ch.paramIdx(), ch.text());
                    }
                    return true;
                }
            }
            boolean handled = super.mouseClicked(mx, my, button);
            if (in1 != null && overBox(in1, mx, my)) { focusOnly(in1); return true; }
            if (in2 != null && overBox(in2, mx, my)) { focusOnly(in2); return true; }
            return handled;
        }
        if (phase == PHASE_MAIN) {
            for (int i = 0; i < 4; i++) {
                Rect r = menuRect(i);
                if (mx >= r.x && mx <= r.x + r.w && my >= r.y && my <= r.y + r.h) { handleMenu(i); return true; }
            }
            return false;
        }
        if (mx >= panelX + 6 && mx <= panelX + panelW - 6 && my >= listContentTop()) {
            List<String> list = filtered();
            int idx = (int) ((my - listContentTop()) / ROW_H);
            int abs = scroll + idx;
            if (idx < visible && abs < list.size()) {
                selectedIdx = abs;
                openAt(list, abs);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (phase == PHASE_INPUT) {
            if (selectorOpen && mx >= selX && mx <= selX + selW && my >= selY && my <= selY + selH) {
                int maxScroll = Math.max(0, selectorIds.size() - selVisibleRows);
                selectorScroll = clamp(selectorScroll + (delta > 0 ? -2 : 2), maxScroll);
                return true;
            }
            return false;
        }
        if (phase == PHASE_LIST || phase == PHASE_METHODS || phase == PHASE_FIELDS) {
            List<String> list = filtered();
            int maxScroll = Math.max(0, list.size() - visible);
            scroll = clamp(scroll + (delta > 0 ? -1 : 1), maxScroll);
            return true;
        }
        return false;
    }

    private static int clamp(int v, int max) { return v < 0 ? 0 : Math.min(v, max); }

    private boolean inputHasFocus() {
        return (in1 != null && in1.isFocused()) || (in2 != null && in2.isFocused())
                || (search != null && search.isFocused());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (phase == PHASE_INPUT) { closeInput(); return true; }
            if (phase == PHASE_MAIN || phase == PHASE_METHODS || phase == PHASE_FIELDS) {
                if (phase == PHASE_MAIN) { backToList(); return true; }
                phase = PHASE_MAIN; scroll = 0; applySearchVisibility(); return true;
            }
            onClose();
            return true;
        }
        if (keyCode == 257 && phase == PHASE_INPUT) { execInput(); return true; }
        if (inputHasFocus()) return super.keyPressed(keyCode, scanCode, modifiers);
        if (phase == PHASE_METHODS && keyCode == 70) { openForceReturn(); return true; }
        if ((phase == PHASE_METHODS || phase == PHASE_FIELDS) && (keyCode == 264 || keyCode == 265)) {
            List<String> list = filtered();
            if (!list.isEmpty()) {
                int maxScroll = Math.max(0, list.size() - visible);
                int nxt = selectedIdx < 0 ? 0 : selectedIdx + (keyCode == 264 ? 1 : -1);
                nxt = clamp(nxt, list.size() - 1);
                selectedIdx = nxt;
                if (nxt < scroll) scroll = nxt;
                else if (nxt >= scroll + visible) scroll = nxt - visible + 1;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ---------------- 渲染 ----------------
    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float partialTicks) {
        renderDataStream(g);
        if (phase == PHASE_MAIN) renderMenu(g, mx, my);
        else if (phase == PHASE_INPUT) renderInputOverlay(g, mx, my);
        else renderPanel(g, mx, my);
        super.render(g, mx, my, partialTicks);
    }

    private Rect menuRect(int i) {
        int col = i % MENU_COLS;
        int row = i / MENU_COLS;
        int gapX = Math.max(16, Math.min(48, width - MENU_COLS * MENU_OPT_W - 40));
        int gapY = Math.max(16, Math.min(48, height - 2 * MENU_OPT_H - 110));
        float cx = width / 2.0f;
        float cyc = height / 2.0f + 10;
        float wx = cx + (col - (MENU_COLS - 1) / 2f) * (MENU_OPT_W + gapX);
        float wy = cyc + (row - 0.5f) * (MENU_OPT_H + gapY);
        return new Rect((int) (wx - MENU_OPT_W / 2f), (int) (wy - MENU_OPT_H / 2f), MENU_OPT_W, MENU_OPT_H, 0);
    }

    /** 主菜单：目标信息浮条 + 冻结徽章 + 四宫格按钮（hover 波纹）。 */
    private void renderMenu(GuiGraphics g, int mx, int my) {
        String[] labels = new String[]{
                loc("editor.mainMenu.invoke"), loc("editor.mainMenu.field"),
                loc("editor.mainMenu.force"), loc("editor.mainMenu.freeze") };
        String subtitle = loc("editor.input.target") + ": " + entityName + "  id " + entityId + "  "
                + (frozen ? loc("editor.input.frozen") : loc("editor.input.thawed"));
        int sw = font.width(subtitle) + 28;
        int sh = 22;
        int sx = width / 2 - sw / 2, sy = 14;
        g.fill(sx, sy, sx + sw, sy + sh, (int) (170 * alpha) << 24 | 0x0C141B);
        g.renderOutline(sx, sy, sw, sh, (int) (90 * alpha) << 24 | 0x3E8C55);
        g.drawCenteredString(font, Component.literal(subtitle), width / 2, sy + 6, (int) (230 * alpha) << 24 | 0x66FFAA);
        renderFreezeBadge(g);

        for (int i = 0; i < 4; i++) {
            Rect r = menuRect(i);
            boolean ho = mx >= r.x && mx <= r.x + r.w && my >= r.y && my <= r.y + r.h;
            boolean isFreeze = i == 3;
            int ba = (int) ((ho ? 255 : 190) * alpha);
            int fa = (int) ((ho ? 120 : 70) * alpha);
            int out = isFreeze ? (frozen ? 0x77CC44 : 0xFFCC44) : 0x55CC88;
            g.fill(r.x, r.y, r.x + r.w, r.y + r.h, fa << 24 | 0x102030);
            g.renderOutline(r.x, r.y, r.w, r.h, ba << 24 | out);
            g.fill(r.x + 1, r.y + 1, r.x + r.w - 1, r.y + 2, (int) (30 * alpha) << 24 | out);
            if (ho) {
                float cycle = (time % 1.2f) / 1.2f;
                for (int wv = 0; wv < 3; wv++) {
                    float p = cycle + wv / 3.0f;
                    p -= (int) p;
                    int expand = 2 + (int) (p * 12);
                    int wa = (int) ((1f - p) * (1f - p) * 90 * alpha);
                    if (wa <= 5) continue;
                    g.renderOutline(r.x - expand, r.y - expand, r.w + expand * 2, r.h + expand * 2,
                            wa << 24 | out);
                }
            }
            int ta = (int) ((ho ? 255 : 175) * alpha);
            g.drawCenteredString(font, Component.literal(labels[i]), r.x + r.w / 2, r.y + 13, ta << 24 | 0xDDFFEE);
        }
    }

    /** 列表面板：标题条（含注册表实体名）、搜索栏、列表（方法签名/字段分色渲染）与滚动条。 */
    private void renderPanel(GuiGraphics g, int mx, int my) {
        if (entityId >= 0 && minecraft != null && minecraft.level != null) {
            net.minecraft.world.entity.Entity e = minecraft.level.getEntity(entityId);
            if (e != null) {
                try {
                    String reg = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
                    String disp = Component.translatable(e.getType().getDescriptionId()).getString();
                    String part = reg + " · " + disp;
                    String hdr = switch (phase) {
                        case PHASE_METHODS -> loc("gui.transc.editor.methods");
                        case PHASE_FIELDS -> loc("gui.transc.editor.fields");
                        default -> loc("gui.transcend.editor.title");
                    };
                    int nx = panelX + PAD + 4 + font.width(hdr);
                    g.drawString(font, Component.literal(part), nx + 12, panelY + 8,
                            (int) (235 * alpha) << 24 | 0x66FFCC);
                } catch (Exception ignored) {}
            }
        }
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, (int) (185 * alpha) << 24 | 0x0B1216);
        g.renderOutline(panelX, panelY, panelW, panelH, (int) (150 * alpha) << 24 | 0x41B366);

        g.fill(panelX, panelY, panelX + panelW, panelY + HEADER_H, (int) (70 * alpha) << 24 | 0x0D2A1B);
        String header;
        int count;
        switch (phase) {
            case PHASE_METHODS -> { header = loc("gui.transc.editor.methods"); count = methodLines.size(); }
            case PHASE_FIELDS -> { header = loc("gui.transc.editor.fields"); count = fieldLines.size(); }
            default -> { header = loc("gui.transcend.editor.title"); count = entityIds.size(); }
        }
        g.drawString(font, Component.literal(header), panelX + PAD + 4, panelY + 8, (int) (235 * alpha) << 24 | 0xFFDDFFEE);
        int cw = font.width("(" + count + ")");
        g.drawString(font, Component.literal("(" + count + ")"), panelX + panelW - PAD - 4 - cw, panelY + 9,
                (int) (150 * alpha) << 24 | 0x88CC88);
        g.fill(panelX, panelY + HEADER_H, panelX + panelW, panelY + HEADER_H + 1, (int) (120 * alpha) << 24 | 0x2D8A55);

        repositionSearch();
        if (search != null && search.isVisible()) {
            int sx = search.getX() - 2, sy = search.getY() - 2;
            int sw = search.getWidth() + 4, sh = search.getHeight() + 4;
            boolean f = search.isFocused();
            g.fill(sx, sy, sx + sw, sy + sh, (int) ((f ? 85 : 40) * alpha) << 24 | 0x001210);
            g.renderOutline(sx, sy, sw, sh, (int) ((f ? 255 : 130) * alpha) << 24 | 0x44FF88);
            g.drawString(font, "⌕", panelX + 6, search.getY() + 3, (int) (170 * alpha) << 24 | 0x66BB88);
        }

        List<String> list = filtered();
        int listTop = listContentTop();
        visible = Math.max(1, (panelY + panelH - listTop - 8) / ROW_H);
        if (scroll > Math.max(0, list.size() - visible)) scroll = Math.max(0, list.size() - visible);
        hoveredIdx = -1;
        int top = listTop;
        if (!infoLine.isEmpty() && (phase == PHASE_METHODS || phase == PHASE_FIELDS)) {
            g.drawString(font, infoLine, panelX + 12, top, (int) (160 * alpha) << 24 | 0x55FFAA);
            top += ROW_H;
        }
        for (int i = 0; i < Math.min(visible, list.size() - scroll); i++) {
            int abs = scroll + i;
            int y = top + i * ROW_H;
            boolean hov = mx >= panelX + 6 && mx <= panelX + panelW - 6 && my >= y && my <= y + ROW_H;
            if (hov) hoveredIdx = abs;
            boolean sel = abs == selectedIdx;
            int rowL = panelX + 4, rowR = panelX + panelW - 4;
            if (sel || hov) {
                int baseCol = sel ? 0xFF9940 : 0xFFFFFF;
                int baseA = sel ? 0x60 : 0x30;
                int segs = 4, rowW = rowR - rowL;
                for (int sg = 0; sg < segs; sg++) {
                    int a = baseA * (segs - sg) / segs;
                    int x1 = rowL + rowW * sg / segs;
                    int x2 = rowL + rowW * (sg + 1) / segs;
                    g.fill(x1, y, x2, y + ROW_H, a << 24 | baseCol);
                }
            } else {
                g.fill(rowL, y, rowR, y + ROW_H, 0x28000000);
            }
            if (sel) g.fill(rowL, y + 1, rowL + 2, y + ROW_H - 1, (int) (255 * alpha) << 24 | 0xFF9940);
            else if (hov) g.fill(rowL, y + 1, rowL + 2, y + ROW_H - 1, (int) (220 * alpha) << 24 | 0x66FFCC);
            String line = list.get(abs);
            int fg = sel ? 0xFFFFDD88 : hov ? 0xFFFFFFFF : 0xFFAACCAA;
            int x = panelX + 12;
            if (phase == PHASE_METHODS) {
                int q = line.indexOf('(');
                int r = line.indexOf(')');
                String name = q > 0 ? line.substring(0, q) : line;
                String params = q > 0 && r > q ? line.substring(q + 1, r) : "";
                String rest = r >= 0 && r + 1 < line.length() ? line.substring(r + 1) : "";
                String[] hp = rest.split(" // ", 2);
                String drawSig = name + "(" + params + ")" + (hp.length == 1 ? rest : hp[0]).trim();
                String evidence = hp.length > 1 ? hp[1] : "";
                g.drawString(font, drawSig, x, y + 4, (int) (200 * alpha) << 24 | 0x88CC88);
                int nx = x + font.width(drawSig);
                if (!evidence.isEmpty())
                    g.drawString(font, " // " + evidence, nx + 2, y + 4, (int) (180 * alpha) << 24 | 0x66FFCC);
                String descKey = "editor.desc." + name;
                if (net.minecraft.client.resources.language.I18n.exists(descKey)
                        && !evidence.contains(net.minecraft.client.resources.language.I18n.get(descKey))) {
                    int textEnd = nx + (evidence.isEmpty() ? 0 : 2 + font.width(" // " + evidence));
                    int rightEdge = panelX + panelW - 10;
                    int avail = rightEdge - textEnd - 12;
                    if (avail >= 24) {
                        String d = net.minecraft.client.resources.language.I18n.get(descKey);
                        if (font.width(d) > avail)
                            d = font.plainSubstrByWidth(d, Math.max(0, avail - font.width("…"))) + "…";
                        g.drawString(font, d, rightEdge - font.width(d), y + 4,
                                (int) (150 * alpha) << 24 | 0x557755, false);
                    }
                }
            } else if (phase == PHASE_FIELDS) {
                int ci = line.indexOf(':');
                int eq = ci >= 0 ? line.indexOf('=', ci) : -1;
                int sl = line.indexOf(" // ", eq + 1);
                String valPart = eq > ci ? line.substring(eq + 1, sl > 0 ? sl : line.length()) : "";
                String hintStr = sl > 0 ? line.substring(sl) : "";
                if (ci > 0) {
                    String name = line.substring(0, ci);
                    String typePart = eq > ci ? line.substring(ci, eq) : line.substring(ci);
                    g.drawString(font, name, x, y + 4, fg);
                    int nx = x + font.width(name);
                    g.drawString(font, typePart, nx, y + 4, (int) (200 * alpha) << 24 | 0x88DDFF);
                    nx += font.width(typePart);
                    if (!valPart.isEmpty())
                        g.drawString(font, valPart, nx, y + 4, (int) (180 * alpha) << 24 | 0x88CC88);
                    if (!hintStr.isEmpty()) {
                        nx += valPart.isEmpty() ? 0 : font.width(valPart);
                        g.drawString(font, hintStr, nx + 2, y + 4, (int) (150 * alpha) << 24 | 0x66FFCC);
                    }
                } else {
                    g.drawString(font, line, x, y + 4, fg);
                }
            } else {
                g.drawString(font, line, x, y + 4, fg);
            }
        }
        if (list.size() > visible) {
            float maxScroll = Math.max(0, list.size() - visible);
            float trackBottom = panelY + panelH - 6;
            float trackTop = top;
            float barH = Math.max(20f, (trackBottom - trackTop) * (float) visible / list.size());
            float barY = trackTop + scroll / Math.max(1f, maxScroll) * ((trackBottom - trackTop) - barH);
            g.fill(panelX + panelW - 4, (int) barY, panelX + panelW - 1, (int) (barY + barH),
                    (int) (130 * alpha) << 24 | 0x44AA44);
        }
        if (list.isEmpty()) {
            String empty = phase == PHASE_LIST ? loc("gui.transcend.editor.hint") : loc("gui.transc.editor.hint");
            g.drawCenteredString(font, Component.literal(empty), panelX + panelW / 2,
                    top + ROW_H / 2 - 4, (int) (90 * alpha) << 24 | 0x6A8F6A);
        }
        renderFreezeBadge(g);
    }

    /** 右上角"已冻结"徽章。 */
    private void renderFreezeBadge(GuiGraphics g) {
        if (!frozen || phase == PHASE_LIST) return;
        String txt = loc("editor.input.frozen");
        int w = font.width(txt) + 18;
        int x = width - w - 10;
        g.fill(x, 8, x + w, 30, 0xD3000000);
        g.renderOutline(x, 8, w, 22, 0xFFC87800);
        g.drawString(font, Component.literal(txt), x + 9, 13, 0xFFFFD866);
    }

    /** 输入弹窗：标题/提示/两个输入框/预设 chip 区/侧边选择器。 */
    private void renderInputOverlay(GuiGraphics g, int mx, int my) {
        renderFreezeBadge(g);
        chipRects.clear();

        net.minecraft.world.entity.Entity target =
                (minecraft != null && minecraft.level != null && entityId >= 0)
                        ? minecraft.level.getEntity(entityId) : null;
        String[] paramTypes = inputAction == 0 ? splitTypes(savedParamTypes) : new String[0];
        String exampleStr = paramTypes.length > 0 ? exampleFor(target) : null;
        int ow = 360;
        int chipMaxW = ow - 32;
        int presetH = 0;
        if (exampleStr != null) presetH += 18;
        for (String t : paramTypes) {
            presetH += 12;
            presetH += chipRowsFor(effectiveChips(t, target), chipMaxW) * 18;
        }
        if (presetH > 0) presetH += 16;

        int cx = width / 2, cy = height / 2;
        int oh = Math.min(160 + presetH, height - 16);
        int ox = cx - ow / 2, oy = cy - oh / 2;

        g.fill(0, 0, width, height, (int) (90 * alpha) << 24);
        g.fill(ox, oy, ox + ow, oy + oh, (int) (245 * alpha) << 24 | 0x0E131B);
        g.renderOutline(ox, oy, ow, oh, (int) (255 * alpha) << 24 | 0x55FF88);

        String title = loc(inputAction == 0 ? "editor.input.invokeTitle"
                : inputAction == 1 ? "editor.input.setTitle" : "editor.input.forceTitle");
        g.drawCenteredString(font, Component.literal("§l" + title), cx, oy + 10, (int) (255 * alpha) << 24 | 0x55FF55);

        String hint;
        if (inputAction == 0) hint = savedParamTypes.isEmpty() ? loc("editor.input.noArgs")
                : loc("editor.input.needArgs") + ": " + savedParamTypes;
        else if (inputAction == 1) hint = loc("editor.input.setHint");
        else hint = loc("editor.input.forceHint") + ": " + savedReturnType;
        g.drawCenteredString(font, Component.literal(hint), cx, oy + 26, (int) (190 * alpha) << 24 | 0xDDEE88);

        String label1 = loc(inputAction == 1 ? "editor.input.field" : "editor.input.name");
        String label2 = loc(inputAction == 1 ? "editor.input.newval"
                : inputAction == 2 ? "editor.input.placeholder_return" : "editor.input.values");
        if (in1 != null) {
            g.drawCenteredString(font, Component.literal(label1), cx, oy + 44, (int) (190 * alpha) << 24 | 0xCCCCCC);
            drawInputBox(g, in1, cx, oy + 65);
        }
        if (in2 != null) {
            g.drawCenteredString(font, Component.literal(label2), cx, oy + 84, (int) (190 * alpha) << 24 | 0xCCCCCC);
            drawInputBox(g, in2, cx, oy + 105);
        }

        if (paramTypes.length > 0) {
            int py = oy + 122;
            g.drawString(font, loc("editor.input.presets"), ox + 16, py, (int) (170 * alpha) << 24 | 0x66CCAA);
            py += 14;
            if (exampleStr != null) {
                String exLabel = loc("editor.input.example") + ": " + exampleStr;
                int cwd = Math.min(font.width(exLabel) + 12, chipMaxW);
                int chx = ox + 16;
                boolean chov = mx >= chx && mx <= chx + cwd && my >= py && my <= py + 14;
                g.fill(chx, py, chx + cwd, py + 14, (int) ((chov ? 130 : 70) * alpha) << 24 | 0x3A2A0F);
                g.renderOutline(chx, py, cwd, 14,
                        (int) ((chov ? 255 : 190) * alpha) << 24 | (chov ? 0xFFDD88 : 0xCC9944));
                g.drawString(font, font.plainSubstrByWidth(exLabel, cwd - 10), chx + 6, py + 3,
                        (int) ((chov ? 255 : 220) * alpha) << 24 | 0xFFDD99, false);
                chipRects.add(new ChipRect(chx, py, cwd, 14, exampleStr, -1, false));
                py += 18;
            }
            for (int pi = 0; pi < paramTypes.length; pi++) {
                String t = paramTypes[pi];
                g.drawString(font, loc("editor.input.param") + (pi + 1) + ": " + t, ox + 16, py,
                        (int) (150 * alpha) << 24 | 0x88AACC);
                py += 12;
                String[] chips = effectiveChips(t, target);
                boolean hasBrowse = browsable(t);
                if (chips.length > 0) {
                    int chx = ox + 16;
                    for (int ci2 = 0; ci2 < chips.length; ci2++) {
                        String chip = chips[ci2];
                        boolean isBrowse = hasBrowse && ci2 == chips.length - 1;
                        int cwd = font.width(chip) + 10;
                        if (chx > ox + 16 && chx - (ox + 16) + cwd > chipMaxW) {
                            chx = ox + 16;
                            py += 18;
                        }
                        boolean chov = mx >= chx && mx <= chx + cwd && my >= py && my <= py + 14;
                        boolean selActive = isBrowse && selectorOpen && selectorParamIdx == pi;
                        if (isBrowse) {
                            g.fill(chx, py, chx + cwd, py + 14,
                                    (int) ((chov || selActive ? 140 : 80) * alpha) << 24 | 0x14503A);
                            g.renderOutline(chx, py, cwd, 14,
                                    (int) ((chov || selActive ? 255 : 200) * alpha) << 24 | 0x55FFAA);
                            g.drawString(font, chip, chx + 5, py + 3,
                                    (int) ((chov || selActive ? 255 : 230) * alpha) << 24 | 0xAAFFDD, false);
                        } else {
                            g.fill(chx, py, chx + cwd, py + 14, (int) ((chov ? 110 : 55) * alpha) << 24 | 0x0F3A28);
                            g.renderOutline(chx, py, cwd, 14,
                                    (int) ((chov ? 255 : 120) * alpha) << 24 | (chov ? 0x66FFCC : 0x2F8A5C));
                            g.drawString(font, chip, chx + 5, py + 3,
                                    (int) ((chov ? 255 : 190) * alpha) << 24 | (chov ? 0xCCFFEE : 0x77DDAA), false);
                        }
                        chipRects.add(new ChipRect(chx, py, cwd, 14, chip, pi, isBrowse));
                        chx += cwd + 6;
                    }
                    py += 18;
                }
            }
        }

        g.drawCenteredString(font, Component.literal(loc("editor.input.enter")), cx, oy + oh - 26, (int) (130 * alpha) << 24 | 0x88CCCC);
        g.drawCenteredString(font, Component.literal(loc("editor.input.numeric")), cx, oy + oh - 14, (int) (100 * alpha) << 24 | 0x557766);

        if (selectorOpen) renderSelector(g, mx, my, ox, oy, ow, oh);
    }

    /** 侧边注册表选择器：可滚动列表，放不下弹窗右侧时贴左侧。 */
    private void renderSelector(GuiGraphics g, int mx, int my, int ox, int oy, int ow, int oh) {
        int w = 160;
        int x = ox + ow + 8;
        if (x + w > width - 4) x = ox - 8 - w;
        if (x < 4) x = 4;
        selX = x; selY = oy; selW = w; selH = oh;
        g.fill(x, oy, x + w, oy + oh, (int) (245 * alpha) << 24 | 0x0B1216);
        g.renderOutline(x, oy, w, oh, (int) (255 * alpha) << 24 | 0x41B366);
        g.fill(x, oy, x + w, oy + 16, (int) (90 * alpha) << 24 | 0x0D2A1B);
        String title = loc("editor.input.browse") + " (" + selectorIds.size() + ")";
        g.drawString(font, font.plainSubstrByWidth(title, w - 12), x + 6, oy + 4,
                (int) (235 * alpha) << 24 | 0xDDFFEE, false);
        g.fill(x, oy + 16, x + w, oy + 17, (int) (120 * alpha) << 24 | 0x2D8A55);

        int listTop = oy + 18;
        int rowH = 13;
        selVisibleRows = Math.max(1, (oy + oh - listTop - 2) / rowH);
        if (selectorScroll > Math.max(0, selectorIds.size() - selVisibleRows))
            selectorScroll = Math.max(0, selectorIds.size() - selVisibleRows);
        for (int i = 0; i < Math.min(selVisibleRows, selectorIds.size() - selectorScroll); i++) {
            int idx = selectorScroll + i;
            int ry = listTop + i * rowH;
            boolean hov = mx >= x && mx <= x + w && my >= ry && my <= ry + rowH;
            if (hov) g.fill(x + 1, ry, x + w - 1, ry + rowH, (int) (60 * alpha) << 24 | 0x66FFCC);
            String id = selectorIds.get(idx);
            String show = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
            g.drawString(font, font.plainSubstrByWidth(show, w - 14), x + 6, ry + 2,
                    (int) ((hov ? 255 : 180) * alpha) << 24 | (hov ? 0xCCFFEE : 0x88CCAA), false);
        }
        if (selectorIds.size() > selVisibleRows) {
            float maxScroll = Math.max(1, selectorIds.size() - selVisibleRows);
            float trackTop = listTop, trackBottom = oy + oh - 2;
            float barH = Math.max(12f, (trackBottom - trackTop) * (float) selVisibleRows / selectorIds.size());
            float barY = trackTop + selectorScroll / maxScroll * ((trackBottom - trackTop) - barH);
            g.fill(x + w - 3, (int) barY, x + w - 1, (int) (barY + barH),
                    (int) (130 * alpha) << 24 | 0x44AA44);
        }
    }

    /** 绘制输入框背景/边框并把文字渲染交给 EditBox 本身，同时同步其位置尺寸。 */
    private void drawInputBox(GuiGraphics g, EditBox box, int cx, int cy) {
        int w = Math.min(INPUT_W, box.getWidth());
        int x = cx - w / 2, y = cy - 9, h = 18;
        box.setX(x); box.setY(y); box.setWidth(w); box.setHeight(h);
        boolean f = box.isFocused();
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, (int) ((f ? 90 : 45) * alpha) << 24 | 0x001210);
        g.renderOutline(x - 2, y - 2, w + 4, h + 4, (int) ((f ? 255 : 130) * alpha) << 24 | 0x44FF88);
    }

    /** Matrix 字符雨背景：全部随机性来自列号确定性哈希，流头在屏幕底部淡出。 */
    private void renderDataStream(GuiGraphics g) {
        final int gap = 18;
        final int step = 12;
        int cols = width / gap + 1;
        float fadeStart = height * 0.72f;
        float fadeRange = Math.max(1f, height * 0.28f);
        for (int c = 0; c < cols; c++) {
            long h = (c * 2654435761L) & 0x7FFFFFFFL;
            int x = c * gap + (int) (h % 5) - 2;
            float speed = 34f + (h >> 3) % 46;
            int tail = 14 + (int) ((h >> 9) % 9);
            int total = height + tail * step + 60;
            float phaseOff = ((h >> 5) % 1000) / 1000f;
            int headY = (int) ((time * speed + phaseOff * total) % total) - tail * step;
            for (int k = 0; k < tail; k++) {
                int y = headY - k * step;
                if (y < -step || y > height) continue;
                float fadeBottom = y <= fadeStart ? 1f
                        : Math.max(0f, 1f - (y - fadeStart) / fadeRange);
                if (fadeBottom <= 0.02f) continue;
                char cc = streamChar(c, k, (int) (time * (k == 0 ? 14 : 6)) + (int) (h % 13));
                int a;
                int rgb;
                if (k == 0) {
                    a = (int) (alpha * 200 * fadeBottom);
                    rgb = 0x99FFAA;
                } else {
                    float fade = 1f - k / (float) tail;
                    a = (int) (alpha * (20 + 180 * fade) * fadeBottom);
                    rgb = lerpColor(0x1E5C34, 0x66FF88, fade);
                }
                if (a <= 5) continue;
                g.drawString(font, String.valueOf(cc), x, y, a << 24 | rgb, false);
                if (k == 0) g.drawString(font, String.valueOf(cc), x, y,
                        (int) (alpha * 200 * fadeBottom) << 24 | 0xCCFFDD, false);
            }
        }
        for (int s = 0; s < 14; s++) {
            long h = ((s + 101) * 2654435761L) & 0x7FFFFFFFL;
            float dx = 3f + (h >> 4) % 6;
            float dy = 2f + (h >> 9) % 5;
            float sx = ((h % 977) / 977f * width + time * dx) % (width + 2);
            float sy = (((h >> 7) % 953) / 953f * height + time * dy) % (height + 2);
            int size = 1 + (int) (h % 2);
            float tw = (float) Math.sin(time * (0.6f + (h % 5) * 0.25f) + s * 1.7f);
            int a = (int) (alpha * (15 + 15 * tw));
            if (a < 4) continue;
            g.fill((int) sx, (int) sy, (int) sx + size, (int) sy + size, a << 24 | 0x55CC77);
        }
    }

    private static final String CHARS = "0123456789<>#$%&abcdefghjklnmopqrstuvwxyz";
    private static char streamChar(int col, int k, int t) {
        int i = (col * 31 + k * 7 + t * 5) % CHARS.length();
        return CHARS.charAt(i < 0 ? i + CHARS.length() : i);
    }

    /** 两个 0xRRGGBB 颜色间逐通道线性插值：t=0 → c1，t=1 → c2。 */
    private static int lerpColor(int c1, int c2, float t) {
        t = t < 0f ? 0f : Math.min(1f, t);
        int r = (int) (((c1 >> 16) & 0xFF) + ((((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t));
        int gr = (int) (((c1 >> 8) & 0xFF) + ((((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t));
        int b = (int) ((c1 & 0xFF) + ((((c2 & 0xFF) - (c1 & 0xFF)) * t)));
        return r << 16 | gr << 8 | b;
    }

    private String loc(String key) { return Component.translatable(key).getString(); }
}