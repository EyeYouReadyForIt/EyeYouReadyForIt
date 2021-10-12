package io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageEvent;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.awt.print.PrinterException;
import java.util.function.Predicate;

public interface MessageHook {
	void handle(MessageCreateEvent event);

	Predicate<MessageCreateEvent> getCondition();
}
