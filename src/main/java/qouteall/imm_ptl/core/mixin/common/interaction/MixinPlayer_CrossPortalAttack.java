package qouteall.imm_ptl.core.mixin.common.interaction;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.block_manipulation.CrossPortalEntityInteraction;

@Mixin(Player.class)
public class MixinPlayer_CrossPortalAttack {
    @ModifyExpressionValue(
        method = "attack",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getYRot()F")
    )
    private float ip_remoteAttackYaw(float originalYaw) {
        var context = CrossPortalEntityInteraction.getAttackContext();
        if (context != null && (Object) this == context.attacker()) {
            return context.remoteYaw();
        }
        return originalYaw;
    }
}
