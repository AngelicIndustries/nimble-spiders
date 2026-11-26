package industries.angelic.nimblespiders.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.nimblespiders.common.Vec3d;
import industries.angelic.nimblespiders.common.entity.mob.ClimberRider;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayerCamera {

    @Inject(method = "getPosition", at = @At("HEAD"), cancellable = true)
    private void nimblespiders$riderEye(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        if (ClimberRider.getMount(player) == null) return;
        Vec3d eye = ClimberRider.getEyePosition(player, partialTicks);
        cir.setReturnValue(Vec3.createVectorHelper(eye.x, eye.y, eye.z));
    }
}
