package io.github.yasmramos.axia;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;

/**
 * JUnit 5 extension that starts an embedded PostgreSQL database before tests.
 */
public class EmbeddedPostgresExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    
    private static EmbeddedPostgres embeddedPostgres;
    private static boolean started = false;
    
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!started) {
            started = true;
            startEmbeddedPostgres();
            context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).put("embedded-postgres", this);
        }
    }
    
    private void startEmbeddedPostgres() throws IOException {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setPort(5432)
            .start();
        
        // Set system properties for Ebean to use
        System.setProperty("datasource.db.username", "postgres");
        System.setProperty("datasource.db.password", "postgres");
        System.setProperty("datasource.db.url", embeddedPostgres.getJdbcUrl("postgres", "postgres"));
    }
    
    @Override
    public void close() throws Throwable {
        if (embeddedPostgres != null) {
            embeddedPostgres.close();
        }
    }
    
    public static String getJdbcUrl() {
        return embeddedPostgres != null ? embeddedPostgres.getJdbcUrl("postgres", "postgres") : null;
    }
}
