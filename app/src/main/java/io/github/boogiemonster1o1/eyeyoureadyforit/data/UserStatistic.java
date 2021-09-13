package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class UserStatistic implements RowMapper<UserStatistic> {
	private final int correctAnswers;
	private final int wrongAnswers;
	private final int hintUses;
	private final int gamesWon;
	private final int rank;

	public UserStatistic(int correctAnswers, int wrongAnswers, int hintUses, int gamesWon, int rank) {

		// i had to use all my self control to not make it throw 'thoo'
		if (correctAnswers < 0 || wrongAnswers < 0 || hintUses < 0 || gamesWon < 0 || rank < 0)
			throw new IllegalArgumentException("Statistic values cannot be negative");

		this.correctAnswers = correctAnswers;
		this.wrongAnswers = wrongAnswers;
		this.hintUses = hintUses;
		this.gamesWon = gamesWon;
		this.rank = rank;
	}

	public UserStatistic() {
		this.correctAnswers = 0;
		this.wrongAnswers = 0;
		this.hintUses = 0;
		this.gamesWon = 0;
		this.rank = 0;
	}

	public int getCorrectAnswers() {
		return correctAnswers;
	}

	public int getWrongAnswers() {
		return wrongAnswers;
	}

	public int getHintUses() {
		return hintUses;
	}

	public int getGamesWon() { return gamesWon; }

	public int getRank() { return rank; }

	public UserStatistic add(UserStatistic toAdd) {
		return new UserStatistic(
				this.correctAnswers + toAdd.getCorrectAnswers(),
				this.wrongAnswers + toAdd.getWrongAnswers(),
				this.hintUses + toAdd.getHintUses(),
				this.gamesWon + toAdd.getGamesWon(),
				this.rank
		);
	}

	@Override
	public UserStatistic map(ResultSet rs, StatementContext ctx) throws SQLException {
		return new UserStatistic(
				rs.getInt("correct"),
				rs.getInt("wrong"),
				rs.getInt("hints"),
				rs.getInt("won"),
				rs.getInt("rank")
		);
	}
}
