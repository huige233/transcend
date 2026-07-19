package com.huige233.transcend.client;

import com.huige233.transcend.world.mana.ChunkManaSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ChunkManaMapScreen extends Screen {

    private static final int[] TIER_FILL = {
            0xFF5A1F1F,
            0xFF5C3A1A,
            0xFF1F5C2E,
            0xFF1F3F8C
    };

    private static final int[] TIER_FILL_STABILIZED = {
            0xFF6E2A2A,
            0xFF7F5424,
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

    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final String dimensionName;
    private final float[] mana;
    private final byte[] tier;
    private final boolean[] stabilized;
    private final int side;

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

    private void recomputeLayout() {
        int reservedTop = 56;
        int reservedBot = 64;
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

        gfx.fill(0, 0, this.width, this.height, C_BG_OVERLAY);
        recomputeLayout();

        Component title = Component.translatable("screen.transcend.chunk_mana_map.title", centerX, centerZ);
        gfx.drawCenteredString(this.font, title, this.width / 2, 12, C_TEXT);
        gfx.drawCenteredString(this.font,
                Component.literal(dimensionName).withStyle(ChatFormatting.GRAY),
                this.width / 2, 26, C_TEXT_DIM);
        gfx.drawCenteredString(this.font,
                Component.translatable("screen.transcend.chunk_mana_map.subtitle",
                        side, side, ChunkManaSavedData.MAX_MANA),
                this.width / 2, 38, C_TEXT_DIM);

        int gridSize = side * cellSize;
        gfx.fill(gridOX - 4, gridOY - 4, gridOX + gridSize + 4, gridOY + gridSize + 4, C_BORDER);
        gfx.fill(gridOX - 3, gridOY - 3, gridOX + gridSize + 3, gridOY + gridSize + 3, C_PANEL);

        int hoverIdx = -1;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int idx = (dz + radius) * side + (dx + radius);
                int cx = gridOX + (dx + radius) * cellSize;
                int cy = gridOY + (dz + radius) * cellSize;

                int fill = colorForCell(tier[idx], stabilized[idx]);
                gfx.fill(cx, cy, cx + cellSize, cy + cellSize, fill);

                drawCellBorder(gfx, cx, cy, cellSize, C_CELL_BORDER);

                if (dx == 0 && dz == 0) {
                    drawCellBorder(gfx, cx - 1, cy - 1, cellSize + 2, C_CENTER_RING);
                    drawCellBorder(gfx, cx, cy, cellSize, C_CENTER_RING);
                }

                if (cellSize >= 18) {
                    String txt = formatMana(mana[idx]);
                    int tw = this.font.width(txt);
                    int tx = cx + (cellSize - tw) / 2;
                    int ty = cy + (cellSize - this.font.lineHeight) / 2;
                    gfx.drawString(this.font, txt, tx, ty, C_TEXT, true);
                }

                if (stabilized[idx] && cellSize >= 16) {
                    gfx.drawString(this.font, "★", cx + 2, cy + 1, 0xFFFFEE66, true);
                }

                if (mouseX >= cx && mouseX < cx + cellSize
                        && mouseY >= cy && mouseY < cy + cellSize) {
                    hoverIdx = idx;
                }
            }
        }

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

        int legendY = infoY + 14;
        renderLegend(gfx, legendY);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderLegend(GuiGraphics gfx, int y) {

        String[] keys = {
                "screen.transcend.chunk_mana_map.tier.exhausted",
                "screen.transcend.chunk_mana_map.tier.weak",
                "screen.transcend.chunk_mana_map.tier.stable",
                "screen.transcend.chunk_mana_map.tier.rich"
        };

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

    private static String formatMana(float v) {
        if (v >= 1000f) return String.format("%.1fk", v / 1000f);
        return String.format("%d", (int) v);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
