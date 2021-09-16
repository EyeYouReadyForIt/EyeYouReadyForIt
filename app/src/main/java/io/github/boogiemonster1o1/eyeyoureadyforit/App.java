package io.github.boogiemonster1o1.eyeyoureadyforit;

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
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.RestClient;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.StatsCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.TourneyCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.stats.StatisticsManager;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class App {

    public static final Logger LOGGER = LoggerFactory.getLogger("Eye You Ready For It");
    public static final NumberFormat FORMATTER = NumberFormat.getInstance(Locale.US);
    private static GatewayDiscordClient CLIENT;
    public static final Button HINT_BUTTON = Button.success("hint_button", ReactionEmoji.unicode("💡"), "Hint");
    public static final Button RESET_BUTTON = Button.secondary("reset_button", ReactionEmoji.unicode("🚫"), "Reset");

    private static final String TOKEN = Optional.ofNullable(System.getProperty("eyrfi.token")).orElse(Optional.ofNullable(System.getenv("EYRFI_TOKEN")).orElseThrow(() -> new RuntimeException("Missing token")));

    private static Set<Snowflake> currentGuilds = new HashSet<>();

    public static void main(String[] args) {
        LOGGER.info("Starting Eye You Ready For It");
        LOGGER.info("Using token {}", TOKEN);
        EyeEntry.reload();
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
        if (args.length >= 1 && args[0].equals("reg")) {
            registerCommands(restClient, applicationId)
					.then(Mono.fromRunnable(() -> LOGGER.info("Registered commands!")))
					.subscribe();
        }

        CLIENT.getEventDispatcher()
                .on(GuildCreateEvent.class)
                .filter(event -> !currentGuilds.contains(event.getGuild().getId()))
                .subscribe(event -> {
                    LOGGER.info("New Guild {} added", event.getGuild().getId());
                    StatisticsManager.initDb(event.getGuild().getId())
                            .then(Mono.fromRunnable(() -> LOGGER.info("Guild Data Table created for {}", event.getGuild().getId())))
                            .subscribe();
                });

        CLIENT.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .filter(event -> event.getGuildId().isPresent() && event.getMember().map(member -> !member.isBot()).orElse(false))
                .filter(event -> event.getMessage().getMessageReference().flatMap(MessageReference::getMessageId).map(f -> f.equals(GuildSpecificData.get(event.getMessage().getGuildId().orElseThrow()).getChannel(event.getMessage().getChannelId()).getMessageId())).orElse(false))
                .subscribe(event -> {
                    String content = event.getMessage().getContent().toLowerCase();
                    GuildSpecificData guildData = GuildSpecificData.get(event.getGuildId().orElseThrow());
                    ChannelSpecificData data = guildData.getChannel(event.getMessage().getChannelId());
                    EyeEntry current = data.getCurrent();
                    Snowflake messageId = data.getMessageId();
                    if (current.getName().equalsIgnoreCase(content) || current.getAliases().contains(content) || isFirstName(content, current.getName(), data)) {
                    	event.getMessage().getChannel().flatMap(channel ->
                    		channel.createMessage(MessageCreateSpec
									.builder()
									.addEmbed(EmbedCreateSpec
											.builder()
											.title("Correct!")
											.description(current.getName())
											.color(Color.GREEN)
											.timestamp(Instant.now())
											.build())
									.messageReference(event.getMessage().getId())
									.build()
							)
						).subscribe();

                        event.getMessage().getChannel().flatMap(channel -> channel.getMessageById(messageId))
                                .flatMap(message -> message.edit(MessageEditSpec.builder().components(new ArrayList<>()).build()))
                                .subscribe();
                        data.reset();
                        if (data.isTourney()) {
                            data.getTourneyStatisticsTracker().addCorrect(event.getMember().get().getId());
                            TourneyCommand.next(data, event.getMessage().getAuthor().map(User::getId).orElseThrow().asLong(), event.getMessage().getChannel(), false);
                        }
                    } else {
						event.getMessage().getChannel().flatMap(channel ->
								channel.createMessage(MessageCreateSpec
										.builder()
										.addEmbed(EmbedCreateSpec
												.builder()
												.title("Incorrect!")
												.description(current.getName())
												.color(Color.RED)
												.timestamp(Instant.now())
												.build())
										.messageReference(event.getMessage().getId())
										.build()
								)
						).subscribe();
                        if (data.isTourney()) {
                            data.getTourneyStatisticsTracker().addWrong(event.getMember().get().getId());
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
                .filter(event -> event.getMessageId().equals(GuildSpecificData.get(event.getGuildId().orElseThrow()).getChannel(event.getChannelId()).getMessageId()))
                .subscribe(event -> GuildSpecificData.get(event.getGuildId().orElseThrow()).getChannel(event.getChannelId()).reset());

        CLIENT.getEventDispatcher()
                .on(MessageCreateEvent.class)
                .filter(event -> event.getMember().isPresent() && event.getGuildId().get().asLong() == 859274373084479508L)
                .filter(event -> event.getMessage().getContent().startsWith("!eyeyoureadyforit "))
                .subscribe(event -> {
                    if (event.getMessage().getContent().equals("!eyeyoureadyforit reload")) {
                        event.getMessage().getChannel().flatMap(channel -> channel.createMessage("**Reloading data...**")).subscribe();
                        EyeEntry.reload();
                    } else if (event.getMessage().getContent().equals("!eyeyoureadyforit shutdown")) {
                        CLIENT.logout().block();
                    } else if (event.getMessage().getContent().startsWith("!eyeyoureadyforit db")) {
                        StatisticsManager.initDb(Snowflake.of(event.getMessage().getContent().split(" ")[2]))
                                .then(Mono.fromRunnable(() -> LOGGER.info("Guild Data Table created for {}", event.getMessage().getContent().split(" ")[2])))
                                .subscribe();
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
                ChannelSpecificData csd = gsd.getChannel(event.getInteraction().getChannelId());

                switch (name) {
                    case "eyes":
                        if (csd.getMessageId() != null && csd.getCurrent() != null) {
                            return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**There is already a context**"));
                        }
                        if (csd.isTourney()) {
                            return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**You can not use this command during a tourney**"));
                        }

                        EyeEntry entry = EyeEntry.getRandom();
                        return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(getEyesRequest(entry))).map(data -> {
                            synchronized (GuildSpecificData.LOCK) {
                                csd.setCurrent(entry);
                                csd.setMessageId(Snowflake.of(data.id()));
                            }
                            return data;
                        });
                    case "hint":
                        if (csd.isTourney() && csd.getTourneyData().shouldDisableHints()) {
                            return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**Hints are disabled for this tourney**"));
                        }
                        if (csd.isTourney())
                            csd.getTourneyStatisticsTracker().addHint(event.getInteraction().getUser().getId());
                        return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage(MultipartRequest.ofRequest(WebhookExecuteRequest.builder().content(getHintContent(event)).build())));
                    case "reset":
                        if (csd.isTourney()) {
                            return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**You can not use this command in a tourney**"));
                        }
                        return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(MultipartRequest.ofRequest(WebhookExecuteRequest.builder().addEmbed(addResetFooter(EmbedCreateSpec.builder(), event).asRequest()).build())));
                    case "tourney":
                        return TourneyCommand.handle(event, csd);
                    case "stats":
                        return StatsCommand.handle(event);
                }

                return Mono.empty();
            }

            @Override
            public Publisher<?> onButtonInteract(ButtonInteractEvent event) {
                if (event.getCustomId().equals("hint_button")) {
                    if (GuildSpecificData.get(event.getInteraction().getGuildId().get()).getChannel(event.getInteraction().getChannelId()).isTourney()) {
                    	GuildSpecificData.get(event.getInteraction().getGuildId().get())
								.getChannel(event.getInteraction().getChannelId())
								.getTourneyStatisticsTracker()
								.addHint(event.getInteraction().getUser().getId());
                    }

                    return event.reply(InteractionApplicationCommandCallbackSpec
							.builder()
							.ephemeral(true)
							.content(getHintContent(event))
							.build()
					);

                } else if (event.getCustomId().equals("reset_button")) {
                	return event.reply(InteractionApplicationCommandCallbackSpec
							.builder()
							.addEmbed(addResetFooter(EmbedCreateSpec.builder(), event))
							.build()
					);
                }

                return Mono.empty();
            }
        }).blockLast();
        CLIENT.onDisconnect().block();
    }

    private static boolean isFirstName(String allegedFirst, String name, ChannelSpecificData data) {
        if (data.isTourney() && data.getTourneyData().shouldDisableFirstNames()) {
            return false;
        } else if (name.indexOf(' ') == -1) {
            return false;
        }

        String sub = allegedFirst.substring(0, name.indexOf(' '));

        return sub.equalsIgnoreCase(allegedFirst);
    }

    public static MultipartRequest<WebhookExecuteRequest> getEyesRequest(EyeEntry entry) {
        return MultipartRequest.ofRequest(
        		WebhookExecuteRequest
						.builder()
						.addEmbed(createEyesEmbed(entry).asRequest())
						.addComponent(ActionRow.of(HINT_BUTTON, RESET_BUTTON).getData())
						.build()
		);
    }

    private static String getHintContent(InteractionCreateEvent event) {
        ChannelSpecificData data = GuildSpecificData.get(event.getInteraction().getGuildId().orElseThrow()).getChannel(event.getInteraction().getChannelId());

        if (data.getMessageId() != null && data.getCurrent() != null) {
            return data.getCurrent().getHint();
        }

        return "**There is no context available**";
    }

    private static EmbedCreateSpec addResetFooter(EmbedCreateSpec.Builder eSpec, InteractionCreateEvent event) {
        ChannelSpecificData data = GuildSpecificData.get(event.getInteraction().getGuildId().orElseThrow()).getChannel(event.getInteraction().getChannelId());
        if (data.getMessageId() != null && data.getCurrent() != null) {
            if (!data.getCurrent().getAliases().isEmpty()) {
                eSpec.description("Aliases: " + data.getCurrent().getAliases());
            }
            eSpec.title("The person was **" + data.getCurrent().getName() + "**");
            event.getInteraction().getChannel()
                    .flatMap(channel -> channel.getMessageById(data.getMessageId()))
                    .flatMap(message -> message.edit(MessageEditSpec.builder().components(new ArrayList<>()).build()))
                    .subscribe();
        } else {
            eSpec.title("Reset");
            eSpec.description("But there was no context :p");
        }
        eSpec.timestamp(Instant.now());
        eSpec.color(Color.RED);
        eSpec.addField("Run by", event.getInteraction().getUser().getMention(), false);
        data.reset();
        return eSpec.build();
    }

    public static EmbedCreateSpec createEyesEmbed(EyeEntry entry) {
    	return EmbedCreateSpec
				.builder()
        		.image(entry.getImageUrl())
        		.title("Guess the Person")
      			.description("Reply to this message with the answer")
				.timestamp(Instant.now())
				.build();
    }

    private static Mono registerCommands(RestClient restClient, long applicationId) {
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

    public static GatewayDiscordClient getClient() {
        return CLIENT;
    }
}
