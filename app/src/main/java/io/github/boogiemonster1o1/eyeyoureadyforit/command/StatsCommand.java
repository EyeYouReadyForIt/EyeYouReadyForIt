package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.Color;
import discord4j.rest.util.WebhookMultipartRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.Statistic;
import io.github.boogiemonster1o1.eyeyoureadyforit.util.StatisticsManager;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class StatsCommand {

    public static Publisher<?> handle(SlashCommandEvent event) {


        return handleUserStatsCommand(event);

    }

    private static Mono handleUserStatsCommand(SlashCommandEvent event) {
        // looking at this makes me want to cry

        Mono<User> userMono = Mono.justOrEmpty(event.getOption("user")
                .flatMap(ApplicationCommandInteractionOption::getValue)
        ).flatMap(ApplicationCommandInteractionOptionValue::asUser)
                .switchIfEmpty(Mono.just(event.getInteraction().getUser()));


        return userMono.flatMap(user -> {
            if(user.isBot()) return event.replyEphemeral("**Statistics are not available for bots!**");

            return StatisticsManager.getUserStats(
                    event.getInteraction().getGuildId().get(),
                    user.getId())
                    .defaultIfEmpty(new Statistic())
                    .flatMap(statistic -> {
                        EmbedCreateSpec embedSpec = new EmbedCreateSpec()
                                .setTitle(String.format("User Statistics: %s#%s", user.getUsername(), user.getDiscriminator()))
                                .setThumbnail(user.getAvatarUrl())
                                .setColor(Color.of(0, 93, 186))
                                .addField("Correct answers", Integer.toString(statistic.getCorrectAnswers()), true)
                                .addField("Wrong answers", Integer.toString(statistic.getWrongAnswers()), true)
                                .addField("Hints used", Integer.toString(statistic.getHintUses()), true)
                                .setFooter(String.format("User ID: %s", user.getId().asString()), null)
                                .setTimestamp(Instant.now());

                        return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(new WebhookMultipartRequest(WebhookExecuteRequest.builder().addEmbed(embedSpec.asRequest()).build())));
                    });
        });
    }
}
