package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumenx.mixin.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

/** Показывает содержимое шалкера (сетка 9×3) при наведении на него в любом инвентаре. */
public final class ShulkerPreview {
    public static final Module M = new Module("Shulker preview",
            "See shulker contents without opening it", Category.VISUALS, true, true);
    static final Setting SHIFT  = M.add(Setting.bool("shift_only", "Hold Shift only", false));
    static final Setting EMPTY  = M.add(Setting.bool("show_empty", "Show empty slots", true));
    static final Setting COUNTS = M.add(Setting.bool("counts", "Show counts", true));

    private static final int CELL = 18, COLS = 9, ROWS = 3, PAD = 4;

    static void init() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof HandledScreen<?> hs) {
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, td) -> draw(client, hs, ctx));
            }
        });
    }

    static boolean isShulker(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    private static boolean shiftDown(MinecraftClient mc) {
        long handle = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static void draw(MinecraftClient mc, HandledScreen<?> hs, DrawContext ctx) {
        if (!M.enabled || (SHIFT.bool && !shiftDown(mc))) return;
        HandledScreenAccessor acc = (HandledScreenAccessor) hs;
        Slot slot = acc.lumenx$getFocusedSlot();
        if (slot == null) return;
        ItemStack box = slot.getStack();
        if (!isShulker(box)) return;

        DefaultedList<ItemStack> items = DefaultedList.ofSize(COLS * ROWS, ItemStack.EMPTY);
        ContainerComponent comp = box.get(DataComponentTypes.CONTAINER);
        if (comp != null) comp.copyTo(items);

        int w = COLS * CELL + PAD * 2, h = ROWS * CELL + PAD * 2;
        int sx = acc.lumenx$getX() + slot.x, sy = acc.lumenx$getY() + slot.y;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int px = Math.max(2, Math.min(sw - w - 2, sx + 8 - w / 2));
        int py = sy - h - 6;
        if (py < 2) py = Math.min(sh - h - 2, sy + 24);

        ctx.fill(px - 1, py - 1, px + w + 1, py + h + 1, 0xFFB07CE0);   // рамка
        ctx.fill(px, py, px + w, py + h, 0xF0101018);                    // фон
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = px + PAD + c * CELL, y = py + PAD + r * CELL;
                ItemStack st = items.get(r * COLS + c);
                if (st.isEmpty() && !EMPTY.bool) continue;
                ctx.fill(x, y, x + CELL - 1, y + CELL - 1, 0x30FFFFFF);
                if (st.isEmpty()) continue;
                ctx.drawItem(st, x, y);
                if (COUNTS.bool) ctx.drawStackOverlay(mc.textRenderer, st, x, y);
            }
        }
    }
}
