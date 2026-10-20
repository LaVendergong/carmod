package com.carmod.mixin;

import net.minecraft.world.Container;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Container.class)
public interface ContainerSlotMixin {
	@ModifyConstant(method = "getMaxStackSize()I", constant = @Constant(intValue = 99))
	private int carmod$slotCap(int cap) {
		return 256;
	}
}
