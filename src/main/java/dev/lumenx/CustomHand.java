package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Custom hand: позы рук, размер и 20+ вариантов анимации удара (минимальная панель, как в Pulse).
 * Вызывается из HeldItemRendererMixin. Если стиль удара = Off, удар рисует модуль Swing или ваниль.
 */
public final class CustomHand {
    static final String[] STYLES = {
            "Default", "No swing", "Tilt", "Smooth", "Slide", "Block", "Spin", "Poke", "Whip", "Tap", "Hammer",
            "Wave", "Dash", "Stab", "Drop", "Punch", "Wiggle", "Spiral", "Chop", "Uppercut", "Pendulum",
            "Flip", "Corkscrew"
    };

    public static final Module M = new Module("Custom hand",
            "Hand poses, sizes and swing animations", Category.VISUALS, false, true);
    static final Setting STYLE     = M.add(Setting.mode("style", "Animation", 0, STYLES));
    static final Setting SPEED     = M.add(Setting.num("speed", "Swing speed", 1, 0.5, 3, 0.1));
    static final Setting POWER     = M.add(Setting.num("power", "Swing strength", 1, 0.2, 4, 0.1));
    static final Setting MAIN_SIZE = M.add(Setting.num("main_size", "Main hand size", 1, 0.3, 1.8, 0.05));
    static final Setting OFF_SIZE  = M.add(Setting.num("off_size", "Off hand size", 1, 0.3, 1.8, 0.05));
    static final Setting HEIGHT    = M.add(Setting.num("height", "Hand height", 0, -0.5, 0.5, 0.01));
    static final Setting MAIN_POSE = M.add(Setting.mode("main_pose", "Main pose", 0,
            "Vanilla", "Flat low", "Tilted", "Raised"));
    static final Setting OFF_POSE  = M.add(Setting.mode("off_pose", "Off pose", 0,
            "Vanilla", "Raised", "Tilted"));
    static final Setting NO_DIP    = M.add(Setting.bool("no_dip", "No hit dip", true));

    // Пресеты поз: {x, y, z, rotX, rotY, rotZ, scale}. Приблизительные — точная настройка в Hand editor.
    static final float[][] MAIN_POSES = {
            null,                                            // Vanilla
            { 0.00f, 0.10f, 0.00f,   0f, -20f, 72f, 0.95f }, // Flat low: меч лежит горизонтально внизу
            { 0.05f, 0.00f, 0.00f, -20f,   0f, 25f, 1.00f }, // Tilted
            {-0.05f, 0.12f, 0.10f, -10f, -10f, 15f, 1.00f }  // Raised
    };
    static final float[][] OFF_POSES = {
            null,                                            // Vanilla
            { 0.00f, 0.22f, 0.18f,  -5f,   0f, -18f, 1.20f },// Raised: предмет поднят и ближе к камере
            { 0.05f, 0.05f, 0.05f, -15f,   0f,  20f, 1.00f } // Tilted
    };

    private CustomHand() { }

