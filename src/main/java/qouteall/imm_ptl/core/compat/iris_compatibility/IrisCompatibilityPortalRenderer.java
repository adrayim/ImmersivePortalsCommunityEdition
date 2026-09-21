package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.compat.iris_compatibility.IPIrisHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.SecondaryFrameBuffer;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;

public class IrisCompatibilityPortalRenderer
extends PortalRenderer {
    public static final IrisCompatibilityPortalRenderer instance = new IrisCompatibilityPortalRenderer(false);
    public static final IrisCompatibilityPortalRenderer debugModeInstance = new IrisCompatibilityPortalRenderer(true);
    private SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    private Matrix4f passingModelView = new Matrix4f();
    public boolean isDebugMode;

    public IrisCompatibilityPortalRenderer(boolean isDebugMode) {
        this.isDebugMode = isDebugMode;
    }

    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }

    @Override
    public void onBeforeTranslucentRendering(Matrix4f modelView) {
        if (PortalRendering.isRendering()) {
            return;
        }
        this.passingModelView = modelView;
        GL11.glDisable((int)2960);
    }

    @Override
    public void onAfterTranslucentRendering(Matrix4f modelView) {
    }

    @Override
    public void finishRendering() {
        GL11.glDisable((int)2960);
    }

    @Override
    public void prepareRendering() {
        this.deferredBuffer.prepare();
        IPPortingLibCompat.setIsStencilEnabled(client.getMainRenderTarget(), false);
    }

    protected void doRenderPortal(Portal portal, Matrix4f modelView) {
        if (PortalRendering.isRendering()) {
            return;
        }
        if (!this.testShouldRenderPortal(portal, modelView)) {
            return;
        }
        PortalRendering.pushPortalLayer(portal);
        this.renderPortalContent(portal);
        PortalRendering.popPortalLayer();
        CHelper.enableDepthClamp();
        if (!this.isDebugMode) {
            MyRenderHelper.drawPortalAreaWithFramebuffer(portal, client.getMainRenderTarget(), modelView, RenderSystem.getProjectionMatrix());
        } else {
            MyRenderHelper.drawScreenFrameBuffer(client.getMainRenderTarget(), true, true);
        }
        CHelper.disableDepthClamp();
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
    }

    @Override
    public void invokeWorldRendering(WorldRenderInfo worldRenderInfo) {
        MyGameRenderer.renderWorldNew(worldRenderInfo, Runnable::run);
    }

    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    }

    private boolean testShouldRenderPortal(Portal portal, Matrix4f modelView) {
        return PortalRenderInfo.renderAndDecideVisibility(portal, () -> ViewAreaRenderer.renderPortalArea(portal, Vec3.ZERO, modelView, RenderSystem.getProjectionMatrix(), true, false, false, true));
    }

    @Override
    public void onBeforeHandRendering(Matrix4f modelView) {
        if (PortalRendering.isRendering()) {
            return;
        }
        CHelper.checkGlError();
        IPIrisHelper.newCopyDepthStencil(client.getMainRenderTarget(), (RenderTarget)this.deferredBuffer.fb);
        IPIrisHelper.copyColor(client.getMainRenderTarget(), (RenderTarget)this.deferredBuffer.fb);
        CHelper.checkGlError();
        this.renderPortals(this.passingModelView);
        RenderTarget mainFrameBuffer = client.getMainRenderTarget();
        MyRenderHelper.drawScreenFrameBuffer((RenderTarget)this.deferredBuffer.fb, false, false);
    }

    @Override
    public void onHandRenderingEnded() {
    }

    protected void renderPortals(Matrix4f modelView) {
        List<Portal> portalsToRender = this.getPortalsToRender(modelView);
        for (Portal portal : portalsToRender) {
            this.doRenderPortal(portal, modelView);
        }
    }
}
