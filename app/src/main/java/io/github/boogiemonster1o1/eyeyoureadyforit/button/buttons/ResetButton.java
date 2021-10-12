package io.github.boogiemonster1o1.eyeyoureadyforit.button.buttons;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import io.github.boogiemonster1o1.eyeyoureadyforit.button.ButtonHandler;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.commands.ResetCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import reactor.core.publisher.Mono;

public class ResetButton implements ButtonHandler {
	@Override
	public Button getButton() {
		return Button.secondary("reset_button", ReactionEmoji.unicode("🚫"), "Reset");
	}

	@Override
	public Mono<?> interact(ButtonInteractionEvent event, ChannelSpecificData csd) {
		return event.reply(InteractionApplicationCommandCallbackSpec
				.builder()
				.addEmbed(ResetCommand.addResetFooter(event))
				.build()
		);
	}
}
