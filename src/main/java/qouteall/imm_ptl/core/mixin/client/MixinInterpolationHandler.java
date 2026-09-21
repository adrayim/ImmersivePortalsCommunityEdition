package qouteall.imm_ptl.core.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntity;

@Mixin(InterpolationHandler.class)
public class MixinInterpolationHandler {
    @Shadow @Final private Entity entity;

    @Inject(method = "interpolateTo", at = @At("TAIL"))
    private void ip_snapAcrossPortal(Vec3 target, float yaw, float pitch, CallbackInfo ci) {
        if (IPGlobal.allowClientEntityPosInterpolation) {
            if (((IEEntity) entity).ip_getCollidingPortal() == null ||
                entity.position().distanceToSqr(target) <= 4.0) {
                return;
            }
            McHelper.setPosAndLastTickPos(
                entity, target, target.subtract(McHelper.getWorldVelocity(entity))
            );
            McHelper.updateBoundingBox(entity);
        }
        else {
            entity.setPos(target);
        }
        ((InterpolationHandler) (Object) this).cancel();
    }
}
