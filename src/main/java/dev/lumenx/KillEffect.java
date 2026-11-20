package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

import java.util.concurrent.ThreadLocalRandom;

/** Kill effect: эффект на месте убитой вами цели (Star Glow — разлетающиеся светящиеся осколки). */
public final class KillEffect {
    public static final Module M = new Module("Kill effect",
            "Effect when you kill a target", Category.VISUALS, false, true);
    static final Setting EFFECT = M.add(Setting.mode("effect", "Effect", 0, "Star Glow", "Rings", "Soul", "Fireworks"));
    static final Setting AMOUNT = M.add(Setting.num("amount", "Amount", 30, 10, 100, 5));
    static final Setting COLOR  = M.add(Setting.mode("color", "Color", 1, Fx.PAL));
    static final Setting SIZE   = M.add(Setting.num("size", "Size", 2, 1, 4, 1));

    private static LivingEntity target;
    private static long hitT;
    private static double cx, cy, cz, feet;

    private KillEffect() { }

    static void onHit(LivingEntity e, long now) {
        if (!M.enabled) return;
        target = e; hitT = now;
        remember();
    }

    private static void remember() {
        Box b = target.getBoundingBox();
        cx = (b.minX + b.maxX) / 2.0; cz = (b.minZ + b.maxZ) / 2.0;
        cy = (b.minY + b.maxY) / 2.0; feet = b.minY;
    }

    static void update(long now) {
        if (!M.enabled || target == null) return;
        if (now - hitT > 6000) { target = null; return; }
        if (target.isAlive()) { remember(); return; }
        trigger();
        target = null;
    }

    private static void trigger() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int n = (int) AMOUNT.num;
        float size = (float) SIZE.num;
        switch (EFFECT.mode) {
            case 0 -> { // Star Glow
                for (int i = 0; i < n; i++) {
                    double[] d = HitFx.dir(r);
                    double sp = 1.5 + r.nextDouble() * 3.5;
                    Overlay.add(i % 3 == 0 ? Overlay.STAR : Overlay.SHARD, cx, cy, cz,
                            d[0] * sp, d[1] * sp + 1.0, d[2] * sp, 3f, 0.9f + r.nextFloat() * 0.6f, size,
                            Fx.color(COLOR.mode, r.nextFloat()));
                }
                Overlay.ring(cx, feet + 0.05, cz, 0.2f, 2.2f, 0.8f, Fx.color(COLOR.mode, 0f));
            }
            case 1 -> { // Rings
                for (int k = 0; k < 3; k++) {
                    Overlay.ring(cx, feet + 0.05 + k * 0.55, cz, 0.2f, 2.0f + k * 0.6f, 0.9f + k * 0.2f,
                            Fx.color(COLOR.mode, k / 3f));
                }
            }
            case 2 -> { // Soul: частицы поднимаются вверх
                for (int i = 0; i < n; i++) {
                    Overlay.add(Overlay.TWINKLE, cx + (r.nextDouble() - 0.5) * 0.8, feet + r.nextDouble() * 1.2,
                            cz + (r.nextDouble() - 0.5) * 0.8, 0.0, 1.0 + r.nextDouble() * 1.5, 0.0,
                            -0.5f, 1.2f + r.nextFloat() * 0.8f, size, Fx.color(COLOR.mode, r.nextFloat()));
                }
            }
            default -> { // Fireworks
                for (int i = 0; i < n * 2; i++) {
                    double[] d = HitFx.dir(r);
                    double sp = 2.0 + r.nextDouble() * 4.0;
                    Overlay.add(Overlay.DOT, cx, cy, cz, d[0] * sp, d[1] * sp, d[2] * sp,
                            4f, 0.8f + r.nextFloat() * 0.6f, Math.max(0f, size - 1f), Fx.color(6, r.nextFloat()));
                }
            }
        }
    }
}
