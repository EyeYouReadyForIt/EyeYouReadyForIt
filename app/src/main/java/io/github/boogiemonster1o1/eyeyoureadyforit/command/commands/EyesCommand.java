package io.github.boogiemonster1o1.eyeyoureadyforit.command.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.rest.util.MultipartRequest;
import io.github.boogiemonster1o1.eyeyoureadyforit.button.ButtonManager;
import io.github.boogiemonster1o1.eyeyoureadyforit.button.buttons.HintButton;
import io.github.boogiemonster1o1.eyeyoureadyforit.button.buttons.ResetButton;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandHandler;
import io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandHandlerType;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.ChannelSpecificData;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class EyesCommand implements CommandHandler {

	public static EmbedCreateSpec createEyesEmbed(EyeEntry entry) {
		return EmbedCreateSpec
				.builder()
				.image(entry.getImageUrl())
				.title("Guess the Person")
				.description("Reply to this message with the answer")
				.timestamp(Instant.now())
				.build();
	}

	@Override
	public Mono<?> handle(SlashCommandEvent event) {
		ChannelSpecificData csd = GuildSpecificData
				.get(event.getInteraction().getGuildId().orElseThrow())
				.getChannel(event.getInteraction().getChannelId());

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

	@Override
	public String getName() {
		return "eyes";
	}

	@Override
	public CommandHandlerType getType() {
		return CommandHandlerType.GLOBAL_COMMAND;
	}

	@Override
	public ApplicationCommandRequest asRequest() {
		return ApplicationCommandRequest.builder()
				.name("eyes")
				.description("Shows a pair of eyes")
				.build();
	}

	public MultipartRequest<WebhookExecuteRequest> getEyesRequest(EyeEntry entry) {
		return MultipartRequest.ofRequest(
				WebhookExecuteRequest
						.builder()
						.addEmbed(createEyesEmbed(entry).asRequest())
						.addComponent(ActionRow.of(ButtonManager.getButton(HintButton.class), ButtonManager.getButton(ResetButton.class)).getData())
						.build()
		);
	}
}
