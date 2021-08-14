package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.TourneyData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public final class TourneyCommand {
	public static Publisher<?> handle(SlashCommandEvent event, GuildSpecificData gsd) {
		if (gsd.isTourney()) {
			event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**There is already a tourney**"));
		}
		int rounds = (int) event.getOption("rounds").orElseThrow().getValue().orElseThrow().asLong();
		boolean disableHints = event.getOption("disableHints").flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);
		TourneyData tourneyData = new TourneyData(rounds, disableHints);
		gsd.setTourneyData(tourneyData);

		return Mono.empty();
	}

	public static void next(GuildSpecificData gsd, long answerer) {
		TourneyData data = gsd.getTourneyData();
		data.getLeaderboard()[data.getRound()] = answerer;
	}
}
