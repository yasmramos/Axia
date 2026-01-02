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
 * 
 * <p>Manages the user interface for viewing, creating, editing,
 * and deleting accounts in the chart of accounts.
 * 
 * <p>Features:
 * <ul>
 *   <li>Table view of all accounts with filtering</li>
 *   <li>Search by account code or name</li>
 *   <li>Filter by account type (Asset, Liability, Equity, Income, Expense)</li>
 *   <li>Create new accounts with hierarchical structure</li>
 *   <li>Edit existing account details</li>
 *   <li>Delete accounts (with validation for children and balance)</li>
 *   <li>Export accounts to CSV</li>
 *   <li>Summary statistics panel</li>
 * </ul>
 * 
 * <p>FXML View: AccountsView.fxml
 * 
 * @author Yasmany Ramos Garcia
 * @version 1.0.0
 * @see AccountService
 * @see io.github.yasmramos.axia.model.Account
 */
public class AccountsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AccountsController.class);
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();

    /** Table displaying all accounts */
    @FXML private TableView<Account> accountsTable;
    
    /** Column showing account code */
    @FXML private TableColumn<Account, String> codeColumn;
    
    /** Column showing account name */
    @FXML private TableColumn<Account, String> nameColumn;
    
    /** Column showing account type */
    @FXML private TableColumn<Account, String> typeColumn;
    
    /** Column showing parent account */
    @FXML private TableColumn<Account, String> parentColumn;
    
    /** Column showing account balance */
    @FXML private TableColumn<Account, String> balanceColumn;
    
    /** Column showing active status */
    @FXML private TableColumn<Account, String> activeColumn;
    
    /** Column with action buttons (Edit, Delete) */
    @FXML private TableColumn<Account, Void> actionsColumn;

    /** ComboBox for filtering by account type */
    @FXML private ComboBox<String> typeFilterCombo;
    
    /** TextField for searching accounts */
    @FXML private TextField searchField;
    
    /** CheckBox to show/hide inactive accounts */
    @FXML private CheckBox showInactiveCheck;

    /** Label showing total account count */
    @FXML private Label totalAccountsLabel;
    
    /** Label showing total assets */
    @FXML private Label assetsTotalLabel;
    
    /** Label showing total liabilities */
    @FXML private Label liabilitiesTotalLabel;
    
    /** Label showing total equity */
    @FXML private Label equityTotalLabel;

    private final AccountService accountService;
    private final ExportService exportService;
    private ObservableList<Account> allAccounts;
    private FilteredList<Account> filteredAccounts;

    /**
     * Constructs the AccountsController and initializes services.
     * 
     * <p>Uses Veld DI container to obtain AccountService instance.
     */
    public AccountsController() {
        this.accountService = Veld.get(AccountService.class);
        this.exportService = new ExportService();
    }

    /**
     * Initializes the controller after FXML loading.
     * 
     * <p>Sets up table columns, filters, and loads initial data.
     *
     * @param location the location of the FXML file
     * @param resources the resource bundle for localization
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing AccountsController");
        setupTable();
        setupFilters();
        loadAccounts();
    }

    /**
     * Sets up table column cell value factories.
     * 
     * <p>Configures how each column displays data from Account objects,
     * including formatting for currency values.
     */
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

    /**
     * Sets up the actions column with Edit and Delete buttons.
     */
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

    /**
     * Sets up filter controls.
     * 
     * <p>Populates the type filter combo box with all account types
     * and sets the default selection.
     */
    private void setupFilters() {
        typeFilterCombo.getItems().add("All Types");
        for (AccountType type : AccountType.values()) {
            typeFilterCombo.getItems().add(type.toString());
        }
        typeFilterCombo.getSelectionModel().selectFirst();
    }

    /**
     * Loads accounts from service and populates the table.
     * 
     * <p>Respects the showInactiveCheck to include or exclude
     * inactive accounts from the list.
     */
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

    /**
     * Applies current filters to the accounts list.
     * 
     * <p>Called when filter combo box selection changes or
     * search text is modified.
     */
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

    /**
     * Clears all filters and resets to defaults.
     */
    @FXML
    public void clearFilters() {
        typeFilterCombo.getSelectionModel().selectFirst();
        searchField.clear();
        showInactiveCheck.setSelected(false);
        loadAccounts();
    }

    /**
     * Shows dialog for creating a new account.
     */
    @FXML
    public void showNewAccountDialog() {
        showAccountDialog(null);
    }

    /**
     * Opens edit dialog for the specified account.
     *
     * @param account the account to edit, or null for new account
     */
    private void editAccount(Account account) {
        showAccountDialog(account);
    }

    /**
     * Shows a dialog for creating or editing an account.
     *
     * <p>Displays a form with fields for code, name, type, parent,
     * and active status. Validates input before saving.
     *
     * @param account the account to edit, or null for new account
     */
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

    /**
     * Deletes an account after confirmation.
     * 
     * <p>Shows a confirmation dialog and deletes the account
     * if confirmed. The deletion may fail if the account has
     * child accounts or a non-zero balance.
     *
     * @param account the account to delete
     */
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

    /**
     * Exports accounts to a CSV file.
     * 
     * <p>Opens a file chooser dialog and exports the currently
     * filtered accounts to the selected file.
     */
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

    /**
     * Updates the summary statistics panel.
     */
    private void updateSummary() {
        totalAccountsLabel.setText("Total Accounts: " + filteredAccounts.size());
        
        BigDecimal assets = calculateTotalByType(AccountType.ASSET);
        BigDecimal liabilities = calculateTotalByType(AccountType.LIABILITY);
        BigDecimal equity = calculateTotalByType(AccountType.EQUITY);

        assetsTotalLabel.setText("Assets: " + CURRENCY_FORMAT.format(assets));
        liabilitiesTotalLabel.setText("Liabilities: " + CURRENCY_FORMAT.format(liabilities));
        equityTotalLabel.setText("Equity: " + CURRENCY_FORMAT.format(equity));
    }

    /**
     * Calculates the total balance for accounts of a specific type.
     *
     * @param type the account type to filter by
     * @return the sum of balances for matching accounts
     */
    private BigDecimal calculateTotalByType(AccountType type) {
        return filteredAccounts.stream()
            .filter(a -> a.getType() == type)
            .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Shows an error alert to the user.
     *
     * @param title the alert title
     * @param message the error message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an information alert to the user.
     *
     * @param title the alert title
     * @param message the information message
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
