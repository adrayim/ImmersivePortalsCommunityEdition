package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.TransformationManager;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.my_util.TriangleConsumer;

public class ViewAreaRenderer {
    public static void renderPortalArea(Portal portal, Vec3 fogColor, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, boolean doFaceCulling, boolean doModifyColor, boolean doModifyDepth, boolean doClip) {
        if (doFaceCulling) {
            GlStateManager._enableCull();
        } else {
            GlStateManager._disableCull();
        }
        if (portal.isFuseView() && IPGlobal.maxPortalLayer != 0) {
            GlStateManager._colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        } else if (!doModifyColor) {
            GlStateManager._colorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        } else {
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        }
        if (doModifyDepth) {
            if (portal.isFuseView()) {
                GlStateManager._depthMask((boolean)false);
            } else {
                GlStateManager._depthMask((boolean)true);
            }
        } else {
            GlStateManager._depthMask((boolean)false);
        }
        boolean shouldReverseCull = PortalRendering.isRenderingOddNumberOfMirrors();
        if (shouldReverseCull) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
        if (doClip) {
            if (PortalRendering.isRendering()) {
                FrontClipping.setupInnerClipping(PortalRendering.getActiveClippingPlane(), modelViewMatrix, 0.0);
            }
        } else {
            FrontClipping.disableClipping();
        }
        GlStateManager._enableDepthTest();
        CHelper.enableDepthClamp();
        FrontClipping.updateClippingEquationUniformForCurrentShader(false);
        ViewAreaRenderer.buildPortalViewAreaTrianglesBuffer(fogColor, portal, CHelper.getCurrentCameraPos(), RenderStates.getPartialTick(), doModifyColor ? MyRenderHelper.PORTAL_AREA_PIPELINE : MyRenderHelper.PORTAL_AREA_NO_COLOR_PIPELINE, renderPass -> {
            renderPass.setUniform("IP_ModelViewMat", modelViewMatrix);
            renderPass.setUniform("IP_ProjMat", projectionMatrix);
        });
        GlStateManager._enableCull();
        CHelper.disableDepthClamp();
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GlStateManager._depthMask((boolean)true);
        if (shouldReverseCull) {
            MyRenderHelper.recoverFaceCulling();
        }
        if (PortalRendering.isRendering()) {
            FrontClipping.disableClipping();
        }
        CHelper.checkGlError();
    }

    public static void buildPortalViewAreaTrianglesBuffer(Vec3 fogColor, Portal portal, Vec3 cameraPos, float partialTick) {
        ViewAreaRenderer.buildPortalViewAreaTrianglesBuffer(fogColor, portal, cameraPos, partialTick, MyRenderHelper.PORTAL_AREA_PIPELINE, null);
    }

    public static void buildPortalViewAreaTrianglesBuffer(Vec3 fogColor, Portal portal, Vec3 cameraPos, float partialTick, RenderPipeline pipeline, Consumer<RenderPass> configureRenderPass) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Vec3 originRelativeToCamera = portal.getOriginPos().subtract(cameraPos);
        TriangleConsumer vertexOutput = (p0x, p0y, p0z, p1x, p1y, p1z, p2x, p2y, p2z) -> {
            bufferBuilder.addVertex((float)p0x, (float)p0y, (float)p0z).setColor((float)fogColor.x, (float)fogColor.y, (float)fogColor.z, 1.0f);
            bufferBuilder.addVertex((float)p1x, (float)p1y, (float)p1z).setColor((float)fogColor.x, (float)fogColor.y, (float)fogColor.z, 1.0f);
            bufferBuilder.addVertex((float)p2x, (float)p2y, (float)p2z).setColor((float)fogColor.x, (float)fogColor.y, (float)fogColor.z, 1.0f);
        };
        portal.renderViewAreaMesh(originRelativeToCamera, vertexOutput);
        MeshData meshData = Objects.requireNonNull(bufferBuilder.build());
        MyRenderHelper.drawMesh(pipeline, meshData, configureRenderPass);
    }

    public static void outputTriangle(TriangleConsumer vertexOutput, Vec3 center, Vec3 localXAxis, Vec3 localYAxis, double p0x, double p0y, double p1x, double p1y, double p2x, double p2y) {
        vertexOutput.accept(center.x + p0x * localXAxis.x() + p0y * localYAxis.x(), center.y + p0x * localXAxis.y() + p0y * localYAxis.y(), center.z + p0x * localXAxis.z() + p0y * localYAxis.z(), center.x + p1x * localXAxis.x() + p1y * localYAxis.x(), center.y + p1x * localXAxis.y() + p1y * localYAxis.y(), center.z + p1x * localXAxis.z() + p1y * localYAxis.z(), center.x + p2x * localXAxis.x() + p2y * localYAxis.x(), center.y + p2x * localXAxis.y() + p2y * localYAxis.y(), center.z + p2x * localXAxis.z() + p2y * localYAxis.z());
    }

    @Deprecated
    private static void generateTriangleForNormalShape(TriangleConsumer vertexOutput, Portal portal, Vec3 posInPlayerCoordinate) {
        double w = Math.min(portal.getWidth(), 23333.0);
        double h = Math.min(portal.getHeight(), 23333.0);
        Vec3 localXAxis = portal.getAxisW().scale(w / 2.0);
        Vec3 localYAxis = portal.getAxisH().scale(h / 2.0);
        ViewAreaRenderer.outputFullQuad(vertexOutput, posInPlayerCoordinate, localXAxis, localYAxis);
    }

    @Deprecated
    private static void generateTriangleForGlobalPortal(TriangleConsumer vertexOutput, Portal portal, Vec3 portalOriginLocal) {
        double distance;
        Vec3 cameraPosFromPortalOrigin = portalOriginLocal.scale(-1.0);
        Vec3 cameraPosFromPortalOriginProjected = portal.getLocalVecProjectedToPlane(cameraPosFromPortalOrigin);
        Vec3 localCenter = portalOriginLocal.add(cameraPosFromPortalOriginProjected);
        double r = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 - 16;
        if (TransformationManager.isIsometricView) {
            r *= 2.0;
        }
        if ((distance = Math.abs(cameraPosFromPortalOrigin.dot(portal.getNormal()))) > 200.0) {
            r = r * 200.0 / distance;
        }
        Vec3 localXAxis = portal.getAxisW().scale(r);
        Vec3 localYAxis = portal.getAxisH().scale(r);
        ViewAreaRenderer.outputFullQuad(vertexOutput, localCenter, localXAxis, localYAxis);
    }

    public static void outputFullQuad(TriangleConsumer vertexOutput, Vec3 posInPlayerCoordinate, Vec3 localXAxis, Vec3 localYAxis) {
        ViewAreaRenderer.outputTriangle(vertexOutput, posInPlayerCoordinate, localXAxis, localYAxis, 1.0, 1.0, -1.0, 1.0, 1.0, -1.0);
        ViewAreaRenderer.outputTriangle(vertexOutput, posInPlayerCoordinate, localXAxis, localYAxis, -1.0, 1.0, -1.0, -1.0, 1.0, -1.0);
    }
}
