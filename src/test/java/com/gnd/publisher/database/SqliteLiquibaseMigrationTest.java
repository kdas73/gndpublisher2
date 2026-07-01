package com.gnd.publisher.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteLiquibaseMigrationTest {

    private static final String CHANGELOG = "db/changelog/db.changelog-master.yml";

    @TempDir
    private Path tempDir;

    @Test
    void appliesSchemaAndSeedsRssSourcesIdempotently() throws Exception {
        Path databaseFile = tempDir.resolve("migration-test.sqlite3");
        String url = "jdbc:sqlite:" + databaseFile;

        try (Connection connection = DriverManager.getConnection(url)) {
            migrate(connection);
        }
        try (Connection connection = DriverManager.getConnection(url)) {
            migrate(connection);
        }

        try (Connection connection = DriverManager.getConnection(url)) {
            assertThat(tableExists(connection, "rss_sources")).isTrue();
            assertThat(tableExists(connection, "news_items")).isTrue();
            assertThat(tableExists(connection, "publications")).isTrue();
            assertThat(indexExists(connection, "idx_news_items_run_source")).isTrue();
            assertThat(indexExists(connection, "uk_rss_sources_url")).isTrue();
            assertThat(indexExists(connection, "uk_publications_event_channel_language")).isTrue();

            assertThat(countRows(connection, "rss_sources")).isEqualTo(2);
            assertThat(countRows(
                    connection,
                    "rss_sources",
                    "url = 'https://feeds.feedburner.com/kathimerini/DJpy' AND language = 'el' AND enabled = 1"))
                    .isEqualTo(1);
            assertThat(countRows(
                    connection,
                    "rss_sources",
                    "url = 'https://www.tanea.gr/feed/' AND language = 'el' AND enabled = 1"))
                    .isEqualTo(1);
        }
    }

    private static void migrate(Connection connection) throws Exception {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
            liquibase.update(new Contexts(), new LabelExpression());
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws Exception {
        return countRows(connection, "sqlite_master", "type = 'table' AND name = '" + tableName + "'") == 1;
    }

    private static boolean indexExists(Connection connection, String indexName) throws Exception {
        return countRows(connection, "sqlite_master", "type = 'index' AND name = '" + indexName + "'") == 1;
    }

    private static int countRows(Connection connection, String tableName) throws Exception {
        return countRows(connection, tableName, "1 = 1");
    }

    private static int countRows(Connection connection, String tableName, String whereClause) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
