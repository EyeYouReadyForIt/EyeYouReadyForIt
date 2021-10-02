package io.github.boogiemonster1o1.eyeyoureadyforit.data.stats;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public final class GuildStatistics implements RowMapper<GuildStatistics> {
	private final int missed;
	private final int games;

	public GuildStatistics(int missed, int games) {
		if (missed < 0 || games < 0) throw new IllegalArgumentException("Statistic values cannot be negative");

		this.missed = missed;
		this.games = games;
	}

	public GuildStatistics() {
		this.missed = 0;
		this.games = 0;
	}

	public int getMissed() {
		return missed;
	}

	public int getGames() {
		return games;
	}

	@Override
	public GuildStatistics map(ResultSet rs, StatementContext ctx) throws SQLException {
		return new GuildStatistics(rs.getInt("missed"), rs.getInt("games"));
	}
}
