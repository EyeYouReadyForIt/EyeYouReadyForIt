package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;

import static io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandManager.literal;

public class EyeCommand extends AbstractCommand {
	public EyeCommand(LiteralCommandNode<MessageCreateEvent> node) {
		super(node);
	}

	@Override
	public String getDescription() {
		return "Shows a pair of eyes. Guess who they belong to!";
	}

	@Override
	public boolean hasDescription() {
		return true;
	}

	public static EyeCommand create(CommandDispatcher<MessageCreateEvent> dispatcher) {
		return new EyeCommand(dispatcher.register(literal("eyes").executes(EyeCommand::execute)));
	}

	private static int execute(CommandContext<MessageCreateEvent> event) {
		GuildSpecificData data = App.getGuildSpecificData(event.getSource().getMessage().getGuildId().orElseThrow());
		if (data.getMessageId() != null && data.getCurrent() != null) {
			event.getSource().getMessage().getChannel()
					.flatMap(channel -> channel.createEmbed(spec -> {
						spec.setColor(Color.RED);
						spec.setTitle("There is already a context. Use `e.reset` to reset");
					}))
					.subscribe();
			return -Command.SINGLE_SUCCESS;
		}
		EyeEntry entry = EyeEntry.getRandom();
		event.getSource().getMessage().getChannel().flatMap(channel -> channel.createEmbed(spec -> {
			spec.setImage(entry.getImageUrl());
			spec.setTitle("Guess the Person");
			spec.setDescription("Reply to this message with the answer");
			CommandManager.appendFooter(spec);
		})).subscribe(message -> {
			synchronized (GuildSpecificData.LOCK) {
				data.setCurrent(entry);
				data.setMessageId(message.getId());
			}
		});
		return Command.SINGLE_SUCCESS;
	}
}
