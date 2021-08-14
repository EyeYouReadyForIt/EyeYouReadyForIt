package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.TourneyData;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public final class TourneyCommand {
	public static Publisher<?> handle(SlashCommandEvent event, GuildSpecificData gsd) {
		if (gsd.isTourney()) {
			event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**There is already a tourney**"));
		}
		int rounds = (int) event.getOption("rounds").orElseThrow().getValue().orElseThrow().asLong();
		if (rounds < 3) {
			event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**Choose a valid number equal to or above 3**"));
		}
		boolean disableHints = event.getOption("hintsdisabled").flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);
		TourneyData tourneyData = new TourneyData(rounds, disableHints);
		gsd.setTourneyData(tourneyData);

		next(gsd, 0L, event.getInteraction().getChannel(), true);

		return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage("Let the games begin"));
	}

	public static void next(GuildSpecificData gsd, long answerer, Mono<MessageChannel> channelMono, boolean first) {
		TourneyData data = gsd.getTourneyData();
		int round;
		if (first) {
			channelMono.flatMap(channel -> channel.createMessage("Starting Tourney")).subscribe();
			data.setRound(round = 0);
		} else {
			data.getLeaderboard()[data.getRound()] = answerer;
			if (data.getRound() + 1 >= data.getMaxRounds()) {
				channelMono.flatMap(channel -> channel.createEmbed(spec -> {
					spec.setTitle("Tourney is over :crab:");
					spec.addField("Leaderboard", Arrays.toString(data.getLeaderboard()), false);
				})).subscribe(mess -> {
					gsd.reset();
					gsd.setTourneyData(null);
				});
				return;
			}
			gsd.reset();
			data.setRound(round = data.getRound() + 1);
		}
		EyeEntry entry = EyeEntry.getRandom();
		channelMono.flatMap(channel -> channel.createMessage("Round #" + (data.getRound() + 1))).subscribe();
		channelMono.flatMap(channel -> channel.createEmbed(spec -> {
			App.createEyesEmbed(entry, spec);
		})).subscribe(message1 -> {
			synchronized (GuildSpecificData.LOCK) {
				gsd.setCurrent(entry);
				gsd.setMessageId(message1.getId());
			}
		});

		Schedulers.parallel().schedule(() -> {
			if (round >= data.getMaxRounds()) {
				return;
			}
			if (data.getLeaderboard()[round] == 0L) {
				Mono<Message> mess = channelMono.flatMap(channel -> channel.createMessage(spec -> spec.setContent("Nobody guessed in time...")));
				mess.subscribe(mess1 -> next(gsd, 0L, mess1.getChannel(), false));
			}
		}, 30, TimeUnit.SECONDS);
	}
}
