package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import java.util.HashMap;
import java.util.Map;

import discord4j.common.util.Snowflake;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;

public final class GuildSpecificData {
	public static final Object LOCK = new Object(){};
	private static final Map<Snowflake, GuildSpecificData> GUILD_SPECIFIC_DATA_MAP = new HashMap<>();
	private final Snowflake guildId;
	private Snowflake messageId;
	private EyeEntry current;
	private TourneyData tourneyData = null;

	private GuildSpecificData(Snowflake guildId) {
		this.guildId = guildId;
	}

	public static GuildSpecificData get(Snowflake guildId) {
		return GUILD_SPECIFIC_DATA_MAP.computeIfAbsent(guildId, GuildSpecificData::new);
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

	public boolean isTourney() {
		return this.tourneyData != null;
	}

	public void setTourneyData(TourneyData tourneyData) {
		this.tourneyData = tourneyData;
	}

	public TourneyData getTourneyData() {
		return tourneyData;
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
