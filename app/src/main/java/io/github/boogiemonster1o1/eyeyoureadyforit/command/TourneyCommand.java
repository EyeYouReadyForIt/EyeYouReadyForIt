package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageEditSpec;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.*;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.stats.TourneyStatisticsTracker;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class TourneyCommand {
    public static Publisher<?> handle(SlashCommandEvent event, GuildSpecificData gsd, ChannelSpecificData csd) {
        if (csd.isTourney()) {
            event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**There is already a tourney**"));
        }
        int rounds = (int) event.getOption("rounds").orElseThrow().getValue().orElseThrow().asLong();
        if (rounds < 3) {
            event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**Choose a valid number equal to or above 3**"));
        }
        boolean disableHints = event.getOption("hintsdisabled").flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);
        boolean disableFirstNames = event.getOption("firstnamesdisabled").flatMap(ApplicationCommandInteractionOption::getValue).map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);
        csd.setTourneyData(new TourneyData(rounds, disableHints, disableFirstNames));
        csd.setTourneyStatisticsTracker(new TourneyStatisticsTracker(csd.getGuildSpecificData().getGuildId()));

        next(csd, 0L, event.getInteraction().getChannel(), true);

        return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage("Let the games begin"));
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
                channelMono.flatMap(channel -> channel.createEmbed(spec -> {
                    spec.setTitle("Tourney is over 🦀");
                    long[] distincts = Arrays.stream(data.getLeaderboard()).distinct().filter(l -> l != 0).toArray();
                    List<Long> leaderboard = Arrays.stream(data.getLeaderboard()).boxed().collect(Collectors.toList());
                    if (distincts.length == 0) {
                        spec.addField("Leaderboard", "No participants :/", false);
                    } else if (distincts.length == 1) {
                        spec.addField("Leaderboard", String.format("🥇 <@%s> - %s", distincts[0], leaderboard.stream().mapToLong(l -> l).filter(l -> l != 0).count()), false);
                    } else {
                        ModeContext first = getMostCommon(leaderboard);
                        tracker.setWon(Snowflake.of(first.getMode()));
                        ModeContext second = getMostCommon(leaderboard.stream().filter(l -> l != first.getMode()).collect(Collectors.toList()));
                        String boardMessage = String.format("🥇 <@%s> - %s\n" +
                                "🥈 <@%s> - %s", first.getMode(), first.getCount(), second.getMode(), second.getCount());
                        if (distincts.length >= 3) {
                            ModeContext third = getMostCommon(leaderboard.stream().filter(l -> l != first.getMode()).filter(l -> l != second.getMode()).collect(Collectors.toList()));
                            boardMessage += String.format("\n🥉 <@%s> - %s", third.getMode(), third.getCount());
                        }
                        spec.addField("Leaderboard", boardMessage, false);
                    }
                })).subscribe(mess -> {
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
        Mono<Message> messageMono = channelMono.flatMap(channel -> channel.createMessage(spec -> {
            spec.addEmbed(embed -> App.createEyesEmbed(entry, embed));
            if (csd.getTourneyData().shouldDisableHints()) {
                spec.setComponents(ActionRow.of(App.DISABLED_HINT_BUTTON));
            } else {
                spec.setComponents(ActionRow.of(App.HINT_BUTTON));
            }
        }));
        var id = new Object() {
            Snowflake snowflake = null;
        };
        messageMono.subscribe(message1 -> {
            synchronized (GuildSpecificData.LOCK) {
                csd.setCurrent(entry);
                csd.setMessageId(id.snowflake = message1.getId());
            }
        });

        Schedulers.parallel().schedule(() -> {
            if (round >= data.getMaxRounds()) {
                return;
            }
            if (data.getLeaderboard()[round] == 0L) {
                Mono<Message> mess = channelMono.flatMap(channel -> channel.createMessage(spec -> spec.setContent("Nobody guessed in time...")));
                tracker.addMissed();
                mess.subscribe(mess1 -> next(csd, 0L, mess1.getChannel(), false));
                Mono.justOrEmpty(id.snowflake)
                        .flatMap(sf -> channelMono.flatMap(channel -> channel.getMessageById(sf)))
                        .flatMap(m -> m.edit(MessageEditSpec::setComponents))
                        .subscribe();
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
