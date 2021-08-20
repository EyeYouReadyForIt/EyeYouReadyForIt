package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import java.util.Arrays;

public class TourneyData {
	private int round = 1;
	private final int maxRounds;
	private final long[] leaderboard;
	private final boolean disableHints;
	private boolean disableFirstNames;

	public TourneyData(int maxRounds, boolean disableHints, boolean disableFirstNames) {
		this.maxRounds = maxRounds;
		this.leaderboard = new long[maxRounds];
		this.disableHints = disableHints;
		this.disableFirstNames = disableFirstNames;
		Arrays.fill(leaderboard, 0L);
	}

	public int getRound() {
		return round;
	}

	public int getMaxRounds() {
		return maxRounds;
	}

	public long[] getLeaderboard() {
		return leaderboard;
	}

	public void setRound(int round) {
		this.round = round;
	}

	public boolean shouldDisableHints() {
		return disableHints;
	}

	public boolean shouldDisableFirstNames() {
		return disableFirstNames;
	}
}
