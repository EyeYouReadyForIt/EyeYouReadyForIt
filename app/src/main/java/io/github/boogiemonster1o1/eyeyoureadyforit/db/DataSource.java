package io.github.boogiemonster1o1.eyeyoureadyforit.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.util.Optional;

public final class DataSource {
	private static final Jdbi JDBI;
	private static final HikariConfig CONFIG = new HikariConfig();

	static {
		CONFIG.setJdbcUrl(Optional.ofNullable(System.getenv("EYRFI_DB_URL")).orElseThrow(() -> new RuntimeException("Missing db url")));
		CONFIG.setUsername(Optional.ofNullable(System.getenv("EYRFI_DB_USER")).orElseThrow(() -> new RuntimeException("Missing db username")));
		CONFIG.setPassword(Optional.ofNullable(System.getenv("EYRFI_DB_PASSWORD")).orElseThrow(() -> new RuntimeException("Missing db password")));
		CONFIG.setMaximumPoolSize(6);

		JDBI = Jdbi.create(new HikariDataSource(CONFIG))
				.installPlugin(new SqlObjectPlugin())
				.installPlugin(new PostgresPlugin());
	}

	public static Jdbi get() {
		return JDBI;
	}
}
