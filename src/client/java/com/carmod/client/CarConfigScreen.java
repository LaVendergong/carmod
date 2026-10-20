package com.carmod.client;

import com.carmod.ModConfig;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class CarConfigScreen extends Screen {
	private final Screen parent;

	public CarConfigScreen(Screen parent) {
		super(Component.translatable("screen.carmod.config"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int x = this.width / 2 - 100;
		int y = this.height / 2 - 22;
		this.addRenderableWidget(CycleButton.onOffBuilder(ModConfig.thirdPerson()).create(
				x, y, 200, 20,
				Component.translatable("options.carmod.third_person"),
				(button, value) -> ModConfig.setThirdPerson(value)));
		this.addRenderableWidget(new VolumeSlider(x, y + 24, 200, 20));
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds(x, y + 48, 200, 20)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 48, 0xFFFFFF);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}

	private static final class VolumeSlider extends AbstractSliderButton {
		private VolumeSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), ModConfig.impactVolume());
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			int percent = (int) Math.round(this.value * 100.0);
			this.setMessage(percent == 0
					? Component.translatable("options.carmod.impact_sound.off")
					: Component.translatable("options.carmod.impact_sound", percent));
		}

		@Override
		protected void applyValue() {
			int percent = (int) Math.round(this.value * 100.0);
			this.value = percent / 100.0;
			ModConfig.setImpactVolume(percent);
		}
	}
}
