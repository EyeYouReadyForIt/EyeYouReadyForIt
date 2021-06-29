package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord4j.core.event.domain.message.MessageCreateEvent;

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
		LiteralCommandNode<MessageCreateEvent> node;
		node = dispatcher.register(
				literal("eyes")
		);
		return new EyeCommand(node);
	}
}
