package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import org.reactivestreams.Publisher;

@FunctionalInterface
public interface CommandHandler {
	Publisher<?> handle(SlashCommandEvent event, ChannelSpecificData csd);
}
