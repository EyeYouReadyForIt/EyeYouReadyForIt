package io.github.boogiemonster1o1.eyeyoureadyforit.button;

import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.HintCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.ResetCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import org.reactivestreams.Publisher;

enum ButtonHandlerImpl implements ButtonHandler {
	HINT {
		@Override
		public Publisher<?> interact(ButtonInteractEvent event, ChannelSpecificData csd) {
			if (csd.isTourney()) {
				csd.getTourneyStatisticsTracker().addHint(event.getInteraction().getUser().getId());
			}

			return event.reply(InteractionApplicationCommandCallbackSpec
					.builder()
					.ephemeral(true)
					.content(HintCommand.getHintContent(event))
					.build()
			);
		}
	},
	RESET {
		@Override
		public Publisher<?> interact(ButtonInteractEvent event, ChannelSpecificData csd) {
			return event.reply(InteractionApplicationCommandCallbackSpec
					.builder()
					.addEmbed(ResetCommand.addResetFooter(EmbedCreateSpec.builder(), event))
					.build()
			);
		}
	}
}
