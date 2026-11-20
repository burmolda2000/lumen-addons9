package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** Inventory HUD: содержимое инвентаря (3 ряда + по желанию хотбар) прямо на экране. */
public final class InventoryHud {
    public static final Module M = new Module("Inventory HUD",
            "Shows your inventory on the game screen", Category.VISUALS, true, true);
    static final Setting POS    = M.add(Setting.mode("pos", "Position", 0,
            "Bottom left", "Bottom right", "Top left", "Top right"));
    static final Setting HOTBAR = M.add(Setting.bool("hotbar", "Show hotbar row", false));
    static final Setting COUNTS = M.add(Setting.bool("counts", "Show counts", true));
    static final Setting BG     = M.add(Setting.bool("background", "Background", true));
    static final Setting OFF_X  = M.add(Setting.num("off_x", "Offset X", 4, 0, 300, 1));
    static final Setting OFF_Y  = M.add(Setting.num("off_y", "Offset Y", 4, 0, 300, 1));

    private static final int CELL = 18, COLS = 9, PAD = 4;

    private InventoryHud() { }

    static void init() {
        HudElementRegistry.addLast(Identifier.of(LumenAddons.ID, "inventory_hud"),
                (ctx, tick) -> render(ctx));
    }

    private static void render(DrawContext ctx) {
        if (!M.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.currentScreen instanceof HandledScreen<?>) return;

        PlayerInventory inv = p.getInventory();
        int rows = HOTBAR.bool ? 4 : 3;
        int w = COLS * CELL + PAD * 2;
        int h = rows * CELL + PAD * 2 + (HOTBAR.bool ? 4 : 0);
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int ox = (int) OFF_X.num, oy = (int) OFF_Y.num;

        int px = (POS.mode == 1 || POS.mode == 3) ? sw - w - ox : ox;
        int py = (POS.mode == 0 || POS.mode == 1) ? sh - h - oy : oy;

        if (BG.bool) {
            ctx.fill(px - 1, py - 1, px + w + 1, py + h + 1, 0xFFB07CE0);
            ctx.fill(px, py, px + w, py + h, 0xB0101018);
        }

        // основной инвентарь: слоты 9..35
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < COLS; c++) {
                drawSlot(ctx, mc, inv.getStack(9 + r * COLS + c), px + PAD + c * CELL, py + PAD + r * CELL);
            }
        }
        // хотбар: слоты 0..8
        if (HOTBAR.bool) {
            int hy = py + PAD + 3 * CELL + 4;
            for (int c = 0; c < COLS; c++) {
                drawSlot(ctx, mc, inv.getStack(c), px + PAD + c * CELL, hy);
            }
        }
    }

    private static void drawSlot(DrawContext ctx, MinecraftClient mc, ItemStack st, int x, int y) {
        if (st.isEmpty()) return;
        ctx.drawItem(st, x, y);
        if (COUNTS.bool) ctx.drawStackOverlay(mc.textRenderer, st, x, y);
    }
}
