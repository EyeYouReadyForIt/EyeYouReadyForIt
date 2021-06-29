package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.type.CollectionType;
import io.github.boogiemonster1o1.eyeyoureadyforit.App;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EyeEntry {
	private static List<EyeEntry> ENTRIES = new ArrayList<>();
	private final String imageUrl;
	private final int id;
	private final String name;
	private final String hint;
	private final List<String> aliases;

	@JsonCreator
	public EyeEntry(
			@JsonProperty("imageUrl") String imageUrl,
			@JsonProperty("id") int id,
			@JsonProperty("name") String name,
			@JsonProperty("hint") String hint,
			@JsonProperty("aliases") List<String> aliases
	) {
		this.imageUrl = imageUrl;
		this.id = id;
		this.name = name;
		this.hint = hint;
		this.aliases = aliases;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public int getId() {
		return id;
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
				", id=" + id +
				", name='" + name + '\'' +
				", hint='" + hint + '\'' +
				", aliases=" + aliases +
				'}';
	}

	public static void reload() {
		ENTRIES.clear();
		Path path = Path.of(".", "entries.json");
		CollectionType typeReference = App.OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, EyeEntry.class);
		try {
			ENTRIES = App.OBJECT_MAPPER.readValue(path.toFile(), typeReference);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
