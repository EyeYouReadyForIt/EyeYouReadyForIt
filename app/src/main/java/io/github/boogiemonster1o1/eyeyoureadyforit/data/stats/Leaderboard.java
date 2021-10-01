package io.github.boogiemonster1o1.eyeyoureadyforit.data.stats;

import discord4j.common.util.Snowflake;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class Leaderboard implements RowMapper<Leaderboard> {
	private final Snowflake id;
	private final int gamesWon;
	private final int rank;

	public Leaderboard(Snowflake id, int gamesWon, int rank) {
		this.id = id;
		this.gamesWon = gamesWon;
		this.rank = rank;
	}

	public Leaderboard() {
		this.id = Snowflake.of(0);
		this.gamesWon = 0;
		this.rank = 0;
	}

	public Snowflake getId() {
		return id;
	}

	public int getGamesWon() {
		return gamesWon;
	}

	public int getRank() {
		return rank;
	}

	@Override
	public Leaderboard map(ResultSet rs, StatementContext ctx) throws SQLException {
		return new Leaderboard(
				Snowflake.of(rs.getLong("id")),
				rs.getInt("won"),
				rs.getInt("rank")
		);
	}
}
