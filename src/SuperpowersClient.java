package com.example.superpowers.client;

import com.example.superpowers.network.PowerActionPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SuperpowersClient implements ClientModInitializer {
	private static KeyBinding abilityKey;
	private static KeyBinding menuKey;

	@Override
	public void onInitializeClient() {
		abilityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.superpowers.ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.superpowers"));
		menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.superpowers.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.superpowers"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (abilityKey.wasPressed()) {
				if (client.player != null && client.currentScreen == null) {
					ClientPlayNetworking.send(new PowerActionPayload(0, 0));
				}
			}
			while (menuKey.wasPressed()) {
				if (client.player != null && client.currentScreen == null) {
					client.setScreen(new PowerScreen());
				}
			}
		});
	}
}
