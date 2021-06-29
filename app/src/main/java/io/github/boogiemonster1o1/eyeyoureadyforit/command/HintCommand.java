package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildSpecificData;

import static io.github.boogiemonster1o1.eyeyoureadyforit.command.CommandManager.literal;

public class HintCommand extends AbstractCommand {
	public HintCommand(LiteralCommandNode<MessageCreateEvent> node) {
		super(node);
	}

	@Override
	public String getDescription() {
		return "Gives a hint";
	}

	@Override
	public boolean hasDescription() {
		return true;
	}

	public static HintCommand create(CommandDispatcher<MessageCreateEvent> dispatcher) {
		return new HintCommand(dispatcher.register(literal("hint").executes(HintCommand::execute)));
	}

	private static int execute(CommandContext<MessageCreateEvent> event) {
		GuildSpecificData data = App.getGuildSpecificData(event.getSource().getMessage().getGuildId().orElseThrow());
		if (data.getCurrent() == null) {
			event.getSource().getMessage().getChannel()
					.flatMap(channel -> channel.createEmbed(spec -> {
						spec.setColor(Color.RED);
						spec.setTitle("No Context Available. Start using `e.eyes`");
					}))
					.subscribe();
			return -Command.SINGLE_SUCCESS;
		}
		event.getSource().getMessage().getChannel().flatMap(channel -> {
			return channel.createMessage(spec -> {
				spec.setMessageReference(event.getSource().getMessage().getId());
				spec.setContent(data.getCurrent().getHint());
			});
		}).subscribe();
		return Command.SINGLE_SUCCESS;
	}
}
