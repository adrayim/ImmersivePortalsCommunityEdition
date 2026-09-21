package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.compat.iris_compatibility.IEIrisNewWorldRenderingPipeline;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;
import qouteall.imm_ptl.core.render.renderer.RendererUsingStencil;

public class ExperimentalIrisPortalRenderer
extends PortalRenderer {
    public static final ExperimentalIrisPortalRenderer instance = new ExperimentalIrisPortalRenderer();

    public static void init() {
    }

    @Override
    public boolean replaceFrameBufferClearing() {
        boolean skipClearing = PortalRendering.isRendering();
        return skipClearing;
    }

    @Override
    public void onBeforeTranslucentRendering(Matrix4f modelView) {
    }

    @Override
    public void onBeginIrisTranslucentRendering(Matrix4f modelView) {
        client.renderBuffers().bufferSource().endBatch();
        this.doPortalRendering(modelView);
        ((IEIrisNewWorldRenderingPipeline)Iris.getPipelineManager().getPipeline().get()).ip_setIsRenderingWorld(true);
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
        }
        GL11.glClearStencil((int)0);
        GL11.glClear((int)1024);
        GlStateManager._enableDepthTest();
        GL11.glEnable((int)2960);
    }

    @Override
    public void finishRendering() {
        this.myFinishRendering();
    }

    protected void restoreDepthOfPortalViewArea(Portal portal, Matrix4f modelView) {
        this.setStencilStateForWorldRendering();
        int originalDepthFunc = GL11.glGetInteger((int)2932);
        GL11.glDepthFunc((int)519);
        ViewAreaRenderer.renderPortalArea(portal, Vec3.ZERO, modelView, RenderSystem.getProjectionMatrix(), false, false, true, true);
        GL11.glDepthFunc((int)originalDepthFunc);
    }

    @Override
    public void invokeWorldRendering(WorldRenderInfo worldRenderInfo) {
        WorldRenderingPipeline pipeline = (WorldRenderingPipeline)Iris.getPipelineManager().getPipeline().get();
        SystemTimeUniforms.COUNTER.beginFrame();
        super.invokeWorldRendering(worldRenderInfo);
        SystemTimeUniforms.COUNTER.beginFrame();
        if (pipeline instanceof IrisRenderingPipeline) {
            IrisRenderingPipeline newWorldRenderingPipeline = (IrisRenderingPipeline)pipeline;
            newWorldRenderingPipeline.isBeforeTranslucent = true;
        }
        ((IEIrisNewWorldRenderingPipeline)pipeline).ip_setIsRenderingWorld(false);
    }

    protected void doPortalRendering(Matrix4f modelView) {
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask((boolean)true);
        Profiler.get().popPush("render_portal_total");
        this.renderPortals(modelView);
    }

    private void myFinishRendering() {
        GL11.glStencilFunc((int)519, (int)2333, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7680);
        GL11.glDisable((int)2960);
        GlStateManager._enableDepthTest();
    }

    protected void renderPortals(Matrix4f modelView) {
        List<Portal> portalsToRender = this.getPortalsToRender(modelView);
        ArrayList<Portal> reallyRenderedPortals = new ArrayList<Portal>();
        for (Portal portal : portalsToRender) {
            boolean reallyRendered = this.doRenderPortal(portal, modelView);
            if (!reallyRendered) continue;
            reallyRenderedPortals.add(portal);
        }
        this.setStencilStateForWorldRendering();
        for (Portal reallyRenderedPortal : reallyRenderedPortals) {
            if (reallyRenderedPortal.isFuseView()) continue;
            this.renderPortalViewAreaToStencil(reallyRenderedPortal, modelView);
        }
        this.setStencilStateForWorldRendering();
    }

    private boolean doRenderPortal(Portal portal, Matrix4f modelView) {
        if (RendererUsingStencil.shouldSkipRenderingInsideFuseViewPortal(portal)) {
            return false;
        }
        int outerPortalStencilValue = PortalRendering.getPortalLayer();
        Profiler.get().push("render_view_area");
        boolean anySamplePassed = PortalRenderInfo.renderAndDecideVisibility(portal, () -> this.renderPortalViewAreaToStencil(portal, modelView));
        Profiler.get().pop();
        if (!anySamplePassed) {
            this.setStencilStateForWorldRendering();
            return false;
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
        if (!portal.isFuseView()) {
            this.restoreDepthOfPortalViewArea(portal, modelView);
        }
        ExperimentalIrisPortalRenderer.clampStencilValue(outerPortalStencilValue);
        PortalRendering.popPortalLayer();
        return true;
    }

    public void onAfterIrisDeferredCompositeRendering() {
        int outerPortalStencilValue = PortalRendering.getPortalLayer();
        ExperimentalIrisPortalRenderer.clampStencilValue(outerPortalStencilValue);
        this.setStencilStateForWorldRendering();
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
        ViewAreaRenderer.renderPortalArea(portal, Vec3.ZERO, modelView, RenderSystem.getProjectionMatrix(), true, false, true, true);
    }

    private void clearDepthOfThePortalViewArea(Portal portal) {
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask((boolean)true);
        this.setStencilStateForWorldRendering();
        GL11.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        int originalDepthFunc = GL11.glGetInteger((int)2932);
        GL11.glDepthFunc((int)519);
        GL11.glDepthRange((double)1.0, (double)1.0);
        MyRenderHelper.renderScreenTriangle();
        GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GL11.glDepthFunc((int)originalDepthFunc);
        GL11.glDepthRange((double)0.0, (double)1.0);
    }

    public static void clampStencilValue(int maximumValue) {
        GlStateManager._depthMask((boolean)true);
        GL11.glStencilFunc((int)513, (int)maximumValue, (int)255);
        GL11.glStencilOp((int)7680, (int)7681, (int)7681);
        GL11.glDepthMask((boolean)false);
        GL11.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        GlStateManager._disableDepthTest();
        MyRenderHelper.renderScreenTriangle();
        GL11.glDepthMask((boolean)true);
        GL11.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GlStateManager._enableDepthTest();
    }

    private void setStencilStateForWorldRendering() {
        int thisPortalStencilValue = PortalRendering.getPortalLayer();
        GL11.glStencilFunc((int)514, (int)thisPortalStencilValue, (int)255);
        GL11.glStencilOp((int)7680, (int)7680, (int)7680);
    }
}
