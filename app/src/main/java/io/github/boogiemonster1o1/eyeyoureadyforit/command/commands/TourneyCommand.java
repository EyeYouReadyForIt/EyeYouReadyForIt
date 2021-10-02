package io.github.boogiemonster1o1.eyeyoureadyforit.command.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.button.ButtonManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.button.buttons.HintButton;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandHandler;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandHandlerType;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ModeContext;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.TourneyData;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.stats.TourneyStatisticsTracker;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public final class TourneyCommand implements CommandHandler {
	@Override
	public Mono<?> handle(ChatInputInteractionEvent event) {
		ChannelSpecificData csd = GuildSpecificData
				.get(event.getInteraction().getGuildId().orElseThrow())
				.getChannel(event.getInteraction().getChannelId());

		if (csd.isTourney()) {
			return event.deferReply().withEphemeral(Boolean.TRUE).then(event.getInteractionResponse().createFollowupMessage("**There is already a tourney**"));
		}

		int rounds = (int) event.getOption("rounds").orElseThrow().getValue().orElseThrow().asLong();
		if (rounds < 3) {
			return event.deferReply().withEphemeral(Boolean.TRUE).then(event.getInteractionResponse().createFollowupMessage("**Choose a valid number equal to or above 3**"));
		}

		boolean disableHints = event.getOption("hintsdisabled").flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);
		boolean disableFirstNames = event.getOption("firstnamesdisabled").flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);
		csd.setTourneyData(new TourneyData(rounds, disableHints, disableFirstNames));
		csd.setTourneyStatisticsTracker(new TourneyStatisticsTracker(csd.getGuildSpecificData().getGuildId()));

		next(csd, 0L, event.getInteraction().getChannel(), true);

		return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage("Let the games begin"));
	}

	@Override
	public String getName() {
		return "tourney";
	}

	@Override
	public CommandHandlerType getType() {
		return CommandHandlerType.GLOBAL_COMMAND;
	}

	@Override
	public ApplicationCommandRequest asRequest() {
		return ApplicationCommandRequest
				.builder()
				.name("tourney")
				.description("Starts a tourney")
				.addOption(
						ApplicationCommandOptionData
								.builder()
								.required(true)
								.type(Type.INTEGER.getValue())
								.name("rounds")
								.description("Number of rounds. From 5 to 10")
								.build()
				)
				.addOption(
						ApplicationCommandOptionData
								.builder()
								.required(false)
								.type(Type.BOOLEAN.getValue())
								.name("hintsdisabled")
								.description("Whether Hints are disabled")
								.build()
				)
				.addOption(
						ApplicationCommandOptionData
								.builder()
								.required(false)
								.type(Type.BOOLEAN.getValue())
								.name("firstnamesdisabled")
								.description("Whether first names are disabled")
								.build()
				)
				.build();
	}

	public static void next(ChannelSpecificData csd, long answerer, Mono<MessageChannel> channelMono, boolean justStarted) {
		TourneyData data = csd.getTourneyData();
		TourneyStatisticsTracker tracker = csd.getTourneyStatisticsTracker();
		int round;
		if (justStarted) {
			channelMono.flatMap(channel -> channel.createMessage("Starting Tourney")).subscribe();
			data.setRound(round = 0);
		} else {
			data.getLeaderboard()[data.getRound()] = answerer;
			if (data.getRound() + 1 >= data.getMaxRounds()) {
				channelMono.flatMap(channel -> {
					List<Long> leaderboard = Arrays.stream(data.getLeaderboard()).filter(l -> l != 0).boxed().collect(Collectors.toList());
					int distincts = (int) leaderboard.stream().distinct().count();
					String boardMessage = "No participants :/";

					if (distincts == 1) {
						boardMessage = String.format("🥇 <@%s> - %s", leaderboard.get(0), leaderboard.stream().mapToLong(l -> l).count());
					} else if (distincts > 1) {
						ModeContext first = getMostCommon(leaderboard);
						tracker.setWon(Snowflake.of(first.getMode()));
						ModeContext second = getMostCommon(leaderboard.stream().filter(l -> l != first.getMode()).collect(Collectors.toList()));
						boardMessage = String.format("🥇 <@%s> - %s\n" +
								"🥈 <@%s> - %s", first.getMode(), first.getCount(), second.getMode(), second.getCount());
						if (distincts >= 3) {
							ModeContext third = getMostCommon(leaderboard.stream().filter(l -> l != first.getMode()).filter(l -> l != second.getMode()).collect(Collectors.toList()));
							boardMessage += String.format("\n🥉 <@%s> - %s", third.getMode(), third.getCount());
						}
					}

					return channel.createMessage(EmbedCreateSpec
							.builder()
							.title("Tourney is over 🦀")
							.addField("Leaderboard", boardMessage, false)
							.build()
					);
				}).subscribe(mess -> {
					tracker.commit();
					csd.reset();
					csd.setTourneyData(null);
					csd.setTourneyStatisticsTracker(null);
				});
				return;
			}
			csd.reset();
			data.setRound(round = data.getRound() + 1);
		}
		EyeEntry entry = EyeEntry.getRandom();
		channelMono.flatMap(channel -> channel.createMessage("Round #" + App.FORMATTER.format(data.getRound() + 1))).subscribe();
		channelMono.flatMap(channel -> channel.createMessage(
				MessageCreateSpec
						.builder()
						.addEmbed(EyesCommand.createEyesEmbed(entry))
						.addComponent(!data.shouldDisableHints() ? ActionRow.of(ButtonManager.getButton(HintButton.class)) : ActionRow.of(ButtonManager.getButton(HintButton.class).disabled()))
						.build()
		)).subscribe(message -> {
			synchronized (GuildSpecificData.LOCK) {
				csd.setCurrent(entry);
				csd.setMessageId(message.getId());
			}
		});

		Schedulers.parallel().schedule(() -> {
			if (round >= data.getMaxRounds()) {
				return;
			}
			if (data.getLeaderboard()[round] == 0) {
				channelMono.flatMap(channel -> channel.createMessage("Nobody guessed in time..."))
						.then(Mono.fromRunnable(tracker::addMissed))
						.then(Mono.justOrEmpty(csd.getMessageId())
								.flatMap(sf -> channelMono.flatMap(channel -> channel.getMessageById(sf)))
								.flatMap(message -> message.edit(MessageEditSpec.create().withComponents())))
						.subscribe(message -> next(csd, 0L, message.getChannel(), false));
			}
		}, 30, TimeUnit.SECONDS);
	}

	private static ModeContext getMostCommon(List<? extends Number> participants) {
		if (participants.stream().distinct().count() == 1)
			return new ModeContext(participants.get(0).longValue(), Collections.frequency(participants, participants.get(0)));

		List<Long> list = participants.stream().map(Number::longValue).collect(Collectors.toList());
		long mode = 0;
		int modeCount = 0;
		HashMap<Long, Integer> hashMap = new HashMap<>();

		for (long i : list) {
			hashMap.put(i, hashMap.getOrDefault(i, 0) + 1);
		}

		for (Map.Entry<Long, Integer> entry : hashMap.entrySet()) {
			if (entry.getValue() > modeCount) {
				mode = entry.getKey();
				modeCount = entry.getValue();
			}
		}

		return new ModeContext(mode, modeCount);
	}
}
