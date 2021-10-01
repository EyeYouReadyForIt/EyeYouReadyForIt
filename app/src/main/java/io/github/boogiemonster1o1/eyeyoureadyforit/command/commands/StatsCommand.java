package io.github.boogiemonster1o1.eyeyoureadyforit.command.commands;

import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandHandler;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandHandlerType;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.GuildStatistics;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.Leaderboard;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.UserStatistics;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.stats.StatisticsManager;
import reactor.core.publisher.Mono;

public final class StatsCommand implements CommandHandler {
	@Override
	public Mono<?> handle(SlashCommandEvent event) {
		if (event.getOption("server").isPresent()) {
			return handleGuildStatsCommand(event);
		}

		return handleUserStatsCommand(event);
	}

	@Override
	public String getName() {
		return "stats";
	}

	@Override
	public CommandHandlerType getType() {
		return CommandHandlerType.GLOBAL_COMMAND;
	}

	@Override
	public ApplicationCommandRequest asRequest() {
		return ApplicationCommandRequest
				.builder()
				.name("stats")
				.description("Looks up user and server statistics")
				.addOption(
						ApplicationCommandOptionData
								.builder()
								.type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
								.name("users")
								.description("Looks up statistics for a selected user")
								.addOption(ApplicationCommandOptionData
										.builder()
										.required(false)
										.type(ApplicationCommandOptionType.USER.getValue())
										.name("user")
										.description("User to look up, defaults to command user")
										.build()
								)
								.build()

				)
				.addOption(
						ApplicationCommandOptionData
								.builder()
								.type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
								.name("server")
								.description("Looks up statistics for the current server")
								.build()

				)
				.build();
	}

	private static Mono<?> handleUserStatsCommand(SlashCommandEvent event) {
		// looking at this makes me want to cry

		Mono<User> userMono = Mono.justOrEmpty(event.getOption("users").get().getOption("user")
				.flatMap(ApplicationCommandInteractionOption::getValue)
		).flatMap(ApplicationCommandInteractionOptionValue::asUser)
				.switchIfEmpty(Mono.just(event.getInteraction().getUser()));


		return userMono.flatMap(user -> {
			if (user.isBot()) {
				return event.reply(InteractionApplicationCommandCallbackSpec
						.builder()
						.content("**Statistics are not available for bots!**")
						.ephemeral(true)
						.build()
				);
			}

			return StatisticsManager.getUserStats(
					event.getInteraction().getGuildId().orElseThrow(),
					user.getId())
					.defaultIfEmpty(new UserStatistics())
					.flatMap(statistic -> {
						EmbedCreateSpec embedSpec = EmbedCreateSpec
								.builder()
								.title(String.format("User Statistics: %s#%s", user.getUsername(), user.getDiscriminator()))
								.thumbnail(user.getAvatarUrl())
								.color(Color.of(0, 93, 186))
								.addField("Correct Answers", App.FORMATTER.format(statistic.getCorrectAnswers()), true)
								.addField("Wrong Answers", App.FORMATTER.format(statistic.getWrongAnswers()), true)
								.addField("Hints Used", App.FORMATTER.format(statistic.getHintsUsed()), true)
								.addField("Tourneys Won", App.FORMATTER.format(statistic.getGamesWon()), true)
								.addField("Server Rank", App.FORMATTER.format(statistic.getRank()), true)
								.footer(String.format("User ID: %s", user.getId().asString()), null)
								.timestamp(Instant.now())
								.build();

						return event.acknowledge().then(
								event.getInteractionResponse().createFollowupMessage(
										MultipartRequest.ofRequest(WebhookExecuteRequest
												.builder()
												.addEmbed(embedSpec.asRequest())
												.build()
										)
								)
						);
					});
		});
	}

	private static Mono<?> handleGuildStatsCommand(SlashCommandEvent event) {
		return event.getInteraction().getGuild().flatMap(guild -> StatisticsManager.getGuildStats(guild.getId())
				.defaultIfEmpty(new GuildStatistics())
				.flatMap(statistic -> {
					return StatisticsManager.getLeaderboard(guild.getId()).flatMap(board -> {
						ArrayList<Leaderboard> realBoard = (ArrayList<Leaderboard>) board
								.stream()
								.filter(e -> e.getId().asLong() != 0 && e.getGamesWon() != 0)
								.collect(Collectors.toList());

						String lbString = "Nobody has won yet!";
						String first = "\n";
						String second = "\n";
						String third = "\n";

						// tf is this
						// send help
						for (Leaderboard lb : realBoard) {
							switch (lb.getRank()) {
								case 1:
									first = String.format("🥇 - <@%s> - %s wins\n", lb.getId().asString(), App.FORMATTER.format(lb.getGamesWon()));
									break;
								case 2:
									second = String.format("🥈 - <@%s> - %s wins\n", lb.getId().asString(), App.FORMATTER.format(lb.getGamesWon()));
									break;
								case 3:
									third = String.format("🥉 - <@%s> - %s wins", lb.getId().asString(), App.FORMATTER.format(lb.getGamesWon()));
									break;
							}
						}

						lbString = first + second + third;

						EmbedCreateSpec embedSpec = EmbedCreateSpec
								.builder()
								.title(String.format("Server Statistics: %s", guild.getName()))
								.color(Color.of(0, 93, 186))
								.addField("Tourneys Played", App.FORMATTER.format(statistic.getGames()), true)
								.addField("Eyes Missed", App.FORMATTER.format(statistic.getMissed()), true)
								.addField("Top 3 Players", lbString, false)
								.footer(String.format("Guild ID: %s", guild.getId().asString()), null)
								.timestamp(Instant.now())
								.build();

						return event.acknowledge().then(
								event.getInteractionResponse().createFollowupMessage(
										MultipartRequest.ofRequest(WebhookExecuteRequest
												.builder()
												.addEmbed(embedSpec.asRequest())
												.build()
										)
								)
						);
					});
				}));
	}
}
