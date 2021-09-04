package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.ArrayList;

public interface DataDao {

    @SqlQuery("SELECT * FROM eyes_entries")
    ArrayList<EyeEntry> getEyes();

    @SqlQuery("SELECT * FROM guild_data.data_<table> WHERE id = :id")
    @RegisterRowMapper(UserStatistic.class)
    UserStatistic getUserStats(@Define("table") String guildId, @Bind("id") long userId);

    @SqlQuery("SELECT * FROM guild_data.data_<table> WHERE id = 0")
    GuildStatistic getGuildStats(@Define("table") String guildId);

    @SqlUpdate(
            "CREATE TABLE IF NOT EXISTS guild_data.data_<table> (\n" +
                    "id BIGINT PRIMARY KEY,\n" +
                    "correct INTEGER,\n" +
                    "wrong INTEGER,\n" +
                    "hints INTEGER,\n" +
                    "missed INTEGER,\n" +
                    "games INTEGER);")
    void createTable(@Define("table") String guildId);

    @SqlUpdate("INSERT INTO guild_data.data_<table> (id, missed, games) VALUES (0, 0, 0) ON CONFLICT DO NOTHING")
    void addGuildDataRow(@Define("table") String guildId);

    @SqlUpdate("INSERT INTO guild_data.data_<table> AS d (id, correct, wrong, hints) VALUES (:id, :correct, :wrong, :hints)\n" +
            "ON CONFLICT (id) DO UPDATE SET correct = d.correct + :correct, wrong = d.wrong + :wrong, hints = d.hints + :hints")
    void addTourneyUserStats(
            @Define("table") String guildId,
            @Bind("id") long userId,
            @Bind("correct") int correct,
            @Bind("wrong") int wrong,
            @Bind("hints") int hints
    );

    @SqlUpdate("UPDATE guild_data.data_<table> SET missed = missed + :missed, games = games + 1 WHERE id = 0")
    void addTourneyGuildStats(@Define("table") String guildId, @Bind("missed") int missed);

}
