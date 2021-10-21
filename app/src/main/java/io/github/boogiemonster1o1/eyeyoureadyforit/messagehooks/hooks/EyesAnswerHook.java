package io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks.hooks;

import java.time.Instant;
import java.util.ArrayList;
import java.util.function.Predicate;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.MessageReference;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Color;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.commands.TourneyCommand;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.messagehooks.MessageHook;

public final class EyesAnswerHook implements MessageHook {
	@Override
	public void handle(MessageCreateEvent event, GatewayDiscordClient client) {
		String content = event.getMessage().getContent().toLowerCase();
		GuildSpecificData guildData = GuildSpecificData.get(event.getGuildId().orElseThrow());
		ChannelSpecificData data = guildData.getChannel(event.getMessage().getChannelId());
		EyeEntry current = data.getCurrent();
		Snowflake messageId = data.getMessageId();
		if (current.getName().equalsIgnoreCase(content) || current.getAliases().contains(content) || isFirstName(content, current.getName(), data)) {
			event.getMessage().getChannel().flatMap(channel ->
					channel.createMessage(MessageCreateSpec
							.builder()
							.addEmbed(EmbedCreateSpec
									.builder()
									.title("Correct!")
									.description(current.getName())
									.color(Color.GREEN)
									.timestamp(Instant.now())
									.build())
							.messageReference(event.getMessage().getId())
							.build()
					)
			).subscribe();

			event.getMessage().getChannel().flatMap(channel -> channel.getMessageById(messageId))
					.flatMap(message -> message.edit(MessageEditSpec.builder().components(new ArrayList<>()).build()))
					.subscribe();
			data.reset();
			if (data.isTourney()) {
				data.getTourneyStatisticsTracker().addCorrect(event.getMember().orElseThrow().getId());
				TourneyCommand.next(data, event.getMessage().getAuthor().map(User::getId).orElseThrow().asLong(), event.getMessage().getChannel(), false);
			}
		} else {
			event.getMessage().getChannel().flatMap(channel ->
					channel.createMessage(MessageCreateSpec
							.builder()
							.addEmbed(EmbedCreateSpec
									.builder()
									.title("Incorrect!")
									.description(current.getName())
									.color(Color.RED)
									.timestamp(Instant.now())
									.build())
							.messageReference(event.getMessage().getId())
							.build()
					)
			).subscribe();
			if (data.isTourney()) {
				data.getTourneyStatisticsTracker().addWrong(event.getMember().orElseThrow().getId());
			}
		}
	}

	@Override
	public Predicate<MessageCreateEvent> getCondition() {
		return event -> event.getMessage()
				.getMessageReference()
				.flatMap(MessageReference::getMessageId)
				.map(id -> id.equals(GuildSpecificData.get(event.getMessage()
								.getGuildId().orElseThrow())
						.getChannel(event.getMessage().getChannelId())
						.getMessageId())
				)
				.orElse(false);
	}

	private static boolean isFirstName(String allegedFirst, String name, ChannelSpecificData data) {
		if (data.isTourney() && data.getTourneyData().shouldDisableFirstNames()) {
			return false;
		}
		if (!name.matches(".*\\s+.*")) {
			return false;
		}
		return allegedFirst.split(".*\\s+.*")[0].equalsIgnoreCase(name.split(".*\\s+.*")[0]);
	}
}
