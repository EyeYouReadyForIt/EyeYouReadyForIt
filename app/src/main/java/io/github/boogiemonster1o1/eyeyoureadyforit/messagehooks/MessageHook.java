package io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks;

import java.util.function.Predicate;
import discord4j.core.event.domain.message.MessageCreateEvent;

public interface MessageHook {
	void handle(MessageCreateEvent event);

	Predicate<MessageCreateEvent> getCondition();
}
