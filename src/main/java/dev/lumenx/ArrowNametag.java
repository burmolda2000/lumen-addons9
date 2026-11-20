package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.util.Proj;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.math.Vec3d;

/** Arrow nametag: подпись над летящей стрелой (кто выпустил и расстояние). */
public final class ArrowNametag {
    public static final Module M = new Module("Arrow nametag",
            "Shows who shot the arrow in flight", Category.VISUALS, false, true);
    static final Setting COLOR = M.add(Setting.mode("color", "Color", 1, Fx.PAL));
    static final Setting DIST  = M.add(Setting.bool("distance", "Show distance", true));
    static final Setting MAXD  = M.add(Setting.num("max_dist", "Max distance", 48, 8, 96, 4));

    private static final float[] A = new float[3];

    private ArrowNametag() { }

    static void draw(DrawContext ctx, MinecraftClient mc, ClientWorld w, ClientPlayerEntity me, float td) {
        if (!M.enabled) return;
        double md = MAXD.num;
        for (Entity e : w.getOtherEntities(me, me.getBoundingBox().expand(md),
                x -> x instanceof PersistentProjectileEntity && x.getVelocity().lengthSquared() > 0.01)) {
            Vec3d lp = e.getLerpedPos(td);
            if (!Fx.to(lp.x, lp.y + 0.2, lp.z, A)) continue;

            Entity owner = ((PersistentProjectileEntity) e).getOwner();
            String name = owner != null ? owner.getName().getString() : e.getName().getString();
            String text = name;
            if (DIST.bool) text += " " + Math.round(me.distanceTo(e)) + "m";

            int rgb = Fx.color(COLOR.mode, 0f);
            int tw = mc.textRenderer.getWidth(text);
            Fx.glowDot(ctx, A[0], A[1], rgb, 0.9f, 1);
            ctx.drawText(mc.textRenderer, text, Math.round(A[0] - tw / 2f), Math.round(A[1]) - 12,
                    0xFF000000 | rgb, true);
        }
    }
}
