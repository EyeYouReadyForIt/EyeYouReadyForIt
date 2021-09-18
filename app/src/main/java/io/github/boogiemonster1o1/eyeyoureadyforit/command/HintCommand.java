package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.MultipartRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;

public class HintCommand {
	public static Publisher<?> handle(SlashCommandEvent event, ChannelSpecificData csd) {
		if (csd.isTourney() && csd.getTourneyData().shouldDisableHints()) {
			return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**Hints are disabled for this tourney**"));
		}
		if (csd.isTourney())
			csd.getTourneyStatisticsTracker().addHint(event.getInteraction().getUser().getId());
		return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage(MultipartRequest.ofRequest(WebhookExecuteRequest.builder().content(HintCommand.getHintContent(event)).build())));
	}

	public static String getHintContent(InteractionCreateEvent event) {
		ChannelSpecificData data = GuildSpecificData.get(event.getInteraction().getGuildId().orElseThrow()).getChannel(event.getInteraction().getChannelId());

		if (data.getMessageId() != null && data.getCurrent() != null) {
			return data.getCurrent().getHint();
		}

		return "**There is no context available**";
	}
}
