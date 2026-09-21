package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

public class MyRenderHelper {
    public static final Minecraft client = Minecraft.getInstance();
    public static final RenderPipeline PORTAL_DRAW_FB_IN_AREA_PIPELINE = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(McHelper.newResourceLocation("immersive_portals:pipeline/portal_draw_fb_in_area")).withVertexShader(McHelper.newResourceLocation("immersive_portals:core/portal_draw_fb_in_area")).withFragmentShader(McHelper.newResourceLocation("immersive_portals:core/portal_draw_fb_in_area")).withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES).withSampler("DiffuseSampler").withUniform("IP_ModelViewMat", UniformType.MATRIX4X4).withUniform("IP_ProjMat", UniformType.MATRIX4X4).withUniform("w", UniformType.FLOAT).withUniform("h", UniformType.FLOAT).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withCull(false).withoutBlend().build());
    public static final RenderPipeline PORTAL_AREA_PIPELINE = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(McHelper.newResourceLocation("immersive_portals:pipeline/portal_area")).withVertexShader(McHelper.newResourceLocation("immersive_portals:core/portal_area")).withFragmentShader(McHelper.newResourceLocation("immersive_portals:core/portal_area")).withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES).withUniform("IP_ModelViewMat", UniformType.MATRIX4X4).withUniform("IP_ProjMat", UniformType.MATRIX4X4).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withoutBlend().build());
    public static final RenderPipeline PORTAL_AREA_NO_COLOR_PIPELINE = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(McHelper.newResourceLocation("immersive_portals:pipeline/portal_area_no_color")).withVertexShader(McHelper.newResourceLocation("immersive_portals:core/portal_area")).withFragmentShader(McHelper.newResourceLocation("immersive_portals:core/portal_area")).withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES).withUniform("IP_ModelViewMat", UniformType.MATRIX4X4).withUniform("IP_ProjMat", UniformType.MATRIX4X4).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withColorWrite(false).withDepthWrite(true).withoutBlend().build());
    public static final RenderPipeline PORTAL_AREA_NO_COLOR_NO_DEPTH_PIPELINE = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(McHelper.newResourceLocation("immersive_portals:pipeline/portal_area_no_color_no_depth")).withVertexShader(McHelper.newResourceLocation("immersive_portals:core/portal_area")).withFragmentShader(McHelper.newResourceLocation("immersive_portals:core/portal_area")).withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES).withUniform("IP_ModelViewMat", UniformType.MATRIX4X4).withUniform("IP_ProjMat", UniformType.MATRIX4X4).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withColorWrite(false).withDepthWrite(false).withoutBlend().build());
    public static final RenderPipeline PORTAL_AREA_DEPTH_CLEAR_PIPELINE = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(McHelper.newResourceLocation("immersive_portals:pipeline/portal_area_depth_clear")).withVertexShader(McHelper.newResourceLocation("immersive_portals:core/portal_area")).withFragmentShader(McHelper.newResourceLocation("immersive_portals:core/portal_area")).withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES).withUniform("IP_ModelViewMat", UniformType.MATRIX4X4).withUniform("IP_ProjMat", UniformType.MATRIX4X4).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withColorWrite(false).withDepthWrite(true).withoutBlend().build());
    public static final RenderPipeline BLIT_SCREEN_NOBLEND_PIPELINE = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(McHelper.newResourceLocation("immersive_portals:pipeline/blit_screen_noblend")).withVertexShader(McHelper.newResourceLocation("immersive_portals:core/blit_screen_noblend")).withFragmentShader(McHelper.newResourceLocation("immersive_portals:core/blit_screen_noblend")).withVertexFormat(DefaultVertexFormat.BLIT_SCREEN, VertexFormat.Mode.QUADS).withSampler("DiffuseSampler").withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withoutBlend().build());
    private static boolean debugEnabled = false;

    public static void init() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void drawMesh(RenderPipeline pipeline, MeshData meshData, Consumer<RenderPass> configureRenderPass) {
        try (MeshData meshData2 = meshData;){
            MeshData.DrawState drawState = meshData.drawState();
            GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Immersive Portals vertex buffer", BufferType.VERTICES, BufferUsage.STREAM_WRITE, meshData.vertexBuffer());
            GpuBuffer indexBuffer = null;
            boolean ownsIndexBuffer = false;
            try {
                VertexFormat.IndexType indexType;
                if (meshData.indexBuffer() != null) {
                    indexBuffer = RenderSystem.getDevice().createBuffer(() -> "Immersive Portals index buffer", BufferType.INDICES, BufferUsage.STREAM_WRITE, meshData.indexBuffer());
                    indexType = drawState.indexType();
                    ownsIndexBuffer = true;
                } else {
                    RenderSystem.AutoStorageIndexBuffer sequentialBuffer = RenderSystem.getSequentialBuffer((VertexFormat.Mode)drawState.mode());
                    indexBuffer = sequentialBuffer.getBuffer(drawState.indexCount());
                    indexType = sequentialBuffer.type();
                }
                RenderTarget target = client.getMainRenderTarget();
                try (RenderPass renderPass = target.getDepthTexture() != null ? RenderSystem.getDevice().createCommandEncoder().createRenderPass(target.getColorTexture(), OptionalInt.empty(), target.getDepthTexture(), OptionalDouble.empty()) : RenderSystem.getDevice().createCommandEncoder().createRenderPass(target.getColorTexture(), OptionalInt.empty());){
                    renderPass.setPipeline(pipeline);
                    renderPass.setVertexBuffer(0, vertexBuffer);
                    renderPass.setIndexBuffer(indexBuffer, indexType);
                    if (configureRenderPass != null) {
                        configureRenderPass.accept(renderPass);
                    }
                    renderPass.drawIndexed(0, drawState.indexCount());
                }
            }
            finally {
                vertexBuffer.close();
                if (ownsIndexBuffer && indexBuffer != null) {
                    indexBuffer.close();
                }
            }
        }
    }

    public static void drawPortalAreaWithFramebuffer(Portal portal, RenderTarget textureProvider, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        MyRenderHelper.drawPortalAreaWithFramebuffer(portal, textureProvider, modelViewMatrix, projectionMatrix, CHelper.getCurrentCameraPos());
    }

    public static void drawPortalAreaWithFramebuffer(Portal portal, RenderTarget textureProvider, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Vec3 cameraPos) {
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask((boolean)true);
        RenderTarget target = client.getMainRenderTarget();
        GlStateManager._viewport((int)0, (int)0, (int)target.viewWidth, (int)target.viewHeight);
        GlStateManager._disableBlend();
        ViewAreaRenderer.buildPortalViewAreaTrianglesBuffer(Vec3.ZERO, portal, cameraPos, RenderStates.getPartialTick(), PORTAL_DRAW_FB_IN_AREA_PIPELINE, renderPass -> {
            renderPass.bindSampler("DiffuseSampler", textureProvider.getColorTexture());
            renderPass.setUniform("IP_ModelViewMat", modelViewMatrix);
            renderPass.setUniform("IP_ProjMat", projectionMatrix);
            renderPass.setUniform("w", new float[]{textureProvider.viewWidth});
            renderPass.setUniform("h", new float[]{textureProvider.viewHeight});
        });
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate((int)770, (int)771, (int)1, (int)0);
    }

    public static void renderScreenTriangle() {
        MyRenderHelper.renderScreenTriangle(255, 255, 255, 255);
    }

    public static void renderScreenTriangleNoColor(boolean writeDepth) {
        MyRenderHelper.renderScreenTriangle(255, 255, 255, 255, writeDepth ? PORTAL_AREA_DEPTH_CLEAR_PIPELINE : PORTAL_AREA_NO_COLOR_NO_DEPTH_PIPELINE);
    }

    public static void renderScreenTriangle(Vec3 color) {
        MyRenderHelper.renderScreenTriangle((int)(color.x * 255.0), (int)(color.y * 255.0), (int)(color.z * 255.0), 255);
    }

    public static void testOneTriangle(int r, int g, int b, int a) {
        Matrix4f identityMatrix = new Matrix4f();
        identityMatrix.identity();
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(-1.0f, 1.0f, 0.0f).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1.0f, -1.0f, 0.0f).setColor(r, g, b, a);
        bufferBuilder.addVertex(1.0f, -1.0f, 0.0f).setColor(r, g, b, a);
        bufferBuilder.addVertex(1.0f, 0.0f, 0.0f).setColor(r, g, b, a);
        bufferBuilder.addVertex(0.0f, 1.0f, 0.0f).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1.0f, 0.0f, 0.0f).setColor(r, g, b, a);
        MeshData meshData = bufferBuilder.build();
        if (meshData != null) {
            MyRenderHelper.drawMesh(PORTAL_AREA_PIPELINE, meshData, renderPass -> {
                renderPass.setUniform("IP_ModelViewMat", identityMatrix);
                renderPass.setUniform("IP_ProjMat", identityMatrix);
            });
        }
    }

    @IPVanillaCopy
    public static void renderScreenTriangle(int r, int g, int b, int a) {
        MyRenderHelper.renderScreenTriangle(r, g, b, a, PORTAL_AREA_PIPELINE);
    }

    private static void renderScreenTriangle(int r, int g, int b, int a, RenderPipeline pipeline) {
        Matrix4f identityMatrix = new Matrix4f();
        identityMatrix.identity();
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(1.0f, -1.0f, 0.0f).setColor(r, g, b, a);
        bufferBuilder.addVertex(1.0f, 1.0f, 0.0f).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1.0f, 1.0f, 0.0f).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1.0f, 1.0f, 0.0f).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1.0f, -1.0f, 0.0f).setColor(r, g, b, a);
        bufferBuilder.addVertex(1.0f, -1.0f, 0.0f).setColor(r, g, b, a);
        MeshData meshData = bufferBuilder.build();
        if (meshData != null) {
            MyRenderHelper.drawMesh(pipeline, meshData, renderPass -> {
                renderPass.setUniform("IP_ModelViewMat", identityMatrix);
                renderPass.setUniform("IP_ProjMat", identityMatrix);
            });
        }
    }

    public static void drawScreenFrameBuffer(RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha) {
        int x = 0;
        int y = 0;
        int viewportWidth = textureProvider.viewWidth;
        int viewportHeight = textureProvider.viewHeight;
        MyRenderHelper.drawFramebufferWithCoordinatesAndDimensions(textureProvider, doUseAlphaBlend, doEnableModifyAlpha, x, y, viewportWidth, viewportHeight);
    }

    public static void drawFramebuffer(RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha, float xMin, float xMax, float yMin, float yMax) {
        MyRenderHelper.drawFramebufferWithCoordinatesAndDimensions(textureProvider, doUseAlphaBlend, doEnableModifyAlpha, 0, 0, client.getWindow().getWidth(), client.getWindow().getHeight());
    }

    public static void drawFramebufferWithViewport(RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha, float left, float right, float bottom, float up, int viewportWidth, int viewportHeight) {
        MyRenderHelper.drawFramebufferWithCoordinatesAndDimensions(textureProvider, doUseAlphaBlend, doEnableModifyAlpha, 0, 0, viewportWidth, viewportHeight);
    }

    public static void drawFramebufferWithBounds(RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha, int xMin, int xMax, int yMin, int yMax) {
        MyRenderHelper.drawFramebufferWithCoordinatesAndDimensions(textureProvider, doUseAlphaBlend, doEnableModifyAlpha, xMin, yMin, Mth.abs((int)(xMax - xMin)), Mth.abs((int)(yMax - yMin)));
    }

    @IPVanillaCopy
    public static void drawFramebufferWithCoordinatesAndDimensions(RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha, int x, int y, int viewportWidth, int viewportHeight) {
        CHelper.checkGlError();
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask((boolean)false);
        GlStateManager._viewport((int)x, (int)(textureProvider.viewHeight - viewportHeight - y), (int)viewportWidth, (int)viewportHeight);
        if (doUseAlphaBlend) {
            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate((int)1, (int)771, (int)0, (int)1);
        } else {
            GlStateManager._disableBlend();
        }
        if (doEnableModifyAlpha) {
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        } else {
            GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)false);
        }
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
        bufferBuilder.addVertex(0.0f, 0.0f, 0.0f);
        bufferBuilder.addVertex(1.0f, 0.0f, 0.0f);
        bufferBuilder.addVertex(1.0f, 1.0f, 0.0f);
        bufferBuilder.addVertex(0.0f, 1.0f, 0.0f);
        MyRenderHelper.drawMesh(doUseAlphaBlend ? RenderPipelines.GUI_TEXTURED : BLIT_SCREEN_NOBLEND_PIPELINE, bufferBuilder.buildOrThrow(), renderPass -> renderPass.bindSampler("DiffuseSampler", textureProvider.getColorTexture()));
        GlStateManager._depthMask((boolean)true);
        GlStateManager._colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate((int)770, (int)771, (int)1, (int)0);
        CHelper.checkGlError();
    }

    public static void lateUpdateLight() {
        if (!ClientWorldLoader.getIsInitialized()) {
            return;
        }
        ClientWorldLoader.getClientWorlds().forEach(world -> {
            if (!RenderStates.isDimensionRendered((ResourceKey<Level>)world.dimension())) {
                world.getChunkSource().getLightEngine().runLightUpdates();
            }
        });
    }

    public static void earlyRemoteUpload() {
        if (!ClientWorldLoader.getIsInitialized()) {
            return;
        }
        ClientWorldLoader.WORLD_RENDERER_MAP.forEach((dim, worldRenderer) -> {
            if (MyRenderHelper.client.level.dimension() != dim) {
                worldRenderer.getSectionRenderDispatcher().uploadAllPendingUploads();
            }
        });
    }

    public static void applyMirrorFaceCulling() {
        GL11.glCullFace((int)1028);
    }

    public static void recoverFaceCulling() {
        GL11.glCullFace((int)1029);
    }

    public static void clearAlphaTo1(RenderTarget mcFrameBuffer) {
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(mcFrameBuffer.getColorTexture(), -16777216);
    }

    public static void restoreViewPort() {
        Minecraft client = Minecraft.getInstance();
        RenderTarget target = client.getMainRenderTarget();
        GlStateManager._viewport((int)0, (int)0, (int)target.viewWidth, (int)target.viewHeight);
    }

    public static float transformFogDistance(float value) {
        Portal renderingPortal;
        if (!WorldRenderInfo.isFogEnabled()) {
            return value * 23333.0f;
        }
        if (PortalRendering.isRendering() && (renderingPortal = PortalRendering.getRenderingPortal()).isFuseView()) {
            return value * 23333.0f;
        }
        return value;
    }

    public static void debugFramebufferDepth() {
        if (!debugEnabled) {
            return;
        }
        debugEnabled = false;
        int width = MyRenderHelper.client.getMainRenderTarget().width;
        int height = MyRenderHelper.client.getMainRenderTarget().height;
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer floatBuffer = directBuffer.asFloatBuffer();
        GL11.glReadPixels((int)0, (int)0, (int)width, (int)height, (int)6402, (int)5126, (FloatBuffer)floatBuffer);
        float[] data = new float[width * height];
        floatBuffer.rewind();
        floatBuffer.get(data);
        float maxValue = (float)IntStream.range(0, data.length).mapToDouble(i -> data[i]).max().getAsDouble();
        float minValue = (float)IntStream.range(0, data.length).mapToDouble(i -> data[i]).min().getAsDouble();
        byte[] grayData = new byte[width * height];
        for (int i2 = 0; i2 < data.length; ++i2) {
            float datum = data[i2];
            datum = (datum - minValue) / (maxValue - minValue);
            grayData[i2] = (byte)(datum * 255.0f);
        }
        BufferedImage bufferedImage = new BufferedImage(width, height, 10);
        bufferedImage.setData(Raster.createRaster(bufferedImage.getSampleModel(), new DataBufferByte(grayData, grayData.length), new Point()));
        System.out.println("oops");
    }

    public static void debugFramebufferColorRed() {
        if (!debugEnabled) {
            return;
        }
        debugEnabled = false;
        int width = MyRenderHelper.client.getMainRenderTarget().width;
        int height = MyRenderHelper.client.getMainRenderTarget().height;
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer floatBuffer = directBuffer.asFloatBuffer();
        GL11.glReadPixels((int)0, (int)0, (int)width, (int)height, (int)6403, (int)5126, (FloatBuffer)floatBuffer);
        float[] data = new float[width * height];
        floatBuffer.rewind();
        floatBuffer.get(data);
        float maxValue = (float)IntStream.range(0, data.length).mapToDouble(i -> data[i]).max().getAsDouble();
        float minValue = (float)IntStream.range(0, data.length).mapToDouble(i -> data[i]).min().getAsDouble();
        byte[] grayData = new byte[width * height];
        for (int i2 = 0; i2 < data.length; ++i2) {
            float datum = data[i2];
            datum = (datum - minValue) / (maxValue - minValue);
            grayData[i2] = (byte)(datum * 255.0f);
        }
        BufferedImage bufferedImage = new BufferedImage(width, height, 10);
        bufferedImage.setData(Raster.createRaster(bufferedImage.getSampleModel(), new DataBufferByte(grayData, grayData.length), new Point()));
        System.out.println("oops");
    }
}
