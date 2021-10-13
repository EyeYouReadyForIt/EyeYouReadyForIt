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
		for (MessageHook hook : MESSAGE_HOOK_SET) {
			if (hook.getCondition().test(event)) {
				hook.handle(event);
			}
		}

		return Mono.empty();
	}
}
