package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.utils.ErrorHelper;
import org.reactivestreams.Publisher;
import org.reflections.Reflections;
import reactor.core.publisher.Mono;

public final class CommandManager {
	private static final Map<String, CommandHandler> COMMANDS = new ConcurrentHashMap<>();
	private static final Reflections reflections = new Reflections("io.github.boogiemonster1o1.eyeyoureadyforit.command.commands");

	public static void init(GatewayDiscordClient client) {
		COMMANDS.clear();
		reflections.getSubTypesOf(CommandHandler.class)
				.forEach(aClass -> {
					try {
						CommandHandler handler = aClass.getConstructor().newInstance();
						COMMANDS.put(handler.getName(), handler);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

		client.getEventDispatcher()
				.on(ChatInputInteractionEvent.class)
				.flatMap((ChatInputInteractionEvent event1) -> accept(event1, client))
				.onErrorContinue((error, event) -> {
					error.printStackTrace();
					ErrorHelper.sendErrorEmbed(error, (ChatInputInteractionEvent) event);
				})
				.subscribe();
	}

	private static Publisher<?> accept(ChatInputInteractionEvent event, GatewayDiscordClient client) {
		if (event.getInteraction().getGuildId().isEmpty()) {
			return event.reply("You can only run this command in a guild");
		}

		CommandHandler commandHandler = COMMANDS.get(event.getCommandName());

		if (commandHandler == null) {
			return Mono.empty();
		}

		ChannelSpecificData csd = GuildSpecificData.get(event.getInteraction().getGuildId().get()).getChannel(event.getInteraction().getChannelId());

		return commandHandler.handle(event, csd, client);
	}

	public static Mono<?> registerSlashCommands(GatewayDiscordClient client) {
		App.LOGGER.info("REGISTERING COMMANDS YEE HAW");

		for (CommandHandler handler : COMMANDS.values()) {
			switch (handler.getType()) {
				case GLOBAL_COMMAND:
					client.getRestClient()
							.getApplicationService()
							.createGlobalApplicationCommand(
									client.getRestClient().getApplicationId().block(),
									handler.asRequest()
							)
							.doOnError(Throwable::printStackTrace)
							.onErrorResume(e -> Mono.empty())
							.block();
					break;

				case ADMIN_COMMAND:
					client.getRestClient()
							.getApplicationService()
							.createGuildApplicationCommand(
									client.getRestClient().getApplicationId().block(),
									859274373084479508L,
									handler.asRequest()
							)
							.doOnError(Throwable::printStackTrace)
							.onErrorResume(e -> Mono.empty())
							.block();
					break;
			}
		}

		return Mono.empty();
	}
}
