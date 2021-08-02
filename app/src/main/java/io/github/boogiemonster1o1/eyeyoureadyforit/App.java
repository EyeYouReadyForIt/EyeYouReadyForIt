package io.github.boogiemonster1o1.eyeyoureadyforit;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
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
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.RestClient;
import discord4j.rest.util.Color;
import discord4j.rest.util.WebhookMultipartRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class App {
	public static final Logger LOGGER = LoggerFactory.getLogger("Eye You Ready For It");
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	private static final CommandManager COMMAND_MANAGER = new CommandManager();
	private static GatewayDiscordClient CLIENT;
	private static final Map<Snowflake, GuildSpecificData> GUILD_SPECIFIC_DATA_MAP = new HashMap<>();
	private static final Button HINT_BUTTON = Button.success("hint_button", ReactionEmoji.unicode("\uD83D\uDCA1"), "Hint");
	private static final Button RESET_BUTTON = Button.secondary("reset_button", ReactionEmoji.unicode("\uD83D\uDEAB"), "Reset");

	public static void main(String[] args) {
		String token = args[0];
		LOGGER.info("Starting Eye You Ready For It");
		LOGGER.info("Using token {}", token);
		DiscordClient discordClient = DiscordClientBuilder.create(token).build();
		CLIENT = discordClient.login()
				.blockOptional()
				.orElseThrow();
		CLIENT.getEventDispatcher()
				.on(ReadyEvent.class)
				.subscribe(event -> {
					EyeEntry.reload();
					LOGGER.info("Logged in as [{}#{}]", event.getData().user().username(), event.getData().user().discriminator());
					LOGGER.info("Guilds: {}", event.getGuilds().size());
					LOGGER.info("Gateway version: {}", event.getGatewayVersion());
					LOGGER.info("Session ID: {}", event.getSessionId());
					LOGGER.info("Shard Info: Index {}, Count {}", event.getShardInfo().getIndex(), event.getShardInfo().getCount());
				});

		RestClient restClient = CLIENT.getRestClient();
		long applicationId = restClient.getApplicationId().block();
		if (args.length >= 2 && args[1].equals("reg")) {
			registerCommands(restClient, applicationId);
		}

		CLIENT.getEventDispatcher()
				.on(MessageCreateEvent.class)
				.filter(event -> event.getGuildId().isPresent() && event.getMember().map(member -> !member.isBot()).orElse(false))
				.filter(event -> event.getMessage().getMessageReference().flatMap(MessageReference::getMessageId).map(f -> f.equals(App.getGuildSpecificData(event.getMessage().getGuildId().orElseThrow()).getMessageId())).orElse(false))
				.subscribe(event -> {
					String content = event.getMessage().getContent().toLowerCase();
					GuildSpecificData data = App.getGuildSpecificData(event.getMessage().getGuildId().orElseThrow());
					EyeEntry current = data.getCurrent();
					if (current.getName().equalsIgnoreCase(content) || current.getAliases().contains(content)) {
						event.getMessage().getChannel().flatMap(channel -> channel.createMessage(mspec -> {
							mspec.setEmbed(spec -> {
								spec.setTitle("Correct!");
								spec.setDescription(current.getName());
								spec.setColor(Color.GREEN);
								CommandManager.appendFooter(spec);
							});
							mspec.setMessageReference(event.getMessage().getId());
						})).subscribe();
						data.reset();
					} else {
						event.getMessage().getChannel().flatMap(channel -> channel.createMessage(mspec -> {
							mspec.setEmbed(spec -> {
								spec.setTitle("Incorrect!");
								spec.setColor(Color.RED);
								CommandManager.appendFooter(spec);
							});
							mspec.setMessageReference(event.getMessage().getId());
						})).subscribe();
					}
				});

		CLIENT.getEventDispatcher()
				.on(MessageDeleteEvent.class)
				.filterWhen(
						event -> Mono.justOrEmpty(event.getMessage().flatMap(Message::getAuthor))
								.map(user -> user.getId().equals(getClient().getSelfId()))
				)
				.filter(event -> event.getGuildId().isPresent())
				.filter(event -> event.getMessageId().equals(getGuildSpecificData(event.getGuildId().orElseThrow()).getMessageId()))
				.subscribe(event -> getGuildSpecificData(event.getGuildId().orElseThrow()).reset());

		CLIENT.on(new ReactiveEventAdapter() {
			@Override
			public Publisher<?> onSlashCommand(SlashCommandEvent event) {
				if (event.getInteraction().getGuildId().isEmpty()) {
					return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage("You can only run this command in a guild"));
				}

				String name = event.getCommandName();

				switch (name) {
					case "eyes":
						EyeEntry entry = EyeEntry.getRandom();
						return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(new WebhookMultipartRequest(WebhookExecuteRequest.builder().addEmbed(createEyesEmbed(entry, new EmbedCreateSpec()).asRequest()).addComponent(ActionRow.of(HINT_BUTTON, RESET_BUTTON).getData()).build()))).map(data -> {
							GuildSpecificData gsd = App.getGuildSpecificData(event.getInteraction().getGuildId().orElseThrow());
							synchronized (GuildSpecificData.LOCK) {
								gsd.setCurrent(entry);
								gsd.setMessageId(Snowflake.of(data.id()));
							}
							return data;
						});
					case "hint":
						return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage(new WebhookMultipartRequest(WebhookExecuteRequest.builder().content(getHintContent(event)).build())));
					case "reset":
						return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(new WebhookMultipartRequest(WebhookExecuteRequest.builder().addEmbed(addResetFooter(new EmbedCreateSpec(), event).asRequest()).build())));
				}

				return Mono.empty();
			}

			@Override
			public Publisher<?> onButtonInteract(ButtonInteractEvent event) {
				if (event.getCustomId().equals("hint_button")) {
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

	private static String getHintContent(InteractionCreateEvent event) {
		GuildSpecificData data = App.getGuildSpecificData(event.getInteraction().getGuildId().orElseThrow());

		if (data.getMessageId() != null && data.getCurrent() != null) {
			return data.getCurrent().getHint();
		}

		return "**There is no context available**";
	}

	private static EmbedCreateSpec addResetFooter(EmbedCreateSpec eSpec, InteractionCreateEvent event) {
		GuildSpecificData data = App.getGuildSpecificData(event.getInteraction().getGuildId().orElseThrow());
		if (data.getMessageId() != null && data.getCurrent() != null) {
			if (!data.getCurrent().getAliases().isEmpty()) {
				eSpec.setDescription("Aliases: " + data.getCurrent().getAliases());
			}
			eSpec.setTitle("The person was **" + data.getCurrent().getName() + "**");
		} else {
			eSpec.setTitle("Reset");
			eSpec.setDescription("But there was no context :p");
		}
		CommandManager.appendFooter(eSpec);
		eSpec.setColor(Color.RED);
		eSpec.addField("Run by", event.getInteraction().getUser().getMention(), false);
		data.reset();
		return eSpec;
	}

	private static EmbedCreateSpec createEyesEmbed(EyeEntry entry, EmbedCreateSpec spec) {
		spec.setImage(entry.getImageUrl());
		spec.setTitle("Guess the Person");
		spec.setDescription("Reply to this message with the answer");
		CommandManager.appendFooter(spec);
		return spec;
	}

	private static void registerCommands(RestClient restClient, long applicationId) {
		restClient.getApplicationService()
				.createGuildApplicationCommand(
						applicationId,
						859274373084479508L,
						ApplicationCommandRequest.builder()
								.name("eyes")
								.description("Shows a pair of eyes")
								.build()
				)
				.doOnError(Throwable::printStackTrace)
				.onErrorResume(e -> Mono.empty())
				.block();
		restClient.getApplicationService()
				.createGuildApplicationCommand(
						applicationId,
						859274373084479508L,
						ApplicationCommandRequest.builder()
								.name("hint")
								.description("Shows a hint")
								.build()
				)
				.doOnError(Throwable::printStackTrace)
				.onErrorResume(e -> Mono.empty())
				.block();
		restClient.getApplicationService()
				.createGuildApplicationCommand(
						applicationId,
						859274373084479508L,
						ApplicationCommandRequest.builder()
								.name("reset")
								.description("Resets")
								.build()
				)
				.doOnError(Throwable::printStackTrace)
				.onErrorResume(e -> Mono.empty())
				.block();
	}

	public static CommandManager getCommandManager() {
		return COMMAND_MANAGER;
	}

	public static GatewayDiscordClient getClient() {
		return CLIENT;
	}

	public static GuildSpecificData getGuildSpecificData(Snowflake guildId) {
		return GUILD_SPECIFIC_DATA_MAP.computeIfAbsent(guildId, GuildSpecificData::new);
	}
}
