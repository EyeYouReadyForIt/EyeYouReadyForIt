package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import discord4j.common.util.Snowflake;

import java.util.HashMap;
import java.util.Map;

public final class GuildSpecificData {
    public static final Object LOCK = new Object() {
    };
    private static final Map<Snowflake, GuildSpecificData> GUILD_SPECIFIC_DATA_MAP = new HashMap<>();
    private final Map<Snowflake, ChannelSpecificData> channelSpecificData = new HashMap<>();
    private final Snowflake guildId;

    private GuildSpecificData(Snowflake guildId) {
        this.guildId = guildId;
    }

    public static GuildSpecificData get(Snowflake guildId) {
        return GUILD_SPECIFIC_DATA_MAP.computeIfAbsent(guildId, GuildSpecificData::new);
    }

    public ChannelSpecificData getChannel(Snowflake channelId) {
        return channelSpecificData.computeIfAbsent(channelId, (id) -> new ChannelSpecificData(id, this));
    }

    public Snowflake getGuildId() {
        return guildId;
    }
}
