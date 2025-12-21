package io.github.yasmramos.axia.ui.controller;

import io.github.yasmramos.veld.Veld;
import javafx.scene.layout.GridPane;

import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.AccountType;
import io.github.yasmramos.axia.service.AccountService;
import io.github.yasmramos.axia.export.ExportService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Chart of Accounts view.
 * Manages account listing, creation, editing, and deletion.
 *
 * @author Yasmany Ramos Garcia
 */
public class AccountsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AccountsController.class);
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();

    @FXML private TableView<Account> accountsTable;
    @FXML private TableColumn<Account, String> codeColumn;
    @FXML private TableColumn<Account, String> nameColumn;
    @FXML private TableColumn<Account, String> typeColumn;
    @FXML private TableColumn<Account, String> parentColumn;
    @FXML private TableColumn<Account, String> balanceColumn;
    @FXML private TableColumn<Account, String> activeColumn;
    @FXML private TableColumn<Account, Void> actionsColumn;

    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private TextField searchField;
    @FXML private CheckBox showInactiveCheck;

    @FXML private Label totalAccountsLabel;
    @FXML private Label assetsTotalLabel;
    @FXML private Label liabilitiesTotalLabel;
    @FXML private Label equityTotalLabel;

    private final AccountService accountService;
    private final ExportService exportService;
    private ObservableList<Account> allAccounts;
    private FilteredList<Account> filteredAccounts;

    public AccountsController() {
        this.accountService = Veld.get(AccountService.class);
        this.exportService = new ExportService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing AccountsController");
        setupTable();
        setupFilters();
        loadAccounts();
    }

    private void setupTable() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getType().toString()));
        parentColumn.setCellValueFactory(data -> {
            Account parent = data.getValue().getParent();
            return new SimpleStringProperty(parent != null ? parent.getCode() + " - " + parent.getName() : "");
        });
        balanceColumn.setCellValueFactory(data -> {
            BigDecimal balance = data.getValue().getBalance();
            return new SimpleStringProperty(balance != null ? CURRENCY_FORMAT.format(balance) : "$0.00");
        });
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
                editBtn.setOnAction(e -> editAccount(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteAccount(getTableView().getItems().get(getIndex())));
                deleteBtn.getStyleClass().add("button-danger");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupFilters() {
        typeFilterCombo.getItems().add("All Types");
        for (AccountType type : AccountType.values()) {
            typeFilterCombo.getItems().add(type.toString());
        }
        typeFilterCombo.getSelectionModel().selectFirst();
    }

    private void loadAccounts() {
        List<Account> accounts = showInactiveCheck.isSelected() 
            ? accountService.findAll() 
            : accountService.findActive();
        
        allAccounts = FXCollections.observableArrayList(accounts);
        filteredAccounts = new FilteredList<>(allAccounts, p -> true);
        accountsTable.setItems(filteredAccounts);
        
        updateSummary();
        logger.debug("Loaded {} accounts", accounts.size());
    }

    @FXML
    public void applyFilter() {
        String typeFilter = typeFilterCombo.getValue();
        String searchText = searchField.getText().toLowerCase();

        filteredAccounts.setPredicate(account -> {
            boolean matchesType = "All Types".equals(typeFilter) || 
                account.getType().toString().equals(typeFilter);
            boolean matchesSearch = searchText.isEmpty() ||
                account.getCode().toLowerCase().contains(searchText) ||
                account.getName().toLowerCase().contains(searchText);
            return matchesType && matchesSearch;
        });

        updateSummary();
    }

    @FXML
    public void clearFilters() {
        typeFilterCombo.getSelectionModel().selectFirst();
        searchField.clear();
        showInactiveCheck.setSelected(false);
        loadAccounts();
    }

    @FXML
    public void showNewAccountDialog() {
        showAccountDialog(null);
    }

    private void editAccount(Account account) {
        showAccountDialog(account);
    }

    private void showAccountDialog(Account account) {
        Dialog<Account> dialog = new Dialog<>();
        dialog.setTitle(account == null ? "New Account" : "Edit Account");

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField codeField = new TextField(account != null ? account.getCode() : "");
        TextField nameField = new TextField(account != null ? account.getName() : "");
        ComboBox<AccountType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(AccountType.values()));
        ComboBox<Account> parentCombo = new ComboBox<>(FXCollections.observableArrayList(accountService.findActive()));
        CheckBox activeCheck = new CheckBox();
        activeCheck.setSelected(account == null || account.isActive());

        if (account != null) {
            typeCombo.setValue(account.getType());
            parentCombo.setValue(account.getParent());
        }

        grid.add(new Label("Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label("Parent:"), 0, 3);
        grid.add(parentCombo, 1, 3);
        grid.add(new Label("Active:"), 0, 4);
        grid.add(activeCheck, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    if (account == null) {
                        return accountService.create(
                            codeField.getText(),
                            nameField.getText(),
                            typeCombo.getValue(),
                            parentCombo.getValue()
                        );
                    } else {
                        account.setCode(codeField.getText());
                        account.setName(nameField.getText());
                        account.setType(typeCombo.getValue());
                        account.setParent(parentCombo.getValue());
                        account.setActive(activeCheck.isSelected());
                        return accountService.update(account);
                    }
                } catch (Exception e) {
                    showError("Error saving account", e.getMessage());
                }
            }
            return null;
        });

        Optional<Account> result = dialog.showAndWait();
        if (result.isPresent()) {
            loadAccounts();
        }
    }

    private void deleteAccount(Account account) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Account");
        confirm.setHeaderText("Delete account: " + account.getCode() + " - " + account.getName());
        confirm.setContentText("Are you sure? This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                accountService.delete(account.getId());
                loadAccounts();
                logger.info("Account deleted: {}", account.getCode());
            } catch (Exception e) {
                showError("Error deleting account", e.getMessage());
            }
        }
    }

    @FXML
    public void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Accounts");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName("accounts.csv");

        File file = fileChooser.showSaveDialog(accountsTable.getScene().getWindow());
        if (file != null) {
            try {
                exportService.exportAccountsToCsv(allAccounts, file.getAbsolutePath());
                showInfo("Export Successful", "Accounts exported to " + file.getName());
            } catch (Exception e) {
                showError("Export Error", e.getMessage());
            }
        }
    }

    private void updateSummary() {
        totalAccountsLabel.setText("Total Accounts: " + filteredAccounts.size());
        
        BigDecimal assets = calculateTotalByType(AccountType.ASSET);
        BigDecimal liabilities = calculateTotalByType(AccountType.LIABILITY);
        BigDecimal equity = calculateTotalByType(AccountType.EQUITY);

        assetsTotalLabel.setText("Assets: " + CURRENCY_FORMAT.format(assets));
        liabilitiesTotalLabel.setText("Liabilities: " + CURRENCY_FORMAT.format(liabilities));
        equityTotalLabel.setText("Equity: " + CURRENCY_FORMAT.format(equity));
    }

    private BigDecimal calculateTotalByType(AccountType type) {
        return filteredAccounts.stream()
            .filter(a -> a.getType() == type)
            .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
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
