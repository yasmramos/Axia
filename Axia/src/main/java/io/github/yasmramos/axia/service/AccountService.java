package io.github.yasmramos.axia.service;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.AccountType;
import io.github.yasmramos.axia.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Account business operations.
 *
 * <p>Provides account management including creation, updates,
 * and initialization of the default chart of accounts.
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;

    @Inject
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        log.debug("AccountService initialized");
    }

    public Account create(String code, String name, AccountType type, Account parent) {
        log.info("Creating account: {} - {}", code, name);
        
        if (accountRepository.findByCode(code).isPresent()) {
            log.error("Account already exists with code: {}", code);
            throw new IllegalArgumentException("Account already exists with code: " + code);
        }

        Account account = new Account();
        account.setCode(code);
        account.setName(name);
        account.setType(type);
        account.setParent(parent);
        account.setBalance(BigDecimal.ZERO);
        account.setActive(true);

        accountRepository.save(account);
        log.info("Account created successfully: {} (ID: {})", code, account.getId());
        return account;
    }

    public Account update(Account account) {
        log.info("Updating account: {}", account.getCode());
        accountRepository.update(account);
        log.debug("Account updated: {}", account.getCode());
        return account;
    }

    public void delete(Long id) {
        log.info("Deleting account with ID: {}", id);
        
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Account not found with ID: {}", id);
                    return new IllegalArgumentException("Account not found");
                });

        if (!account.getChildren().isEmpty()) {
            log.error("Cannot delete account {} - has child accounts", account.getCode());
            throw new IllegalArgumentException("Cannot delete account with child accounts");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            log.error("Cannot delete account {} - has balance", account.getCode());
            throw new IllegalArgumentException("Cannot delete account with balance");
        }

        accountRepository.delete(account);
        log.info("Account deleted: {}", account.getCode());
    }

    public Optional<Account> findById(Long id) {
        log.debug("Finding account by ID: {}", id);
        return accountRepository.findById(id);
    }

    public Optional<Account> findByCode(String code) {
        log.debug("Finding account by code: {}", code);
        return accountRepository.findByCode(code);
    }

    public List<Account> findAll() {
        log.debug("Retrieving all accounts");
        return accountRepository.findAll();
    }

    public List<Account> findByType(AccountType type) {
        log.debug("Finding accounts by type: {}", type);
        return accountRepository.findByType(type);
    }

    public List<Account> findActive() {
        log.debug("Retrieving active accounts");
        return accountRepository.findActive();
    }

    public List<Account> findRootAccounts() {
        log.debug("Retrieving root accounts");
        return accountRepository.findRootAccounts();
    }

    public List<Account> findRoots() {
        log.debug("Retrieving root accounts");
        return accountRepository.findRootAccounts();
    }

    public List<Account> getChartOfAccounts() {
        log.debug("Retrieving chart of accounts");
        return accountRepository.findAll();
    }

    public void initializeDefaultAccounts() {
        if (!accountRepository.findAll().isEmpty()) {
            log.info("Chart of accounts already initialized, skipping");
            return;
        }

        log.info("Initializing default chart of accounts");

        // Assets
        Account assets = create("1", "ASSETS", AccountType.ASSET, null);
        create("1.1", "Current Assets", AccountType.ASSET, assets);
        create("1.1.01", "Cash", AccountType.ASSET, findByCode("1.1").orElse(null));
        create("1.1.02", "Banks", AccountType.ASSET, findByCode("1.1").orElse(null));
        create("1.1.03", "Accounts Receivable", AccountType.ASSET, findByCode("1.1").orElse(null));
        create("1.2", "Non-Current Assets", AccountType.ASSET, assets);

        // Liabilities
        Account liabilities = create("2", "LIABILITIES", AccountType.LIABILITY, null);
        create("2.1", "Current Liabilities", AccountType.LIABILITY, liabilities);
        create("2.1.01", "Accounts Payable", AccountType.LIABILITY, findByCode("2.1").orElse(null));
        create("2.1.02", "Taxes Payable", AccountType.LIABILITY, findByCode("2.1").orElse(null));

        // Equity
        Account equity = create("3", "EQUITY", AccountType.EQUITY, null);
        create("3.1", "Share Capital", AccountType.EQUITY, equity);
        create("3.2", "Retained Earnings", AccountType.EQUITY, equity);

        // Income
        Account income = create("4", "INCOME", AccountType.INCOME, null);
        create("4.1", "Operating Income", AccountType.INCOME, income);
        create("4.1.01", "Sales Revenue", AccountType.INCOME, findByCode("4.1").orElse(null));

        // Expenses
        Account expenses = create("5", "EXPENSES", AccountType.EXPENSE, null);
        create("5.1", "Operating Expenses", AccountType.EXPENSE, expenses);
        create("5.1.01", "Payroll Expenses", AccountType.EXPENSE, findByCode("5.1").orElse(null));
        create("5.1.02", "Administrative Expenses", AccountType.EXPENSE, findByCode("5.1").orElse(null));

        log.info("Default chart of accounts initialized successfully");
    }

    public List<Account> findChildren(Long parentId) {
        log.debug("Finding children accounts for parent ID: {}", parentId);
        return accountRepository.findChildren(parentId);
    }

    public void deactivate(Long id) {
        log.info("Deactivating account with ID: {}", id);
        
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Account not found with ID: {}", id);
                    return new IllegalArgumentException("Account not found");
                });
        
        account.setActive(false);
        accountRepository.update(account);
        log.debug("Account deactivated: {}", account.getCode());
    }

    public List<Account> searchByName(String name) {
        log.debug("Searching accounts by name: {}", name);
        return accountRepository.findAll().stream()
                .filter(a -> a.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }

    public long count() {
        log.debug("Counting all accounts");
        return accountRepository.count();
    }
}
