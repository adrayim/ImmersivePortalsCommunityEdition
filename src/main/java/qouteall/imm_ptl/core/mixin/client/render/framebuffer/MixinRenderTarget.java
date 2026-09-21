package qouteall.imm_ptl.core.mixin.client.render.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import qouteall.imm_ptl.core.ducks.IEFrameBuffer;

@Mixin(RenderTarget.class)
public class MixinRenderTarget implements IEFrameBuffer {
    @Unique private boolean ip_stencilEnabled;

    @Override
    public boolean ip_getIsStencilBufferEnabled() {
        return ip_stencilEnabled;
    }

    @Override
    public void ip_setIsStencilBufferEnabledAndReload(boolean enabled) {
        if (enabled) {
            throw new UnsupportedOperationException("Stencil render targets need the 1.21.5 GPU backend");
        }
        ip_stencilEnabled = false;
    }
}
