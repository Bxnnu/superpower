package com.example.superpowers;

import com.example.superpowers.network.PowerActionPayload;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PowerManager {
	private static final Map<UUID, Integer> readyAt = new HashMap<>();
	private static final Map<UUID, Float> maxFall = new HashMap<>();

	private record Passive(RegistryEntry<StatusEffect> effect, int amplifier) {}

	// ---------------------------------------------------------------- packets

	public static void handle(ServerPlayerEntity player, PowerActionPayload payload) {
		switch (payload.action()) {
			case 0 -> useAbility(player);
			case 1 -> setPower(player, Power.byOrdinal(payload.value()));
			default -> {}
		}
	}

	// ---------------------------------------------------------------- choosing

	public static Power getPower(ServerPlayerEntity player) {
		return PowerState.get(player.getServer()).get(player.getUuid());
	}

	public static void setPower(ServerPlayerEntity player, Power power) {
		PowerState state = PowerState.get(player.getServer());
		Power old = state.get(player.getUuid());
		state.set(player.getUuid(), power);

		for (Passive p : passives(old)) player.removeStatusEffect(p.effect());
		if (old == Power.FLIGHT) setFlight(player, false);
		maxFall.remove(player.getUuid());

		refresh(player);
		player.sendMessage(Text.literal("Power: ").append(power.displayName()), true);
	}

	/** Re-applies passive effects. Called on join, respawn, and every second. */
	public static void refresh(ServerPlayerEntity player) {
		Power power = getPower(player);
		for (Passive p : passives(power)) {
			player.addStatusEffect(new StatusEffectInstance(p.effect(), 60, p.amplifier(), true, false, true));
		}
		if (power == Power.FLIGHT) setFlight(player, true);
	}

	private static List<Passive> passives(Power power) {
		return switch (power) {
			case SPEED -> List.of(new Passive(StatusEffects.SPEED, 7), new Passive(StatusEffects.JUMP_BOOST, 1));
			case JUMP -> List.of(new Passive(StatusEffects.JUMP_BOOST, 8));
			case FIREBALL -> List.of(new Passive(StatusEffects.FIRE_RESISTANCE, 0));
			case STRENGTH -> List.of(new Passive(StatusEffects.STRENGTH, 4),
					new Passive(StatusEffects.RESISTANCE, 1),
					new Passive(StatusEffects.JUMP_BOOST, 3));
			default -> List.of();
		};
	}

	private static void setFlight(ServerPlayerEntity player, boolean on) {
		if (player.isCreative() || player.isSpectator()) return;
		PlayerAbilities abilities = player.getAbilities();
		if (on && !abilities.allowFlying) {
			abilities.allowFlying = true;
			abilities.setFlySpeed(0.12f);
			player.sendAbilitiesUpdate();
		} else if (!on && abilities.allowFlying) {
			abilities.allowFlying = false;
			abilities.flying = false;
			abilities.setFlySpeed(0.05f);
			player.sendAbilitiesUpdate();
		}
	}

	// ---------------------------------------------------------------- per tick

	public static void tick(MinecraftServer server) {
		PowerState state = PowerState.get(server);
		int now = server.getTicks();

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			Power power = state.get(player.getUuid());
			if (power == Power.NONE) continue;

			if (now % 20 == 0) refresh(player);

			switch (power) {
				case SPEED -> {
					if (player.isSprinting()) {
						player.getServerWorld().spawnParticles(ParticleTypes.ELECTRIC_SPARK,
								player.getX(), player.getY() + 0.3, player.getZ(), 4, 0.25, 0.3, 0.25, 0.1);
					}
				}
				case JUMP -> {
					// sneak in the air to glide down gently
					if (player.isSneaking() && !player.isOnGround() && player.getVelocity().y < 0) {
						player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 10, 0, true, false, false));
					}
				}
				case STRENGTH -> trackSlam(player);
				default -> {}
			}
		}
	}

	private static void trackSlam(ServerPlayerEntity player) {
		UUID id = player.getUuid();
		float prev = maxFall.getOrDefault(id, 0f);
		if (player.isOnGround()) {
			if (prev >= 4f) slam(player);
			maxFall.put(id, 0f);
		} else {
			float fall = (float) player.fallDistance;
			if (fall > prev) maxFall.put(id, fall);
		}
	}

	private static void slam(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		double x = player.getX(), y = player.getY(), z = player.getZ();
		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
		world.spawnParticles(ParticleTypes.POOF, x, y + 0.1, z, 60, 2.5, 0.1, 2.5, 0.15);
		world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 0.7f);

		Box area = player.getBoundingBox().expand(6);
		for (Entity target : world.getOtherEntities(player, area, e -> e instanceof LivingEntity && e.isAlive())) {
			target.damage(world.getDamageSources().playerAttack(player), 8f);
			target.addVelocity(0, 0.8, 0);
			target.velocityModified = true;
		}
	}

	// ---------------------------------------------------------------- abilities

	public static void useAbility(ServerPlayerEntity player) {
		Power power = getPower(player);
		if (power != Power.LASER && power != Power.FIREBALL) {
			player.sendMessage(Text.literal("Your power is passive - no ability to fire."), true);
			return;
		}
		int now = player.getServer().getTicks();
		if (now < readyAt.getOrDefault(player.getUuid(), 0)) return;

		if (power == Power.LASER) {
			readyAt.put(player.getUuid(), now + 8);
			laser(player);
		} else {
			readyAt.put(player.getUuid(), now + 25);
			fireball(player);
		}
	}

	private static void laser(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		Vec3d start = player.getEyePos();
		Vec3d dir = player.getRotationVec(1.0f);
		Vec3d end = start.add(dir.multiply(40));

		BlockHitResult blockHit = world.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
		if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getPos();

		Box box = new Box(start, end).expand(1.0);
		for (Entity target : world.getOtherEntities(player, box, e -> e instanceof LivingEntity && e.isAlive())) {
			Optional<Vec3d> hit = target.getBoundingBox().expand(0.3).raycast(start, end);
			if (hit.isPresent()) {
				target.damage(world.getDamageSources().playerAttack(player), 6f);
				target.setOnFireFor(4);
			}
		}

		double length = start.distanceTo(end);
		for (double d = 0.6; d < length; d += 0.4) {
			Vec3d p = start.add(dir.multiply(d));
			world.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 1f, 2f);
	}

	private static void fireball(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		Vec3d start = player.getEyePos();
		Vec3d dir = player.getRotationVec(1.0f);
		Vec3d far = start.add(dir.multiply(48));

		BlockHitResult blockHit = world.raycast(new RaycastContext(start, far,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
		Vec3d limit = blockHit.getType() != HitResult.Type.MISS ? blockHit.getPos() : far;
		Vec3d impact = limit;

		// stop at the first creature in the way
		double best = start.squaredDistanceTo(limit);
		for (Entity target : world.getOtherEntities(player, new Box(start, limit).expand(1.0),
				e -> e instanceof LivingEntity && e.isAlive())) {
			Optional<Vec3d> hit = target.getBoundingBox().expand(0.3).raycast(start, limit);
			if (hit.isPresent()) {
				double dist = start.squaredDistanceTo(hit.get());
				if (dist < best) {
					best = dist;
					impact = hit.get();
				}
			}
		}

		// fiery trail
		double length = start.distanceTo(impact);
		for (double d = 0.8; d < length; d += 0.8) {
			Vec3d p = start.add(dir.multiply(d));
			world.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 3, 0.05, 0.05, 0.05, 0.01);
			world.spawnParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.01);
		}
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1f, 0.8f);

		// explosion at the impact point
		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
		world.spawnParticles(ParticleTypes.FLAME, impact.x, impact.y, impact.z, 80, 1.0, 0.8, 1.0, 0.1);
		world.playSound(null, impact.x, impact.y, impact.z,
				SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2f, 1f);

		for (Entity target : world.getOtherEntities(player, new Box(impact, impact).expand(5),
				e -> e instanceof LivingEntity && e.isAlive())) {
			if (target.getPos().distanceTo(impact) <= 5) {
				target.damage(world.getDamageSources().playerAttack(player), 10f);
				target.setOnFireFor(5);
			}
		}

		if (Superpowers.FIREBALL_BREAKS_BLOCKS) {
			world.createExplosion(null, impact.x, impact.y, impact.z, 3.0f, false, World.ExplosionSourceType.TNT);
		}
	}
}
