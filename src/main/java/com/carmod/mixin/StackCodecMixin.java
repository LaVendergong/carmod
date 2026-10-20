package com.carmod.mixin;

import com.mojang.serialization.Codec;

import net.minecraft.util.ExtraCodecs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExtraCodecs.class)
public abstract class StackCodecMixin {
	@Inject(method = "intRange", at = @At("HEAD"), cancellable = true)
	private static void carmod$widenStackRange(int min, int max, CallbackInfoReturnable<Codec<Integer>> cir) {
		if (min == 1 && max == 99) {
			cir.setReturnValue(ExtraCodecs.intRange(min, 256));
		}
	}
}
