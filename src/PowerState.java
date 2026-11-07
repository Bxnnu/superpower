package com.example.superpowers;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Saves each player's chosen power with the world. */
public class PowerState extends PersistentState {
	private static final PersistentState.Type<PowerState> TYPE =
			new PersistentState.Type<>(PowerState::new, PowerState::fromNbt, null);

	private final Map<UUID, Integer> powers = new HashMap<>();

	public static PowerState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, Superpowers.MOD_ID);
	}

	public Power get(UUID player) {
		return Power.byOrdinal(powers.getOrDefault(player, 0));
	}

	public void set(UUID player, Power power) {
		powers.put(player, power.ordinal());
		markDirty();
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		NbtCompound map = new NbtCompound();
		powers.forEach((id, value) -> map.putInt(id.toString(), value));
		nbt.put("powers", map);
		return nbt;
	}

	private static PowerState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
		PowerState state = new PowerState();
		NbtCompound map = nbt.getCompound("powers");
		for (String key : map.getKeys()) {
			state.powers.put(UUID.fromString(key), map.getInt(key));
		}
		return state;
	}
}
