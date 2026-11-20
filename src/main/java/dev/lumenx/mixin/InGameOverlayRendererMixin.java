package dev.lumenx.mixin;

import dev.lumenx.LowFire;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    /** Меняем вертикальное смещение огня в renderFireOverlay. */
    @ModifyArg(method = "renderFireOverlay",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"),
            index = 1)
    private static float lumenx$lowFire(float y) {
        return LowFire.adjust(y);
    }
}
