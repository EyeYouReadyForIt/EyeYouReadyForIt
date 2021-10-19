package io.github.boogiemonster1o1.eyeyoureadyforit.button;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.Button;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import reactor.core.publisher.Mono;

public interface ButtonHandler {
	Button getButton();

	Mono<?> interact(ButtonInteractionEvent event, ChannelSpecificData csd);
}
