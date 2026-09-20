package qouteall.imm_ptl.core.mixin.common.interaction;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.block_manipulation.CrossPortalEntityInteraction;

@Mixin(DamageSource.class)
public class MixinDamageSource_CrossPortalAttack {
    @Inject(method = "getSourcePosition", at = @At("HEAD"), cancellable = true)
    private void ip_remoteAttackSourcePosition(CallbackInfoReturnable<Vec3> cir) {
        var context = CrossPortalEntityInteraction.getAttackContext();
        if (context != null && ((DamageSource) (Object) this).getEntity() == context.attacker()) {
            cir.setReturnValue(context.remotePosition());
        }
    }
}
