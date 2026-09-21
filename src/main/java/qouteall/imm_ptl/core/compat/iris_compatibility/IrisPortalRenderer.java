package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.compat.iris_compatibility.IPIrisHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.SecondaryFrameBuffer;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;

public class IrisPortalRenderer
extends PortalRenderer {
    public static final IrisPortalRenderer instance = new IrisPortalRenderer();
    private SecondaryFrameBuffer[] deferredFbs = new SecondaryFrameBuffer[0];
    private boolean portalRenderingNeeded = false;
    private boolean nextFramePortalRenderingNeeded = false;

    IrisPortalRenderer() {
        IPGlobal.PRE_GAME_RENDER_EVENT.register(() -> this.updateNeedsPortalRendering());
    }

    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }

    @Override
    public void prepareRendering() {
        Validate.isTrue((!PortalRendering.isRendering() ? 1 : 0) != 0);
        boolean bl = IPCGlobal.useSeparatedStencilFormat = !IPMcHelper.isNvidiaVideocard();
        if (this.deferredFbs.length != PortalRendering.getMaxPortalLayer() + 1) {
            for (SecondaryFrameBuffer fb : this.deferredFbs) {
                fb.fb.destroyBuffers();
            }
            this.deferredFbs = new SecondaryFrameBuffer[PortalRendering.getMaxPortalLayer() + 1];
            for (int i = 0; i < this.deferredFbs.length; ++i) {
                this.deferredFbs[i] = new SecondaryFrameBuffer();
            }
        }
        CHelper.checkGlError();
        for (SecondaryFrameBuffer deferredFb : this.deferredFbs) {
            deferredFb.prepare();
            IPPortingLibCompat.setIsStencilEnabled((RenderTarget)deferredFb.fb, true);
            GL11.glClearColor((float)1.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glClearDepth((double)1.0);
            GL11.glClearStencil((int)0);
            GL11.glClear((int)17664);
            CHelper.checkGlError();
        }
        IPPortingLibCompat.setIsStencilEnabled(client.getMainRenderTarget(), false);
    }

    private void updateNeedsPortalRendering() {
        this.portalRenderingNeeded = this.nextFramePortalRenderingNeeded;
        this.nextFramePortalRenderingNeeded = false;
    }

    @Override
    public void onBeforeHandRendering(Matrix4f modelView) {
        this.doMainRenderings(modelView);
    }

    private void doMainRenderings(Matrix4f modelView) {
        CHelper.checkGlError();
        RenderTarget mcFrameBuffer = client.getMainRenderTarget();
        int portalLayer = PortalRendering.getPortalLayer();
        if (this.portalRenderingNeeded) {
            CHelper.doCheckGlError();
            IPIrisHelper.copyDepthStencil(mcFrameBuffer, (RenderTarget)this.deferredFbs[portalLayer].fb, true, false);
            int errorCode = GL11.glGetError();
            if (errorCode != 0) {
                IPGlobal.renderMode = IPGlobal.RenderMode.compatibility;
                CHelper.printChat("[Immersive Portals]Switched to compatibility portal rendering mode. Portal-in-portal wont' be rendered");
            }
            this.initStencilForLayer(portalLayer);
            GL11.glEnable((int)2960);
            GL11.glStencilFunc((int)514, (int)portalLayer, (int)255);
            GL11.glStencilOp((int)7680, (int)7680, (int)7680);
            MyRenderHelper.drawScreenFrameBuffer(mcFrameBuffer, false, true);
            GL11.glDisable((int)2960);
        }
        this.renderPortals(modelView);
        if (portalLayer == 0) {
            this.finish();
        }
    }

    @Override
    public void onHandRenderingEnded() {
    }

    private void initStencilForLayer(int portalLayer) {
        if (portalLayer == 0) {
            GL11.glClearStencil((int)0);
            GL11.glClear((int)1024);
        } else {
            CHelper.checkGlError();
            IPIrisHelper.copyDepthStencil((RenderTarget)this.deferredFbs[portalLayer - 1].fb, (RenderTarget)this.deferredFbs[portalLayer].fb, false, true);
            CHelper.checkGlError();
        }
    }

    @Override
    public void onBeforeTranslucentRendering(Matrix4f modelView) {
    }

    @Override
    public void onAfterTranslucentRendering(Matrix4f modelView) {
    }

    @Override
    public void finishRendering() {
    }

    private void finish() {
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        if (RenderStates.getRenderedPortalNum() == 0) {
            return;
        }
        if (!this.portalRenderingNeeded) {
            return;
        }
        RenderTarget mainFrameBuffer = client.getMainRenderTarget();
        this.deferredFbs[0].fb.blitToScreen();
        CHelper.checkGlError();
    }

    protected void doRenderPortal(Portal portal, Matrix4f modelView) {
        this.nextFramePortalRenderingNeeded = true;
        if (!this.portalRenderingNeeded) {
            return;
        }
        if (!this.tryRenderViewAreaInDeferredBufferAndIncreaseStencil(portal, modelView)) {
            return;
        }
        PortalRendering.pushPortalLayer(portal);
        this.renderPortalContent(portal);
        int innerLayer = PortalRendering.getPortalLayer();
        PortalRendering.popPortalLayer();
        int outerLayer = PortalRendering.getPortalLayer();
        if (innerLayer > PortalRendering.getMaxPortalLayer()) {
            return;
        }
        GL11.glEnable((int)2960);
        GL11.glStencilFunc((int)514, (int)innerLayer, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7680);
        MyRenderHelper.drawScreenFrameBuffer((RenderTarget)this.deferredFbs[innerLayer].fb, true, false);
        GL11.glDisable((int)2960);
    }

    private boolean tryRenderViewAreaInDeferredBufferAndIncreaseStencil(Portal portal, Matrix4f modelView) {
        int portalLayer = PortalRendering.getPortalLayer();
        this.initStencilForLayer(portalLayer);
        GL11.glEnable((int)2960);
        GL11.glStencilFunc((int)514, (int)portalLayer, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7682);
        GlStateManager._enableDepthTest();
        boolean result = PortalRenderInfo.renderAndDecideVisibility(portal, () -> ViewAreaRenderer.renderPortalArea(portal, Vec3.ZERO, modelView, RenderSystem.getProjectionMatrix(), true, true, true, true));
        GL11.glDisable((int)2960);
        return result;
    }

    @Override
    public void invokeWorldRendering(WorldRenderInfo worldRenderInfo) {
        MyGameRenderer.renderWorldNew(worldRenderInfo, Runnable::run);
    }

    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    }

    protected void renderPortals(Matrix4f modelView) {
        List<Portal> portalsToRender = this.getPortalsToRender(modelView);
        for (Portal portal : portalsToRender) {
            this.doRenderPortal(portal, modelView);
        }
    }
}
