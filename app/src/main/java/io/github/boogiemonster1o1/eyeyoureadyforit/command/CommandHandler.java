package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.reactivestreams.Publisher;

public interface CommandHandler {
	Publisher<?> handle(ChatInputInteractionEvent event);

	String getName();

	CommandHandlerType getType();

	ApplicationCommandRequest asRequest();
}

