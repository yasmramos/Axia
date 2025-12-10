package com.axia.config;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;

public class DatabaseConfig {

    private static Database database;

    private DatabaseConfig() {}

    public static synchronized Database getDatabase() {
        if (database == null) {
            io.ebean.config.DatabaseConfig config = new io.ebean.config.DatabaseConfig();
            config.setName("db");
            config.loadFromProperties();
            config.setDefaultServer(true);
            config.setRegister(true);

            database = DatabaseFactory.create(config);
        }
        return database;
    }

    public static void shutdown() {
        if (database != null) {
            database.shutdown();
            database = null;
        }
    }
}
