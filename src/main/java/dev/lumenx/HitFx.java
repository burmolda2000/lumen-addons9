package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

import java.util.concurrent.ThreadLocalRandom;

/** Hit particles и Crit particles: светящиеся искры и звёзды в момент удара. */
public final class HitFx {
    public static final Module HIT = new Module("Hit particles",
            "Glowing sparks when you hit someone", Category.VISUALS, false, true);
    static final Setting H_STYLE  = HIT.add(Setting.mode("style", "Style", 0, "Sparks", "Dots", "Stars"));
    static final Setting H_AMOUNT = HIT.add(Setting.num("amount", "Amount", 18, 4, 60, 2));
    static final Setting H_SIZE   = HIT.add(Setting.num("size", "Size", 2, 1, 4, 1));
    static final Setting H_COLOR  = HIT.add(Setting.mode("color", "Color", 1, Fx.PAL));

    public static final Module CRIT = new Module("Crit particles",
            "Star burst on critical hits", Category.VISUALS, false, true);
    static final Setting C_AMOUNT = CRIT.add(Setting.num("amount", "Amount", 14, 4, 50, 2));
    static final Setting C_SIZE   = CRIT.add(Setting.num("size", "Size", 2, 1, 4, 1));
    static final Setting C_COLOR  = CRIT.add(Setting.mode("color", "Color", 2, Fx.PAL));
    static final Setting C_RING   = CRIT.add(Setting.bool("ring", "Ring", true));

    private HitFx() { }

    static void onHit(LivingEntity e, boolean crit) {
        boolean critFx = crit && CRIT.enabled;
        if (!HIT.enabled && !critFx) return;
        Box b = e.getBoundingBox();
        double cx = (b.minX + b.maxX) / 2.0, cz = (b.minZ + b.maxZ) / 2.0;
        double cy = b.minY + (b.maxY - b.minY) * 0.65;
        ThreadLocalRandom r = ThreadLocalRandom.current();

        if (HIT.enabled && !critFx) {
            int n = (int) H_AMOUNT.num;
            int kind = H_STYLE.mode == 0 ? Overlay.SHARD : H_STYLE.mode == 1 ? Overlay.DOT : Overlay.STAR;
            for (int i = 0; i < n; i++) {
                double[] d = dir(r);
                double sp = 1.2 + r.nextDouble() * 2.5;
                Overlay.add(kind, cx, cy, cz, d[0] * sp, d[1] * sp, d[2] * sp,
                        5f, 0.35f + r.nextFloat() * 0.4f, (float) H_SIZE.num, Fx.color(H_COLOR.mode, r.nextFloat()));
            }
        }
        if (critFx) {
            int n = (int) C_AMOUNT.num;
            for (int i = 0; i < n; i++) {
                double[] d = dir(r);
                double sp = 2.0 + r.nextDouble() * 3.0;
                Overlay.add(Overlay.STAR, cx, cy, cz, d[0] * sp, d[1] * sp, d[2] * sp,
                        2f, 0.5f + r.nextFloat() * 0.4f, (float) C_SIZE.num, Fx.color(C_COLOR.mode, r.nextFloat()));
            }
            if (C_RING.bool) {
                Overlay.ring(cx, b.minY + 0.05, cz, 0.2f, 1.5f, 0.6f, Fx.color(C_COLOR.mode, 0f));
            }
        }
    }

    /** Случайное направление на сфере. */
    static double[] dir(ThreadLocalRandom r) {
        double z = r.nextDouble() * 2.0 - 1.0;
        double a = r.nextDouble() * Math.PI * 2.0;
        double s = Math.sqrt(1.0 - z * z);
        return new double[] { s * Math.cos(a), z, s * Math.sin(a) };
    }
}
