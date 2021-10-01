package io.github.boogiemonster1o1.eyeyoureadyforit.button;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;
import org.reflections.Reflections;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("NullableProblems")
public class ButtonManager {
	public static final Button HINT_BUTTON = Button.success("hint_button", ReactionEmoji.unicode("💡"), "Hint");
	public static final Button RESET_BUTTON = Button.secondary("reset_button", ReactionEmoji.unicode("🚫"), "Reset");
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
			public Publisher<?> onButtonInteract(ButtonInteractEvent event) {
				return accept(event);
			}
		}).subscribe();
	}

	public static Publisher<?> accept(ButtonInteractEvent event) {
		Snowflake guildId = event.getInteraction().getGuildId().orElseThrow();
		ChannelSpecificData csd = GuildSpecificData.get(guildId).getChannel(event.getInteraction().getChannelId());
		ButtonHandler handler = BUTTON_MAP.get(event.getCustomId());

		if (handler == null) {
			return Mono.empty();
		}

		return handler.interact(event, csd);
	}

	public static Button getButton(Class<? extends ButtonHandler> buttonClass) {
		try {
			for (Map.Entry<String, ButtonHandler> entry : BUTTON_MAP.entrySet()) {
				if (entry.getValue().getClass() == buttonClass) return entry.getValue().getButton();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
