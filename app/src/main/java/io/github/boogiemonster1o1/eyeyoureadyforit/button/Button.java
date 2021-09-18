package io.github.boogiemonster1o1.eyeyoureadyforit.button;

import java.util.concurrent.Flow.Publisher;

import discord4j.core.event.domain.interaction.ButtonInteractEvent;

public interface Button {
	Publisher<?> interact(ButtonInteractEvent event);
}
