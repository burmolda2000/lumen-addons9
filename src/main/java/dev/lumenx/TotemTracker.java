package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Totem tracker: считает использованные тотемы у игроков вокруг.
 * Всплытие определяется по эффекту «Поглощение II» (5 сек), который даёт только тотем.
 */
public final class TotemTracker {
    public static final Module M = new Module("Totem tracker",
            "Counts totem pops", Category.VISUALS, false, true);
    static final Setting LIST  = M.add(Setting.bool("list", "Show list", true));
    static final Setting CHAT  = M.add(Setting.bool("chat", "Chat message", true));
    static final Setting MAX   = M.add(Setting.num("max", "Max entries", 6, 2, 12, 1));
    static final Setting OFF_X = M.add(Setting.num("off_x", "Offset X", 4, 0, 400, 1));
    static final Setting OFF_Y = M.add(Setting.num("off_y", "Offset Y", 90, 0, 300, 1));

    private static final Map<String, Integer> POPS = new LinkedHashMap<>();
    private static final Map<String, Long> WHEN = new HashMap<>();
    private static final Map<Integer, Integer> PREV_ABS = new HashMap<>();
    private static ClientWorld lastWorld;

    private TotemTracker() { }

    static void update(ClientWorld w, ClientPlayerEntity me, long now) {
        if (!M.enabled) return;
        if (w != lastWorld) { lastWorld = w; POPS.clear(); WHEN.clear(); PREV_ABS.clear(); }

        for (AbstractClientPlayerEntity p : w.getPlayers()) {
            StatusEffectInstance abs = p.getStatusEffect(StatusEffects.ABSORPTION);
            int dur = abs != null && abs.getAmplifier() == 1 ? abs.getDuration() : 0;
            int prev = PREV_ABS.getOrDefault(p.getId(), 0);
            PREV_ABS.put(p.getId(), dur);
            if (dur >= 90 && prev < 80) {
                String name = p.getName().getString();
                int n = POPS.merge(name, 1, Integer::sum);
                WHEN.put(name, now);
                if (CHAT.bool) me.sendMessage(Text.literal("Totem: " + name + " popped (" + n + ")"), false);
            }
        }
    }

    static void hud(DrawContext ctx, MinecraftClient mc, long now) {
        if (!M.enabled || !LIST.bool || POPS.isEmpty()) return;
        List<Map.Entry<String, Integer>> list = new ArrayList<>(POPS.entrySet());
        list.sort((a, b) -> Long.compare(WHEN.getOrDefault(b.getKey(), 0L), WHEN.getOrDefault(a.getKey(), 0L)));
        int x = (int) OFF_X.num, y = (int) OFF_Y.num;
        int rows = Math.min(list.size(), (int) MAX.num);
        int w = 120;
        Fx.fill(ctx, x - 3, y - 3, x + w, y + rows * 11 + 11, 0x101018, 0.7f);
        ctx.drawText(mc.textRenderer, "Totems", x, y, 0xFFFFD700, true);
        for (int i = 0; i < rows; i++) {
            Map.Entry<String, Integer> e = list.get(i);
            long ago = (now - WHEN.getOrDefault(e.getKey(), now)) / 1000L;
            ctx.drawText(mc.textRenderer, e.getKey() + "  x" + e.getValue() + "  " + ago + "s",
                    x, y + 11 + i * 11, 0xFFFFFFFF, true);
        }
    }
}
