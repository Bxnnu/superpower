package com.example.superpowers;

import com.example.superpowers.network.PowerActionPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Superpowers implements ModInitializer {
	public static final String MOD_ID = "superpowers";

	/** Set to true to let the fireball destroy blocks like TNT. */
	public static final boolean FIREBALL_BREAKS_BLOCKS = false;

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(PowerActionPayload.ID, PowerActionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(PowerActionPayload.ID,
				(payload, context) -> PowerManager.handle(context.player(), payload));

		ServerTickEvents.END_SERVER_TICK.register(PowerManager::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PowerManager.refresh(handler.player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> PowerManager.refresh(newPlayer));
		CommandRegistrationCallback.EVENT.register(PowerCommand::register);
	}
}
