package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import java.time.Duration;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Message;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.TourneyData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public final class TourneyCommand {
	public static Publisher<?> handle(SlashCommandEvent event, GuildSpecificData gsd) {
		if (gsd.isTourney()) {
			event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**There is already a tourney**"));
		}
		int rounds = (int) event.getOption("rounds").orElseThrow().getValue().orElseThrow().asLong() - 1;
		boolean disableHints = event.getOption("disableHints").flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);
		TourneyData tourneyData = new TourneyData(rounds, disableHints);
		gsd.setTourneyData(tourneyData);

		next(gsd, 0L, event.getInteraction().getMessage().orElseThrow(), true);

		return Mono.empty();
	}

	public static void next(GuildSpecificData gsd, long answerer, Message message, boolean first) {
		TourneyData data = gsd.getTourneyData();
		int round;
		if (first) {
			message.getChannel().flatMap(channel -> channel.createMessage("Starting Tourney")).subscribe();
			data.setRound(round = 0);
		} else {
			data.getLeaderboard()[data.getRound()] = answerer;
			gsd.reset();
			data.setRound(round = data.getRound() + 1);
		}
		if (round == data.getMaxRounds()) {
			message.getChannel().flatMap(channel -> channel.createMessage("Tourney is over :crab:")).subscribe(mess -> {
				gsd.reset();
				gsd.setTourneyData(null);
			});
		}
		EyeEntry entry = EyeEntry.getRandom();
		message.getChannel().flatMap(channel -> channel.createMessage("Round #" + (data.getRound() + 1))).subscribe();
		message.getChannel().flatMap(channel -> channel.createEmbed(spec -> {
			App.createEyesEmbed(entry, spec);
		})).subscribe(message1 -> {
			synchronized (GuildSpecificData.LOCK) {
				gsd.setCurrent(entry);
				gsd.setMessageId(message1.getId());
			}
		});
		Mono.just(message)
				.flatMap(Message::getChannel)
				.flatMap(channel -> {
					if (data.getLeaderboard()[round] == 0L) {
						Mono<Message> mess = channel.createMessage(spec -> {
							spec.setMessageReference(gsd.getMessageId());
							spec.setContent("Nobody guessed in time...");
						});
						mess.subscribe(mess1 -> next(gsd, 0L, mess1, false));
						return mess;
					}
					return Mono.empty();
				})
				.delaySubscription(Duration.ofSeconds(10))
				.subscribe();
	}
}
