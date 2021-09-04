package io.github.boogiemonster1o1.eyeyoureadyforit.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.GuildStatistic;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataSource {

    private static final Jdbi jdbi;
    private static HikariConfig config = new HikariConfig();

    static {
        config.setJdbcUrl(Optional.ofNullable(System.getProperty("eyrfi.dbURL")).orElse(Optional.ofNullable(System.getenv("EYRFI_DB_URL")).orElseThrow(() -> new RuntimeException("Missing db url"))));
        config.setUsername(Optional.ofNullable(System.getProperty("eyrfi.dbUser")).orElse(Optional.ofNullable(System.getenv("EYRFI_DB_USER")).orElseThrow(() -> new RuntimeException("Missing db username"))));
        config.setPassword(Optional.ofNullable(System.getProperty("eyrfi.dbPassword")).orElse(Optional.ofNullable(System.getenv("EYRFI_DB_PASSWORD")).orElseThrow(() -> new RuntimeException("Missing db password"))));
        config.setMaximumPoolSize(6);

        jdbi = Jdbi.create(new HikariDataSource(config))
                .installPlugin(new SqlObjectPlugin())
                .installPlugin(new PostgresPlugin())
                .registerRowMapper(EyeEntry.class, (rs, ctx) -> new EyeEntry(
                        rs.getString("name"),
                        rs.getString("image_url"),
                        rs.getString("hint"),
                        Arrays.stream((Object[]) rs.getArray("aliases").getArray()).map(Object::toString).collect(Collectors.toList())
                ))
                .registerRowMapper(GuildStatistic.class, (rs, ctx) -> new GuildStatistic(
                        rs.getInt("missed"),
                        rs.getInt("games")
                ));
    }

    public static Jdbi get() {
        return jdbi;
    }
}