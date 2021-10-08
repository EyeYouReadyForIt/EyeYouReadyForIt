package io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks;

import discord4j.core.event.domain.message.MessageEvent;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

public interface MessageHook {
	Mono<?> handle(MessageEvent event);

	Predicate<MessageEvent> getCondition();
}
