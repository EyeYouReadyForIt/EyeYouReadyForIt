package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GuildStatistic implements RowMapper<GuildStatistic> {
	private final int missed;
	private final int games;

	public GuildStatistic(int missed, int games) {
		if (missed < 0 || games < 0) throw new IllegalArgumentException("Statistic values cannot be negative");

		this.missed = missed;
		this.games = games;
	}

	public int getMissed() {
		return missed;
	}

	public int getGames() {
		return games;
	}

	@Override
	public GuildStatistic map(ResultSet rs, StatementContext ctx) throws SQLException {
		return new GuildStatistic(rs.getInt("missed"), rs.getInt("games"));
	}
}
