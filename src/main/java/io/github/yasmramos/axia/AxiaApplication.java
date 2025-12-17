package io.github.yasmramos.axia;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.service.AccountService;
import io.github.yasmramos.axia.service.FiscalYearService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Axia Accounting System - Main Application Entry Point.
 * 
 * <p>Provides initialization and shutdown methods for the accounting system.
 * On first run, creates the default chart of accounts and current fiscal year.
 * 
 * <p>Usage with JavaFX:
 * <pre>
 * public class MainApp extends Application {
 *     &#64;Override
 *     public void init() {
 *         AxiaApplication.initialize();
 *     }
 *     
 *     &#64;Override
 *     public void stop() {
 *         AxiaApplication.shutdown();
 *     }
 * }
 * </pre>
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
public class AxiaApplication {

    private static final Logger log = LoggerFactory.getLogger(AxiaApplication.class);

    /**
     * Initializes the accounting system.
     * 
     * <p>Performs the following:
     * <ul>
     *   <li>Establishes database connection</li>
     *   <li>Creates default chart of accounts (if not exists)</li>
     *   <li>Creates current fiscal year (if not exists)</li>
     * </ul>
     */
    public static void initialize() {
        log.info("Initializing Axia Accounting System");
        
        // Initialize database connection
        DatabaseManager.getDatabase();

        // Initialize default data
        AccountService accountService = new AccountService();
        accountService.initializeDefaultAccounts();

        FiscalYearService fiscalYearService = new FiscalYearService();
        fiscalYearService.initializeCurrentYear();
        
        log.info("Axia Accounting System initialized successfully");
    }

    /**
     * Shuts down the accounting system.
     * 
     * <p>Releases database connections and resources.
     * Should be called when the application terminates.
     */
    public static void shutdown() {
        log.info("Shutting down Axia Accounting System");
        DatabaseManager.shutdown();
    }

    /**
     * Main entry point for standalone execution.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        log.info("Starting Axia Accounting System...");
        
        try {
            initialize();
            log.info("System ready");
        } catch (Exception e) {
            log.error("Initialization error: {}", e.getMessage(), e);
        } finally {
            shutdown();
        }
    }
}
