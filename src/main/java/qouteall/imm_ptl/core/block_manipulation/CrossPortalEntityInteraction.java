package qouteall.imm_ptl.core.block_manipulation;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;
import qouteall.imm_ptl.core.portal.PortalUtils;
import net.minecraft.resources.ResourceKey;

public class CrossPortalEntityInteraction {
    public record AttackContext(ServerPlayer attacker, Vec3 remotePosition, float remoteYaw) {}

    private static final ThreadLocal<AttackContext> ATTACK_CONTEXT = new ThreadLocal<>();

    @Nullable
    public static AttackContext getAttackContext() {
        return ATTACK_CONTEXT.get();
    }

    private static BlockHitResult findBlockHit(Level world, Entity viewer, Vec3 from, Vec3 to) {
        ClipContext context = new ClipContext(
            from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, viewer
        );
        return BlockGetter.traverseBlocks(
            from, to, context,
            (clipContext, blockPos) -> {
                var state = world.getBlockState(blockPos);
                if (state.getBlock() == PortalPlaceholderBlock.instance ||
                    state.getBlock() == Blocks.BARRIER) {
                    return null;
                }
                return world.clipWithInteractionOverride(
                    clipContext.getFrom(), clipContext.getTo(), blockPos,
                    clipContext.getBlockShape(state, world, blockPos), state
                );
            },
            clipContext -> BlockHitResult.miss(
                to, Direction.getApproximateNearest(from.subtract(to)), BlockPos.containing(to)
            )
        );
    }

    @Nullable
    public static EntityHitResult findTarget(
        Level world, Entity viewer, Vec3 from, Vec3 to
    ) {
        BlockHitResult blockHit = findBlockHit(world, viewer, from, to);
        Vec3 unobstructedEnd = blockHit.getType() == HitResult.Type.BLOCK
            ? blockHit.getLocation() : to;

        return ProjectileUtil.getEntityHitResult(
            world, viewer, from, unobstructedEnd,
            new AABB(from, unobstructedEnd).inflate(1.0),
            entity -> entity != viewer && !(entity instanceof Portal) &&
                entity.isPickable() && !entity.isSpectator()
        );
    }

    public static class RemoteCallables {
        public static void attackEntity(
            ServerPlayer player, ResourceKey<Level> dimension, int entityId
        ) {
            if (!BlockManipulationServer.canDoCrossPortalInteractionEvent.invoker().test(player)) {
                return;
            }

            double reach = player.entityInteractionRange();
            var portalHit = PortalUtils.raytracePortalFromEntityView(
                player, 1.0f, reach, true, portal -> portal.isInteractableBy(player)
            );
            if (portalHit.isEmpty()) {
                return;
            }

            Portal portal = portalHit.get().getFirst();
            if (portal.getDestDim() != dimension) {
                return;
            }
            ServerLevel remoteWorld = player.server.getLevel(dimension);
            if (remoteWorld == null) {
                return;
            }

            Vec3 eye = player.getEyePosition();
            Vec3 portalPoint = portalHit.get().getSecond().hitPos();
            BlockHitResult nearBlock = findBlockHit(player.level(), player, eye, portalPoint);
            if (nearBlock.getType() == HitResult.Type.BLOCK &&
                eye.distanceToSqr(nearBlock.getLocation()) + 0.0001 < eye.distanceToSqr(portalPoint)) {
                return;
            }

            Vec3 direction = portal.transformLocalVecNonScale(player.getViewVector(1.0f)).normalize();
            Vec3 from = portal.transformPoint(portalPoint).add(direction.scale(0.001));
            Vec3 to = portal.transformPoint(
                eye.add(player.getViewVector(1.0f).scale(reach))
            );
            EntityHitResult hit = findTarget(remoteWorld, player, from, to);
            if (hit == null || hit.getEntity().getId() != entityId) {
                return;
            }

            Vec3 remotePosition = portal.transformPoint(player.position());
            float remoteYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            ATTACK_CONTEXT.set(new AttackContext(player, remotePosition, remoteYaw));
            try {
                PacketRedirection.withForceRedirect(remoteWorld, () -> player.attack(hit.getEntity()));
            }
            finally {
                ATTACK_CONTEXT.remove();
            }
        }
    }
}
