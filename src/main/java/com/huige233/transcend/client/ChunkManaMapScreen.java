package com.huige233.transcend.client;

import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 区块魔力地图 GUI — 由 {@code /tr_mana_map} 指令触发，展示玩家周围一圈区块的魔力浓度与地脉分级。
 *
 * <p>布局：以玩家所在区块为中心的方阵网格（边长 2*radius+1）。
 * 每格显示当前魔力数值，背景按 {@link com.huige233.transcend.world.mana.ChunkManaSavedData.Tier} 着色，
 * 中心格高亮金边，已注册稳定器的区块左上角加 ★ 标记。
 *
 * <p>悬停某格时显示该区块绝对坐标 + 魔力 / 上限 + 分级名 + 稳定器状态。
 */
public class ChunkManaMapScreen extends Screen {

    // ─── 调色板 ─────────────────────────────────────────────────────
    /** 0 = EXHAUSTED, 1 = WEAK, 2 = STABLE, 3 = RICH */
    private static final int[] TIER_FILL = {
            0xFF5A1F1F, // EXHAUSTED — 暗红
            0xFF5C3A1A, // WEAK      — 橙褐
            0xFF1F5C2E, // STABLE    — 绿
            0xFF1F3F8C  // RICH      — 蓝
    };
    /** 稳定器存在时叠加偏亮色（用于 WEAK 等档加成提示） */
    private static final int[] TIER_FILL_STABILIZED = {
            0xFF6E2A2A,
            0xFF7F5424, // 稳定器下 WEAK 视觉略亮
            0xFF26703A,
            0xFF254AA8
    };
    private static final int C_BG_OVERLAY  = 0xC0050510;
    private static final int C_PANEL       = 0xE008081A;
    private static final int C_BORDER      = 0xFF2A2A5A;
    private static final int C_CELL_BORDER = 0xFF000000;
    private static final int C_CENTER_RING = 0xFFFFCC00;
    private static final int C_TEXT        = 0xFFFFFFFF;
    private static final int C_TEXT_DIM    = 0xFFAAAACC;

    // ─── 数据 ───────────────────────────────────────────────────────
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final String dimensionName;
    private final float[] mana;
    private final byte[] tier;
    private final boolean[] stabilized;
    private final int side; // 2*radius + 1

    // 自适应几何（每帧根据视口重算）
    private int cellSize;
    private int gridOX;
    private int gridOY;

