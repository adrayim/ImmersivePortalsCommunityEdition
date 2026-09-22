package qouteall.imm_ptl.core.mixin.client.render.shader;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.render.FrontClipping;

@Mixin(GlProgram.class)
public class MixinGlProgramClipping {
    @Unique private int ip_clippingUniformLocation = Integer.MIN_VALUE;

    @Inject(method = "setDefaultUniforms", at = @At("TAIL"))
    private void ip_setClippingUniform(
        VertexFormat.Mode mode, Matrix4f modelView, Matrix4f projection,
        float screenWidth, float screenHeight, CallbackInfo ci
    ) {
        if (!IPGlobal.enableClippingMechanism) {
            return;
        }

        GlProgram program = (GlProgram) (Object) this;
        if (ip_clippingUniformLocation == Integer.MIN_VALUE) {
            ip_clippingUniformLocation = GL20.glGetUniformLocation(
                program.getProgramId(), "iportal_ClippingEquation"
            );
        }
        boolean usesCameraRelativePosition = program.MODEL_OFFSET != null ||
            program.getDebugLabel().contains("portal_area");
        double[] equation = FrontClipping.isClippingEnabled
            ? (usesCameraRelativePosition
                ? FrontClipping.getActiveClipPlaneEquationBeforeModelView()
                : FrontClipping.getActiveClipPlaneEquationAfterModelView())
            : null;
        if (ip_clippingUniformLocation < 0) {
            return;
        }
        if (equation == null) {
            GL20.glUniform4f(ip_clippingUniformLocation, 0, 0, 0, 1);
        }
        else {
            GL20.glUniform4f(
                ip_clippingUniformLocation,
                (float) equation[0], (float) equation[1],
                (float) equation[2], (float) equation[3]
            );
        }
    }
}
