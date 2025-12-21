package io.github.yasmramos.axia.repository;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.AccountType;
import io.ebean.Database;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Account entity persistence operations.
 * 
 * <p>Provides CRUD operations and specialized queries for accounts
 * in the chart of accounts.
 * 
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class AccountRepository {

    private final Database db;

    /**
     * Creates a new AccountRepository instance.
     */
    @Inject
    public AccountRepository() {
        this.db = DatabaseManager.getDatabase();
    }

    /**
     * Persists a new account.
     * @param account the account to save
     */
    public void save(Account account) {
        db.save(account);
    }

    /**
     * Updates an existing account.
     * @param account the account to update
     */
    public void update(Account account) {
        db.update(account);
    }

    /**
     * Deletes an account.
     * @param account the account to delete
     */
    public void delete(Account account) {
        db.delete(account);
    }

    /**
     * Finds an account by ID.
     * @param id the account ID
     * @return the account or empty if not found
     */
    public Optional<Account> findById(Long id) {
        return Optional.ofNullable(db.find(Account.class, id));
    }

    /**
     * Finds an account by its unique code.
     * @param code the account code
     * @return the account or empty if not found
     */
    public Optional<Account> findByCode(String code) {
        return db.find(Account.class)
                .where()
                .eq("code", code)
                .findOneOrEmpty();
    }

    /**
     * Retrieves all accounts ordered by code.
     * @return list of all accounts
     */
    public List<Account> findAll() {
        return db.find(Account.class)
                .orderBy("code")
                .findList();
    }

    /**
     * Finds all accounts of a specific type.
     * @param type the account type
     * @return list of matching accounts
     */
    public List<Account> findByType(AccountType type) {
        return db.find(Account.class)
                .where()
                .eq("type", type)
                .orderBy("code")
                .findList();
    }

    /**
     * Retrieves all active accounts.
     * @return list of active accounts
     */
    public List<Account> findActive() {
        return db.find(Account.class)
                .where()
                .eq("active", true)
                .orderBy("code")
                .findList();
    }

    /**
     * Finds all root-level accounts (no parent).
     * @return list of root accounts
     */
    public List<Account> findRootAccounts() {
        return db.find(Account.class)
                .where()
                .isNull("parent")
                .orderBy("code")
                .findList();
    }

    /**
     * Finds all child accounts of a given parent.
     * @param parent the parent account
     * @return list of child accounts
     */
    public List<Account> findByParent(Account parent) {
        return db.find(Account.class)
                .where()
                .eq("parent", parent)
                .orderBy("code")
                .findList();
    }
}