    public ChunkManaMapScreen(int centerX, int centerZ, int radius, String dimensionName,
                              float[] mana, byte[] tier, boolean[] stabilized) {
        super(Component.translatable("screen.transcend.chunk_mana_map"));
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.dimensionName = dimensionName;
        this.mana = mana;
        this.tier = tier;
        this.stabilized = stabilized;
        this.side = 2 * radius + 1;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.transcend.chunk_mana_map.close"),
                        b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20)
                .build());
    }

    /** 根据当前视口决定单元格大小，目标：网格高占视口 70%、宽不溢出。 */
    private void recomputeLayout() {
        int reservedTop = 56;     // 标题 + 维度行 + 间距
        int reservedBot = 64;     // 提示行 + 关闭按钮区
        int availH = Math.max(80, this.height - reservedTop - reservedBot);
        int availW = Math.max(80, this.width - 24);

        int byH = availH / side;
        int byW = availW / side;
        cellSize = Math.max(16, Math.min(40, Math.min(byH, byW)));

        int gridSize = side * cellSize;
        gridOX = (this.width - gridSize) / 2;
        gridOY = reservedTop + (availH - gridSize) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // 半透明全屏遮罩 — 不调用 renderBackground 避免暗化背景过深，让玩家仍能看到世界
        gfx.fill(0, 0, this.width, this.height, C_BG_OVERLAY);
        recomputeLayout();

        // ── 顶部标题 ──
        Component title = Component.translatable("screen.transcend.chunk_mana_map.title", centerX, centerZ);
        gfx.drawCenteredString(this.font, title, this.width / 2, 12, C_TEXT);
        gfx.drawCenteredString(this.font,
                Component.literal(dimensionName).withStyle(ChatFormatting.GRAY),
                this.width / 2, 26, C_TEXT_DIM);
        gfx.drawCenteredString(this.font,
                Component.translatable("screen.transcend.chunk_mana_map.subtitle",
                        side, side, ChunkManaSavedData.MAX_MANA),
                this.width / 2, 38, C_TEXT_DIM);

        // ── 网格容器外框 ──
        int gridSize = side * cellSize;
        gfx.fill(gridOX - 4, gridOY - 4, gridOX + gridSize + 4, gridOY + gridSize + 4, C_BORDER);
        gfx.fill(gridOX - 3, gridOY - 3, gridOX + gridSize + 3, gridOY + gridSize + 3, C_PANEL);

        // ── 单元格 ──
        int hoverIdx = -1;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int idx = (dz + radius) * side + (dx + radius);
                int cx = gridOX + (dx + radius) * cellSize;
                int cy = gridOY + (dz + radius) * cellSize;

                int fill = colorForCell(tier[idx], stabilized[idx]);
                gfx.fill(cx, cy, cx + cellSize, cy + cellSize, fill);

                // 单元格细边
                drawCellBorder(gfx, cx, cy, cellSize, C_CELL_BORDER);

                // 中心格高亮金边
                if (dx == 0 && dz == 0) {
                    drawCellBorder(gfx, cx - 1, cy - 1, cellSize + 2, C_CENTER_RING);
                    drawCellBorder(gfx, cx, cy, cellSize, C_CENTER_RING);
                }

                // 魔力数值（字号自适应：cell ≥ 24 时显示数值）
                if (cellSize >= 18) {
                    String txt = formatMana(mana[idx]);
                    int tw = this.font.width(txt);
                    int tx = cx + (cellSize - tw) / 2;
                    int ty = cy + (cellSize - this.font.lineHeight) / 2;
                    gfx.drawString(this.font, txt, tx, ty, C_TEXT, true);
                }

                // 稳定器标记
                if (stabilized[idx] && cellSize >= 16) {
                    gfx.drawString(this.font, "★", cx + 2, cy + 1, 0xFFFFEE66, true);
                }

                if (mouseX >= cx && mouseX < cx + cellSize
                        && mouseY >= cy && mouseY < cy + cellSize) {
                    hoverIdx = idx;
                }
            }
        }

        // ── 悬停信息 ──
        int infoY = gridOY + gridSize + 10;
        if (hoverIdx >= 0) {
            int dz = hoverIdx / side - radius;
            int dx = hoverIdx % side - radius;
            int absX = centerX + dx;
            int absZ = centerZ + dz;
            Component info = Component.translatable(
                    "screen.transcend.chunk_mana_map.cell",
                    absX, absZ,
                    String.format("%.1f", mana[hoverIdx]),
                    String.format("%.0f", ChunkManaSavedData.MAX_MANA),
                    Component.translatable(tierKey(tier[hoverIdx]))
                            .withStyle(tierColor(tier[hoverIdx])),
                    stabilized[hoverIdx]
                            ? Component.literal(" ★").withStyle(ChatFormatting.YELLOW)
                            : Component.empty()
            );
            gfx.drawCenteredString(this.font, info, this.width / 2, infoY, C_TEXT);
        } else {
            gfx.drawCenteredString(this.font,
                    Component.translatable("screen.transcend.chunk_mana_map.hover_hint")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    this.width / 2, infoY, 0xFF666688);
        }

        // ── 图例 ──
        int legendY = infoY + 14;
        renderLegend(gfx, legendY);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderLegend(GuiGraphics gfx, int y) {
        // 4 个色块横排居中
        String[] keys = {
                "screen.transcend.chunk_mana_map.tier.exhausted",
                "screen.transcend.chunk_mana_map.tier.weak",
                "screen.transcend.chunk_mana_map.tier.stable",
                "screen.transcend.chunk_mana_map.tier.rich"
        };
        // 估算总宽
        int swatch = 8;
        int gap = 6;
        int sep = 14;
        int totalW = 0;
        int[] widths = new int[4];
        for (int i = 0; i < 4; i++) {
            widths[i] = this.font.width(Component.translatable(keys[i]));
            totalW += swatch + gap + widths[i];
            if (i < 3) totalW += sep;
        }
        int x = (this.width - totalW) / 2;
        for (int i = 0; i < 4; i++) {
            gfx.fill(x, y + 1, x + swatch, y + 1 + swatch, TIER_FILL[i]);
            gfx.fill(x, y + 1, x + swatch, y + 2, 0xFF000000);
            gfx.fill(x, y + swatch, x + swatch, y + 1 + swatch, 0xFF000000);
            gfx.fill(x, y + 1, x + 1, y + 1 + swatch, 0xFF000000);
            gfx.fill(x + swatch - 1, y + 1, x + swatch, y + 1 + swatch, 0xFF000000);
            x += swatch + gap;
            gfx.drawString(this.font, Component.translatable(keys[i]), x, y, C_TEXT_DIM, false);
            x += widths[i] + sep;
        }
    }

    private static int colorForCell(byte t, boolean stabilized) {
        int ord = (t >= 0 && t < TIER_FILL.length) ? t : 0;
        return stabilized ? TIER_FILL_STABILIZED[ord] : TIER_FILL[ord];
    }

    private static String tierKey(byte t) {
        return switch (t) {
            case 0 -> "screen.transcend.chunk_mana_map.tier.exhausted";
            case 1 -> "screen.transcend.chunk_mana_map.tier.weak";
            case 2 -> "screen.transcend.chunk_mana_map.tier.stable";
            case 3 -> "screen.transcend.chunk_mana_map.tier.rich";
            default -> "screen.transcend.chunk_mana_map.tier.exhausted";
        };
    }

    private static ChatFormatting tierColor(byte t) {
        return switch (t) {
            case 0 -> ChatFormatting.RED;
            case 1 -> ChatFormatting.GOLD;
            case 2 -> ChatFormatting.GREEN;
            case 3 -> ChatFormatting.AQUA;
            default -> ChatFormatting.GRAY;
        };
    }

    private static void drawCellBorder(GuiGraphics gfx, int x, int y, int size, int color) {
        gfx.fill(x, y, x + size, y + 1, color);
        gfx.fill(x, y + size - 1, x + size, y + size, color);
        gfx.fill(x, y, x + 1, y + size, color);
        gfx.fill(x + size - 1, y, x + size, y + size, color);
    }

    /** 紧凑数值格式化：>= 1000 用 "1.2k" 形式；否则整数。 */
    private static String formatMana(float v) {
        if (v >= 1000f) return String.format("%.1fk", v / 1000f);
        return String.format("%d", (int) v);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
