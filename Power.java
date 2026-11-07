package com.example.superpowers;

import net.minecraft.text.Text;

public enum Power {
	NONE("none", "No Power"),
	SPEED("speed", "Super Speed"),
	JUMP("jump", "Super Jump"),
	FLIGHT("flight", "Flight"),
	LASER("laser", "Laser Eyes"),
	FIREBALL("fireball", "Fireball"),
	STRENGTH("strength", "Super Strength");

	public final String id;
	public final String label;

	Power(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public Text displayName() {
		return Text.literal(label);
	}

	public static Power byOrdinal(int ordinal) {
		Power[] all = values();
		return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
	}

	public static Power byId(String id) {
		for (Power p : values()) {
			if (p.id.equalsIgnoreCase(id)) return p;
		}
		return null;
	}
}
