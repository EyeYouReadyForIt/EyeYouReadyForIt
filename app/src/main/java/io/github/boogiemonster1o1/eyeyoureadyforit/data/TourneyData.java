package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import java.util.ArrayList;
import java.util.List;

public class TourneyData {
	private int round = 1;
	private final int maxRounds;
	private final ArrayList<Long> leaderboard;
	private final boolean disableHints;
	private final boolean disableFirstNames;

	public TourneyData(int maxRounds, boolean disableHints, boolean disableFirstNames) {
		this.maxRounds = maxRounds;
		this.leaderboard = new ArrayList<>();
		this.disableHints = disableHints;
		this.disableFirstNames = disableFirstNames;
	}

	public int getRound() {
		return round;
	}

	public int getMaxRounds() {
		return maxRounds;
	}

	public List<Long> getLeaderboard() {
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
