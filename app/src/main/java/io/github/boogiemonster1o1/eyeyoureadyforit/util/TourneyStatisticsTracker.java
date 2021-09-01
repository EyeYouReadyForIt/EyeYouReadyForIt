package io.github.boogiemonster1o1.eyeyoureadyforit.util;

import discord4j.common.util.Snowflake;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.Statistic;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class TourneyStatisticsTracker {
    //shitcode of the highest order

    private HashMap<Snowflake, Statistic> statsMap = new HashMap<>();
    private static final Map<Snowflake, TourneyStatisticsTracker> TOURNEY_STATISTICS_TRACKER_MAP = new HashMap<>();
    private int correct;
    private int wrong;
    private int missed;
    private Snowflake guildId;

    public TourneyStatisticsTracker(Snowflake guildId) { this.guildId = guildId; }

    public static TourneyStatisticsTracker get(Snowflake guildId) {
        return TOURNEY_STATISTICS_TRACKER_MAP.computeIfAbsent(guildId, TourneyStatisticsTracker::new);
    }

    public static void reset(Snowflake guildId) {
        TOURNEY_STATISTICS_TRACKER_MAP.put(guildId, new TourneyStatisticsTracker(guildId));
    }

    public void addCorrect(Snowflake user) {
        this.statsMap.put(user, this.statsMap.getOrDefault(user, new Statistic(0, 0, 0)).add(new Statistic(1, 0, 0)));
        this.correct++;
    }

    public void addWrong(Snowflake user) {
        this.statsMap.put(user, this.statsMap.getOrDefault(user, new Statistic(0, 0, 0)).add(new Statistic(0, 1, 0)));
        this.wrong++;
    }

    public void addHint(Snowflake user) {
        this.statsMap.put(user, this.statsMap.getOrDefault(user, new Statistic(0, 0, 0)).add(new Statistic(0, 0, 1)));
    }

    public void addMissed() {
        this.missed++;
    }

    public void commit(String connectionString, String user, String password) {
        try {
            Connection con = DriverManager.getConnection(connectionString, user, password);
            PreparedStatement setData = con.prepareStatement(String.format("INSERT INTO guild_data.data_%s AS d (id, correct, wrong, hints) VALUES (?, 0, 0, 0)\n" +
                    "ON CONFLICT (id) DO UPDATE SET correct = d.correct + ?, wrong = d.wrong + ?, hints = d.hints + ? ", this.guildId.asString()));

            for(Map.Entry<Snowflake, Statistic> entry : statsMap.entrySet()) {
                setData.setLong(1, entry.getKey().asLong());
                setData.setInt(2, entry.getValue().getCorrectAnswers());
                setData.setInt(3, entry.getValue().getWrongAnswers());
                setData.setInt(4, entry.getValue().getHintUses());
                setData.executeUpdate();
            }

            PreparedStatement setGuildData = con.prepareStatement(String.format("UPDATE guild_data.data_%s SET missed = missed + ?, games = games + 1 WHERE id = 0", this.guildId.asString()));
            setGuildData.setInt(1, missed);
            setGuildData.executeUpdate();

            setData.close();
            setGuildData.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
