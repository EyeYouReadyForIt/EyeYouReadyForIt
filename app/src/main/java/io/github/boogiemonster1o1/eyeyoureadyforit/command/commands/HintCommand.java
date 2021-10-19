package io.github.boogiemonster1o1.eyeyoureadyforit.command.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandHandler;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandHandlerType;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import reactor.core.publisher.Mono;

public final class HintCommand implements CommandHandler {
	@Override
	public Mono<?> handle(ChatInputInteractionEvent event) {
		ChannelSpecificData csd = GuildSpecificData
				.get(event.getInteraction().getGuildId().orElseThrow())
				.getChannel(event.getInteraction().getChannelId());

		if (csd.isTourney() && csd.getTourneyData().shouldDisableHints()) {
			return event.deferReply().withEphemeral(Boolean.TRUE).then(event.getInteractionResponse().createFollowupMessage("**Hints are disabled for this tourney**"));
		}
		if (csd.isTourney())
			csd.getTourneyStatisticsTracker().addHint(event.getInteraction().getUser().getId());
		return event.deferReply().withEphemeral(Boolean.TRUE).then(event.getInteractionResponse().createFollowupMessage(MultipartRequest.ofRequest(WebhookExecuteRequest.builder().addEmbed(HintCommand.getHintContent(event).asRequest()).build())));
	}

	@Override
	public String getName() {
		return "hint";
	}

	@Override
	public CommandHandlerType getType() {
		return CommandHandlerType.GLOBAL_COMMAND;
	}

	@Override
	public ApplicationCommandRequest asRequest() {
		return ApplicationCommandRequest.builder()
				.name("hint")
				.description("Shows a hint")
				.build();
	}

	public static EmbedCreateSpec getHintContent(InteractionCreateEvent event) {
		ChannelSpecificData data = GuildSpecificData.get(event.getInteraction().getGuildId().orElseThrow()).getChannel(event.getInteraction().getChannelId());

		if (data.getMessageId() != null && data.getCurrent() != null) {
			return EmbedCreateSpec
					.builder()
					.title("Hint")
					.description(data.getCurrent().getHint())
					.color(Color.GREEN)
					.build();
		}

		return EmbedCreateSpec.builder().description("**But there was no context :p**").color(Color.RED).build();
	}
}
