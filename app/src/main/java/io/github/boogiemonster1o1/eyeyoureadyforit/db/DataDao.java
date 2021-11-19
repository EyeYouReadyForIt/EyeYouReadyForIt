package io.github.boogiemonster1o1.eyeyoureadyforit.db;

import java.util.ArrayList;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.GuildStatistics;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.Leaderboard;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.UserStatistics;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface DataDao {
	@SqlQuery("SELECT * FROM eyes_entries")
	@RegisterRowMapper(EyeEntry.class)
	ArrayList<EyeEntry> getEyes();

	@SqlQuery(
			"SELECT * FROM (" +
					"SELECT id," +
					"correct," +
					"wrong," +
					"hints," +
					"won," +
					"RANK () OVER (ORDER BY won DESC) rank " +
					"FROM guild_data.data_<table>" +
					") WHERE id = :id")
	@RegisterRowMapper(UserStatistics.class)
	UserStatistics getUserStats(@Define("table") String guildId, @Bind("id") long userId);

	@SqlQuery("SELECT * FROM guild_data.data_<table> WHERE id = 0")
	@RegisterRowMapper(GuildStatistics.class)
	GuildStatistics getGuildStats(@Define("table") String guildId);

	@SqlQuery("SELECT TRIM(LEADING 'data_' FROM TABLE_NAME) FROM information_schema.tables \n" +
			"WHERE table_schema LIKE 'guild_data' \n" +
			"AND TABLE_NAME LIKE 'data\\_%'")
	ArrayList<String> getCurrentGuilds();

	@SqlQuery(
			"SELECT id, " +
					"won," +
					"RANK () OVER (ORDER BY won DESC) rank " +
					"FROM guild_data.data_<table> " +
					"LIMIT 3")
	@RegisterRowMapper(Leaderboard.class)
	ArrayList<Leaderboard> getLeaderboard(@Define("table") String guildId);

	@SqlUpdate(
			"CREATE TABLE IF NOT EXISTS guild_data.data_<table> (\n" +
					"id BIGINT PRIMARY KEY,\n" +
					"correct INTEGER,\n" +
					"wrong INTEGER,\n" +
					"hints INTEGER,\n" +
					"won INTEGER,\n" +
					"missed INTEGER,\n" +
					"games INTEGER);")
	void createTable(@Define("table") String guildId);

	@SqlUpdate("INSERT INTO guild_data.data_<table> (id, missed, games) VALUES (0, 0, 0) ON CONFLICT DO NOTHING")
	void addGuildDataRow(@Define("table") String guildId);

	@SqlUpdate("INSERT INTO guild_data.data_<table> AS d (id, correct, wrong, hints, won) " +
			"VALUES (:id, :correct, :wrong, :hints, :won)\n" +
			"ON CONFLICT (id) DO UPDATE " +
			"SET correct = d.correct + :correct, " +
			"wrong = d.wrong + :wrong, " +
			"hints = d.hints + :hints, " +
			"won = d.won + :won")
	void addTourneyUserStats(
			@Define("table") String guildId,
			@Bind("id") long userId,
			@Bind("correct") int correct,
			@Bind("wrong") int wrong,
			@Bind("hints") int hints,
			@Bind("won") int won
	);

	@SqlUpdate("UPDATE guild_data.data_<table> SET missed = missed + :missed, games = games + 1 WHERE id = 0")
	void addTourneyGuildStats(@Define("table") String guildId, @Bind("missed") int missed);
}
