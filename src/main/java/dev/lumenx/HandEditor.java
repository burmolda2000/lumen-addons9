package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;

/** Hand editor: точная настройка рук ползунками. Когда включён, заменяет позы из Custom hand. */
public final class HandEditor {
    public static final Module M = new Module("Hand editor",
            "Fine-tune hand position (overrides poses)", Category.VISUALS, false, true);
    static final Setting MAIN_ON = M.add(Setting.bool("main_on", "Main hand", true));
    static final Setting X      = M.add(Setting.num("x", "Main: Offset X", 0, -1, 1, 0.05));
    static final Setting Y      = M.add(Setting.num("y", "Main: Offset Y", 0, -1, 1, 0.05));
    static final Setting Z      = M.add(Setting.num("z", "Main: Offset Z", 0, -1, 1, 0.05));
    static final Setting ROT_X  = M.add(Setting.num("rot_x", "Main: Rotate X", 0, -180, 180, 5));
    static final Setting ROT_Y  = M.add(Setting.num("rot_y", "Main: Rotate Y", 0, -180, 180, 5));
    static final Setting ROT_Z  = M.add(Setting.num("rot_z", "Main: Rotate Z", 0, -180, 180, 5));
    static final Setting SCALE  = M.add(Setting.num("scale", "Main: Scale", 1, 0.3, 1.8, 0.05));
    static final Setting OFF_ON = M.add(Setting.bool("off_on", "Off hand", false));
    static final Setting OX     = M.add(Setting.num("off_x", "Off: Offset X", 0, -1, 1, 0.05));
    static final Setting OY     = M.add(Setting.num("off_y", "Off: Offset Y", 0, -1, 1, 0.05));
    static final Setting OZ     = M.add(Setting.num("off_z", "Off: Offset Z", 0, -1, 1, 0.05));
    static final Setting OROT_X = M.add(Setting.num("off_rot_x", "Off: Rotate X", 0, -180, 180, 5));
    static final Setting OROT_Y = M.add(Setting.num("off_rot_y", "Off: Rotate Y", 0, -180, 180, 5));
    static final Setting OROT_Z = M.add(Setting.num("off_rot_z", "Off: Rotate Z", 0, -180, 180, 5));
    static final Setting OSCALE = M.add(Setting.num("off_scale", "Off: Scale", 1, 0.3, 1.8, 0.05));

    private HandEditor() { }
}
