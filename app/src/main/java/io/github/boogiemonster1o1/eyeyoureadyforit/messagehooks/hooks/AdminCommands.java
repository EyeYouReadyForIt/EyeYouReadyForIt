package io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks.hooks;

import discord4j.core.event.domain.message.MessageCreateEvent;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.stats.StatisticsManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks.MessageHook;
import reactor.core.publisher.Mono;
import java.util.function.Predicate;

public class AdminCommands implements MessageHook {
	@Override
	public void handle(MessageCreateEvent event) {
		switch(event.getMessage().getContent().split("\\s+")[1]) {
			case "shutdown":
				event
						.getMessage()
						.getChannel()
						.flatMap(channel -> channel.createMessage("**Shutting down...**"))
						.then(App.getClient().logout())
						.subscribe();
				break;

			case "reload":
				event
						.getMessage()
						.getChannel()
						.flatMap(channel -> channel.createMessage("**Reloading data...**"))
						.then(Mono.fromRunnable((EyeEntry::reload)))
						.subscribe();
				break;

			case "db":
				StatisticsManager.initDb(event.getGuildId().orElseThrow())
						.then(event
								.getMessage()
								.getChannel()
								.flatMap(channel -> {
									App.LOGGER.info("Attempted to create table for guild {}", event.getGuildId().orElseThrow().asString());
									return channel.createMessage(String.format("Attempted to create table for guild %s", event.getGuildId().orElseThrow().asString()));
								}))
						.subscribe();
				break;

			case "error":
				throw new RuntimeException("Error triggered by command");

			default:
				event
						.getMessage()
						.getChannel()
						.flatMap(channel -> channel.createMessage("**No such command was found**"))
						.subscribe();
				break;

		}
	}

	@Override
	public Predicate<MessageCreateEvent> getCondition() {
		return event -> event.getMessage().getContent().startsWith("!eyeyoureadyforit")
				&& event.getGuildId().get().asLong() == 859274373084479508L;
	}
}
