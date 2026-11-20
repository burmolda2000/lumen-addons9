package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

/**
 * Block overlay: подсветка блока под прицелом.
 * Стили: Neon (свечущий контур + рамка), Glass (заливка граней), Classic (тонкий контур).
 * Дополнительно: сканирующая линия, угловые метки, внутренняя рамка, точная форма блока.
 */
public final class BlockOverlay {
    public static final Module M = new Module("Block overlay",
            "Highlights the block you look at", Category.VISUALS, false, true);
    static final Setting STYLE   = M.add(Setting.mode("style", "Style", 0, "Neon", "Glass", "Classic"));
    static final Setting COLOR   = M.add(Setting.mode("color", "Color", 0, Fx.PAL));
    static final Setting ANIM    = M.add(Setting.mode("anim", "Animation", 1, "Solid", "Pulse", "Wave"));
    static final Setting FILL    = M.add(Setting.num("fill", "Fill strength (%)", 28, 0, 70, 2));
    static final Setting FACES   = M.add(Setting.bool("faces", "Fill all faces", true));
    static final Setting INNER   = M.add(Setting.bool("inner", "Inner frame", true));
    static final Setting CORNERS = M.add(Setting.bool("corners", "Corner marks", true));
    static final Setting SCAN    = M.add(Setting.bool("scan", "Scan line", true));
    static final Setting EXACT   = M.add(Setting.bool("exact", "Exact shape", true));
    static final Setting GLOW    = M.add(Setting.bool("glow", "Glow", true));

    private static final float[][] P = new float[8][3];
    private static final float[] T = new float[3];
    private static final float[] XS = new float[4], YS = new float[4];
    private static final float[] IX = new float[4], IY = new float[4];
    private static final float[] AX = new float[4], AY = new float[4];

    private BlockOverlay() { }

