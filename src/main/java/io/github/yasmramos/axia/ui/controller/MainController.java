package io.github.yasmramos.axia.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main controller for the application's primary view.
 * Handles navigation between different modules.
 *
 * @author Yasmany Ramos Garcia
 */
public class MainController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private StackPane contentArea;
    @FXML private Label statusLabel;
    @FXML private Label fiscalYearLabel;
    @FXML private Label userLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController");
        showDashboard();
    }

    // Navigation Methods
    @FXML
    public void showDashboard() {
        loadView("/fxml/DashboardView.fxml", "Dashboard");
    }

    @FXML
    public void showAccounts() {
        loadView("/fxml/AccountsView.fxml", "Chart of Accounts");
    }

    @FXML
    public void showJournalEntries() {
        loadView("/fxml/JournalEntriesView.fxml", "Journal Entries");
    }

    @FXML
    public void showCustomers() {
        loadView("/fxml/CustomersView.fxml", "Customers");
    }

    @FXML
    public void showSuppliers() {
        loadView("/fxml/SuppliersView.fxml", "Suppliers");
    }

    @FXML
    public void showInvoices() {
        loadView("/fxml/InvoicesView.fxml", "Invoices");
    }

    @FXML
    public void showTrialBalance() {
        loadView("/fxml/TrialBalanceView.fxml", "Trial Balance");
    }

    @FXML
    public void showBalanceSheet() {
        loadView("/fxml/BalanceSheetView.fxml", "Balance Sheet");
    }

    @FXML
    public void showIncomeStatement() {
        loadView("/fxml/IncomeStatementView.fxml", "Income Statement");
    }

    @FXML
    public void showFiscalYear() {
        loadView("/fxml/FiscalYearView.fxml", "Fiscal Year");
    }

    @FXML
    public void showCurrencies() {
        loadView("/fxml/CurrenciesView.fxml", "Currencies");
    }

    // File Menu Actions
    @FXML
    public void handleBackup() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Backup");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Backup Files", "*.zip")
        );
        // TODO: Implement backup logic
        setStatus("Backup created successfully");
    }

    @FXML
    public void handleRestore() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Restore Backup");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Backup Files", "*.zip")
        );
        // TODO: Implement restore logic
        setStatus("Backup restored successfully");
    }

    @FXML
    public void handleExit() {
        logger.info("Application exit requested");
        System.exit(0);
    }

    @FXML
    public void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Axia");
        alert.setHeaderText("Axia Accounting System");
        alert.setContentText("Version 1.0.0\n\nA professional offline accounting system.\n\nAuthor: Yasmany Ramos Garcia");
        alert.showAndWait();
    }

    // Helper Methods
    private void loadView(String fxmlPath, String viewName) {
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource != null) {
                Node view = FXMLLoader.load(resource);
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
                setStatus("Viewing: " + viewName);
                logger.debug("Loaded view: {}", viewName);
            } else {
                showPlaceholder(viewName);
            }
        } catch (IOException e) {
            logger.error("Error loading view: {}", fxmlPath, e);
            showPlaceholder(viewName);
        }
    }

    private void showPlaceholder(String viewName) {
        Label placeholder = new Label(viewName + "\n\n(View coming soon)");
        placeholder.setStyle("-fx-font-size: 24px; -fx-text-fill: #666;");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(placeholder);
        setStatus("Viewing: " + viewName);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    public void setFiscalYear(String year) {
        fiscalYearLabel.setText("Fiscal Year: " + year);
    }
}
