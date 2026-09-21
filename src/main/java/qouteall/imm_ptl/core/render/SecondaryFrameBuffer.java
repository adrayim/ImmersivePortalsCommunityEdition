package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import qouteall.q_misc_util.Helper;

public class SecondaryFrameBuffer {
    public TextureTarget fb;

    public void prepare() {
        RenderTarget mainFrameBuffer = Minecraft.getInstance().getMainRenderTarget();
        int width = mainFrameBuffer.viewWidth;
        int height = mainFrameBuffer.viewHeight;
        this.prepare(width, height);
    }

    public void prepare(int width, int height) {
        if (this.fb == null) {
            this.fb = new TextureTarget("immersive_portals_secondary", width, height, true);
            Helper.log("Secondary Framebuffer init");
        }
        if (width != this.fb.viewWidth || height != this.fb.viewHeight) {
            this.fb.resize(width, height);
            Helper.log("Secondary Framebuffer resized");
        }
    }
}
