package io.github.yasmramos.axia.repository;

import io.github.yasmramos.axia.config.DatabaseConfig;
import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.AccountType;
import io.ebean.Database;

import java.util.List;
import java.util.Optional;

public class AccountRepository {

    private final Database db;

    public AccountRepository() {
        this.db = DatabaseConfig.getDatabase();
    }

    public void save(Account account) {
        db.save(account);
    }

    public void update(Account account) {
        db.update(account);
    }

    public void delete(Account account) {
        db.delete(account);
    }

    public Optional<Account> findById(Long id) {
        return Optional.ofNullable(db.find(Account.class, id));
    }

    public Optional<Account> findByCode(String code) {
        return db.find(Account.class)
                .where()
                .eq("code", code)
                .findOneOrEmpty();
    }

    public List<Account> findAll() {
        return db.find(Account.class)
                .orderBy("code")
                .findList();
    }

    public List<Account> findByType(AccountType type) {
        return db.find(Account.class)
                .where()
                .eq("type", type)
                .orderBy("code")
                .findList();
    }

    public List<Account> findActive() {
        return db.find(Account.class)
                .where()
                .eq("active", true)
                .orderBy("code")
                .findList();
    }

    public List<Account> findRootAccounts() {
        return db.find(Account.class)
                .where()
                .isNull("parent")
                .orderBy("code")
                .findList();
    }

    public List<Account> findByParent(Account parent) {
        return db.find(Account.class)
                .where()
                .eq("parent", parent)
                .orderBy("code")
                .findList();
    }
}
