package qouteall.imm_ptl.core.render.renderer;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.FogRendererContext;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;
import qouteall.q_misc_util.Helper;

public class RendererUsingStencil
extends PortalRenderer {
    private int lastPortalRenderFrame = -1;
    private int lastRenderedPortalCount = -1;
    private boolean loggedPortalRenderAttempt = false;

    @Override
    public boolean replaceFrameBufferClearing() {
        boolean skipClearing = WorldRenderInfo.isRendering();
        if (skipClearing && WorldRenderInfo.getTopRenderInfo().doRenderSky) {
            GlStateManager._depthMask((boolean)false);
            MyRenderHelper.renderScreenTriangle(FogRendererContext.getCurrentFogColor.get());
            GlStateManager._depthMask((boolean)true);
        }
        return skipClearing;
    }

    @Override
    public void onBeforeTranslucentRendering(Matrix4f modelView) {
        this.doPortalRenderingOnceThisFrame(modelView, false);
    }

    @Override
    public void onBeforeHandRendering(Matrix4f modelView) {
        this.doPortalRenderingOnceThisFrame(modelView, true);
    }

    private void doPortalRenderingOnceThisFrame(Matrix4f modelView, boolean isFallback) {
        if (this.lastPortalRenderFrame == RenderStates.frameIndex) {
            if (!isFallback) {
                return;
            }
            if (this.lastRenderedPortalCount > 0) {
                if (!IrisInterface.invoker.isShaders()) {
                    return;
                }
                this.prepareRendering();
            } else {
                GL11.glEnable((int)2960);
            }
        }
        this.lastPortalRenderFrame = RenderStates.frameIndex;
        int before = RenderStates.getRenderedPortalNum();
        this.doPortalRendering(modelView);
        this.lastRenderedPortalCount = RenderStates.getRenderedPortalNum() - before;
    }

    protected void doPortalRendering(Matrix4f modelView) {
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask((boolean)true);
        Profiler.get().popPush("render_portal_total");
        this.renderPortals(modelView);
        if (PortalRendering.isRendering()) {
            this.setStencilStateForWorldRendering();
        } else {
            this.myFinishRendering();
        }
    }

    protected void renderPortals(Matrix4f modelView) {
        List<Portal> portalsToRender = this.getPortalsToRender(modelView);
        if (!this.loggedPortalRenderAttempt && !portalsToRender.isEmpty()) {
            this.loggedPortalRenderAttempt = true;
            Helper.log("Stencil renderer found " + portalsToRender.size() + " portal(s) to render");
        }
        for (Portal portal : portalsToRender) {
            this.doRenderPortal(portal, modelView);
        }
    }

    @Override
    public void onAfterTranslucentRendering(Matrix4f modelView) {
    }

    @Override
    public void onHandRenderingEnded() {
    }

    @Override
    public void prepareRendering() {
        if (!IPPortingLibCompat.getIsStencilEnabled(client.getMainRenderTarget())) {
            IPPortingLibCompat.setIsStencilEnabled(client.getMainRenderTarget(), true);
            if (Minecraft.useShaderTransparency()) {
                // empty if block
            }
        }
        GL11.glClearStencil((int)0);
        GL11.glClear((int)1024);
        GlStateManager._enableDepthTest();
        GL11.glEnable((int)2960);
    }

    @Override
    public void finishRendering() {
    }

    private void myFinishRendering() {
        GL11.glStencilFunc((int)519, (int)2333, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7680);
        GL11.glStencilMask((int)255);
        GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GL11.glDepthMask((boolean)true);
        GL11.glDepthRange((double)0.0, (double)1.0);
        GL11.glDisable((int)2960);
        GlStateManager._enableDepthTest();
    }

    protected void doRenderPortal(Portal portal, Matrix4f modelView) {
        if (RendererUsingStencil.shouldSkipRenderingInsideFuseViewPortal(portal)) {
            return;
        }
        int outerPortalStencilValue = PortalRendering.getPortalLayer();
        Profiler.get().push("render_view_area");
        boolean anySamplePassed = PortalRenderInfo.renderAndDecideVisibility(portal, () -> this.renderPortalViewAreaToStencil(portal, modelView));
        Profiler.get().pop();
        if (!anySamplePassed) {
            this.setStencilStateForWorldRendering();
            return;
        }
        PortalRendering.pushPortalLayer(portal);
        int thisPortalStencilValue = outerPortalStencilValue + 1;
        if (!portal.isFuseView()) {
            Profiler.get().push("clear_depth_of_view_area");
            this.clearDepthOfThePortalViewArea(portal);
            Profiler.get().pop();
        }
        this.setStencilStateForWorldRendering();
        this.renderPortalContent(portal);
        PortalRendering.popPortalLayer();
        if (!portal.isFuseView()) {
            this.restoreDepthOfPortalViewArea(portal, modelView, thisPortalStencilValue);
        }
        RendererUsingStencil.clampStencilValue(outerPortalStencilValue);
    }

    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    }

    private void renderPortalViewAreaToStencil(Portal portal, Matrix4f modelView) {
        int outerPortalStencilValue = PortalRendering.getPortalLayer();
        GL11.glStencilFunc((int)514, (int)outerPortalStencilValue, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7682);
        GL11.glStencilMask((int)255);
        FrontClipping.updateInnerClipping(modelView);
        ViewAreaRenderer.renderPortalArea(portal, Vec3.ZERO, modelView, PortalRenderer.getCurrentProjectionMatrix(), true, false, true, true);
    }

    private void clearDepthOfThePortalViewArea(Portal portal) {
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask((boolean)true);
        this.setStencilStateForWorldRendering();
        GL11.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        int originalDepthFunc = GL11.glGetInteger((int)2932);
        GL11.glDepthFunc((int)519);
        GL11.glDepthRange((double)1.0, (double)1.0);
        MyRenderHelper.renderScreenTriangleNoColor(true);
        GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GL11.glDepthFunc((int)originalDepthFunc);
        GL11.glDepthRange((double)0.0, (double)1.0);
    }

    protected void restoreDepthOfPortalViewArea(Portal portal, Matrix4f modelView, int portalStencilValue) {
        RendererUsingStencil.setStencilLimitation(portalStencilValue);
        int originalDepthFunc = GL11.glGetInteger((int)2932);
        GL11.glDepthFunc((int)519);
        ViewAreaRenderer.renderPortalArea(portal, Vec3.ZERO, modelView, PortalRenderer.getCurrentProjectionMatrix(), false, false, true, true);
        GL11.glDepthFunc((int)originalDepthFunc);
    }

    public static void clampStencilValue(int maximumValue) {
        GlStateManager._depthMask((boolean)true);
        GL11.glStencilFunc((int)513, (int)maximumValue, (int)255);
        GL11.glStencilOp((int)7680, (int)7681, (int)7681);
        GL11.glDepthMask((boolean)false);
        GL11.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        GlStateManager._disableDepthTest();
        MyRenderHelper.renderScreenTriangleNoColor(false);
        GL11.glDepthMask((boolean)true);
        GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GlStateManager._enableDepthTest();
    }

    private void setStencilStateForWorldRendering() {
        int thisPortalStencilValue = PortalRendering.getPortalLayer();
        RendererUsingStencil.setStencilLimitation(thisPortalStencilValue);
    }

    public static void setStencilLimitation(int stencilValue) {
        GL11.glStencilFunc((int)514, (int)stencilValue, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7680);
    }

    public static boolean shouldSkipRenderingInsideFuseViewPortal(Portal portal) {
        Vec3 transformedCameraPos;
        if (!PortalRendering.isRendering()) {
            return false;
        }
        Portal renderingPortal = PortalRendering.getRenderingPortal();
        if (!renderingPortal.isFuseView()) {
            return false;
        }
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        return cameraPos.distanceToSqr(transformedCameraPos = portal.transformPoint(renderingPortal.transformPoint(cameraPos))) < 0.1;
    }
}
