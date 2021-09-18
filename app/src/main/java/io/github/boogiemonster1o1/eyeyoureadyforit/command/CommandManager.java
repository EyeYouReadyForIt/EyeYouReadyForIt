package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.util.ApplicationCommandOptionType;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@SuppressWarnings("NullableProblems")
public final class CommandManager {
	private static final Map<String, Command> COMMAND_MAP = new ConcurrentHashMap<>();

	public static void init() {
		register("tourney", TourneyCommand::handle);
		register("stats", ((event, csd) -> StatsCommand.handle(event)));
		register("reset", ResetCommand::handle);
		register("hint", HintCommand::handle);
		register("eyes", EyesCommand::handle);

		App.CLIENT.on(new ReactiveEventAdapter() {
			@Override
			public Publisher<?> onSlashCommand(SlashCommandEvent event) {
				return accept(event);
			}
		}).blockLast();
	}

	private static void register(String name, Command handler) {
		COMMAND_MAP.put(name, handler);
	}

	public static Publisher<?> accept(SlashCommandEvent event) {
		if (event.getInteraction().getGuildId().isEmpty()) {
			return event.reply("You can only run this command in a guild");
		}

		Command command = COMMAND_MAP.get(event.getCommandName());
		GuildSpecificData gsd = GuildSpecificData.get(event.getInteraction().getGuildId().orElseThrow());
		ChannelSpecificData csd = gsd.getChannel(event.getInteraction().getChannelId());
		if (command == null) {
			return Mono.empty();
		}

		return command.handle(event, csd);
	}

	public static Mono<?> registerSlashCommands(RestClient restClient, long applicationId) {
		App.LOGGER.info("REGISTERING COMMANDS YEE HAW");

		restClient.getApplicationService()
				.createGlobalApplicationCommand(
						applicationId,
						ApplicationCommandRequest.builder()
								.name("eyes")
								.description("Shows a pair of eyes")
								.build()
				)
				.doOnError(Throwable::printStackTrace)
				.onErrorResume(e -> Mono.empty())
				.block();

		restClient.getApplicationService()
				.createGlobalApplicationCommand(
						applicationId,
						ApplicationCommandRequest.builder()
								.name("hint")
								.description("Shows a hint")
								.build()
				)
				.doOnError(Throwable::printStackTrace)
				.onErrorResume(e -> Mono.empty())
				.block();

		restClient.getApplicationService()
				.createGlobalApplicationCommand(
						applicationId,
						ApplicationCommandRequest.builder()
								.name("reset")
								.description("Resets")
								.build()
				)
				.doOnError(Throwable::printStackTrace)
				.onErrorResume(e -> Mono.empty())
				.block();

		restClient.getApplicationService().createGlobalApplicationCommand(
				applicationId,
				ApplicationCommandRequest
						.builder()
						.name("tourney")
						.description("Starts a tourney")
						.addOption(
								ApplicationCommandOptionData
										.builder()
										.required(true)
										.type(ApplicationCommandOptionType.INTEGER.getValue())
										.name("rounds")
										.description("Number of rounds. From 5 to 10")
										.build()
						)
						.addOption(
								ApplicationCommandOptionData
										.builder()
										.required(false)
										.type(ApplicationCommandOptionType.BOOLEAN.getValue())
										.name("hintsdisabled")
										.description("Whether Hints are disabled")
										.build()
						)
						.addOption(
								ApplicationCommandOptionData
										.builder()
										.required(false)
										.type(ApplicationCommandOptionType.BOOLEAN.getValue())
										.name("firstnamesdisabled")
										.description("Whether first names are disabled")
										.build()
						)
						.build()
		).doOnError(Throwable::printStackTrace).onErrorResume(e -> Mono.empty()).block();

		restClient.getApplicationService().createGlobalApplicationCommand(
				applicationId,
				ApplicationCommandRequest
						.builder()
						.name("stats")
						.description("Looks up user and server statistics")
						.addOption(
								ApplicationCommandOptionData
										.builder()
										.type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
										.name("users")
										.description("Looks up statistics for a selected user")
										.addOption(ApplicationCommandOptionData
												.builder()
												.required(false)
												.type(ApplicationCommandOptionType.USER.getValue())
												.name("user")
												.description("User to look up, defaults to command user")
												.build()
										)
										.build()

						)
						.addOption(
								ApplicationCommandOptionData
										.builder()
										.type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
										.name("server")
										.description("Looks up statistics for the current server")
										.build()

						)
						.build())
				.doOnError(Throwable::printStackTrace)
				.onErrorResume(e -> Mono.empty())
				.block();

		return Mono.empty();
	}
}
