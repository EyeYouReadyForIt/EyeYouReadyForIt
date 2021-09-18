package io.github.boogiemonster1o1.eyeyoureadyforit.button;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.HintCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.ResetCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@SuppressWarnings("NullableProblems")
public class ButtonManager {
	public static final Button HINT_BUTTON = Button.success("hint_button", ReactionEmoji.unicode("💡"), "Hint");
	public static final Button RESET_BUTTON = Button.secondary("reset_button", ReactionEmoji.unicode("🚫"), "Reset");

	public static void init() {
		App.CLIENT.on(new ReactiveEventAdapter() {
			@Override
			public Publisher<?> onButtonInteract(ButtonInteractEvent event) {
				return accept(event);
			}
		}).blockLast();
	}

	public static Publisher<?> accept(ButtonInteractEvent event) {
		Snowflake guildId = event.getInteraction().getGuildId().orElseThrow();
		ChannelSpecificData csd = GuildSpecificData.get(guildId).getChannel(event.getInteraction().getChannelId());
		if (event.getCustomId().equals("hint_button")) {
			if (csd.isTourney()) {
				csd.getTourneyStatisticsTracker().addHint(event.getInteraction().getUser().getId());
			}

			return event.reply(InteractionApplicationCommandCallbackSpec
					.builder()
					.ephemeral(true)
					.content(HintCommand.getHintContent(event))
					.build()
			);

		} else if (event.getCustomId().equals("reset_button")) {
			return event.reply(InteractionApplicationCommandCallbackSpec
					.builder()
					.addEmbed(ResetCommand.addResetFooter(EmbedCreateSpec.builder(), event))
					.build()
			);
		}

		return Mono.empty();
	}
}
