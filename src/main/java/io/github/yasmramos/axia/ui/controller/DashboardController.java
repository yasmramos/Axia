package io.github.yasmramos.axia.ui.controller;

import io.github.yasmramos.axia.dashboard.DashboardService;
import io.github.yasmramos.axia.model.JournalEntry;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the Dashboard view.
 * Displays KPIs and summary information.
 *
 * @author Yasmany Ramos Garcia
 */
public class DashboardController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();

    @FXML private Label totalAccountsLabel;
    @FXML private Label totalCustomersLabel;
    @FXML private Label totalSuppliersLabel;
    @FXML private Label pendingInvoicesLabel;
    @FXML private Label receivableLabel;
    @FXML private Label payableLabel;
    @FXML private Label netPositionLabel;

    @FXML private TableView<JournalEntry> recentEntriesTable;
    @FXML private TableColumn<JournalEntry, String> dateColumn;
    @FXML private TableColumn<JournalEntry, String> descriptionColumn;
    @FXML private TableColumn<JournalEntry, String> debitColumn;
    @FXML private TableColumn<JournalEntry, String> creditColumn;

    private final DashboardService dashboardService;

    public DashboardController() {
        this.dashboardService = new DashboardService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing DashboardController");
        setupTable();
        loadData();
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getDate().format(DATE_FORMAT)));
        descriptionColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getDescription()));
        debitColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(formatCurrency(data.getValue().getTotalDebit())));
        creditColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(formatCurrency(data.getValue().getTotalCredit())));
    }

    private void loadData() {
        try {
            // Load summary stats
            Map<String, Object> stats = dashboardService.getSummaryStats();
            totalAccountsLabel.setText(String.valueOf(stats.getOrDefault("totalAccounts", 0)));
            totalCustomersLabel.setText(String.valueOf(stats.getOrDefault("totalCustomers", 0)));
            totalSuppliersLabel.setText(String.valueOf(stats.getOrDefault("totalSuppliers", 0)));
            pendingInvoicesLabel.setText(String.valueOf(stats.getOrDefault("pendingInvoices", 0)));

            // Load financial KPIs
            LocalDate now = LocalDate.now();
            Map<String, BigDecimal> kpis = dashboardService.getFinancialKPIs(
                now.withDayOfYear(1), now
            );
            receivableLabel.setText(formatCurrency(kpis.getOrDefault("accountsReceivable", BigDecimal.ZERO)));
            payableLabel.setText(formatCurrency(kpis.getOrDefault("accountsPayable", BigDecimal.ZERO)));
            
            BigDecimal netPosition = kpis.getOrDefault("accountsReceivable", BigDecimal.ZERO)
                .subtract(kpis.getOrDefault("accountsPayable", BigDecimal.ZERO));
            netPositionLabel.setText(formatCurrency(netPosition));

            logger.debug("Dashboard data loaded successfully");
        } catch (Exception e) {
            logger.error("Error loading dashboard data", e);
        }
    }

    @FXML
    public void newJournalEntry() {
        logger.debug("New journal entry requested");
        // TODO: Open journal entry dialog
    }

    @FXML
    public void newInvoice() {
        logger.debug("New invoice requested");
        // TODO: Open invoice dialog
    }

    @FXML
    public void newCustomer() {
        logger.debug("New customer requested");
        // TODO: Open customer dialog
    }

    @FXML
    public void newSupplier() {
        logger.debug("New supplier requested");
        // TODO: Open supplier dialog
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        return CURRENCY_FORMAT.format(amount);
    }
}
