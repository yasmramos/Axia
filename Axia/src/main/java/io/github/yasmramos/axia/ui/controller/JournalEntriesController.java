package io.github.yasmramos.axia.ui.controller;

import io.github.yasmramos.veld.Veld;

import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.service.*;
import io.github.yasmramos.axia.export.ExportService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Journal Entries view.
 *
 * @author Yasmany Ramos Garcia
 */
public class JournalEntriesController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(JournalEntriesController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();

    @FXML private TableView<JournalEntry> entriesTable;
    @FXML private TableColumn<JournalEntry, String> numberColumn;
    @FXML private TableColumn<JournalEntry, String> dateColumn;
    @FXML private TableColumn<JournalEntry, String> descriptionColumn;
    @FXML private TableColumn<JournalEntry, String> referenceColumn;
    @FXML private TableColumn<JournalEntry, String> debitTotalColumn;
    @FXML private TableColumn<JournalEntry, String> creditTotalColumn;
    @FXML private TableColumn<JournalEntry, Void> actionsColumn;

    @FXML private TableView<JournalEntryLine> linesTable;
    @FXML private TableColumn<JournalEntryLine, String> lineAccountColumn;
    @FXML private TableColumn<JournalEntryLine, String> lineDescColumn;
    @FXML private TableColumn<JournalEntryLine, String> lineDebitColumn;
    @FXML private TableColumn<JournalEntryLine, String> lineCreditColumn;

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private TextField searchField;

    @FXML private Label totalEntriesLabel;
    @FXML private Label totalDebitLabel;
    @FXML private Label totalCreditLabel;
    @FXML private Label balanceLabel;

    private final JournalEntryService entryService;
    private final AccountService accountService;
    private final ExportService exportService;
    private ObservableList<JournalEntry> allEntries;
    private FilteredList<JournalEntry> filteredEntries;

    public JournalEntriesController() {
        this.entryService = Veld.get(JournalEntryService.class);
        this.accountService = Veld.get(AccountService.class);
        this.exportService = new ExportService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing JournalEntriesController");
        setupEntriesTable();
        setupLinesTable();
        loadEntries();

        entriesTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> showEntryLines(newVal)
        );
    }

    private void setupEntriesTable() {
        numberColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(String.valueOf(data.getValue().getEntryNumber())));
        dateColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getDate().format(DATE_FORMAT)));
        descriptionColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getDescription()));
        referenceColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getReference()));
        debitTotalColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(formatCurrency(data.getValue().getTotalDebit())));
        creditTotalColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(formatCurrency(data.getValue().getTotalCredit())));

        setupActionsColumn();
    }

    private void setupLinesTable() {
        lineAccountColumn.setCellValueFactory(data -> {
            Account acc = data.getValue().getAccount();
            return new SimpleStringProperty(acc != null ? acc.getCode() + " - " + acc.getName() : "");
        });
        lineDescColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getDescription()));
        lineDebitColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(formatCurrency(data.getValue().getDebit())));
        lineCreditColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(formatCurrency(data.getValue().getCredit())));
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");

            {
                deleteBtn.setOnAction(e -> deleteEntry(getTableView().getItems().get(getIndex())));
                deleteBtn.getStyleClass().add("button-danger");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void loadEntries() {
        List<JournalEntry> entries = entryService.findAll();
        allEntries = FXCollections.observableArrayList(entries);
        filteredEntries = new FilteredList<>(allEntries, p -> true);
        entriesTable.setItems(filteredEntries);
        updateSummary();
        logger.debug("Loaded {} journal entries", entries.size());
    }

    private void showEntryLines(JournalEntry entry) {
        if (entry != null && entry.getLines() != null) {
            linesTable.setItems(FXCollections.observableArrayList(entry.getLines()));
        } else {
            linesTable.getItems().clear();
        }
    }

    @FXML
    public void applyFilter() {
        String searchText = searchField.getText().toLowerCase();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        filteredEntries.setPredicate(entry -> {
            boolean matchesSearch = searchText.isEmpty() ||
                (entry.getDescription() != null && entry.getDescription().toLowerCase().contains(searchText)) ||
                (entry.getReference() != null && entry.getReference().toLowerCase().contains(searchText));
            boolean matchesFrom = fromDate == null || !entry.getDate().isBefore(fromDate);
            boolean matchesTo = toDate == null || !entry.getDate().isAfter(toDate);
            
            return matchesSearch && matchesFrom && matchesTo;
        });

        updateSummary();
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        loadEntries();
    }

    @FXML
    public void showNewEntryDialog() {
        Dialog<JournalEntry> dialog = new Dialog<>();
        dialog.setTitle("New Journal Entry");
        dialog.setResizable(true);

        VBox content = new VBox(10);
        
        // Header fields
        GridPane header = new GridPane();
        header.setHgap(10);
        header.setVgap(10);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField descField = new TextField();
        descField.setPrefWidth(300);
        TextField refField = new TextField();

        header.add(new Label("Date:"), 0, 0);
        header.add(datePicker, 1, 0);
        header.add(new Label("Description:"), 0, 1);
        header.add(descField, 1, 1);
        header.add(new Label("Reference:"), 0, 2);
        header.add(refField, 1, 2);

        // Lines section
        VBox linesSection = new VBox(5);
        Label linesLabel = new Label("Entry Lines:");
        linesLabel.setStyle("-fx-font-weight: bold;");
        
        TableView<LineEntry> linesInput = new TableView<>();
        linesInput.setEditable(true);
        linesInput.setPrefHeight(200);

        ObservableList<LineEntry> lineEntries = FXCollections.observableArrayList();
        lineEntries.add(new LineEntry()); // Start with one empty line
        lineEntries.add(new LineEntry());
        linesInput.setItems(lineEntries);

        TableColumn<LineEntry, String> accCol = new TableColumn<>("Account");
        accCol.setPrefWidth(200);
        TableColumn<LineEntry, String> descCol = new TableColumn<>("Description");
        descCol.setPrefWidth(150);
        TableColumn<LineEntry, String> debitCol = new TableColumn<>("Debit");
        debitCol.setPrefWidth(100);
        TableColumn<LineEntry, String> creditCol = new TableColumn<>("Credit");
        creditCol.setPrefWidth(100);

        linesInput.getColumns().addAll(accCol, descCol, debitCol, creditCol);

        Button addLineBtn = new Button("+ Add Line");
        addLineBtn.setOnAction(e -> lineEntries.add(new LineEntry()));

        linesSection.getChildren().addAll(linesLabel, linesInput, addLineBtn);
        content.getChildren().addAll(header, new Separator(), linesSection);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(600);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    JournalEntry entry = entryService.create(
                        datePicker.getValue(),
                        descField.getText(),
                        refField.getText()
                    );
                    return entry;
                } catch (Exception e) {
                    showError("Error creating entry", e.getMessage());
                }
            }
            return null;
        });

        Optional<JournalEntry> result = dialog.showAndWait();
        if (result.isPresent()) {
            loadEntries();
        }
    }

    private void deleteEntry(JournalEntry entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Entry");
        confirm.setHeaderText("Delete entry #" + entry.getEntryNumber());
        confirm.setContentText("Are you sure? This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                entryService.delete(entry.getId());
                loadEntries();
            } catch (Exception e) {
                showError("Error deleting entry", e.getMessage());
            }
        }
    }

    @FXML
    public void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Journal Entries");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("journal_entries.csv");

        File file = fileChooser.showSaveDialog(entriesTable.getScene().getWindow());
        if (file != null) {
            try {
                exportService.exportJournalEntriesToCsv(allEntries, file.getAbsolutePath());
                showInfo("Export Successful", "Entries exported to " + file.getName());
            } catch (Exception e) {
                showError("Export Error", e.getMessage());
            }
        }
    }

    private void updateSummary() {
        totalEntriesLabel.setText("Total Entries: " + filteredEntries.size());

        BigDecimal totalDebit = filteredEntries.stream()
            .map(JournalEntry::getTotalDebit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = filteredEntries.stream()
            .map(JournalEntry::getTotalCredit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalDebitLabel.setText("Total Debit: " + formatCurrency(totalDebit));
        totalCreditLabel.setText("Total Credit: " + formatCurrency(totalCredit));
        balanceLabel.setText("Balance: " + formatCurrency(totalDebit.subtract(totalCredit)));
    }

    private String formatCurrency(BigDecimal amount) {
        return amount != null ? CURRENCY_FORMAT.format(amount) : "$0.00";
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper class for line entry input
    public static class LineEntry {
        private Account account;
        private String description = "";
        private BigDecimal debit = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;

        public Account getAccount() { return account; }
        public void setAccount(Account account) { this.account = account; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getDebit() { return debit; }
        public void setDebit(BigDecimal debit) { this.debit = debit; }
        public BigDecimal getCredit() { return credit; }
        public void setCredit(BigDecimal credit) { this.credit = credit; }
    }
}
