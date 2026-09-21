package qouteall.imm_ptl.core.render.renderer;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.SecondaryFrameBuffer;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;

public class RendererUsingFrameBuffer
extends PortalRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    SecondaryFrameBuffer secondaryFrameBuffer = new SecondaryFrameBuffer();
    private int lastPortalRenderFrame = -1;
    private int lastRenderedPortalCount = -1;
    private static final Set<Integer> diagnosticLoggedPortalIds = new HashSet<Integer>();

    @Override
    public void onBeforeTranslucentRendering(Matrix4f modelView) {
        this.renderPortalsOnceThisFrame(modelView, false);
    }

    @Override
    public void onBeforeHandRendering(Matrix4f modelView) {
        this.renderPortalsOnceThisFrame(modelView, true);
    }

    private void renderPortalsOnceThisFrame(Matrix4f modelView, boolean isFallback) {
        if (!(this.lastPortalRenderFrame != RenderStates.frameIndex || isFallback && this.lastRenderedPortalCount <= 0)) {
            return;
        }
        this.lastPortalRenderFrame = RenderStates.frameIndex;
        int before = RenderStates.getRenderedPortalNum();
        this.renderPortals(modelView);
        this.lastRenderedPortalCount = RenderStates.getRenderedPortalNum() - before;
    }

    @Override
    public void onAfterTranslucentRendering(Matrix4f modelView) {
    }

    @Override
    public void onHandRenderingEnded() {
    }

    @Override
    public void finishRendering() {
    }

    @Override
    public void prepareRendering() {
        this.secondaryFrameBuffer.prepare();
        GlStateManager._enableDepthTest();
        GL11.glDisable((int)2960);
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
        RenderTarget oldFrameBuffer = client.getMainRenderTarget();
        Matrix4f outerModelView = new Matrix4f((Matrix4fc)modelView);
        Matrix4f outerProjection = new Matrix4f((Matrix4fc)PortalRenderer.getCurrentProjectionMatrix());
        Vec3 outerCameraPos = CHelper.getCurrentCameraPos();
        this.logPortalProjectionDiagnostics(portal, outerModelView, outerProjection, outerCameraPos);
        ((IEMinecraftClient)client).ip_setFrameBuffer((RenderTarget)this.secondaryFrameBuffer.fb);
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(this.secondaryFrameBuffer.fb.getColorTexture(), -65281, this.secondaryFrameBuffer.fb.getDepthTexture(), 1.0);
        GL11.glDisable((int)2960);
        this.renderPortalContent(portal);
        ((IEMinecraftClient)client).ip_setFrameBuffer(oldFrameBuffer);
        PortalRendering.popPortalLayer();
        CHelper.enableDepthClamp();
        this.renderSecondBufferIntoMainBuffer(portal, outerModelView, outerProjection, outerCameraPos);
        CHelper.disableDepthClamp();
        MyRenderHelper.debugFramebufferDepth();
    }

    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    }

    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }

    private boolean testShouldRenderPortal(Portal portal, Matrix4f modelView) {
        Matrix4f projection = PortalRenderer.getCurrentProjectionMatrix();
        return PortalRenderInfo.renderAndDecideVisibility(portal, () -> ViewAreaRenderer.renderPortalArea(portal, Vec3.ZERO, modelView, projection, true, false, false, true));
    }

    private void renderSecondBufferIntoMainBuffer(Portal portal, Matrix4f modelView, Matrix4f projection, Vec3 cameraPos) {
        MyRenderHelper.drawPortalAreaWithFramebuffer(portal, (RenderTarget)this.secondaryFrameBuffer.fb, modelView, projection, cameraPos);
    }

    protected void renderPortals(Matrix4f modelView) {
        List<Portal> portalsToRender = this.getPortalsToRender(modelView);
        for (Portal portal : portalsToRender) {
            this.doRenderPortal(portal, modelView);
        }
    }

    private void logPortalProjectionDiagnostics(Portal portal, Matrix4f modelView, Matrix4f projection, Vec3 cameraPos) {
        if (!diagnosticLoggedPortalIds.add(portal.getId())) {
            return;
        }
        LOGGER.info("[ImmPtl 1.21.5 portal diag] framebuffer portal id={} type={} originDim={} destDim={} origin={} dest={} camera={} size={}x{} axisW={} axisH={} normal={} relOrigin={}", new Object[]{portal.getId(), portal.getClass().getSimpleName(), portal.level().dimension().location(), portal.getDestDim().location(), RendererUsingFrameBuffer.formatVec(portal.getOriginPos()), RendererUsingFrameBuffer.formatVec(portal.getDestPos()), RendererUsingFrameBuffer.formatVec(cameraPos), RendererUsingFrameBuffer.formatDouble(portal.getWidth()), RendererUsingFrameBuffer.formatDouble(portal.getHeight()), RendererUsingFrameBuffer.formatVec(portal.getAxisW()), RendererUsingFrameBuffer.formatVec(portal.getAxisH()), RendererUsingFrameBuffer.formatVec(portal.getNormal()), RendererUsingFrameBuffer.formatVec(portal.getOriginPos().subtract(cameraPos))});
        if (portal instanceof BreakablePortalEntity) {
            BreakablePortalEntity breakablePortal = (BreakablePortalEntity)portal;
            if (breakablePortal.blockPortalShape != null) {
                BlockPortalShape shape = breakablePortal.blockPortalShape;
                Vec3 shapeCenter = shape.innerAreaBox.getCenterVec();
                LOGGER.info("[ImmPtl 1.21.5 portal diag] block shape portal id={} axis={} innerBox={} totalBox={} area={} shapeCenter={} shapeMinusPortal={}", new Object[]{portal.getId(), shape.axis, shape.innerAreaBox, shape.totalAreaBox, shape.area.size(), RendererUsingFrameBuffer.formatVec(shapeCenter), RendererUsingFrameBuffer.formatVec(shapeCenter.subtract(portal.getOriginPos()))});
            }
        }
        LOGGER.info("[ImmPtl 1.21.5 portal diag] projected portal id={} cornersNdc={}", (Object)portal.getId(), (Object)RendererUsingFrameBuffer.getProjectedCornerSummary(portal, modelView, projection, cameraPos));
    }

    private static String getProjectedCornerSummary(Portal portal, Matrix4f modelView, Matrix4f projection, Vec3 cameraPos) {
        double halfWidth = portal.getWidth() / 2.0;
        double halfHeight = portal.getHeight() / 2.0;
        Vec3 originRel = portal.getOriginPos().subtract(cameraPos);
        Vec3 axisW = portal.getAxisW();
        Vec3 axisH = portal.getAxisH();
        Vec3[] corners = new Vec3[]{originRel.add(axisW.scale(-halfWidth)).add(axisH.scale(-halfHeight)), originRel.add(axisW.scale(halfWidth)).add(axisH.scale(-halfHeight)), originRel.add(axisW.scale(halfWidth)).add(axisH.scale(halfHeight)), originRel.add(axisW.scale(-halfWidth)).add(axisH.scale(halfHeight))};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < corners.length; ++i) {
            if (i != 0) {
                builder.append(" ");
            }
            builder.append(i).append(":").append(RendererUsingFrameBuffer.projectToNdc(corners[i], modelView, projection));
        }
        return builder.toString();
    }

    private static String projectToNdc(Vec3 cameraRelativePos, Matrix4f modelView, Matrix4f projection) {
        Vector4f vector = new Vector4f((float)cameraRelativePos.x, (float)cameraRelativePos.y, (float)cameraRelativePos.z, 1.0f);
        modelView.transform(vector);
        projection.transform(vector);
        if ((double)Math.abs(vector.w()) < 1.0E-6) {
            return String.format("clip(%.3f,%.3f,%.3f,w=%.6f)", Float.valueOf(vector.x()), Float.valueOf(vector.y()), Float.valueOf(vector.z()), Float.valueOf(vector.w()));
        }
        return String.format("ndc(%.3f,%.3f,%.3f,w=%.3f)", Float.valueOf(vector.x() / vector.w()), Float.valueOf(vector.y() / vector.w()), Float.valueOf(vector.z() / vector.w()), Float.valueOf(vector.w()));
    }

    private static String formatVec(Vec3 vec) {
        return String.format("(%.3f, %.3f, %.3f)", vec.x, vec.y, vec.z);
    }

    private static String formatDouble(double value) {
        return String.format("%.3f", value);
    }
}
