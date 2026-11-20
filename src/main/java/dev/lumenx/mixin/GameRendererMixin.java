package dev.lumenx.mixin;

import dev.lumenx.AspectRatio;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Подменяет соотношение сторон в матрице проекции мира.
 * Метод ищется по регулярному выражению, а вызов — по JOML (он не обфусцирован),
 * поэтому не зависит от названия метода в конкретной версии Minecraft.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @ModifyArg(method = "/.*/", remap = false, require = 0, index = 1,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;"))
    private float lumenx$aspectA(float aspect) { return AspectRatio.apply(aspect); }

    @ModifyArg(method = "/.*/", remap = false, require = 0, index = 1,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;"))
    private float lumenx$aspectB(float aspect) { return AspectRatio.apply(aspect); }

    @ModifyArg(method = "/.*/", remap = false, require = 0, index = 1,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lorg/joml/Matrix4f;setPerspective(FFFF)Lorg/joml/Matrix4f;"))
    private float lumenx$aspectC(float aspect) { return AspectRatio.apply(aspect); }

    @ModifyArg(method = "/.*/", remap = false, require = 0, index = 1,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lorg/joml/Matrix4f;setPerspective(FFFFZ)Lorg/joml/Matrix4f;"))
    private float lumenx$aspectD(float aspect) { return AspectRatio.apply(aspect); }
}
