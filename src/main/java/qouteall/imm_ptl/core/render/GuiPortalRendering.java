package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

@Environment(value=EnvType.CLIENT)
public class GuiPortalRendering {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private static RenderTarget renderingFrameBuffer = null;
    private static final HashMap<RenderTarget, WorldRenderInfo> renderingTasks = new HashMap();

    @Nullable
    public static RenderTarget getRenderingFrameBuffer() {
        return renderingFrameBuffer;
    }

    public static boolean isRendering() {
        return GuiPortalRendering.getRenderingFrameBuffer() != null;
    }

    private static void renderWorldIntoFrameBuffer(WorldRenderInfo worldRenderInfo, RenderTarget framebuffer) {
        RenderStates.basicProjectionMatrix = null;
        CHelper.checkGlError();
        ((IECamera)RenderStates.originalCamera).ip_resetState(worldRenderInfo.cameraPos, worldRenderInfo.world);
        Validate.isTrue((renderingFrameBuffer == null ? 1 : 0) != 0);
        renderingFrameBuffer = framebuffer;
        MyRenderHelper.restoreViewPort();
        RenderTarget mcFb = MyGameRenderer.client.getMainRenderTarget();
        Validate.isTrue((mcFb != framebuffer ? 1 : 0) != 0);
        ((IEMinecraftClient)MyGameRenderer.client).ip_setFrameBuffer(framebuffer);
        if (!worldRenderInfo.doRenderSky) {
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
            if (framebuffer.getDepthTexture() != null) {
                RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(framebuffer.getColorTexture(), 0, framebuffer.getDepthTexture(), 1.0);
            } else {
                RenderSystem.getDevice().createCommandEncoder().clearColorTexture(framebuffer.getColorTexture(), 0);
            }
        }
        IPCGlobal.renderer.prepareRendering();
        IPCGlobal.renderer.invokeWorldRendering(worldRenderInfo);
        IPCGlobal.renderer.finishRendering();
        ((IEMinecraftClient)MyGameRenderer.client).ip_setFrameBuffer(mcFb);
        renderingFrameBuffer = null;
        MyRenderHelper.restoreViewPort();
        CHelper.checkGlError();
        RenderStates.basicProjectionMatrix = null;
    }

    public static void submitNextFrameRendering(WorldRenderInfo worldRenderInfo, RenderTarget renderTarget) {
        if (!ClientWorldLoader.getIsInitialized()) {
            LOGGER.error("Trying to submit world rendering task before client world is initialized", new Throwable());
            return;
        }
        Validate.isTrue((!renderingTasks.containsKey(renderTarget) ? 1 : 0) != 0);
        RenderTarget mcFB = Minecraft.getInstance().getMainRenderTarget();
        if (renderTarget.width != mcFB.width || renderTarget.height != mcFB.height) {
            renderTarget.resize(mcFB.width, mcFB.height);
            LOGGER.info("Resized Framebuffer for GUI Portal Rendering");
        }
        renderingTasks.put(renderTarget, worldRenderInfo);
    }

    public static void _onGameRenderEnd() {
        renderingTasks.forEach((frameBuffer, worldRendering) -> GuiPortalRendering.renderWorldIntoFrameBuffer(worldRendering, frameBuffer));
        renderingTasks.clear();
    }

    public static void _init() {
        IPCGlobal.CLIENT_CLEANUP_EVENT.register(renderingTasks::clear);
    }
}
