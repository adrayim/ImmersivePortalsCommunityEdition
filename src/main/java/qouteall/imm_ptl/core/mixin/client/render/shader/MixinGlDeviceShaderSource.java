package qouteall.imm_ptl.core.mixin.client.render.shader;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.shaders.ShaderType;
import java.util.function.BiFunction;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.render.ShaderCodeTransformation;

@Mixin(GlDevice.class)
public class MixinGlDeviceShaderSource {
    @WrapOperation(
        method = "compileShader",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private Object ip_transformShaderSource(
        BiFunction<?, ?, ?> sourceProvider, Object id, Object type, Operation<Object> original
    ) {
        Object source = original.call(sourceProvider, id, type);
        if (source instanceof String code && id instanceof ResourceLocation location &&
            type instanceof ShaderType shaderType) {
            return ShaderCodeTransformation.transform(shaderType, location.toString(), code);
        }
        return source;
    }
}
