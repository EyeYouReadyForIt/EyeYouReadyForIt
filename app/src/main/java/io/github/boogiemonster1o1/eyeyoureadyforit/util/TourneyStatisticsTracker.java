package io.github.boogiemonster1o1.eyeyoureadyforit.util;

import discord4j.common.util.Snowflake;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.DataDao;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.Statistic;

import java.util.HashMap;
import java.util.Map;

public final class TourneyStatisticsTracker {
    //shitcode of the highest order

    private final HashMap<Snowflake, Statistic> statsMap = new HashMap<>();
    private static final Map<Snowflake, TourneyStatisticsTracker> TOURNEY_STATISTICS_TRACKER_MAP = new HashMap<>();
    private int missed;
    private final Snowflake guildId;
    private final Snowflake channelId;

    public TourneyStatisticsTracker(Snowflake guildId, Snowflake channelId) {
        this.guildId = guildId;
        this.channelId = channelId;
    }

    public static TourneyStatisticsTracker get(Snowflake guildId, Snowflake channelId) {
        return TOURNEY_STATISTICS_TRACKER_MAP.computeIfAbsent(channelId, );
    }

    public static void reset(Snowflake guildId) {
        TOURNEY_STATISTICS_TRACKER_MAP.put(guildId, new TourneyStatisticsTracker(guildId));
    }

    public void addCorrect(Snowflake user) {
        statsMap.put(user, statsMap.getOrDefault(user, new Statistic()).add(new Statistic(1, 0, 0)));
    }

    public void addWrong(Snowflake user) {
        statsMap.put(user, statsMap.getOrDefault(user, new Statistic()).add(new Statistic(0, 1, 0)));
    }

    public void addHint(Snowflake user) {
        statsMap.put(user, statsMap.getOrDefault(user, new Statistic()).add(new Statistic(0, 0, 1)));
    }

    public void addMissed() {
        this.missed++;
    }

    public void commit() {
        DataSource.get().withExtension(DataDao.class, dao -> {
            for(Map.Entry<Snowflake, Statistic> entry : statsMap.entrySet()){
                dao.addTourneyUserStats(
                        channelId.asString(),
                        entry.getKey().asLong(),
                        entry.getValue().getCorrectAnswers(),
                        entry.getValue().getWrongAnswers(),
                        entry.getValue().getHintUses()
                );
            }

            dao.addTourneyGuildStats(guildId.asString(), missed);
            return null;
        });
    }
}
