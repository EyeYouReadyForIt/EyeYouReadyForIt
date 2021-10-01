package io.github.boogiemonster1o1.eyeyoureadyforit.button;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;
import org.reflections.Reflections;
import reactor.core.publisher.Mono;

@SuppressWarnings("NullableProblems")
public class ButtonManager {
	public static final Reflections reflections = new Reflections("io.github.boogiemonster1o1.eyeyoureadyforit.button.buttons");
	private static final Map<String, ButtonHandler> BUTTON_MAP = new ConcurrentHashMap<>();

	public static void init() {
		for (Class<? extends ButtonHandler> buttonClass : reflections.getSubTypesOf(ButtonHandler.class)) {
			try {
				ButtonHandler handler = buttonClass.getConstructor().newInstance();
				BUTTON_MAP.put(handler.getButton().getCustomId().orElseThrow(), handler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		App.CLIENT.on(new ReactiveEventAdapter() {
			@Override
			public Publisher<?> onButtonInteraction(ButtonInteractionEvent event) {
				return accept(event);
			}
		}).subscribe();
	}

	public static Publisher<?> accept(ButtonInteractionEvent event) {
		Snowflake guildId = event.getInteraction().getGuildId().orElseThrow();
		ChannelSpecificData csd = GuildSpecificData.get(guildId).getChannel(event.getInteraction().getChannelId());
		ButtonHandler handler = BUTTON_MAP.get(event.getCustomId());

		if (handler == null) {
			return Mono.empty();
		}

		return handler.interact(event, csd);
	}

	public static Button getButton(Class<? extends ButtonHandler> buttonClass) {
		return BUTTON_MAP.values()
				.stream()
				.filter(e -> e.getClass() == buttonClass)
				.findFirst().orElseThrow()
				.getButton();
	}
}
