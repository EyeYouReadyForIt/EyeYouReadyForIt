package io.github.boogiemonster1o1.eyeyoureadyforit.utils;

import java.time.Instant;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.rest.util.Color;

public final class ErrorHelper {
	public static void sendErrorEmbed(Throwable exception, MessageCreateEvent event) {
		if (event == null) {
			return;
		}

		event.getMessage().getChannel().flatMap(channel ->
				channel.createMessage(createEmbed(exception))
		).subscribe();
	}

	public static void sendErrorEmbed(Throwable exception, InteractionCreateEvent event) {
		if (event == null) {
			return;
		}

		event.reply(InteractionApplicationCommandCallbackSpec
						.builder()
						.addEmbed(createEmbed(exception))
						.ephemeral(true)
						.build())
				.subscribe();
	}

	private static EmbedCreateSpec createEmbed(Throwable exception) {
		return EmbedCreateSpec
				.builder()
				.title(String.format("Error: %s", exception.getClass().getName()))
				.description(String.format("```%s```", exception))
				.color(Color.RED)
				.timestamp(Instant.now())
				.build();
	}
}
