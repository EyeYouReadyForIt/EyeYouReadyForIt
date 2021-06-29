package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class CommandManager {
	private static final String PREFIX = "e.";
	private final CommandDispatcher<MessageCreateEvent> dispatcher;
	private final Set<AbstractCommand> commandList;
	private final HashMap<String, AbstractCommand> literal2CommandMap;
	private boolean init = false;

	public CommandManager() {
		this.literal2CommandMap = new HashMap<>();
		this.commandList = new HashSet<>();
		this.dispatcher = new CommandDispatcher<>();
	}

	public void init() {
		if (this.init) {
			return;
		}
		this.init = true;
		this.register(EyeCommand.create(this.dispatcher));
		this.dispatcher.register(
				literal("help")
						.executes(ctx -> {
							ctx.getSource().getMessage().getChannel().flatMap(channel -> channel.createEmbed(spec -> {
								spec.setColor(Color.DISCORD_WHITE);
								spec.setTitle("Commands");
								this.commandList.forEach(command -> {
									if (command.hasDescription()) spec.addField(command.getLiteral(), command.getUsage(ctx), true);
								});
								CommandManager.appendFooter(spec);
							})).subscribe();
							return Command.SINGLE_SUCCESS;
						})
						.then(
								argument("command", StringArgumentType.string())
										.executes(ctx -> {
											String commandStr = ctx.getArgument("command", String.class);
											AbstractCommand command = Optional.ofNullable(this.literal2CommandMap.get(commandStr)).orElseThrow(() -> CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create());
											if (!command.hasDescription()) {
												return Command.SINGLE_SUCCESS;
											}
											ctx.getSource().getMessage().getChannel().flatMap(channel -> channel.createEmbed(spec -> {
												spec.setColor(Color.CINNABAR);
												spec.setTitle(commandStr);
												spec.addField("Usage", "`" + command.getUsage(ctx) + "`", false);
												spec.addField("Description", command.getDescription(), false);
												CommandManager.appendFooter(spec);
											})).subscribe();
											return Command.SINGLE_SUCCESS;
										})
						)
		);
		Collection<String> commands = this.commandList.stream().map(AbstractCommand::getLiteral).collect(Collectors.toSet());
		commands.add("help");
		App.getClient()
				.on(MessageCreateEvent.class)
				.filter(event -> event.getMember().isPresent() && !event.getMember().get().isBot())
				.filter(event -> event.getGuildId().isPresent())
				.filter(event -> event.getMessage().getContent().startsWith(PREFIX)
						&& commands.contains(event.getMessage()
						.getContent()
						.substring(PREFIX.length())
						.split(" ")[0]))
				.subscribe(event -> {
					try {
						this.dispatcher.execute(event.getMessage().getContent().substring(1), event);
					} catch (CommandSyntaxException e) {
						error(event, e);
					}
				});
	}

	public void register(AbstractCommand abstractCommand) {
		this.commandList.add(abstractCommand);
		this.literal2CommandMap.put(abstractCommand.getLiteral(), abstractCommand);
		this.dispatcher.getRoot().addChild(abstractCommand.getNode());
	}

	public CommandDispatcher<MessageCreateEvent> getDispatcher() {
		return this.dispatcher;
	}

	public static LiteralArgumentBuilder<MessageCreateEvent> literal(String name) {
		return LiteralArgumentBuilder.literal(name);
	}

	public static <T> RequiredArgumentBuilder<MessageCreateEvent, T> argument(String name, ArgumentType<T> argumentType) {
		return RequiredArgumentBuilder.argument(name, argumentType);
	}

	public static void appendFooter(EmbedCreateSpec spec) {
		spec.setTimestamp(Instant.now());
	}

	public static void error(MessageCreateEvent event, CommandSyntaxException e) {
		event.getMessage().getChannel().flatMap(channel -> channel.createEmbed(spec -> {
			spec.setTitle("Error parsing command: ")
					.setColor(Color.RED)
					.setDescription(e.getMessage());
			appendFooter(spec);
		})).subscribe();
	}

	public static final SimpleCommandExceptionType NO_PERM = new SimpleCommandExceptionType(() -> "You do not have permission to run this command!");
}
