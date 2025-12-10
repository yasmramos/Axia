package com.axia.repository;

import com.axia.config.DatabaseConfig;
import com.axia.model.Customer;
import io.ebean.Database;

import java.util.List;
import java.util.Optional;

public class CustomerRepository {

    private final Database db;

    public CustomerRepository() {
        this.db = DatabaseConfig.getDatabase();
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
