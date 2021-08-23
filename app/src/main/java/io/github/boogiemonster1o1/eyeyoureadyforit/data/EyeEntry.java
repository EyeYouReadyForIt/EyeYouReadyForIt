package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class EyeEntry {
	private static final Random RANDOM = new Random(ThreadLocalRandom.current().nextLong());
	private static final List<EyeEntry> ENTRIES = new ArrayList<>();
	private final String imageUrl;
	private final String name;
	private final String hint;
	private final List<String> aliases;

	@JsonCreator
	public EyeEntry(
			@JsonProperty("imageUrl") String imageUrl,
			@JsonProperty("name") String name,
			@JsonProperty("hint") String hint,
			@JsonProperty("aliases") List<String> aliases
	) {
		this.imageUrl = imageUrl;
		this.name = name;
		this.hint = hint;
		this.aliases = aliases;
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

	public static void reload(String connectionString, String user, String password) {
		Path dbDir = Path.of(".", "db");
		String s = "SELECT * FROM eyes_entries";

		try (Connection conn = DriverManager.getConnection("jdbc:" + connectionString, user, password)) {
			try (Statement statement = conn.createStatement()) {
				try (ResultSet set = statement.executeQuery(s)) {
					ENTRIES.clear();
					while (set.next()) {
						String name = set.getString("name");
						String imageUrl = set.getString("image_url");
						String hint = set.getString("hint");
						List<String> aliases = Arrays.stream((Object[]) set.getArray("aliases").getArray()).map(Object::toString).collect(Collectors.toList());
						ENTRIES.add(new EyeEntry(imageUrl, name, hint, aliases));
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static EyeEntry getRandom() {
		return ENTRIES.get(RANDOM.nextInt(ENTRIES.size()));
	}
}
