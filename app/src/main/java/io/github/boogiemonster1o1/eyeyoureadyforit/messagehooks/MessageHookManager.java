package io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks;

import discord4j.core.event.domain.message.MessageEvent;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import org.reflections.Reflections;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

public class MessageHookManager {
	private static final Reflections reflections = new Reflections("io.github.boogiemonster1o1.eyeyoureadyforit.messagehandler.handlers");
	private static final Set<MessageHook> MESSAGE_HOOK_SET = new HashSet<>();

	public static void init() {
		MESSAGE_HOOK_SET.clear();
		for (Class<? extends MessageHook> messageHookClass : reflections.getSubTypesOf(MessageHook.class)) {
			try {
				MESSAGE_HOOK_SET.add(messageHookClass.getConstructor().newInstance());
			} catch (Exception e) {
				e.printStackTrace();
			}

			App.getClient()
					.getEventDispatcher()
					.on(MessageEvent.class)
					.flatMap(event -> handle(event)
							.doOnError(Throwable::printStackTrace)
							.onErrorResume(e -> Mono.empty())
					)
					.subscribe();
		}
	}

	public static Mono<?> handle(MessageEvent event) {
		for(MessageHook hook : MESSAGE_HOOK_SET) {
			if(hook.getCondition().test(event)) {
				return hook.handle(event);
			}
		}

		return Mono.empty();
	}
}
