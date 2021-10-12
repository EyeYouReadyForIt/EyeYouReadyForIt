package io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import discord4j.core.event.domain.message.MessageCreateEvent;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.utils.ErrorHelper;
import org.reflections.Reflections;
import reactor.core.publisher.Mono;

public class MessageHookManager {
	private static final Reflections reflections = new Reflections("io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks.hooks");
	private static final Set<MessageHook> MESSAGE_HOOK_SET = new HashSet<>();

	public static void init() {
		MESSAGE_HOOK_SET.clear();
		reflections.getSubTypesOf(MessageHook.class)
				.stream()
				.forEach(aClass -> {
					try {
						MESSAGE_HOOK_SET.add(aClass.getConstructor().newInstance());
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

		App.getClient()
				.getEventDispatcher()
				.on(MessageCreateEvent.class)
				.flatMap(event -> accept(event))
				.onErrorContinue((error, event) -> {
					error.printStackTrace();
					ErrorHelper.sendErrorEmbed(error, (MessageCreateEvent) event);
				})
				.subscribe();
	}

	private static Mono<?> accept(MessageCreateEvent event) {
		for (MessageHook hook : MESSAGE_HOOK_SET) {
			if (getBaseCondition().and(hook.getCondition()).test(event)) {
				hook.handle(event);
			}
		}

		return Mono.empty();
	}

	public static Predicate<MessageCreateEvent> getBaseCondition() {
		return event -> event instanceof MessageCreateEvent
				&& event.getGuildId().isPresent()
				&& event.getMember().isPresent()
				&& event.getMember().map(member -> !member.isBot()).orElse(false)
				&& !event.getMessage().getAuthor().get().equals(event.getClient().getSelf().block());
	}
}
