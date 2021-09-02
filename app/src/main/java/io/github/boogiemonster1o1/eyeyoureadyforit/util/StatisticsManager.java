package io.github.boogiemonster1o1.eyeyoureadyforit.util;

import discord4j.common.util.Snowflake;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.DataDao;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.Statistic;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StatisticsManager {

    public static Mono initDb(Snowflake guildId) {
        return Mono.fromRunnable(() -> {
            DataSource.get().withExtension(DataDao.class, dao -> {
                dao.createTable(guildId.asString());
                dao.addGuildDataRow(guildId.asString());
                return null;
            });
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
