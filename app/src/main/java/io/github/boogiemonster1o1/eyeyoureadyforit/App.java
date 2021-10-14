package io.github.boogiemonster1o1.eyeyoureadyforit;

import java.text.NumberFormat;
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
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import io.github.boogiemonster1o1.eyeyoureadyforit.button.ButtonManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.stats.StatisticsManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks.MessageHookManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class App {

	// eyes™

	public static final Logger LOGGER = LoggerFactory.getLogger("Eye You Ready For It");
	public static final NumberFormat FORMATTER = NumberFormat.getInstance(Locale.US);
	private static final String TOKEN = Optional.ofNullable(System.getenv("EYRFI_TOKEN")).orElseThrow(() -> new RuntimeException("Missing token"));
	private static GatewayDiscordClient CLIENT;
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
				.on(MessageDeleteEvent.class)
				.filter(event -> event
						.getMessage()
						.flatMap(Message::getAuthor)
						.map(User::getId)
						.equals(getClient().getSelfId())
				)
				.filter(event -> event.getGuildId().isPresent())
				.filter(event -> event.getMessageId().equals(GuildSpecificData.get(event.getGuildId().orElseThrow()).getChannel(event.getChannelId()).getMessageId()))
				.subscribe(event -> GuildSpecificData.get(event.getGuildId().orElseThrow()).getChannel(event.getChannelId()).reset());

		CommandManager.init();
		ButtonManager.init();
		MessageHookManager.init();
		CLIENT.onDisconnect().block();
	}

	public static GatewayDiscordClient getClient() {
		return CLIENT;
	}
}