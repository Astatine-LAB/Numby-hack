package cqb13.NumbyHack.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cqb13.NumbyHack.modules.general.Confetti;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.world.phys.Vec3;

@Mixin(TotemParticle.class)
public abstract class TotemParticleMixin extends SimpleAnimatedParticle {

    protected TotemParticleMixin(ClientLevel world, double x, double y, double z, SpriteSet spriteProvider,
            float upwardsAcceleration) {
        super(world, x, y, z, spriteProvider, upwardsAcceleration);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConfettiConstructor(ClientLevel world, double x, double y, double z, double velocityX,
            double velocityY, double velocityZ, SpriteSet spriteProvider, CallbackInfo ci) {
        Confetti confetti = Modules.get().get(Confetti.class);
        TotemParticle totemParticle = ((TotemParticle) (Object) this);
        if (confetti.isActive()) {
            Vec3 colorOne = confetti.getColorOne();
            Vec3 colorTwo = confetti.getColorTwo();
            if (this.random.nextInt(4) == 0) {
                totemParticle.setColor((float) colorOne.x, (float) colorOne.y, (float) colorOne.z);
            } else {
                totemParticle.setColor((float) colorTwo.x, (float) colorTwo.y, (float) colorTwo.z);
            }
        }
    }

}
