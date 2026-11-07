package com.example.superpowers;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;

public class PowerCommand {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
								CommandRegistryAccess registryAccess,
								CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("power")
				.executes(ctx -> {
					ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
					Power current = PowerState.get(ctx.getSource().getServer()).get(player.getUuid());
					ctx.getSource().sendFeedback(() -> Text.literal("Your power: " + current.label
							+ ". Use /power <speed|jump|flight|laser|fireball|strength|none>"), false);
					return Command.SINGLE_SUCCESS;
				})
				.then(CommandManager.argument("name", StringArgumentType.word())
						.suggests((ctx, builder) -> CommandSource.suggestMatching(
								Arrays.stream(Power.values()).map(p -> p.id), builder))
						.executes(ctx -> {
							ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
							Power power = Power.byId(StringArgumentType.getString(ctx, "name"));
							if (power == null) {
								ctx.getSource().sendError(Text.literal("Unknown power."));
								return 0;
							}
							PowerManager.setPower(player, power);
							return Command.SINGLE_SUCCESS;
						})));
	}
}
