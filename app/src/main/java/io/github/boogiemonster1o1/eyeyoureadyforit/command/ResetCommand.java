package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import java.time.Instant;
import java.util.ArrayList;

import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.Color;
import discord4j.rest.util.MultipartRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;

public class ResetCommand {
	public static Publisher<?> handle(SlashCommandEvent event, ChannelSpecificData csd) {
		if (csd.isTourney()) {
			return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**You can not use this command in a tourney**"));
		}
		return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(MultipartRequest.ofRequest(WebhookExecuteRequest.builder().addEmbed(addResetFooter(EmbedCreateSpec.builder(), event).asRequest()).build())));
	}

	public static EmbedCreateSpec addResetFooter(EmbedCreateSpec.Builder eSpec, InteractionCreateEvent event) {
		ChannelSpecificData data = GuildSpecificData.get(event.getInteraction().getGuildId().orElseThrow()).getChannel(event.getInteraction().getChannelId());
		if (data.getMessageId() != null && data.getCurrent() != null) {
			if (!data.getCurrent().getAliases().isEmpty()) {
				eSpec.description("Aliases: " + data.getCurrent().getAliases());
			}
			eSpec.title("The person was **" + data.getCurrent().getName() + "**");
			event.getInteraction().getChannel()
					.flatMap(channel -> channel.getMessageById(data.getMessageId()))
					.flatMap(message -> message.edit(MessageEditSpec.builder().components(new ArrayList<>()).build()))
					.subscribe();
		} else {
			eSpec.title("Reset");
			eSpec.description("But there was no context :p");
		}
		eSpec.timestamp(Instant.now());
		eSpec.color(Color.RED);
		eSpec.addField("Run by", event.getInteraction().getUser().getMention(), false);
		data.reset();
		return eSpec.build();
	}
}
