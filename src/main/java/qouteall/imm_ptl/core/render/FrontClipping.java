package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.q_misc_util.my_util.Plane;

public class FrontClipping {
    private static final Minecraft client = Minecraft.getInstance();
    private static double[] activeClipPlaneEquationBeforeModelView;
    private static double[] activeClipPlaneAfterModelView;
    public static boolean isClippingEnabled;
    public static final double ADJUSTMENT = 0.01;

    public static void disableClipping() {
        if (IPGlobal.enableClippingMechanism && isClippingEnabled) {
            GL11.glDisable((int)12288);
            isClippingEnabled = false;
        }
    }

    private static void enableClipping() {
        if (IPGlobal.enableClippingMechanism && !isClippingEnabled) {
            GL11.glEnable((int)12288);
            isClippingEnabled = true;
        }
    }

    public static void updateInnerClipping(PoseStack matrixStack) {
        Matrix4f modelView = matrixStack.last().pose();
        FrontClipping.updateInnerClipping(modelView);
    }

    public static void updateInnerClipping(Matrix4f modelView) {
        if (PortalRendering.isRendering()) {
            FrontClipping.setupInnerClipping(PortalRendering.getActiveClippingPlane(), modelView, 0.0);
        } else {
            FrontClipping.disableClipping();
        }
    }

    public static void setupInnerClipping(Plane clipping, Matrix4f modelView, double adjustment) {
        if (!IPCGlobal.useFrontClipping) {
            return;
        }
        if (clipping != null) {
            activeClipPlaneEquationBeforeModelView = FrontClipping.getClipEquationInner(clipping.pos(), clipping.normal(), adjustment);
            activeClipPlaneAfterModelView = FrontClipping.transformClipEquation(activeClipPlaneEquationBeforeModelView, modelView);
            FrontClipping.enableClipping();
        } else {
            activeClipPlaneEquationBeforeModelView = null;
            FrontClipping.disableClipping();
        }
    }

    private static double[] transformClipEquation(double[] equation, Matrix4f modelView) {
        Vector4f eq = new Vector4f((float)equation[0], (float)equation[1], (float)equation[2], (float)equation[3]);
        Matrix4f m = new Matrix4f((Matrix4fc)modelView);
        m.invert();
        m.transpose();
        m.transform(eq);
        return new double[]{eq.x(), eq.y(), eq.z(), eq.w()};
    }

    private static double[] getClipEquationInner(Vec3 clippingPoint, Vec3 clippingDirection, double correction) {
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        Vec3 planeNormal = clippingDirection;
        Vec3 portalPos = clippingPoint.add(planeNormal.scale(correction)).subtract(cameraPos);
        double c = planeNormal.scale(-1.0).dot(portalPos);
        return new double[]{planeNormal.x, planeNormal.y, planeNormal.z, c};
    }

    public static void setupOuterClipping(PoseStack matrixStack, Portal portal) {
        if (!IPCGlobal.useFrontClipping) {
            return;
        }
        double[] clipEquationOuter = FrontClipping.getClipEquationOuter(portal);
        if (clipEquationOuter != null) {
            activeClipPlaneEquationBeforeModelView = clipEquationOuter;
            activeClipPlaneAfterModelView = FrontClipping.transformClipEquation(activeClipPlaneEquationBeforeModelView, matrixStack.last().pose());
            FrontClipping.enableClipping();
        } else {
            activeClipPlaneEquationBeforeModelView = null;
            FrontClipping.disableClipping();
        }
    }

    private static double @Nullable [] getClipEquationOuter(Portal portal) {
        @Nullable Plane outerClipping = portal.getPortalShape().getOuterClipping(portal.getThisSideState());
        if (outerClipping == null) {
            return null;
        }
        Vec3 planeNormal = outerClipping.normal();
        Vec3 cameraPos = FrontClipping.client.gameRenderer.getMainCamera().getPosition();
        Vec3 portalPos = outerClipping.pos().subtract(cameraPos);
        double c = planeNormal.scale(-1.0).dot(portalPos);
        return new double[]{planeNormal.x, planeNormal.y, planeNormal.z, c};
    }

    public static double[] getActiveClipPlaneEquationBeforeModelView() {
        return activeClipPlaneEquationBeforeModelView;
    }

    public static double[] getActiveClipPlaneEquationAfterModelView() {
        return activeClipPlaneAfterModelView;
    }

    public static void updateClippingEquationUniformForCurrentShader(boolean isRenderingEntities) {
        if (!IPGlobal.enableClippingMechanism) {
            return;
        }
    }

    public static void unsetClippingUniform() {
        if (!IPGlobal.enableClippingMechanism) {
            return;
        }
    }

    static {
        isClippingEnabled = false;
    }
}
