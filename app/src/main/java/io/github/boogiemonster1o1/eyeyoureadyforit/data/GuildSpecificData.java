package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import discord4j.common.util.Snowflake;

public final class GuildSpecificData {
	public static final Object LOCK = new Object(){};
	private final Snowflake guildId;
	private Snowflake messageId;
	private EyeEntry current;

	public GuildSpecificData(Snowflake guildId) {
		this.guildId = guildId;
	}

	public Snowflake getGuildId() {
		return guildId;
	}

	public Snowflake getMessageId() {
		return messageId;
	}

	public void setMessageId(Snowflake messageId) {
		this.messageId = messageId;
	}

	public EyeEntry getCurrent() {
		return current;
	}

	public void setCurrent(EyeEntry current) {
		this.current = current;
	}

	public void reset() {
		synchronized (GuildSpecificData.LOCK) {
			this.setCurrent(null);
			this.setMessageId(null);
		}
	}
}
