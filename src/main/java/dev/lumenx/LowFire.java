package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

/** Low fire: опускает огонь на экране, когда игрок горит (чем выше значение, тем меньше огня). */
public final class LowFire {
    public static final Module M = new Module("Low fire",
            "Reduces the fire overlay while you burn", Category.VISUALS, true, true);
    static final Setting REDUCE = M.add(Setting.num("reduce", "Reduction (%)", 60, 0, 100, 5));

    private LowFire() { }

    /** Вызывается из миксина: y — вертикальное смещение огня (в ванили -0.3). */
    public static float adjust(float y) {
        if (!M.enabled) return y;
        return y - (float) (REDUCE.num / 100.0 * 0.6);
    }
}
