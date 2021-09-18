package io.github.boogiemonster1o1.eyeyoureadyforit.db.stats;

import java.util.HashMap;
import java.util.Map;

import discord4j.common.util.Snowflake;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.UserStatistics;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.DataDao;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.DataSource;

public final class TourneyStatisticsTracker {
	//shitcode of the highest order
	// agreed ^ 100%

	private final HashMap<Snowflake, UserStatistics> statsMap = new HashMap<>();
	private int missed;
	private final Snowflake guildId;

	public TourneyStatisticsTracker(Snowflake guildId) {
		this.guildId = guildId;
	}

	public void addCorrect(Snowflake user) {
		statsMap.put(user, statsMap.getOrDefault(user, new UserStatistics()).add(new UserStatistics(1, 0, 0, 0, 0)));
	}

	public void addWrong(Snowflake user) {
		statsMap.put(user, statsMap.getOrDefault(user, new UserStatistics()).add(new UserStatistics(0, 1, 0, 0, 0)));
	}

	public void addHint(Snowflake user) {
		statsMap.put(user, statsMap.getOrDefault(user, new UserStatistics()).add(new UserStatistics(0, 0, 1, 0, 0)));
	}

	public void setWon(Snowflake user) {
		statsMap.put(user, statsMap.getOrDefault(user, new UserStatistics()).add(new UserStatistics(0, 0, 0, 1, 0)));
	}

	public void addMissed() {
		this.missed++;
	}

	public void commit() {
		DataSource.get().withExtension(DataDao.class, dao -> {
			for (Map.Entry<Snowflake, UserStatistics> entry : statsMap.entrySet()) {
				dao.addTourneyUserStats(
						guildId.asString(),
						entry.getKey().asLong(),
						entry.getValue().getCorrectAnswers(),
						entry.getValue().getWrongAnswers(),
						entry.getValue().getHintsUsed(),
						entry.getValue().getGamesWon()
				);
			}

			dao.addTourneyGuildStats(guildId.asString(), missed);
			return null;
		});
	}
}
