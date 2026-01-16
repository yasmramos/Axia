package io.github.yasmramos.axia.ui.controller;

import io.github.yasmramos.veld.Veld;

import io.github.yasmramos.axia.model.Customer;
import io.github.yasmramos.axia.service.CustomerService;
import io.github.yasmramos.axia.export.ExportService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Customers view.
 * Manages customer listing, creation, editing, and deletion.
 *
 * @author Yasmany Ramos Garcia
 */
public class CustomersController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(CustomersController.class);

    @FXML private TableView<Customer> customersTable;
    @FXML private TableColumn<Customer, String> codeColumn;
    @FXML private TableColumn<Customer, String> nameColumn;
    @FXML private TableColumn<Customer, String> taxIdColumn;
    @FXML private TableColumn<Customer, String> emailColumn;
    @FXML private TableColumn<Customer, String> phoneColumn;
    @FXML private TableColumn<Customer, String> addressColumn;
    @FXML private TableColumn<Customer, String> activeColumn;
    @FXML private TableColumn<Customer, Void> actionsColumn;

    @FXML private TextField searchField;
    @FXML private CheckBox showInactiveCheck;

    @FXML private Label totalCustomersLabel;
    @FXML private Label activeCustomersLabel;
    @FXML private Label totalReceivableLabel;

    private final CustomerService customerService;
    private final ExportService exportService;
    private ObservableList<Customer> allCustomers;
    private FilteredList<Customer> filteredCustomers;

    public CustomersController() {
        this.customerService = Veld.get(CustomerService.class);
        this.exportService = new ExportService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing CustomersController");
        setupTable();
        loadCustomers();
    }

    private void setupTable() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        taxIdColumn.setCellValueFactory(new PropertyValueFactory<>("taxId"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        activeColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isActive() ? "Yes" : "No"));

        setupActionsColumn();
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox container = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> editCustomer(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteCustomer(getTableView().getItems().get(getIndex())));
                deleteBtn.getStyleClass().add("button-danger");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void loadCustomers() {
        List<Customer> customers = showInactiveCheck.isSelected() 
            ? customerService.findAll() 
            : customerService.findActive();
        
        allCustomers = FXCollections.observableArrayList(customers);
        filteredCustomers = new FilteredList<>(allCustomers, p -> true);
        customersTable.setItems(filteredCustomers);
        
        updateSummary();
        logger.debug("Loaded {} customers", customers.size());
    }

    @FXML
    public void applyFilter() {
        String searchText = searchField.getText().toLowerCase();

        filteredCustomers.setPredicate(customer -> {
            if (searchText.isEmpty()) return true;
            return customer.getName().toLowerCase().contains(searchText) ||
                   (customer.getCode() != null && customer.getCode().toLowerCase().contains(searchText)) ||
                   (customer.getTaxId() != null && customer.getTaxId().toLowerCase().contains(searchText));
        });

        updateSummary();
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        showInactiveCheck.setSelected(false);
        loadCustomers();
    }

    @FXML
    public void showNewCustomerDialog() {
        showCustomerDialog(null);
    }

    private void editCustomer(Customer customer) {
        showCustomerDialog(customer);
    }

    private void showCustomerDialog(Customer customer) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle(customer == null ? "New Customer" : "Edit Customer");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField codeField = new TextField(customer != null ? customer.getCode() : "");
        TextField nameField = new TextField(customer != null ? customer.getName() : "");
        TextField taxIdField = new TextField(customer != null ? customer.getTaxId() : "");
        TextField emailField = new TextField(customer != null ? customer.getEmail() : "");
        TextField phoneField = new TextField(customer != null ? customer.getPhone() : "");
        TextArea addressArea = new TextArea(customer != null ? customer.getAddress() : "");
        addressArea.setPrefRowCount(3);
        CheckBox activeCheck = new CheckBox();
        activeCheck.setSelected(customer == null || customer.isActive());

        grid.add(new Label("Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Tax ID:"), 0, 2);
        grid.add(taxIdField, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailField, 1, 3);
        grid.add(new Label("Phone:"), 0, 4);
        grid.add(phoneField, 1, 4);
        grid.add(new Label("Address:"), 0, 5);
        grid.add(addressArea, 1, 5);
        grid.add(new Label("Active:"), 0, 6);
        grid.add(activeCheck, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    if (customer == null) {
                        return customerService.create(
                            codeField.getText(),
                            nameField.getText(),
                            taxIdField.getText(),
                            addressArea.getText(),
                            "",
                            phoneField.getText(),
                            emailField.getText()
                        );
                    } else {
                        customer.setCode(codeField.getText());
                        customer.setName(nameField.getText());
                        customer.setTaxId(taxIdField.getText());
                        customer.setEmail(emailField.getText());
                        customer.setPhone(phoneField.getText());
                        customer.setAddress(addressArea.getText());
                        customer.setActive(activeCheck.isSelected());
                        return customerService.update(customer);
                    }
                } catch (Exception e) {
                    showError("Error saving customer", e.getMessage());
                }
            }
            return null;
        });

        Optional<Customer> result = dialog.showAndWait();
        if (result.isPresent()) {
            loadCustomers();
        }
    }

    private void deleteCustomer(Customer customer) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Customer");
        confirm.setHeaderText("Delete customer: " + customer.getName());
        confirm.setContentText("Are you sure? This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                customerService.delete(customer.getId());
                loadCustomers();
                logger.info("Customer deleted: {}", customer.getName());
            } catch (Exception e) {
                showError("Error deleting customer", e.getMessage());
            }
        }
    }

    @FXML
    public void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Customers");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("customers.csv");

        File file = fileChooser.showSaveDialog(customersTable.getScene().getWindow());
        if (file != null) {
            try {
                exportService.exportCustomersToCsv(allCustomers, file.getAbsolutePath());
                showInfo("Export Successful", "Customers exported to " + file.getName());
            } catch (Exception e) {
                showError("Export Error", e.getMessage());
            }
        }
    }

    private void updateSummary() {
        long total = filteredCustomers.size();
        long active = filteredCustomers.stream().filter(Customer::isActive).count();
        
        totalCustomersLabel.setText("Total Customers: " + total);
        activeCustomersLabel.setText("Active: " + active);
        totalReceivableLabel.setText("Total Receivable: $0.00"); // TODO: Calculate from invoices
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
