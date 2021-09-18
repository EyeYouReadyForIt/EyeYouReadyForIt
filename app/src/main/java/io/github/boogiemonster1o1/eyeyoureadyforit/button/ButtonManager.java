package io.github.boogiemonster1o1.eyeyoureadyforit.button;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@SuppressWarnings("NullableProblems")
public class ButtonManager {
	public static final Button HINT_BUTTON = Button.success("hint_button", ReactionEmoji.unicode("💡"), "Hint");
	public static final Button RESET_BUTTON = Button.secondary("reset_button", ReactionEmoji.unicode("🚫"), "Reset");
	private static final Map<String, ButtonHandler> BUTTONS = new ConcurrentHashMap<>();

	public static void init() {
		App.CLIENT.on(new ReactiveEventAdapter() {
			@Override
			public Publisher<?> onButtonInteract(ButtonInteractEvent event) {
				return accept(event);
			}
		}).blockLast();
		BUTTONS.put(HINT_BUTTON.getCustomId().orElseThrow(), ButtonHandlerImpl.HINT);
		BUTTONS.put(RESET_BUTTON.getCustomId().orElseThrow(), ButtonHandlerImpl.RESET);
	}

	public static Publisher<?> accept(ButtonInteractEvent event) {
		Snowflake guildId = event.getInteraction().getGuildId().orElseThrow();
		ChannelSpecificData csd = GuildSpecificData.get(guildId).getChannel(event.getInteraction().getChannelId());
		ButtonHandler handler = BUTTONS.get(event.getCustomId());

		if (handler == null) {
			return Mono.empty();
		}

		return handler.interact(event, csd);
	}
}
