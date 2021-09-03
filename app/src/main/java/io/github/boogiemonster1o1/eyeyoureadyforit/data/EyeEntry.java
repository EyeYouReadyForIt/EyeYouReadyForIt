package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import io.github.boogiemonster1o1.eyeyoureadyforit.App;
import io.github.boogiemonster1o1.eyeyoureadyforit.util.DataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class EyeEntry {
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
}
