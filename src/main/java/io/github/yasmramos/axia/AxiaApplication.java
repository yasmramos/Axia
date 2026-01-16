package io.github.yasmramos.axia;

import io.github.yasmramos.veld.Veld;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.service.AccountService;
import io.github.yasmramos.axia.service.FiscalYearService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Axia Accounting System - JavaFX Application Entry Point.
 * 
 * <p>Main application class that launches the JavaFX UI and manages
 * the application lifecycle including database initialization and shutdown.
 * 
 * @author Yasmany Ramos Garcia
 * @version 1.0.0
 */
public class AxiaApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(AxiaApplication.class);

    @Override
    public void init() {
        log.info("Initializing Axia Accounting System");
        
        // Initialize database connection
        DatabaseManager.getDatabase();

        // Initialize default data
        AccountService accountService = Veld.get(AccountService.class);
        accountService.initializeDefaultAccounts();

        FiscalYearService fiscalYearService = Veld.get(FiscalYearService.class);
        fiscalYearService.initializeCurrentYear();
        
        log.info("Axia Accounting System initialized successfully");
    }

    @Override
    public void start(Stage primaryStage) {
        log.info("Starting Axia UI");
        
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml"));
            Scene scene = new Scene(root, 1200, 800);
            
            // Load CSS if available
            var cssUrl = getClass().getResource("/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            primaryStage.setTitle("Axia Accounting System");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.show();
            
            log.info("Axia UI started successfully");
        } catch (IOException e) {
            log.error("Failed to load main view", e);
            throw new RuntimeException("Failed to start application", e);
        }
    }

    @Override
    public void stop() {
        log.info("Shutting down Axia Accounting System");
        DatabaseManager.shutdown();
    }

    /**
     * Main entry point.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
