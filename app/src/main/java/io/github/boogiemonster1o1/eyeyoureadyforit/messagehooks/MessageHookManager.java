package io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks;

import java.util.HashSet;
import java.util.Set;
import discord4j.core.event.domain.message.MessageCreateEvent;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.utils.ErrorHelper;
import org.reflections.Reflections;
import reactor.core.publisher.Mono;

public final class MessageHookManager {
	private static final Reflections reflections = new Reflections("io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks.hooks");
	private static final Set<MessageHook> MESSAGE_HOOKS = new HashSet<>();


	public static void init() {
		MESSAGE_HOOKS.clear();
		reflections.getSubTypesOf(MessageHook.class)
				.forEach(aClass -> {
					try {
						MESSAGE_HOOKS.add(aClass.getConstructor().newInstance());
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

		App.getClient()
				.getEventDispatcher()
				.on(MessageCreateEvent.class)
				.filterWhen(event -> event.getClient().getSelf().flatMap(self ->
						Mono.just(event.getGuildId().isPresent()
								&& event.getMember().isPresent()
								&& event.getMember().map(member -> !member.isBot()).orElse(false)
								&& !event.getMessage().getAuthor().get().equals(self))))
				.flatMap(MessageHookManager::accept)
				.onErrorContinue((error, event) -> {
					error.printStackTrace();
					ErrorHelper.sendErrorEmbed(error, (MessageCreateEvent) event);
				})
				.subscribe();
	}

	private static Mono<?> accept(MessageCreateEvent event) {
		for (MessageHook hook : MESSAGE_HOOKS) {
			if (hook.getCondition().test(event)) {
				hook.handle(event);
			}
		}

		return Mono.empty();
	}
}
