package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord4j.core.event.domain.message.MessageCreateEvent;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;

import static io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandManager.literal;

public class ResetCommand extends AbstractCommand {
	public ResetCommand(LiteralCommandNode<MessageCreateEvent> node) {
		super(node);
	}

	@Override
	public String getDescription() {
		return "Resets the current context";
	}

	@Override
	public boolean hasDescription() {
		return true;
	}

	public static ResetCommand create(CommandDispatcher<MessageCreateEvent> dispatcher) {
		return new ResetCommand(dispatcher.register(literal("reset").executes(ResetCommand::execute)));
	}

	private static int execute(CommandContext<MessageCreateEvent> event) {
		GuildSpecificData data = App.getGuildSpecificData(event.getSource().getMessage().getGuildId().orElseThrow());
		if (data.getMessageId() != null && data.getCurrent() != null) {
			event.getSource().getMessage().getChannel().flatMap(channel -> channel.createMessage(spec -> {
				spec.setMessageReference(data.getMessageId());
				String content = "The person was **" + data.getCurrent().getName() + "**";
				if (!data.getCurrent().getAliases().isEmpty()) {
					content += "\n" + "Aliases: " + data.getCurrent().getAliases();
				}
				spec.setContent(content);
			})).subscribe();
		}
		data.reset();
		return Command.SINGLE_SUCCESS;
	}
}
