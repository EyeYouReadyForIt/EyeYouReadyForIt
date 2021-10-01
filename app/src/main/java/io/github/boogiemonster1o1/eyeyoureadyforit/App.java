package io.github.boogiemonster1o1.eyeyoureadyforit;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.object.MessageReference;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Color;
import io.github.boogiemonster1o1.eyeyoureadyforit.button.ButtonManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.commands.TourneyCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.stats.StatisticsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@SuppressWarnings("NullableProblems")
public class App {

	public static final Logger LOGGER = LoggerFactory.getLogger("Eye You Ready For It");
	public static final NumberFormat FORMATTER = NumberFormat.getInstance(Locale.US);

	public static GatewayDiscordClient CLIENT;
	private static final String TOKEN = Optional.ofNullable(System.getenv("EYRFI_TOKEN")).orElseThrow(() -> new RuntimeException("Missing token"));
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

		if (args.length >= 1 && args[0].equals("reg")) {
			CommandManager.registerSlashCommands()
					.then(Mono.fromRunnable(() -> LOGGER.info("Registered commands!")))
					.subscribe();
		}

		initCommands();

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
				.doOnError(Throwable::printStackTrace)
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
							data.getTourneyStatisticsTracker().addCorrect(event.getMember().orElseThrow().getId());
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
							data.getTourneyStatisticsTracker().addWrong(event.getMember().orElseThrow().getId());
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
				.filter(event -> event.getMember().isPresent() && event.getGuildId().isPresent() && event.getGuildId().get().asLong() == 859274373084479508L)
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

		CommandManager.init();
		ButtonManager.init();
		CLIENT.onDisconnect().block();
	}

	private static void initCommands() {
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

	public static GatewayDiscordClient getClient() {
		return CLIENT;
	}
}
