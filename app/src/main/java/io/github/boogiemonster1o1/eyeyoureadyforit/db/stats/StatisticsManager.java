package io.github.boogiemonster1o1.eyeyoureadyforit.db.stats;

import discord4j.common.util.Snowflake;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.DataDao;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.GuildStatistics;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.Leaderboard;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.UserStatistics;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.DataSource;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;

public final class StatisticsManager {

	public static Mono initDb(Snowflake guildId) {
		return Mono.fromRunnable(() -> {
			DataSource.get().withExtension(DataDao.class, dao -> {
				dao.createTable(guildId.asString());
				dao.addGuildDataRow(guildId.asString());
				return null;
			});
		}).subscribeOn(Schedulers.boundedElastic());
	}

	public static Mono<UserStatistics> getUserStats(Snowflake guildId, Snowflake userId) {
		return Mono.fromCallable(() -> DataSource.get().withExtension(DataDao.class, dao ->
				dao.getUserStats(guildId.asString(), userId.asLong())
		)).subscribeOn(Schedulers.boundedElastic());
	}

	public static Mono<GuildStatistics> getGuildStats(Snowflake guildId) {
		return Mono.fromCallable(() -> DataSource.get().withExtension(DataDao.class, dao ->
				dao.getGuildStats(guildId.asString())
		)).subscribeOn(Schedulers.boundedElastic());
	}

	public static Mono<ArrayList<Leaderboard>> getLeaderboard(Snowflake guildId) {
		return Mono.fromCallable(() -> DataSource.get().withExtension(DataDao.class, dao ->
				dao.getLeaderboard(guildId.asString())
		)).subscribeOn(Schedulers.boundedElastic());
	}
}
