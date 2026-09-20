package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.FogParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import qouteall.imm_ptl.core.render.MyRenderHelper;

@Mixin(value = RenderSystem.class, remap = false)
public class MixinRenderSystem_Fog {
    @ModifyVariable(method = "setShaderFog", at = @At("HEAD"), argsOnly = true)
    private static FogParameters onSetShaderFog(FogParameters fog) {
        return new FogParameters(
            MyRenderHelper.transformFogDistance(fog.start()),
            MyRenderHelper.transformFogDistance(fog.end()),
            fog.shape(), fog.red(), fog.green(), fog.blue(), fog.alpha()
        );
    }
}
