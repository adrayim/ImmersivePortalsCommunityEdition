package qouteall.imm_ptl.core.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayDeque;
import java.util.Stack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.chunk_loading.PerformanceLevel;
import qouteall.imm_ptl.core.ducks.IERenderSection;
import qouteall.imm_ptl.core.miscellaneous.ClientPerformanceMonitor;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.nether_portal.BlockTraverse;
import qouteall.imm_ptl.core.render.ImmPtlViewArea;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

@Environment(value=EnvType.CLIENT)
public class VisibleSectionDiscovery {
    private static ImmPtlViewArea builtChunks;
    private static Frustum vanillaFrustum;
    private static ObjectArrayList<SectionRenderDispatcher.RenderSection> resultHolder;
    private static final ArrayDeque<SectionRenderDispatcher.RenderSection> tempQueue;
    private static SectionPos cameraSectionPos;
    private static long timeMark;
    private static int viewDistance;
    private static final Stack<ObjectArrayList<SectionRenderDispatcher.RenderSection>> listCaches;

    public static void discoverVisibleSections(ClientLevel world, ImmPtlViewArea builtChunks_, Camera camera, Frustum vanillaFrustum_, ObjectArrayList<SectionRenderDispatcher.RenderSection> resultHolder_) {
        builtChunks = builtChunks_;
        vanillaFrustum = vanillaFrustum_;
        resultHolder = resultHolder_;
        resultHolder.clear();
        tempQueue.clear();
        VisibleSectionDiscovery.updateViewDistance();
        timeMark = System.nanoTime();
        Vec3 cameraPos = camera.getPosition();
        vanillaFrustum.prepare(cameraPos.x, cameraPos.y, cameraPos.z);
        cameraSectionPos = SectionPos.of((BlockPos)BlockPos.containing((Position)cameraPos));
        SectionPos modifiedVisibleSectionIterationOrigin = null;
        if (PortalRendering.isRendering()) {
            Portal renderingPortal = PortalRendering.getRenderingPortal();
            modifiedVisibleSectionIterationOrigin = renderingPortal.getPortalShape().getModifiedVisibleSectionIterationOrigin(renderingPortal, cameraPos);
        }
        if (modifiedVisibleSectionIterationOrigin != null) {
            VisibleSectionDiscovery.checkSection(modifiedVisibleSectionIterationOrigin.getX(), modifiedVisibleSectionIterationOrigin.getY(), modifiedVisibleSectionIterationOrigin.getZ(), true);
        } else if (cameraPos.y < (double)world.getMinY()) {
            VisibleSectionDiscovery.discoverBottomOrTopLayerVisibleChunks(VisibleSectionDiscovery.builtChunks.minSectionY);
        } else if (cameraPos.y > (double)world.getMaxY()) {
            VisibleSectionDiscovery.discoverBottomOrTopLayerVisibleChunks(VisibleSectionDiscovery.builtChunks.endSectionY - 1);
        } else {
            VisibleSectionDiscovery.checkSection(cameraSectionPos.x(), cameraSectionPos.y(), cameraSectionPos.z(), true);
        }
        while (!tempQueue.isEmpty()) {
            SectionRenderDispatcher.RenderSection curr = tempQueue.poll();
            int cx = SectionPos.blockToSectionCoord((int)curr.getRenderOrigin().getX());
            int cy = SectionPos.blockToSectionCoord((int)curr.getRenderOrigin().getY());
            int cz = SectionPos.blockToSectionCoord((int)curr.getRenderOrigin().getZ());
            VisibleSectionDiscovery.checkSection(cx + 1, cy, cz, false);
            VisibleSectionDiscovery.checkSection(cx - 1, cy, cz, false);
            VisibleSectionDiscovery.checkSection(cx, cy + 1, cz, false);
            VisibleSectionDiscovery.checkSection(cx, cy - 1, cz, false);
            VisibleSectionDiscovery.checkSection(cx, cy, cz + 1, false);
            VisibleSectionDiscovery.checkSection(cx, cy, cz - 1, false);
        }
        resultHolder = null;
        builtChunks = null;
        vanillaFrustum = null;
    }

    private static void updateViewDistance() {
        int distance = WorldRenderInfo.getRenderDistance();
        viewDistance = PerformanceLevel.getPortalRenderingDistance(ClientPerformanceMonitor.level, distance);
    }

    private static boolean isVisible(SectionRenderDispatcher.RenderSection builtChunk) {
        AABB box = builtChunk.getBoundingBox();
        return vanillaFrustum.isVisible(box);
    }

    private static void discoverBottomOrTopLayerVisibleChunks(int cy) {
        BlockTraverse.searchOnPlane(cameraSectionPos.x(), cameraSectionPos.z(), viewDistance - 1, (cx, cz) -> {
            VisibleSectionDiscovery.checkSection(cx, cy, cz, false);
            return null;
        });
    }

    private static void checkSection(int cx, int cy, int cz, boolean skipFrustumTest) {
        IERenderSection ieRenderSection;
        if (Math.abs(cx - cameraSectionPos.x()) > viewDistance) {
            return;
        }
        if (Math.abs(cy - cameraSectionPos.y()) > viewDistance) {
            return;
        }
        if (Math.abs(cz - cameraSectionPos.z()) > viewDistance) {
            return;
        }
        SectionRenderDispatcher.RenderSection builtChunk = builtChunks.rawFetch(cx, cy, cz, timeMark);
        if (builtChunk != null && (ieRenderSection = (IERenderSection)builtChunk).portal_getMark() != timeMark) {
            ieRenderSection.portal_setMark(timeMark);
            if (skipFrustumTest || VisibleSectionDiscovery.isVisible(builtChunk)) {
                tempQueue.add(builtChunk);
                resultHolder.add(builtChunk);
            }
        }
    }

    public static ObjectArrayList<SectionRenderDispatcher.RenderSection> takeList() {
        if (listCaches.isEmpty()) {
            return new ObjectArrayList();
        }
        return listCaches.pop();
    }

    public static void returnList(ObjectArrayList<SectionRenderDispatcher.RenderSection> list) {
        list.clear();
        listCaches.push(list);
    }

    public static void init() {
        IPCGlobal.CLIENT_CLEANUP_EVENT.register(VisibleSectionDiscovery::cleanUp);
        ClientWorldLoader.CLIENT_DIMENSION_DYNAMIC_REMOVE_EVENT.register(dim -> VisibleSectionDiscovery.cleanUp());
    }

    private static void cleanUp() {
        listCaches.clear();
        resultHolder = null;
        builtChunks = null;
        vanillaFrustum = null;
    }

    static {
        tempQueue = new ArrayDeque();
        listCaches = new Stack();
    }
}
