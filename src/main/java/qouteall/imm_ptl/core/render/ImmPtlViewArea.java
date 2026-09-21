package qouteall.imm_ptl.core.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlClientChunkMap;
import qouteall.imm_ptl.core.ducks.IERenderSection;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;

@Environment(value=EnvType.CLIENT)
public class ImmPtlViewArea
extends ViewArea {
    private final SectionRenderDispatcher factory;
    private final Long2ObjectOpenHashMap<Column> columnMap = new Long2ObjectOpenHashMap();
    private final Long2ObjectOpenHashMap<Preset> presets = new Long2ObjectOpenHashMap();
    private Preset currentPreset = null;
    @Nullable
    private SectionPos currentCameraSectionPos = null;
    public final int minSectionY;
    public final int endSectionY;
    private boolean isAlive = true;

    public static void init() {
        ImmPtlClientChunkMap.clientChunkUnloadSignal.connect(section -> {
            ViewArea viewArea;
            ResourceKey dimension = section.getLevel().dimension();
            LevelRenderer worldRenderer = ClientWorldLoader.WORLD_RENDERER_MAP.get(dimension);
            if (worldRenderer != null && (viewArea = ((IEWorldRenderer)worldRenderer).ip_getBuiltChunkStorage()) instanceof ImmPtlViewArea) {
                ImmPtlViewArea immPtlViewArea = (ImmPtlViewArea)viewArea;
                immPtlViewArea.onChunkUnload(section.getPos().x, section.getPos().z);
            }
        });
        IPGlobal.POST_CLIENT_TICK_EVENT.register(() -> {
            if (ClientWorldLoader.getIsInitialized()) {
                for (ClientLevel world : ClientWorldLoader.getClientWorlds()) {
                    LevelRenderer worldRenderer = ClientWorldLoader.getWorldRenderer((ResourceKey<Level>)world.dimension());
                    ViewArea viewArea = ((IEWorldRenderer)worldRenderer).ip_getBuiltChunkStorage();
                    if (!(viewArea instanceof ImmPtlViewArea)) continue;
                    ImmPtlViewArea immPtlViewArea = (ImmPtlViewArea)viewArea;
                    immPtlViewArea.tick();
                }
            }
        });
    }

    public ImmPtlViewArea(SectionRenderDispatcher sectionBuilder, Level world, int r, LevelRenderer worldRenderer) {
        super(sectionBuilder, world, r, worldRenderer);
        this.factory = sectionBuilder;
        int cacheSize = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
        if (IPGlobal.cacheGlBuffer) {
            cacheSize /= 10;
        }
        this.minSectionY = McHelper.getMinSectionY((LevelAccessor)world);
        this.endSectionY = McHelper.getMaxSectionYExclusive((LevelAccessor)world);
    }

    protected void createSections(SectionRenderDispatcher sectionRenderDispatcher) {
        int num = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
        this.sections = new SectionRenderDispatcher.RenderSection[num];
    }

    public void releaseAllBuffers() {
        Set<SectionRenderDispatcher.RenderSection> allActiveBuiltChunks = this.getAllActiveBuiltChunks();
        allActiveBuiltChunks.forEach(ImmPtlViewArea::releaseBuffers);
        this.columnMap.clear();
        this.presets.clear();
        this.isAlive = false;
    }

    public void repositionCamera(SectionPos cameraSectionPos) {
        Profiler.get().push("built_section_storage");
        boolean cameraSectionChanged = this.currentCameraSectionPos == null || !this.currentCameraSectionPos.equals((Object)cameraSectionPos);
        this.currentCameraSectionPos = cameraSectionPos;
        int cameraChunkX = cameraSectionPos.x();
        int cameraChunkZ = cameraSectionPos.z();
        ChunkPos cameraChunkPos = new ChunkPos(cameraChunkX, cameraChunkZ);
        Preset preset = (Preset)this.presets.computeIfAbsent(cameraChunkPos.toLong(), whatever -> this.createPresetByChunkPos(cameraChunkX, cameraChunkZ));
        preset.lastActiveTime = System.nanoTime();
        this.sections = preset.data;
        this.currentPreset = preset;
        if (cameraSectionChanged && !WorldRenderInfo.isRendering()) {
            this.levelRenderer.getSectionOcclusionGraph().invalidate();
        }
        Profiler.get().pop();
    }

    public void setDirty(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread) {
        SectionRenderDispatcher.RenderSection builtChunk = this.provideBuiltChunkByChunkPos(sectionX, sectionY, sectionZ);
        builtChunk.setDirty(reRenderOnMainThread);
    }

    public SectionRenderDispatcher.RenderSection provideBuiltChunkByChunkPos(int cx, int cy, int cz) {
        Column column = this.provideColumn(ChunkPos.asLong((int)cx, (int)cz));
        int offsetChunkY = Mth.clamp((int)(cy - McHelper.getMinSectionY((LevelAccessor)this.level)), (int)0, (int)(McHelper.getYSectionNumber((LevelAccessor)this.level) - 1));
        return column.sections[offsetChunkY];
    }

    private Preset createPresetByChunkPos(int sectionX, int sectionZ) {
        SectionRenderDispatcher.RenderSection[] sections1 = new SectionRenderDispatcher.RenderSection[this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ];
        for (int cx = 0; cx < this.sectionGridSizeX; ++cx) {
            int xBlockSize = this.sectionGridSizeX * 16;
            int xStart = (sectionX << 4) - xBlockSize / 2;
            int px = xStart + Math.floorMod(cx * 16 - xStart, xBlockSize);
            for (int cz = 0; cz < this.sectionGridSizeZ; ++cz) {
                int zBlockSize = this.sectionGridSizeZ * 16;
                int zStart = (sectionZ << 4) - zBlockSize / 2;
                int pz = zStart + Math.floorMod(cz * 16 - zStart, zBlockSize);
                Validate.isTrue((px % 16 == 0 ? 1 : 0) != 0);
                Validate.isTrue((pz % 16 == 0 ? 1 : 0) != 0);
                Column column = this.provideColumn(ChunkPos.asLong((int)(px >> 4), (int)(pz >> 4)));
                for (int offsetCy = 0; offsetCy < this.sectionGridSizeY; ++offsetCy) {
                    int index = this.getChunkIndex(cx, offsetCy, cz);
                    sections1[index] = column.sections[offsetCy];
                }
            }
        }
        return new Preset(sections1);
    }

    private void foreachPresetCoveredChunkPoses(int centerChunkX, int centerChunkZ, LongConsumer func) {
        SectionRenderDispatcher.RenderSection[] sections1 = new SectionRenderDispatcher.RenderSection[this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ];
        for (int cx = 0; cx < this.sectionGridSizeX; ++cx) {
            int xBlockSize = this.sectionGridSizeX * 16;
            int xStart = (centerChunkX << 4) - xBlockSize / 2;
            int px = xStart + Math.floorMod(cx * 16 - xStart, xBlockSize);
            for (int cz = 0; cz < this.sectionGridSizeZ; ++cz) {
                int zBlockSize = this.sectionGridSizeZ * 16;
                int zStart = (centerChunkZ << 4) - zBlockSize / 2;
                int pz = zStart + Math.floorMod(cz * 16 - zStart, zBlockSize);
                Validate.isTrue((px % 16 == 0 ? 1 : 0) != 0);
                Validate.isTrue((pz % 16 == 0 ? 1 : 0) != 0);
                long sectionPos = ChunkPos.asLong((int)(px >> 4), (int)(pz >> 4));
                func.accept(sectionPos);
            }
        }
    }

    private int getChunkIndex(int x, int y, int z) {
        return (z * this.sectionGridSizeY + y) * this.sectionGridSizeX + x;
    }

    public Column provideColumn(long sectionPos) {
        return (Column)this.columnMap.computeIfAbsent(sectionPos, this::createColumn);
    }

    private Column createColumn(long sectionPos) {
        SectionRenderDispatcher.RenderSection[] array = new SectionRenderDispatcher.RenderSection[this.sectionGridSizeY];
        int sectionX = ChunkPos.getX((long)sectionPos);
        int sectionZ = ChunkPos.getZ((long)sectionPos);
        int minY = McHelper.getMinY((LevelAccessor)this.level);
        for (int offsetCY = 0; offsetCY < this.sectionGridSizeY; ++offsetCY) {
            SectionRenderDispatcher.RenderSection builtChunk;
            SectionRenderDispatcher sectionRenderDispatcher = this.factory;
            Objects.requireNonNull(sectionRenderDispatcher);
            array[offsetCY] = builtChunk = sectionRenderDispatcher.new RenderSection(0, SectionPos.asLong(sectionX, this.minSectionY + offsetCY, sectionZ));
        }
        return new Column(array);
    }

    private void tick() {
        if (!this.isAlive) {
            return;
        }
        ClientLevel worldClient = Minecraft.getInstance().level;
        if (worldClient != null) {
            if (GcMonitor.isMemoryNotEnough()) {
                if (worldClient.getGameTime() % 3L == 0L) {
                    this.purge();
                }
            } else if (worldClient.getGameTime() % 213L == 66L) {
                this.purge();
            }
        }
    }

    private void purge() {
        Profiler.get().push("my_built_section_storage_purge");
        long dropTime = Helper.secondToNano(GcMonitor.isMemoryNotEnough() ? 3.0 : 20.0);
        long currentTime = System.nanoTime();
        this.presets.long2ObjectEntrySet().removeIf(entry -> {
            Preset preset = (Preset)entry.getValue();
            long centerChunkPos = entry.getLongKey();
            boolean shouldDropPreset = this.shouldDropPreset(dropTime, currentTime, preset);
            if (!shouldDropPreset) {
                this.foreachPresetCoveredChunkPoses(ChunkPos.getX((long)centerChunkPos), ChunkPos.getZ((long)centerChunkPos), columnChunkPos -> {
                    Column column = (Column)this.columnMap.get(columnChunkPos);
                    column.mark = currentTime;
                });
            }
            return shouldDropPreset;
        });
        long timeThreshold = Helper.secondToNano(5.0);
        ArrayDeque toDelete = new ArrayDeque();
        this.columnMap.long2ObjectEntrySet().removeIf(entry -> {
            boolean shouldRemove;
            Column column = (Column)entry.getValue();
            boolean bl = shouldRemove = currentTime - column.mark > timeThreshold;
            if (shouldRemove) {
                toDelete.addAll(Arrays.asList(column.sections));
            }
            return shouldRemove;
        });
        if (!toDelete.isEmpty()) {
            IPGlobal.PRE_GAME_RENDER_TASK_LIST.addTask(() -> {
                if (toDelete.isEmpty()) {
                    return true;
                }
                for (int num = 0; !toDelete.isEmpty() && num < 100; ++num) {
                    SectionRenderDispatcher.RenderSection builtChunk = (SectionRenderDispatcher.RenderSection)toDelete.poll();
                    ImmPtlViewArea.releaseBuffers(builtChunk);
                }
                return false;
            });
        }
        Profiler.get().pop();
    }

    private boolean shouldDropPreset(long dropTime, long currentTime, Preset preset) {
        if (preset.data == this.sections) {
            return false;
        }
        return currentTime - preset.lastActiveTime > dropTime;
    }

    private Set<SectionRenderDispatcher.RenderSection> getAllActiveBuiltChunks() {
        HashSet<SectionRenderDispatcher.RenderSection> result = new HashSet<SectionRenderDispatcher.RenderSection>();
        this.presets.forEach((key, preset) -> result.addAll(Arrays.asList(preset.data)));
        if (this.sections != null) {
            result.addAll(Arrays.asList(this.sections));
        }
        result.remove(null);
        return result;
    }

    public int getManagedSectionNum() {
        return this.columnMap.size() * this.sectionGridSizeY;
    }

    public String getDebugString() {
        return String.format("Built Section Storage Columns:%s", this.columnMap.size());
    }

    public int getRadius() {
        return (this.sectionGridSizeX - 1) / 2;
    }

    public boolean isRegionActive(int cxStart, int czStart, int cxEnd, int czEnd) {
        for (int cx = cxStart; cx <= cxEnd; ++cx) {
            for (int cz = czStart; cz <= czEnd; ++cz) {
                if (!this.columnMap.containsKey(ChunkPos.asLong((int)cx, (int)cz))) continue;
                return true;
            }
        }
        return false;
    }

    public void onChunkUnload(int sectionX, int sectionZ) {
        long sectionPos = ChunkPos.asLong((int)sectionX, (int)sectionZ);
        Column column = (Column)this.columnMap.get(sectionPos);
        if (column != null) {
            for (SectionRenderDispatcher.RenderSection builtChunk : column.sections) {
                ((IERenderSection)builtChunk).portal_fullyReset();
            }
        }
    }

    public SectionRenderDispatcher.RenderSection getSectionFromRawArray(BlockPos sectionOrigin, SectionRenderDispatcher.RenderSection[] sections) {
        int i = Mth.floorDiv((int)sectionOrigin.getX(), (int)16);
        int j = Mth.floorDiv((int)(sectionOrigin.getY() - McHelper.getMinY((LevelAccessor)this.level)), (int)16);
        int k = Mth.floorDiv((int)sectionOrigin.getZ(), (int)16);
        if (j >= 0 && j < this.sectionGridSizeY) {
            i = Mth.positiveModulo((int)i, (int)this.sectionGridSizeX);
            k = Mth.positiveModulo((int)k, (int)this.sectionGridSizeZ);
            return sections[this.getChunkIndex(i, j, k)];
        }
        return null;
    }

    public SectionPos getCameraSectionPos() {
        return this.currentCameraSectionPos != null ? this.currentCameraSectionPos : super.getCameraSectionPos();
    }

    private static void releaseBuffers(SectionRenderDispatcher.RenderSection renderSection) {
        RenderType.chunkBufferLayers().forEach(renderType -> {
            SectionRenderDispatcher.SectionBuffers buffers = renderSection.getBuffers(renderType);
            if (buffers != null) {
                buffers.close();
            }
        });
    }

    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pos) {
        return this.getRenderSection(SectionPos.asLong((BlockPos)pos));
    }

    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSection(long sectionPos) {
        int zIndex;
        int yIndex;
        int sectionX = SectionPos.x((long)sectionPos);
        int sectionY = SectionPos.y((long)sectionPos);
        int sectionZ = SectionPos.z((long)sectionPos);
        if (sectionY < this.minSectionY || sectionY >= this.endSectionY) {
            return null;
        }
        int xIndex = Mth.positiveModulo((int)sectionX, (int)this.sectionGridSizeX);
        int sectionIndex = this.getChunkIndex(xIndex, yIndex = sectionY - this.minSectionY, zIndex = Mth.positiveModulo((int)sectionZ, (int)this.sectionGridSizeZ));
        SectionRenderDispatcher.RenderSection result = this.sections[sectionIndex];
        if (result == null || result.getSectionNode() != sectionPos) {
            this.sections[sectionIndex] = result = this.provideBuiltChunkByChunkPos(sectionX, sectionY, sectionZ);
        }
        ((IERenderSection)result).portal_setIndex(sectionIndex);
        return result;
    }

    @Nullable
    public SectionRenderDispatcher.RenderSection rawFetch(int cx, int cy, int cz, long timeMark) {
        if (cy < this.minSectionY || cy >= this.endSectionY) {
            return null;
        }
        long l = ChunkPos.asLong((int)cx, (int)cz);
        Column column = this.provideColumn(l);
        column.mark = timeMark;
        int yOffset = cy - this.minSectionY;
        return column.sections[yOffset];
    }

    @Nullable
    public SectionRenderDispatcher.RenderSection rawGet(int cx, int cy, int cz) {
        if (cy < this.minSectionY || cy >= this.endSectionY) {
            return null;
        }
        long l = ChunkPos.asLong((int)cx, (int)cz);
        Column column = (Column)this.columnMap.get(l);
        if (column == null) {
            return null;
        }
        int yOffset = cy - this.minSectionY;
        return column.sections[yOffset];
    }

    public static class Preset {
        public final SectionRenderDispatcher.RenderSection[] data;
        public long lastActiveTime = 0L;

        public Preset(SectionRenderDispatcher.RenderSection[] data) {
            this.data = data;
        }
    }

    public static class Column {
        public long mark = 0L;
        public SectionRenderDispatcher.RenderSection[] sections;

        public Column(SectionRenderDispatcher.RenderSection[] sections) {
            this.sections = sections;
        }
    }
}
