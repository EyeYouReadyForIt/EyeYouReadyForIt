package io.github.boogiemonster1o1.eyeyoureadyforit.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.EyeEntry;
import io.github.boogiemonster1o1.eyeyoureadyforit.data.stats.GuildStatistics;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataSource {
	private static final Jdbi JDBI;
	public static final String DB_URL = Optional.ofNullable(System.getProperty("eyrfi.dbURL")).orElse(Optional.ofNullable(System.getenv("EYRFI_DB_URL")).orElseThrow(() -> new RuntimeException("Missing db url")));
	public static final String DB_USER = Optional.ofNullable(System.getProperty("eyrfi.dbUser")).orElse(Optional.ofNullable(System.getenv("EYRFI_DB_USER")).orElseThrow(() -> new RuntimeException("Missing db username")));
	public static final String DB_PASS = Optional.ofNullable(System.getProperty("eyrfi.dbPassword")).orElse(Optional.ofNullable(System.getenv("EYRFI_DB_PASSWORD")).orElseThrow(() -> new RuntimeException("Missing db password")));
	private static final HikariConfig CONFIG = new HikariConfig();

	static {
		CONFIG.setJdbcUrl(DB_URL);
		CONFIG.setUsername(DB_USER);
		CONFIG.setPassword(DB_PASS);
		CONFIG.setMaximumPoolSize(6);

		JDBI = Jdbi.create(new HikariDataSource(CONFIG))
				.installPlugin(new SqlObjectPlugin())
				.installPlugin(new PostgresPlugin());
	}

	public static Jdbi get() {
		return JDBI;
	}
}
