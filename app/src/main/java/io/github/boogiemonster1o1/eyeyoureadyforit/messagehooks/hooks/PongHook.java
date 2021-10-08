package io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks.hooks;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.spec.MessageCreateSpec;
import io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks.MessageHook;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

public class PongHook implements MessageHook {
	@Override
	public Mono<?> handle(MessageEvent event) {
		return ((MessageCreateEvent) event)
				.getMessage()
				.getChannel()
				.flatMap(channel -> channel.createMessage(
						MessageCreateSpec
								.builder()
								.messageReference(((MessageCreateEvent) event).getMessage().getId())
								.content("Pong!")
								.build())
				);
	}

	@Override
	public Predicate<MessageEvent> getCondition() {
		return event -> event instanceof MessageCreateEvent && ((MessageCreateEvent) event).getMessage().getContent().contains("ping");
	}
}
