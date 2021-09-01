package io.github.boogiemonster1o1.eyeyoureadyforit;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.object.MessageReference;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.RestClient;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import discord4j.rest.util.WebhookMultipartRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.util.StatisticsManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.util.TourneyStatisticsTracker;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.TourneyCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class App {
	public static final Logger LOGGER = LoggerFactory.getLogger("Eye You Ready For It");
	private static GatewayDiscordClient CLIENT;
	public static final Button HINT_BUTTON = Button.success("hint_button", ReactionEmoji.unicode("\uD83D\uDCA1"), "Hint");
	public static final Button DISABLED_HINT_BUTTON = Button.success("disabled_hint_button", ReactionEmoji.unicode("\uD83D\uDCA1"), "Hint").disabled();
	public static final Button RESET_BUTTON = Button.secondary("reset_button", ReactionEmoji.unicode("\uD83D\uDEAB"), "Reset");

	public static final String URL = Optional.ofNullable(System.getProperty("eyrfi.dbURL")).orElse(Optional.ofNullable(System.getenv("EYRFI_DB_URL")).orElseThrow(() -> new RuntimeException("Missing db url")));
	private static final String TOKEN = Optional.ofNullable(System.getProperty("eyrfi.token")).orElse(Optional.ofNullable(System.getenv("EYRFI_TOKEN")).orElseThrow(() -> new RuntimeException("Missing token")));
	public static final String USERNAME = Optional.ofNullable(System.getProperty("eyrfi.dbUser")).orElse(Optional.ofNullable(System.getenv("EYRFI_DB_USER")).orElseThrow(() -> new RuntimeException("Missing db username")));
	public static final String PASSWORD = Optional.ofNullable(System.getProperty("eyrfi.dbPassword")).orElse(Optional.ofNullable(System.getenv("EYRFI_DB_PASSWORD")).orElseThrow(() -> new RuntimeException("Missing db password")));

	public static Set<Snowflake> currentGuilds = new HashSet<>();

	public static void main(String[] args) {
		LOGGER.info("Starting Eye You Ready For It");
		LOGGER.info("Using token {}", TOKEN);
		EyeEntry.reload(URL, USERNAME, PASSWORD);
		DiscordClient discordClient = DiscordClientBuilder.create(TOKEN).build();
		CLIENT = discordClient.login()
				.blockOptional()
				.orElseThrow();

		CLIENT.getEventDispatcher()
				.on(ReadyEvent.class)
				.subscribe(event -> {
					LOGGER.info("Logged in as [{}#{}]", event.getData().user().username(), event.getData().user().discriminator());
					LOGGER.info("Guilds: {}", event.getGuilds().size());
					LOGGER.info("Gateway version: {}", event.getGatewayVersion());
					LOGGER.info("Session ID: {}", event.getSessionId());
					LOGGER.info("Shard Info: Index {}, Count {}", event.getShardInfo().getIndex(), event.getShardInfo().getCount());

					currentGuilds = event.getGuilds().stream().map(ReadyEvent.Guild::getId).collect(Collectors.toSet());
				});

		RestClient restClient = CLIENT.getRestClient();
		long applicationId = restClient.getApplicationId().block();
		if (args.length >= 2 && args[1].equals("reg")) {
			registerCommands(restClient, applicationId);
		}

		CLIENT.getEventDispatcher()
				.on(GuildCreateEvent.class)
				.filter(event -> !currentGuilds.contains(event.getGuild().getId()))
				.subscribe(event -> StatisticsManager.initDb(event.getGuild().getId(), App.URL, App.USERNAME, App.PASSWORD));

		CLIENT.getEventDispatcher()
				.on(MessageCreateEvent.class)
				.filter(event -> event.getGuildId().isPresent() && event.getMember().map(member -> !member.isBot()).orElse(false))
				.filter(event -> event.getMessage().getMessageReference().flatMap(MessageReference::getMessageId).map(f -> f.equals(GuildSpecificData.get(event.getMessage().getGuildId().orElseThrow()).getMessageId())).orElse(false))
				.subscribe(event -> {
					String content = event.getMessage().getContent().toLowerCase();
					GuildSpecificData data = GuildSpecificData.get(event.getMessage().getGuildId().orElseThrow());
					EyeEntry current = data.getCurrent();
					Snowflake messageId = data.getMessageId();
					if (current.getName().equalsIgnoreCase(content) || current.getAliases().contains(content) || isFirstName(content, current.getName(), data)) {
						event.getMessage().getChannel().flatMap(channel -> channel.createMessage(mspec -> {
							mspec.addEmbed(spec -> {
								spec.setTitle("Correct!");
								spec.setDescription(current.getName());
								spec.setColor(Color.GREEN);
								spec.setTimestamp(Instant.now());
							});
							mspec.setMessageReference(event.getMessage().getId());
						})).subscribe();
						event.getMessage().getChannel().flatMap(channel -> channel.getMessageById(messageId))
								.flatMap(message -> message.edit(MessageEditSpec::setComponents))
								.subscribe();
						data.reset();
						if (data.isTourney()) {
							TourneyStatisticsTracker.get(data.getGuildId()).addCorrect(event.getMember().get().getId());
							TourneyCommand.next(data, event.getMessage().getAuthor().map(User::getId).orElseThrow().asLong(), event.getMessage().getChannel(), false);
						}
					} else {
						event.getMessage().getChannel().flatMap(channel -> channel.createMessage(mspec -> {
							mspec.addEmbed(spec -> {
								spec.setTitle("Incorrect!");
								spec.setColor(Color.RED);
								spec.setTimestamp(Instant.now());
							});
							mspec.setMessageReference(event.getMessage().getId());
						})).subscribe();
						if (data.isTourney()) {
							TourneyStatisticsTracker.get(data.getGuildId()).addWrong(event.getMember().get().getId());
						}
					}
				});

		CLIENT.getEventDispatcher()
				.on(MessageDeleteEvent.class)
				.filterWhen(
						event -> Mono.justOrEmpty(event.getMessage().flatMap(Message::getAuthor))
								.map(user -> user.getId().equals(getClient().getSelfId()))
				)
				.filter(event -> event.getGuildId().isPresent())
				.filter(event -> event.getMessageId().equals(GuildSpecificData.get(event.getGuildId().orElseThrow()).getMessageId()))
				.subscribe(event -> GuildSpecificData.get(event.getGuildId().orElseThrow()).reset());

		CLIENT.getEventDispatcher()
				.on(MessageCreateEvent.class)
				.filter(event -> event.getMember().isPresent() && event.getMember().get().getId().asLong() == 699945276156280893L)
				.filter(event -> event.getMessage().getContent().startsWith("!eyeyoureadyforit "))
				.subscribe(event -> {
					if (event.getMessage().getContent().equals("!eyeyoureadyforit reload")) {
						LOGGER.info("Reloading data...");
						event.getMessage().getChannel().flatMap(channel -> channel.createMessage("**Reloading data...**")).subscribe();
						EyeEntry.reload(URL, USERNAME, PASSWORD);
					} else if (event.getMessage().getContent().equals("!eyeyoureadyforit shutdown")) {
						CLIENT.logout().block();
					}
				});

		CLIENT.on(new ReactiveEventAdapter() {
			@Override
			public Publisher<?> onSlashCommand(SlashCommandEvent event) {
				if (event.getInteraction().getGuildId().isEmpty()) {
					return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage("You can only run this command in a guild"));
				}

				String name = event.getCommandName();
				GuildSpecificData gsd = GuildSpecificData.get(event.getInteraction().getGuildId().orElseThrow());

				switch (name) {
					case "eyes":
						if (gsd.getMessageId() != null && gsd.getCurrent() != null) {
							return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**There is already a context**"));
						}
						if (gsd.isTourney()) {
							return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**You can not use this command during a tourney**"));
						}
						EyeEntry entry = EyeEntry.getRandom();
						return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(getEyesRequest(entry))).map(data -> {
							synchronized (GuildSpecificData.LOCK) {
								gsd.setCurrent(entry);
								gsd.setMessageId(Snowflake.of(data.id()));
							}
							return data;
						});
					case "hint":
						if (gsd.isTourney() && gsd.getTourneyData().shouldDisableHints()) {
							return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**Hints are disabled for this tourney**"));
						}
						if (gsd.isTourney()) TourneyStatisticsTracker.get(gsd.getGuildId()).addHint(event.getInteraction().getUser().getId());
						return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage(new WebhookMultipartRequest(WebhookExecuteRequest.builder().content(getHintContent(event)).build())));
					case "reset":
						if (gsd.isTourney()) {
							return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**You can not use this command in a tourney**"));
						}
						return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(new WebhookMultipartRequest(WebhookExecuteRequest.builder().addEmbed(addResetFooter(new EmbedCreateSpec(), event).asRequest()).build())));
					case "tourney":
						return TourneyCommand.handle(event, gsd);
				}

				return Mono.empty();
			}

			@Override
			public Publisher<?> onButtonInteract(ButtonInteractEvent event) {
				if (event.getCustomId().equals("hint_button")) {
					if(GuildSpecificData.get(event.getInteraction().getGuildId().get()).isTourney()) {
						TourneyStatisticsTracker.get(event.getInteraction().getGuildId().get()).addHint(event.getInteraction().getUser().getId());
					}
					return event.reply(spec -> {
						spec.setEphemeral(true);
						spec.setContent(getHintContent(event));
					});
				} else if (event.getCustomId().equals("reset_button")) {
					return event.reply(spec -> spec.addEmbed(eSpec -> addResetFooter(eSpec, event)));
				}

				return Mono.empty();
			}
		}).blockLast();
		CLIENT.onDisconnect().block();
	}

	private static boolean isFirstName(String allegedFirst, String name, GuildSpecificData data) {
		if (data.isTourney() && data.getTourneyData().shouldDisableFirstNames()) {
			return false;
		} else if (name.indexOf(' ') == -1) {
			return false;
		}

		String sub = allegedFirst.substring(0, name.indexOf(' '));

		return sub.equalsIgnoreCase(allegedFirst);
	}

	public static WebhookMultipartRequest getEyesRequest(EyeEntry entry) {
		return new WebhookMultipartRequest(WebhookExecuteRequest.builder().addEmbed(createEyesEmbed(entry, new EmbedCreateSpec()).asRequest()).addComponent(ActionRow.of(HINT_BUTTON, RESET_BUTTON).getData()).build());
	}

	private static String getHintContent(InteractionCreateEvent event) {
		GuildSpecificData data = GuildSpecificData.get(event.getInteraction().getGuildId().orElseThrow());

		if (data.getMessageId() != null && data.getCurrent() != null) {
			return data.getCurrent().getHint();
		}

		return "**There is no context available**";
	}

	private static EmbedCreateSpec addResetFooter(EmbedCreateSpec eSpec, InteractionCreateEvent event) {
		GuildSpecificData data = GuildSpecificData.get(event.getInteraction().getGuildId().orElseThrow());
		if (data.getMessageId() != null && data.getCurrent() != null) {
			if (!data.getCurrent().getAliases().isEmpty()) {
				eSpec.setDescription("Aliases: " + data.getCurrent().getAliases());
			}
			eSpec.setTitle("The person was **" + data.getCurrent().getName() + "**");
			event.getInteraction().getChannel()
					.flatMap(channel -> channel.getMessageById(data.getMessageId()))
					.flatMap(message -> message.edit(MessageEditSpec::setComponents))
					.subscribe();
		} else {
			eSpec.setTitle("Reset");
			eSpec.setDescription("But there was no context :p");
		}
		eSpec.setTimestamp(Instant.now());
		eSpec.setColor(Color.RED);
		eSpec.addField("Run by", event.getInteraction().getUser().getMention(), false);
		data.reset();
		return eSpec;
	}

	public static EmbedCreateSpec createEyesEmbed(EyeEntry entry, EmbedCreateSpec spec) {
		spec.setImage(entry.getImageUrl());
		spec.setTitle("Guess the Person");
		spec.setDescription("Reply to this message with the answer");
		spec.setTimestamp(Instant.now());
		return spec;
	}

	private static void registerCommands(RestClient restClient, long applicationId) {
		LOGGER.info("REGISTERING COMMANDS YEE HAW");
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
	}

	public static GatewayDiscordClient getClient() {
		return CLIENT;
	}
}
