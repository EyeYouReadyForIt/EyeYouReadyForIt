package io.github.boogiemonster1o1.eyeyoureadyforit.util;

import discord4j.common.util.Snowflake;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.DataDao;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildStatistic;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.UserStatistic;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public final class StatisticsManager {

    public static Mono initDb(Snowflake guildId) {
        return Mono.fromRunnable(() -> {
            DataSource.get().withExtension(DataDao.class, dao -> {
                dao.createTable(guildId.asString());
                dao.addGuildDataRow(guildId.asString());
                return Mono.empty();
            });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public static Mono<UserStatistic> getUserStats(Snowflake guildId, Snowflake userId) {
        return Mono.fromCallable(() -> DataSource.get().withExtension(DataDao.class, dao ->
            dao.getUserStats(guildId.asString(), userId.asLong())
        )).subscribeOn(Schedulers.boundedElastic());
    }

    public static Mono<GuildStatistic> getGuildStats(Snowflake guildId) {
        return Mono.fromCallable(() -> DataSource.get().withExtension(DataDao.class, dao ->
                dao.getGuildStats(guildId.asString())
        )).subscribeOn(Schedulers.boundedElastic());
    }
}
