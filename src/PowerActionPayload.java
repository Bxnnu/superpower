package com.example.superpowers.network;

import com.example.superpowers.Superpowers;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server. action 0 = use ability, action 1 = choose power (value = Power ordinal). */
public record PowerActionPayload(int action, int value) implements CustomPayload {
	public static final CustomPayload.Id<PowerActionPayload> ID =
			new CustomPayload.Id<>(Identifier.of(Superpowers.MOD_ID, "action"));

	public static final PacketCodec<RegistryByteBuf, PowerActionPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, PowerActionPayload::action,
			PacketCodecs.VAR_INT, PowerActionPayload::value,
			PowerActionPayload::new);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
