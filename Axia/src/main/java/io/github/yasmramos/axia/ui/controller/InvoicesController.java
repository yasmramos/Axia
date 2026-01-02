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
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Invoices view.
 *
 * @author Yasmany Ramos Garcia
 */
public class InvoicesController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(InvoicesController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();

    @FXML private TableView<Invoice> invoicesTable;
    @FXML private TableColumn<Invoice, String> numberColumn;
    @FXML private TableColumn<Invoice, String> dateColumn;
    @FXML private TableColumn<Invoice, String> dueDateColumn;
    @FXML private TableColumn<Invoice, String> typeColumn;
    @FXML private TableColumn<Invoice, String> partnerColumn;
    @FXML private TableColumn<Invoice, String> subtotalColumn;
    @FXML private TableColumn<Invoice, String> taxColumn;
    @FXML private TableColumn<Invoice, String> totalColumn;
    @FXML private TableColumn<Invoice, String> statusColumn;
    @FXML private TableColumn<Invoice, Void> actionsColumn;

    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField searchField;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;

    @FXML private Label totalInvoicesLabel;
    @FXML private Label totalSalesLabel;
    @FXML private Label totalPurchasesLabel;
    @FXML private Label pendingLabel;

    private final InvoiceService invoiceService;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final ExportService exportService;
    private ObservableList<Invoice> allInvoices;
    private FilteredList<Invoice> filteredInvoices;

    public InvoicesController() {
        this.invoiceService = Veld.get(InvoiceService.class);
        this.customerService = Veld.get(CustomerService.class);
        this.supplierService = Veld.get(SupplierService.class);
        this.exportService = new ExportService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing InvoicesController");
        setupFilters();
        setupTable();
        loadInvoices();
    }

    private void setupFilters() {
        typeFilterCombo.getItems().addAll("All", "SALES", "PURCHASE");
        typeFilterCombo.getSelectionModel().selectFirst();

        statusFilterCombo.getItems().add("All");
        for (InvoiceStatus status : InvoiceStatus.values()) {
            statusFilterCombo.getItems().add(status.toString());
        }
        statusFilterCombo.getSelectionModel().selectFirst();
    }

    private void setupTable() {
        numberColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getInvoiceNumber()));
        dateColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getDate().format(DATE_FORMAT)));
        dueDateColumn.setCellValueFactory(data -> {
            LocalDate due = data.getValue().getDueDate();
            return new SimpleStringProperty(due != null ? due.format(DATE_FORMAT) : "");
        });
        typeColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getType().toString()));
        partnerColumn.setCellValueFactory(data -> {
            Invoice inv = data.getValue();
            if (inv.getCustomer() != null) return new SimpleStringProperty(inv.getCustomer().getName());
            if (inv.getSupplier() != null) return new SimpleStringProperty(inv.getSupplier().getName());
            return new SimpleStringProperty("");
        });
        subtotalColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(formatCurrency(data.getValue().getSubtotal())));
        taxColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(formatCurrency(data.getValue().getTaxAmount())));
        totalColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(formatCurrency(data.getValue().getTotal())));
        statusColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getStatus().toString()));

        setupActionsColumn();
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button payBtn = new Button("Pay");
            private final Button deleteBtn = new Button("Del");
            private final HBox container = new HBox(3, viewBtn, payBtn, deleteBtn);

            {
                viewBtn.setOnAction(e -> viewInvoice(getTableView().getItems().get(getIndex())));
                payBtn.setOnAction(e -> markAsPaid(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteInvoice(getTableView().getItems().get(getIndex())));
                deleteBtn.getStyleClass().add("button-danger");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void loadInvoices() {
        List<Invoice> invoices = invoiceService.findAll();
        allInvoices = FXCollections.observableArrayList(invoices);
        filteredInvoices = new FilteredList<>(allInvoices, p -> true);
        invoicesTable.setItems(filteredInvoices);
        updateSummary();
        logger.debug("Loaded {} invoices", invoices.size());
    }

    @FXML
    public void applyFilter() {
        String typeFilter = typeFilterCombo.getValue();
        String statusFilter = statusFilterCombo.getValue();
        String searchText = searchField.getText().toLowerCase();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        filteredInvoices.setPredicate(invoice -> {
            boolean matchesType = "All".equals(typeFilter) || 
                invoice.getType().toString().equals(typeFilter);
            boolean matchesStatus = "All".equals(statusFilter) || 
                invoice.getStatus().toString().equals(statusFilter);
            boolean matchesSearch = searchText.isEmpty() ||
                invoice.getInvoiceNumber().toLowerCase().contains(searchText);
            boolean matchesFrom = fromDate == null || 
                !invoice.getDate().isBefore(fromDate);
            boolean matchesTo = toDate == null || 
                !invoice.getDate().isAfter(toDate);
            
            return matchesType && matchesStatus && matchesSearch && matchesFrom && matchesTo;
        });

        updateSummary();
    }

    @FXML
    public void clearFilters() {
        typeFilterCombo.getSelectionModel().selectFirst();
        statusFilterCombo.getSelectionModel().selectFirst();
        searchField.clear();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        loadInvoices();
    }

    @FXML
    public void showNewInvoiceDialog() {
        Dialog<Invoice> dialog = new Dialog<>();
        dialog.setTitle("New Invoice");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField numberField = new TextField();
        DatePicker datePicker = new DatePicker(LocalDate.now());
        DatePicker dueDatePicker = new DatePicker(LocalDate.now().plusDays(30));
        ComboBox<InvoiceType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(InvoiceType.values()));
        ComboBox<Customer> customerCombo = new ComboBox<>(FXCollections.observableArrayList(customerService.findActive()));
        ComboBox<Supplier> supplierCombo = new ComboBox<>(FXCollections.observableArrayList(supplierService.findActive()));
        TextField subtotalField = new TextField("0.00");
        TextField taxField = new TextField("0.00");

        typeCombo.setOnAction(e -> {
            boolean isSale = typeCombo.getValue() == InvoiceType.SALE;
            customerCombo.setDisable(!isSale);
            supplierCombo.setDisable(isSale);
        });

        grid.add(new Label("Invoice Number:"), 0, 0);
        grid.add(numberField, 1, 0);
        grid.add(new Label("Date:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Due Date:"), 0, 2);
        grid.add(dueDatePicker, 1, 2);
        grid.add(new Label("Type:"), 0, 3);
        grid.add(typeCombo, 1, 3);
        grid.add(new Label("Customer:"), 0, 4);
        grid.add(customerCombo, 1, 4);
        grid.add(new Label("Supplier:"), 0, 5);
        grid.add(supplierCombo, 1, 5);
        grid.add(new Label("Subtotal:"), 0, 6);
        grid.add(subtotalField, 1, 6);
        grid.add(new Label("Tax:"), 0, 7);
        grid.add(taxField, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    BigDecimal subtotal = new BigDecimal(subtotalField.getText());
                    BigDecimal tax = new BigDecimal(taxField.getText());
                    
                    return invoiceService.create(
                        numberField.getText(),
                        typeCombo.getValue(),
                        datePicker.getValue(),
                        dueDatePicker.getValue(),
                        customerCombo.getValue(),
                        supplierCombo.getValue(),
                        subtotal,
                        tax
                    );
                } catch (Exception e) {
                    showError("Error creating invoice", e.getMessage());
                }
            }
            return null;
        });

        Optional<Invoice> result = dialog.showAndWait();
        if (result.isPresent()) {
            loadInvoices();
        }
    }

    private void viewInvoice(Invoice invoice) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Invoice Details");
        alert.setHeaderText("Invoice: " + invoice.getInvoiceNumber());
        alert.setContentText(
            "Date: " + invoice.getDate() + "\n" +
            "Due Date: " + invoice.getDueDate() + "\n" +
            "Type: " + invoice.getType() + "\n" +
            "Status: " + invoice.getStatus() + "\n" +
            "Subtotal: " + formatCurrency(invoice.getSubtotal()) + "\n" +
            "Tax: " + formatCurrency(invoice.getTaxAmount()) + "\n" +
            "Total: " + formatCurrency(invoice.getTotal())
        );
        alert.showAndWait();
    }

    private void markAsPaid(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            showInfo("Already Paid", "This invoice is already marked as paid.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Mark as Paid");
        confirm.setHeaderText("Mark invoice " + invoice.getInvoiceNumber() + " as paid?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                invoiceService.markAsPaid(invoice.getId());
                loadInvoices();
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        }
    }

    private void deleteInvoice(Invoice invoice) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Invoice");
        confirm.setHeaderText("Delete invoice: " + invoice.getInvoiceNumber());
        confirm.setContentText("Are you sure? This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                invoiceService.delete(invoice.getId());
                loadInvoices();
            } catch (Exception e) {
                showError("Error deleting invoice", e.getMessage());
            }
        }
    }

    @FXML
    public void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Invoices");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("invoices.csv");

        File file = fileChooser.showSaveDialog(invoicesTable.getScene().getWindow());
        if (file != null) {
            try {
                exportService.exportInvoicesToCsv(allInvoices, file.getAbsolutePath());
                showInfo("Export Successful", "Invoices exported to " + file.getName());
            } catch (Exception e) {
                showError("Export Error", e.getMessage());
            }
        }
    }

    private void updateSummary() {
        totalInvoicesLabel.setText("Total: " + filteredInvoices.size());

        BigDecimal sales = filteredInvoices.stream()
            .filter(i -> i.getType() == InvoiceType.SALE)
            .map(Invoice::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal purchases = filteredInvoices.stream()
            .filter(i -> i.getType() == InvoiceType.PURCHASE)
            .map(Invoice::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pending = filteredInvoices.stream()
            .filter(i -> i.getStatus() == InvoiceStatus.DRAFT)
            .map(Invoice::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalSalesLabel.setText("Sales: " + formatCurrency(sales));
        totalPurchasesLabel.setText("Purchases: " + formatCurrency(purchases));
        pendingLabel.setText("Pending: " + formatCurrency(pending));
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
}
