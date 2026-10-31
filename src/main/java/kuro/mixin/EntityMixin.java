package kuro.mixin;

import kuro.Freecam;
import kuro.Freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
   // Fare hareketini karakter yerine Freecam / Freelook kamerasina yonlendir.
   @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
   private void kuro$look(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
      if ((Object)this != MinecraftClient.getInstance().player) {
         return;
      }

      if (Freecam.active) {
         Freecam.look(cursorDeltaX, cursorDeltaY);
         ci.cancel();
      } else if (Freelook.active) {
         Freelook.look(cursorDeltaX, cursorDeltaY);
         ci.cancel();
      }
   }
}
