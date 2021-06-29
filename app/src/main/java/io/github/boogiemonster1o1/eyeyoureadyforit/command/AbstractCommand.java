package io.github.boogiemonster1o1.eyeyoureadyforit.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import discord4j.core.event.domain.message.MessageCreateEvent;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;

public abstract class AbstractCommand {
	protected final LiteralCommandNode<MessageCreateEvent> node;

	public AbstractCommand(LiteralCommandNode<MessageCreateEvent> node) {
		this.node = node;
	}

	public String getUsage(CommandContext<MessageCreateEvent> ctx) {
		return this.getLiteral() + " " + String.join("\n", App.getCommandManager().getDispatcher().getAllUsage(this.node, ctx.getSource(), false));
	}

	public abstract String getDescription();

	public abstract boolean hasDescription();

	public LiteralCommandNode<MessageCreateEvent> getNode() {
		return this.node;
	}

	public String getLiteral() {
		return this.node.getLiteral();
	}
}
