package io.github.boogiemonster1o1.eyeyoureadyforit.data.stats;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public final class UserStatistics implements RowMapper<UserStatistics> {
	private final int correctAnswers;
	private final int wrongAnswers;
	private final int hintsUsed;
	private final int gamesWon;
	private final int rank;

	public UserStatistics(int correctAnswers, int wrongAnswers, int hintUses, int gamesWon, int rank) {

		// i had to use all my self control to not make it throw 'thoo'
		if (correctAnswers < 0 || wrongAnswers < 0 || hintUses < 0 || gamesWon < 0 || rank < 0)
			throw new IllegalArgumentException("Statistic values cannot be negative");

		this.correctAnswers = correctAnswers;
		this.wrongAnswers = wrongAnswers;
		this.hintsUsed = hintUses;
		this.gamesWon = gamesWon;
		this.rank = rank;
	}

	public UserStatistics() {
		this.correctAnswers = 0;
		this.wrongAnswers = 0;
		this.hintsUsed = 0;
		this.gamesWon = 0;
		this.rank = 0;
	}

	public int getCorrectAnswers() {
		return correctAnswers;
	}

	public int getWrongAnswers() {
		return wrongAnswers;
	}

	public int getHintsUsed() {
		return hintsUsed;
	}

	public int getGamesWon() {
		return gamesWon;
	}

	public int getRank() {
		return rank;
	}

	public UserStatistics add(UserStatistics toAdd) {
		return new UserStatistics(
				this.correctAnswers + toAdd.getCorrectAnswers(),
				this.wrongAnswers + toAdd.getWrongAnswers(),
				this.hintsUsed + toAdd.getHintsUsed(),
				this.gamesWon + toAdd.getGamesWon(),
				this.rank
		);
	}

	@Override
	public UserStatistics map(ResultSet rs, StatementContext ctx) throws SQLException {
		return new UserStatistics(
				rs.getInt("correct"),
				rs.getInt("wrong"),
				rs.getInt("hints"),
				rs.getInt("won"),
				rs.getInt("rank")
		);
	}
}
