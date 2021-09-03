package io.github.boogiemonster1o1.eyeyoureadyforit.util;

import discord4j.common.util.Snowflake;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.DataDao;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.Statistic;
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

    public static Mono<Statistic> getUserStats(Snowflake guildId, Snowflake userId) {
        return Mono.fromCallable(() -> DataSource.get().withExtension(DataDao.class, dao ->
            dao.getUserStats(guildId.asString(), userId.asLong())
        )).subscribeOn(Schedulers.boundedElastic());
    }
}
