package io.github.boogiemonster1o1.eyeyoureadyforit.Util;

import discord4j.common.util.Snowflake;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StatisticsManager {
    public static void initDb(Snowflake guildId, String connectionString, String user, String password) {
        try {
            Connection con = DriverManager.getConnection(connectionString, user, password);
            PreparedStatement createTable = con.prepareStatement(String.format("CREATE TABLE IF NOT EXISTS guild_data.data_%s (\n" +
                    "id BIGINT PRIMARY KEY,\n" +
                    "correct INTEGER,\n" +
                    "wrong INTEGER,\n" +
                    "hints INTEGER,\n" +
                    "missed INTEGER,\n" +
                    "games INTEGER);", guildId.asString()));
            createTable.executeUpdate();
            createTable.close();

            PreparedStatement addGuildRow = con.prepareStatement(String.format("INSERT INTO guild_data.data_%s (id, missed, games) VALUES (0, 0, 0) ON CONFLICT DO NOTHING", guildId.asString()));
            addGuildRow.executeUpdate();
            addGuildRow.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
