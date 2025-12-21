package io.github.yasmramos.axia.repository;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.model.Customer;
import io.ebean.Database;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Customer entity persistence operations.
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class CustomerRepository {

    private final Database db;

    @Inject
    public CustomerRepository() {
        this.db = DatabaseManager.getDatabase();
    }

    public void save(Customer customer) {
        db.save(customer);
    }

    public void update(Customer customer) {
        db.update(customer);
    }

    public void delete(Customer customer) {
        db.delete(customer);
    }

    public Optional<Customer> findById(Long id) {
        return Optional.ofNullable(db.find(Customer.class, id));
    }

    public Optional<Customer> findByCode(String code) {
        return db.find(Customer.class)
                .where()
                .eq("code", code)
                .findOneOrEmpty();
    }

    public List<Customer> findAll() {
        return db.find(Customer.class)
                .orderBy("name")
                .findList();
    }

    public List<Customer> findActive() {
        return db.find(Customer.class)
                .where()
                .eq("active", true)
                .orderBy("name")
                .findList();
    }

    public List<Customer> search(String term) {
        return db.find(Customer.class)
                .where()
                .or()
                .icontains("name", term)
                .icontains("code", term)
                .icontains("taxId", term)
                .endOr()
                .orderBy("name")
                .findList();
    }
}
