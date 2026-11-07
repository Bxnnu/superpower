package com.example.superpowers.client;

import com.example.superpowers.Power;
import com.example.superpowers.network.PowerActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PowerScreen extends Screen {
	public PowerScreen() {
		super(Text.literal("Choose Your Power"));
	}

	@Override
	protected void init() {
		int x = this.width / 2 - 75;
		int y = this.height / 2 - 84;
		for (Power power : Power.values()) {
			addDrawableChild(ButtonWidget.builder(power.displayName(), button -> {
				ClientPlayNetworking.send(new PowerActionPayload(1, power.ordinal()));
				close();
			}).dimensions(x, y, 150, 20).build());
			y += 24;
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 100, 0xFFFFFF);
	}
}
