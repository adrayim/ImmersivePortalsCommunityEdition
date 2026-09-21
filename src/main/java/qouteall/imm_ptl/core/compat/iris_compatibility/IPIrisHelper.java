package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;

public class IPIrisHelper {
    public static void copyDepthStencil(RenderTarget from, RenderTarget to, boolean copyDepth, boolean copyStencil) {
        if (copyDepth && from.getDepthTexture() != null && to.getDepthTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(from.getDepthTexture(), to.getDepthTexture(), 0, 0, 0, 0, Math.min(from.width, to.width), Math.min(from.height, to.height), 0);
        }
    }

    private static boolean isCopyImageSubDataSupported() {
        return true;
    }

    public static void newCopyDepthStencil(RenderTarget from, RenderTarget to) {
        IPIrisHelper.copyDepthStencil(from, to, true, false);
    }

    public static void copyColor(RenderTarget from, RenderTarget to) {
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(from.getColorTexture(), to.getColorTexture(), 0, 0, 0, 0, Math.min(from.width, to.width), Math.min(from.height, to.height), 0);
    }
}
