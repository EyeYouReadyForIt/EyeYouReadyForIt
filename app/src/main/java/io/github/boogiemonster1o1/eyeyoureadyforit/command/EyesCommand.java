package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import java.time.Instant;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.MultipartRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.button.ButtonManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import org.reactivestreams.Publisher;

public class EyesCommand {
	public static Publisher<?> handle(SlashCommandEvent event, ChannelSpecificData csd) {
		if (csd.getMessageId() != null && csd.getCurrent() != null) {
			return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**There is already a context**"));
		}
		if (csd.isTourney()) {
			return event.acknowledgeEphemeral().then(event.getInteractionResponse().createFollowupMessage("**You can not use this command during a tourney**"));
		}

		EyeEntry entry = EyeEntry.getRandom();
		return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(getEyesRequest(entry))).map(data -> {
			synchronized (GuildSpecificData.LOCK) {
				csd.setCurrent(entry);
				csd.setMessageId(Snowflake.of(data.id()));
			}
			return data;
		});
	}

	public static MultipartRequest<WebhookExecuteRequest> getEyesRequest(EyeEntry entry) {
		return MultipartRequest.ofRequest(
				WebhookExecuteRequest
						.builder()
						.addEmbed(createEyesEmbed(entry).asRequest())
						.addComponent(ActionRow.of(ButtonManager.HINT_BUTTON, ButtonManager.RESET_BUTTON).getData())
						.build()
		);
	}

	public static EmbedCreateSpec createEyesEmbed(EyeEntry entry) {
    	return EmbedCreateSpec
				.builder()
        		.image(entry.getImageUrl())
        		.title("Guess the Person")
      			.description("Reply to this message with the answer")
				.timestamp(Instant.now())
				.build();
    }
}