    private static boolean isMain(Arm arm) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return arm == mc.options.getMainArm().getValue();
    }

    // ---------- маленькие помощники ----------
    private static void rx(MatrixStack m, float d) { if (d != 0f) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(d)); }
    private static void ry(MatrixStack m, float d) { if (d != 0f) m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(d)); }
    private static void rz(MatrixStack m, float d) { if (d != 0f) m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(d)); }
    private static void tr(MatrixStack m, float x, float y, float z) { m.translate(x, y, z); }
    private static float seg(float p, float a, float b) { return MathHelper.clamp((p - a) / (b - a), 0f, 1f); }
    private static float ease(float t) { return t * t * (3f - 2f * t); }

    /** После ванильного equip-смещения: поза, размер руки и компенсация «просадки» после удара. */
    public static void applyOffset(MatrixStack m, Arm arm, float equipProgress) {
        boolean main = isMain(arm);
        boolean editor = HandEditor.M.enabled && (main ? HandEditor.MAIN_ON.bool : HandEditor.OFF_ON.bool);
        if (!M.enabled && !editor) return;

        // Ваниль опускает руку после каждого удара (перезарядка) — компенсируем (чисто вертикальный сдвиг).
        if (M.enabled && NO_DIP.bool) m.translate(0f, 0.6f * equipProgress, 0f);

        float side = arm == Arm.RIGHT ? 1f : -1f;
        float x = 0f, y = 0f, z = 0f, rx = 0f, ry = 0f, rz = 0f, s = 1f;
        if (editor) {
            x  = (float) (main ? HandEditor.X.num : HandEditor.OX.num);
            y  = (float) (main ? HandEditor.Y.num : HandEditor.OY.num);
            z  = (float) (main ? HandEditor.Z.num : HandEditor.OZ.num);
            rx = (float) (main ? HandEditor.ROT_X.num : HandEditor.OROT_X.num);
            ry = (float) (main ? HandEditor.ROT_Y.num : HandEditor.OROT_Y.num);
            rz = (float) (main ? HandEditor.ROT_Z.num : HandEditor.OROT_Z.num);
            s  = (float) (main ? HandEditor.SCALE.num : HandEditor.OSCALE.num);
        } else {
            int idx = main ? MAIN_POSE.mode : OFF_POSE.mode;
            float[][] poses = main ? MAIN_POSES : OFF_POSES;
            float[] pz = poses[Math.max(0, Math.min(poses.length - 1, idx))];
            if (pz != null) { x = pz[0]; y = pz[1]; z = pz[2]; rx = pz[3]; ry = pz[4]; rz = pz[5]; s = pz[6]; }
            s *= (float) (main ? MAIN_SIZE.num : OFF_SIZE.num);
            y += (float) HEIGHT.num;
        }

        m.translate(side * x, y, z);
        rx(m, rx);
        ry(m, side * ry);
        rz(m, side * rz);
        if (s != 1f) m.scale(s, s, s);
    }

    /** @return true, если удар нарисован нами и ванильную/Lumen-анимацию надо отменить. */
    public static boolean applySwing(MatrixStack m, Arm arm, float rawP) {
        if (!M.enabled || STYLE.mode == 0) return false;
        float i = arm == Arm.RIGHT ? 1f : -1f;
        float k = (float) POWER.num;
        float pi = (float) Math.PI;

        // 1) Убираем ванильный сдвиг руки при ударе: он применяется до equip-смещения,
        //    а оба смещения — чистые переводы, поэтому компенсация точная.
        float vs = MathHelper.sin(MathHelper.sqrt(rawP) * pi);
        m.translate(i * 0.4f * vs,
                -0.2f * MathHelper.sin(MathHelper.sqrt(rawP) * pi * 2f),
                0.2f * MathHelper.sin(rawP * pi));

        // 2) Скорость: ускоряем анимацию, после конца рука стоит в покое
        float spd = (float) SPEED.num;
        float p = spd >= 1f ? Math.min(1f, rawP * spd) : (float) Math.pow(rawP, 1.0 / spd);

        float sp = MathHelper.sin(p * pi);                    // 0 -> 1 -> 0
        float s2 = MathHelper.sin(MathHelper.sqrt(p) * pi);   // резкий старт
        float sq = MathHelper.sin(p * p * pi);                // мягкий старт
        float sw = MathHelper.sin(p * 2f * pi);               // туда-обратно
        float sm = ease(p);                                   // 0 -> 1 плавно

        String name = STYLES[Math.max(0, Math.min(STYLES.length - 1, STYLE.mode))];
        switch (name) {
            case "Tilt" -> {
                rz(m, i * -38f * s2 * k);
                rx(m, -16f * sp * k);
                tr(m, i * -0.05f * sp * k, 0f, 0f);
            }
            case "Smooth" -> {
                ry(m, i * (45f - 12f * sq * k));
                rz(m, i * -14f * s2 * k);
                rx(m, -55f * s2 * k);
                ry(m, i * -45f);
            }
            case "Slide" -> tr(m, 0f, 0f, -0.65f * sp * k);
            case "Block" -> {
                tr(m, i * -0.30f * sp * k, 0.10f * sp * k, -0.05f * sp * k);
                rz(m, i * 30f * sp * k);
                rx(m, -20f * sp * k);
                ry(m, i * -25f * sp * k);
            }
            case "Spin" -> {
                tr(m, 0f, 0f, -0.2f * sp * k);
                ry(m, i * 360f * sm);
            }
            case "Poke" -> {
                tr(m, 0f, 0f, -1.0f * s2 * k);
                rx(m, -10f * s2 * k);
            }
            case "Whip" -> {
                ry(m, i * 45f);
                rx(m, -35f * s2 * k);
                rz(m, i * -95f * sp * k);
                ry(m, i * -45f);
            }
            case "Tap" -> {
                float t = sp * sp;
                tr(m, 0f, -0.12f * t * k, -0.18f * t * k);
                rx(m, -40f * t * k);
            }
            case "Hammer" -> {
                // подъём -> удар -> возврат
                float ang = 55f * ease(seg(p, 0f, 0.3f))
                        - 155f * ease(seg(p, 0.3f, 0.5f))
                        + 100f * ease(seg(p, 0.5f, 1f));
                rx(m, ang * k);
            }
            case "Wave" -> {
                tr(m, i * 0.12f * sw * k, 0f, 0f);
                rz(m, i * 35f * sw * k);
                rx(m, -20f * MathHelper.sin(4f * pi * p) * k);
            }
            case "Dash" -> {
                tr(m, i * -0.4f * sp * k, -0.25f * sp * k, -0.7f * sp * k);
                rx(m, -20f * sp * k);
            }
            case "Stab" -> {
                tr(m, 0f, 0f, -0.7f * sp * k);
                rx(m, -85f * sp * k);
            }
            case "Drop" -> {
                tr(m, 0f, -0.75f * sp * k, 0f);
                rx(m, 25f * sp * k);
            }
            case "Punch" -> {
                tr(m, i * -0.12f * s2 * k, 0.05f * s2 * k, -0.85f * s2 * k);
                rz(m, i * 8f * s2 * k);
                rx(m, -6f * s2 * k);
            }
            case "Wiggle" -> {
                rz(m, i * 18f * MathHelper.sin(6f * pi * p) * sp * k);
                rx(m, 9f * MathHelper.sin(8f * pi * p) * sp * k);
            }
            case "Spiral" -> {
                tr(m, 0f, 0f, -0.35f * sp * k);
                ry(m, i * 360f * sm);
                rx(m, -30f * sp * k);
            }
            case "Chop" -> {
                ry(m, i * 45f);
                rx(m, -110f * s2 * k);
                ry(m, i * -45f);
                tr(m, 0f, 0f, -0.12f * sp * k);
            }
            case "Uppercut" -> {
                ry(m, i * 45f);
                rx(m, 75f * s2 * k);
                rz(m, i * -25f * sp * k);
                ry(m, i * -45f);
                tr(m, 0f, -0.15f * sp * k, 0f);
            }
            case "Pendulum" -> {
                rz(m, i * -55f * sw * k);
                ry(m, i * 15f * sw * k);
            }
            case "Flip" -> rx(m, -360f * sm);
            case "Corkscrew" -> {
                tr(m, 0f, 0f, -0.25f * sp * k);
                rz(m, i * 360f * sm);
            }
            default -> { /* No swing: просто без анимации */ }
        }
        return true;
    }
}
