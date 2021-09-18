package io.github.boogiemonster1o1.eyeyoureadyforit.button;

import discord4j.core.event.domain.interaction.ButtonInteractEvent;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import org.reactivestreams.Publisher;

public interface ButtonHandler {
	Publisher<?> interact(ButtonInteractEvent event, ChannelSpecificData csd);
}
