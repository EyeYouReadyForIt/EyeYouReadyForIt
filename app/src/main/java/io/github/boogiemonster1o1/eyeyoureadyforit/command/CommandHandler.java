package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import reactor.core.publisher.Mono;

public interface CommandHandler {
	Mono<?> handle(ChatInputInteractionEvent event, ChannelSpecificData csd, GatewayDiscordClient client);

	String getName();

	CommandHandlerType getType();

	ApplicationCommandRequest asRequest();
}

