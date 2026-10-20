package com.carmod.mixin;

import net.minecraft.world.item.ItemInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInstance.class)
public interface ItemMaxStackMixin {
	@Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
	private void carmod$raiseStackable(CallbackInfoReturnable<Integer> cir) {
		if (cir.getReturnValueI() > 1) {
			cir.setReturnValue(256);
		}
	}
}
