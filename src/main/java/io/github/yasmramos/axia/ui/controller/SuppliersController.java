package io.github.yasmramos.axia.ui.controller;

import io.github.yasmramos.veld.Veld;

import io.github.yasmramos.axia.model.Supplier;
import io.github.yasmramos.axia.service.SupplierService;
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
 * Controller for the Suppliers view.
 *
 * @author Yasmany Ramos Garcia
 */
public class SuppliersController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SuppliersController.class);

    @FXML private TableView<Supplier> suppliersTable;
    @FXML private TableColumn<Supplier, String> codeColumn;
    @FXML private TableColumn<Supplier, String> nameColumn;
    @FXML private TableColumn<Supplier, String> taxIdColumn;
    @FXML private TableColumn<Supplier, String> emailColumn;
    @FXML private TableColumn<Supplier, String> phoneColumn;
    @FXML private TableColumn<Supplier, String> addressColumn;
    @FXML private TableColumn<Supplier, String> activeColumn;
    @FXML private TableColumn<Supplier, Void> actionsColumn;

    @FXML private TextField searchField;
    @FXML private CheckBox showInactiveCheck;

    @FXML private Label totalSuppliersLabel;
    @FXML private Label activeSuppliersLabel;
    @FXML private Label totalPayableLabel;

    private final SupplierService supplierService;
    private final ExportService exportService;
    private ObservableList<Supplier> allSuppliers;
    private FilteredList<Supplier> filteredSuppliers;

    public SuppliersController() {
        this.supplierService = Veld.get(SupplierService.class);
        this.exportService = new ExportService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing SuppliersController");
        setupTable();
        loadSuppliers();
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
                editBtn.setOnAction(e -> editSupplier(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteSupplier(getTableView().getItems().get(getIndex())));
                deleteBtn.getStyleClass().add("button-danger");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void loadSuppliers() {
        List<Supplier> suppliers = showInactiveCheck.isSelected() 
            ? supplierService.findAll() 
            : supplierService.findActive();
        
        allSuppliers = FXCollections.observableArrayList(suppliers);
        filteredSuppliers = new FilteredList<>(allSuppliers, p -> true);
        suppliersTable.setItems(filteredSuppliers);
        
        updateSummary();
        logger.debug("Loaded {} suppliers", suppliers.size());
    }

    @FXML
    public void applyFilter() {
        String searchText = searchField.getText().toLowerCase();

        filteredSuppliers.setPredicate(supplier -> {
            if (searchText.isEmpty()) return true;
            return supplier.getName().toLowerCase().contains(searchText) ||
                   (supplier.getCode() != null && supplier.getCode().toLowerCase().contains(searchText)) ||
                   (supplier.getTaxId() != null && supplier.getTaxId().toLowerCase().contains(searchText));
        });

        updateSummary();
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        showInactiveCheck.setSelected(false);
        loadSuppliers();
    }

    @FXML
    public void showNewSupplierDialog() {
        showSupplierDialog(null);
    }

    private void editSupplier(Supplier supplier) {
        showSupplierDialog(supplier);
    }

    private void showSupplierDialog(Supplier supplier) {
        Dialog<Supplier> dialog = new Dialog<>();
        dialog.setTitle(supplier == null ? "New Supplier" : "Edit Supplier");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField codeField = new TextField(supplier != null ? supplier.getCode() : "");
        TextField nameField = new TextField(supplier != null ? supplier.getName() : "");
        TextField taxIdField = new TextField(supplier != null ? supplier.getTaxId() : "");
        TextField emailField = new TextField(supplier != null ? supplier.getEmail() : "");
        TextField phoneField = new TextField(supplier != null ? supplier.getPhone() : "");
        TextArea addressArea = new TextArea(supplier != null ? supplier.getAddress() : "");
        addressArea.setPrefRowCount(3);
        CheckBox activeCheck = new CheckBox();
        activeCheck.setSelected(supplier == null || supplier.isActive());

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
                    if (supplier == null) {
                        return supplierService.create(
                            codeField.getText(),
                            nameField.getText(),
                            taxIdField.getText(),
                            addressArea.getText(),
                            "",
                            phoneField.getText(),
                            emailField.getText()
                        );
                    } else {
                        supplier.setCode(codeField.getText());
                        supplier.setName(nameField.getText());
                        supplier.setTaxId(taxIdField.getText());
                        supplier.setEmail(emailField.getText());
                        supplier.setPhone(phoneField.getText());
                        supplier.setAddress(addressArea.getText());
                        supplier.setActive(activeCheck.isSelected());
                        return supplierService.update(supplier);
                    }
                } catch (Exception e) {
                    showError("Error saving supplier", e.getMessage());
                }
            }
            return null;
        });

        Optional<Supplier> result = dialog.showAndWait();
        if (result.isPresent()) {
            loadSuppliers();
        }
    }

    private void deleteSupplier(Supplier supplier) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Supplier");
        confirm.setHeaderText("Delete supplier: " + supplier.getName());
        confirm.setContentText("Are you sure? This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                supplierService.delete(supplier.getId());
                loadSuppliers();
                logger.info("Supplier deleted: {}", supplier.getName());
            } catch (Exception e) {
                showError("Error deleting supplier", e.getMessage());
            }
        }
    }

    @FXML
    public void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Suppliers");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("suppliers.csv");

        File file = fileChooser.showSaveDialog(suppliersTable.getScene().getWindow());
        if (file != null) {
            try {
                exportService.exportSuppliersToCsv(allSuppliers, file.getAbsolutePath());
                showInfo("Export Successful", "Suppliers exported to " + file.getName());
            } catch (Exception e) {
                showError("Export Error", e.getMessage());
            }
        }
    }

    private void updateSummary() {
        long total = filteredSuppliers.size();
        long active = filteredSuppliers.stream().filter(Supplier::isActive).count();
        
        totalSuppliersLabel.setText("Total Suppliers: " + total);
        activeSuppliersLabel.setText("Active: " + active);
        totalPayableLabel.setText("Total Payable: $0.00");
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
