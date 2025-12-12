package io.github.yasmramos.axia;

import io.github.yasmramos.axia.config.DatabaseConfig;
import io.github.yasmramos.axia.service.AccountService;
import io.github.yasmramos.axia.service.FiscalYearService;

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
        // Initialize database connection
        DatabaseConfig.getDatabase();

        // Initialize default data
        AccountService accountService = new AccountService();
        accountService.initializeDefaultAccounts();

        FiscalYearService fiscalYearService = new FiscalYearService();
        fiscalYearService.initializeCurrentYear();
    }

    /**
     * Shuts down the accounting system.
     * 
     * <p>Releases database connections and resources.
     * Should be called when the application terminates.
     */
    public static void shutdown() {
        DatabaseConfig.shutdown();
    }

    /**
     * Main entry point for standalone execution.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("Initializing Axia Accounting System...");
        
        try {
            initialize();
            System.out.println("System initialized successfully.");
            System.out.println("Chart of accounts and fiscal year created.");
        } catch (Exception e) {
            System.err.println("Initialization error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }
}
