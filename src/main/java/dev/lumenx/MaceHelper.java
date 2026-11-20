package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

/** Mace helper: процент «зарядки» булавы (высота падения), бонус урона и подсветка булавы в хотбаре. */
public final class MaceHelper {
    public static final Module M = new Module("Mace helper",
            "Mace charge and highlight", Category.VISUALS, false, true);
    static final Setting FULL      = M.add(Setting.num("full", "Full charge (blocks)", 10, 3, 30, 1));
    static final Setting TEXT      = M.add(Setting.bool("text", "Show text", true));
    static final Setting HIGHLIGHT = M.add(Setting.bool("highlight", "Highlight mace", true));
    static final Setting OFF_X     = M.add(Setting.num("off_x", "Offset X", 0, -200, 200, 1));
    static final Setting OFF_Y     = M.add(Setting.num("off_y", "Offset Y", 24, -100, 200, 1));

    private static double maxY;
    private static double fall;

    private MaceHelper() { }

    /** Высота падения: от верхней точки после отрыва от земли. */
    static void update(ClientPlayerEntity p) {
        if (!M.enabled) return;
        if (p.isOnGround() || p.isTouchingWater() || p.hasVehicle() || p.isClimbing()) {
            maxY = p.getY();
            fall = 0.0;
        } else {
            maxY = Math.max(maxY, p.getY());
            fall = Math.max(0.0, maxY - p.getY());
        }
    }

    /** Бонусный урон булавы за падение (формула ванили). */
    static double bonus(double d) {
        return 4.0 * Math.min(d, 3.0) + 2.0 * Math.min(Math.max(d - 3.0, 0.0), 5.0) + Math.max(d - 8.0, 0.0);
    }

    static void hud(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p) {
        if (!M.enabled) return;
        boolean ready = fall > 1.5;
        float pct = MathHelper.clamp((float) (fall / FULL.num), 0f, 1f);
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();

        // подсветка булавы в хотбаре
        if (HIGHLIGHT.bool) {
            PlayerInventory inv = p.getInventory();
            for (int i = 0; i < 9; i++) {
                if (!inv.getStack(i).isOf(Items.MACE)) continue;
                int x = sw / 2 - 91 + i * 20, y = sh - 22;
                float pulse = ready ? 0.7f + 0.3f * MathHelper.sin(System.currentTimeMillis() / 120f) : 0.45f;
                Fx.rectGlow(ctx, x, y, x + 22, y + 22, ready ? 0x55FF7A : 0xFFB02E, pulse);
            }
        }

        // панель под прицелом, пока булава в руке
        if (TEXT.bool && p.getMainHandStack().isOf(Items.MACE)) {
            int cx = sw / 2 + (int) OFF_X.num, y = sh / 2 + (int) OFF_Y.num;
            String t = String.format("Mace %.1fm  +%.0f dmg  %d%%", fall, bonus(fall), Math.round(pct * 100f));
            int tw = mc.textRenderer.getWidth(t);
            ctx.drawText(mc.textRenderer, t, cx - tw / 2, y, ready ? 0xFF55FF7A : 0xFFFFFFFF, true);
            int bw = 70, bx = cx - bw / 2, by = y + 11;
            Fx.fill(ctx, bx - 1, by - 1, bx + bw + 1, by + 5, 0x000000, 0.6f);
            Fx.fill(ctx, bx, by, bx + Math.round(bw * pct), by + 4, ready ? 0x55FF7A : 0xFFB02E, 0.95f);
        }
    }
}
