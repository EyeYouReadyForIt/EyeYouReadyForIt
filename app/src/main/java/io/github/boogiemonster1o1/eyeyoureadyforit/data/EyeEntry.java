package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.DataDao;
import io.github.boogiemonster1o1.eyeyoureadyforit.db.DataSource;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class EyeEntry implements RowMapper<EyeEntry> {
	private static final Random RANDOM = new Random(ThreadLocalRandom.current().nextLong());
	private static List<EyeEntry> ENTRIES = new ArrayList<>();
	private final String imageUrl;
	private final String name;
	private final String hint;
	private final List<String> aliases;

	public EyeEntry(String name, String imageUrl, String hint, List<String> aliases) {
		this.name = name;
		this.imageUrl = imageUrl;
		this.hint = hint;
		this.aliases = aliases;
	}

	public EyeEntry() {
		this.name = null;
		this.imageUrl = null;
		this.hint = null;
		this.aliases = null;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getName() {
		return name;
	}

	public List<String> getAliases() {
		return aliases;
	}

	public String getHint() {
		return hint;
	}

	@Override
	public String toString() {
		return "EyeEntry{" +
				"imageUrl='" + imageUrl + '\'' +
				", name='" + name + '\'' +
				", hint='" + hint + '\'' +
				", aliases=" + aliases +
				'}';
	}

	public static void reload() {
		ENTRIES = DataSource.get().withExtension(DataDao.class, DataDao::getEyes);
		App.LOGGER.info("Reloading eyes...");
	}

	public static EyeEntry getRandom() {
		return ENTRIES.get(RANDOM.nextInt(ENTRIES.size()));
	}

	@Override
	public EyeEntry map(ResultSet rs, StatementContext ctx) throws SQLException {
		return new EyeEntry(
				rs.getString("name"),
				rs.getString("image_url"),
				rs.getString("hint"),
				Arrays.stream((Object[]) rs.getArray("aliases").getArray()).map(Object::toString).collect(Collectors.toList())
		);
	}
}
