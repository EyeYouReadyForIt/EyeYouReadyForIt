package io.github.boogiemonster1o1.eyeyoureadyforit.button;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.utils.ErrorHelper;
import org.reflections.Reflections;
import reactor.core.publisher.Mono;

@SuppressWarnings("NullableProblems")
public final class ButtonManager {
	private static final Reflections reflections = new Reflections("io.github.boogiemonster1o1.eyeyoureadyforit.button.buttons");
	private static final Map<String, ButtonHandler> BUTTON_MAP = new ConcurrentHashMap<>();

	public static void init() {
		BUTTON_MAP.clear();
		reflections.getSubTypesOf(ButtonHandler.class)
				.stream()
				.forEach(aClass -> {
					try {
						ButtonHandler handler = aClass.getConstructor().newInstance();
						BUTTON_MAP.put(handler.getButton().getCustomId().orElseThrow(), handler);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

		App.getClient().getEventDispatcher()
				.on(ButtonInteractionEvent.class)
				.flatMap(event -> accept(event))
				.onErrorContinue((error, event) -> {
					error.printStackTrace();
					ErrorHelper.sendErrorEmbed(error, (ButtonInteractionEvent) event);
				})
				.subscribe();
	}

	private static Mono<?> accept(ButtonInteractionEvent event) {
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
