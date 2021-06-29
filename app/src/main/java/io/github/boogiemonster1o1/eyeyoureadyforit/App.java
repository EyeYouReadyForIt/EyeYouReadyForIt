package io.github.boogiemonster1o1.eyeyoureadyforit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.object.entity.Message;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class App {
	public static final Logger LOGGER = LoggerFactory.getLogger("Eye You Ready For It");
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	private static final CommandManager COMMAND_MANAGER = new CommandManager();
	private static GatewayDiscordClient CLIENT;
	private static final Map<Snowflake, GuildSpecificData> GUILD_SPECIFIC_DATA_MAP = new HashMap<>();

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
					LOGGER.info("Logged in as {}#{}", event.getData().user().username(), event.getData().user().discriminator());
					LOGGER.info("Guilds: {}", event.getGuilds().size());
					LOGGER.info("Gateway version: {}", event.getGatewayVersion());
					LOGGER.info("Session ID: {}", event.getSessionId());
					LOGGER.info("Shard Info: Index {}, Count {}", event.getShardInfo().getIndex(), event.getShardInfo().getCount());
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
		COMMAND_MANAGER.init();
		EyeEntry.reload();
		CLIENT.onDisconnect().block();
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