    static void draw(DrawContext ctx, MinecraftClient mc, float td, long now) {
        if (!M.enabled) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        ClientWorld w = mc.world;
        if (w == null || mc.player == null) return;

        BlockPos bp = hit.getBlockPos();
        Box b = new Box(bp.getX(), bp.getY(), bp.getZ(), bp.getX() + 1, bp.getY() + 1, bp.getZ() + 1);
        if (EXACT.bool) {
            VoxelShape sh = w.getBlockState(bp).getOutlineShape(w, bp);
            if (!sh.isEmpty()) b = sh.getBoundingBox().offset(bp.getX(), bp.getY(), bp.getZ());
        }
        double e = 0.003;
        b = new Box(b.minX - e, b.minY - e, b.minZ - e, b.maxX + e, b.maxY + e, b.maxZ + e);
        if (!Fx.projectBox(b, P)) return;

        Vec3d cam = mc.player.getCameraPosVec(td);
        float phase = now / 2500f + (ANIM.mode == 2 ? (bp.getX() + bp.getY() + bp.getZ()) * 0.08f : 0f);
        int rgb = Fx.color(COLOR.mode, phase);
        int light = Fx.lerp(rgb, 0xFFFFFF, 0.55f);
        float pulse = ANIM.mode == 1 ? 0.7f + 0.3f * MathHelper.sin(now / 280f) : 1f;
        float fillA = (float) FILL.num / 100f;
        int style = STYLE.mode;
        Direction aimed = hit.getSide();
        boolean haveAimed = false;

        // ---- заливка граней (градиент: основа + более яркий центр) ----
        if (fillA > 0f) {
            for (Direction d : Direction.values()) {
                boolean isAimed = d == aimed;
                if (!isAimed && !(FACES.bool || style == 1)) continue;
                if (!isAimed && !facing(b, d, cam)) continue;
                if (!faceScreen(b, d, XS, YS)) continue;
                float k = brightness(d) * (isAimed ? 1f : 0.55f) * (style == 2 ? 0.6f : 1f);
                float a = fillA * pulse * k;
                Fx.fillQuad(ctx, XS, YS, rgb, a * 0.6f);
                inset(XS, YS, 0.28f, IX, IY);
                Fx.fillQuad(ctx, IX, IY, rgb, a * 0.45f);
                if (isAimed) { System.arraycopy(XS, 0, AX, 0, 4); System.arraycopy(YS, 0, AY, 0, 4); haveAimed = true; }
            }
        }
        if (!haveAimed && faceScreen(b, aimed, XS, YS)) {
            System.arraycopy(XS, 0, AX, 0, 4); System.arraycopy(YS, 0, AY, 0, 4); haveAimed = true;
        }

        // ---- контур ----
        if (style == 0) {
            for (int[] ed : Fx.EDGES) {
                Fx.glowLine(ctx, P[ed[0]][0], P[ed[0]][1], P[ed[1]][0], P[ed[1]][1], rgb, 0.85f * pulse, 0);
            }
        } else {
            Fx.boxEdges(ctx, P, rgb, 0.95f * pulse);
        }
        if (GLOW.bool) {
            for (int i = 0; i < 8; i++) Fx.glowDot(ctx, P[i][0], P[i][1], light, 0.8f * pulse, 1);
        }

        if (!haveAimed) return;

        // ---- внутренняя рамка на грани, куда смотрим ----
        if (INNER.bool) {
            inset(AX, AY, 0.2f, IX, IY);
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) & 3;
                Fx.thinLine(ctx, IX[i], IY[i], IX[j], IY[j], rgb, 0.55f * pulse);
            }
        }

        // ---- угловые метки (Г-образные «скобки») ----
        if (CORNERS.bool) {
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) & 3, prev = (i + 3) & 3;
                float x = AX[i], y = AY[i];
                float nx = x + (AX[next] - x) * 0.22f, ny = y + (AY[next] - y) * 0.22f;
                float px = x + (AX[prev] - x) * 0.22f, py = y + (AY[prev] - y) * 0.22f;
                Fx.thinLine(ctx, x, y, nx, ny, light, 1f);
                Fx.thinLine(ctx, x + 1, y, nx + 1, ny, light, 0.7f);
                Fx.thinLine(ctx, x, y, px, py, light, 1f);
                Fx.thinLine(ctx, x, y + 1, px, py + 1, light, 0.7f);
            }
        }

        // ---- бегущая сканирующая линия ----
        if (SCAN.bool) {
            float s = (now % 1600L) / 1600f;
            for (int t = 0; t < 4; t++) {
                float ss = s - t * 0.035f;
                if (ss < 0f) ss += 1f;
                float ax = AX[0] + (AX[3] - AX[0]) * ss, ay = AY[0] + (AY[3] - AY[0]) * ss;
                float bx = AX[1] + (AX[2] - AX[1]) * ss, by = AY[1] + (AY[2] - AY[1]) * ss;
                float al = (t == 0 ? 0.9f : 0.45f / t) * pulse;
                Fx.thinLine(ctx, ax, ay, bx, by, t == 0 ? light : rgb, al);
            }
        }
    }

    /** Грань повёрнута к камере. */
    private static boolean facing(Box b, Direction d, Vec3d cam) {
        return switch (d) {
            case UP -> cam.y > b.maxY;
            case DOWN -> cam.y < b.minY;
            case NORTH -> cam.z < b.minZ;
            case SOUTH -> cam.z > b.maxZ;
            case WEST -> cam.x < b.minX;
            default -> cam.x > b.maxX;
        };
    }

    private static float brightness(Direction d) {
        return switch (d) {
            case UP -> 1.0f;
            case DOWN -> 0.5f;
            case NORTH, SOUTH -> 0.75f;
            default -> 0.65f;
        };
    }

    /** Проецирует четыре угла грани в экранные координаты. */
    private static boolean faceScreen(Box b, Direction d, float[] xs, float[] ys) {
        double[][] q = face(b, d);
        for (int i = 0; i < 4; i++) {
            if (!Fx.to(q[i][0], q[i][1], q[i][2], T)) return false;
            xs[i] = T[0];
            ys[i] = T[1];
        }
        return true;
    }

    /** Сжимает четырёхугольник к центру на долю k. */
    private static void inset(float[] xs, float[] ys, float k, float[] ox, float[] oy) {
        float cx = (xs[0] + xs[1] + xs[2] + xs[3]) / 4f, cy = (ys[0] + ys[1] + ys[2] + ys[3]) / 4f;
        for (int i = 0; i < 4; i++) {
            ox[i] = xs[i] + (cx - xs[i]) * k;
            oy[i] = ys[i] + (cy - ys[i]) * k;
        }
    }

    /** Четыре угла грани по кругу. */
    private static double[][] face(Box b, Direction side) {
        return switch (side) {
            case UP -> new double[][] { { b.minX, b.maxY, b.minZ }, { b.maxX, b.maxY, b.minZ }, { b.maxX, b.maxY, b.maxZ }, { b.minX, b.maxY, b.maxZ } };
            case DOWN -> new double[][] { { b.minX, b.minY, b.minZ }, { b.maxX, b.minY, b.minZ }, { b.maxX, b.minY, b.maxZ }, { b.minX, b.minY, b.maxZ } };
            case NORTH -> new double[][] { { b.minX, b.minY, b.minZ }, { b.maxX, b.minY, b.minZ }, { b.maxX, b.maxY, b.minZ }, { b.minX, b.maxY, b.minZ } };
            case SOUTH -> new double[][] { { b.minX, b.minY, b.maxZ }, { b.maxX, b.minY, b.maxZ }, { b.maxX, b.maxY, b.maxZ }, { b.minX, b.maxY, b.maxZ } };
            case WEST -> new double[][] { { b.minX, b.minY, b.minZ }, { b.minX, b.minY, b.maxZ }, { b.minX, b.maxY, b.maxZ }, { b.minX, b.maxY, b.minZ } };
            default -> new double[][] { { b.maxX, b.minY, b.minZ }, { b.maxX, b.minY, b.maxZ }, { b.maxX, b.maxY, b.maxZ }, { b.maxX, b.maxY, b.minZ } };
        };
    }
}
