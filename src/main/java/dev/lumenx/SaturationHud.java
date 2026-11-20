package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;

/** Saturation HUD: полоска и число насыщения над шкалой голода. */
public final class SaturationHud {
    public static final Module M = new Module("Saturation HUD",
            "Shows your saturation level", Category.VISUALS, true, true);
    static final Setting NUMBER = M.add(Setting.bool("number", "Show number", true));
    static final Setting FOOD   = M.add(Setting.bool("food", "Show food level", true));
    static final Setting OFF_X  = M.add(Setting.num("off_x", "Offset X", 0, -300, 300, 1));
    static final Setting OFF_Y  = M.add(Setting.num("off_y", "Offset Y", 0, -100, 100, 1));

    private static final int BAR_W = 81, BAR_H = 5;

    private SaturationHud() { }

    static void init() {
        HudElementRegistry.addLast(Identifier.of(LumenAddons.ID, "saturation_hud"),
                (ctx, tick) -> render(ctx));
    }

    private static void render(DrawContext ctx) {
        if (!M.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || p.isCreative() || p.isSpectator()) return;

        float sat = p.getHungerManager().getSaturationLevel();
        int food = p.getHungerManager().getFoodLevel();

        // правый край шкалы голода ванили: центр + 91, высота: низ - 39
        int right = ctx.getScaledWindowWidth() / 2 + 91 + (int) OFF_X.num;
        int x = right - BAR_W;
        int y = ctx.getScaledWindowHeight() - 49 - 11 + (int) OFF_Y.num;

        float ratio = Math.max(0f, Math.min(1f, sat / 20f));
        int rgb = sat < 2f ? 0xFF5A3C : 0xFFD23C;       // почти пусто — краснеет

        ctx.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xA0000000);
        ctx.fill(x, y, x + BAR_W, y + BAR_H, 0xFF2A2A2A);
        int w = Math.round(BAR_W * ratio);
        if (w > 0) ctx.fill(x, y, x + w, y + BAR_H, 0xFF000000 | rgb);

        if (NUMBER.bool) {
            String t = String.format("%.1f", sat);
            if (FOOD.bool) t = t + "  (" + food + ")";
            int tw = mc.textRenderer.getWidth(t);
            ctx.drawText(mc.textRenderer, t, right - tw, y - 10, 0xFFFFFFFF, true);
        }
    }
}
