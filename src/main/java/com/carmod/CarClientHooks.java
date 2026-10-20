package com.carmod;

import com.carmod.entity.CarEntity;

/**
 * Client fills these in. Common code calls them so the entity stays out of the client source set.
 */
public final class CarClientHooks {
	@FunctionalInterface
	public interface InputReader {
		void readInto(CarEntity car);
	}

	public static InputReader inputReader = car -> {
	};

	public static java.util.function.Consumer<CarEntity> afterLocalDrive = car -> {
	};

	public static java.util.function.Consumer<CarEntity> impact = car -> {
	};

	public static java.util.function.Consumer<CarEntity> crash = car -> {
	};

	private CarClientHooks() {
	}
}
