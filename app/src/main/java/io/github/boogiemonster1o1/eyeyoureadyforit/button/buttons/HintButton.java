package io.github.boogiemonster1o1.eyeyoureadyforit.button.buttons;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import io.github.boogiemonster1o1.eyeyoureadyforit.button.ButtonHandler;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.commands.HintCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import reactor.core.publisher.Mono;

public final class HintButton implements ButtonHandler {
	@Override
	public Button getButton() {
		return Button.success("hint_button", ReactionEmoji.unicode("💡"), "Hint");
	}

	@Override
	public Mono<?> interact(ButtonInteractionEvent event, ChannelSpecificData csd, GatewayDiscordClient client) {
		if (csd.isTourney()) {
			csd.getTourneyStatisticsTracker().addHint(event.getInteraction().getUser().getId());
		}

		return event.reply(InteractionApplicationCommandCallbackSpec
				.builder()
				.ephemeral(true)
				.addEmbed(HintCommand.getHintContent(event))
				.build()
		);
	}
}
