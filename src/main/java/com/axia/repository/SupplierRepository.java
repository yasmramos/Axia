package com.axia.repository;

import com.axia.config.DatabaseConfig;
import com.axia.model.Supplier;
import io.ebean.Database;

import java.util.List;
import java.util.Optional;

public class SupplierRepository {

    private final Database db;

    public SupplierRepository() {
        this.db = DatabaseConfig.getDatabase();
    }

    public void save(Supplier supplier) {
        db.save(supplier);
    }

    public void update(Supplier supplier) {
        db.update(supplier);
    }

    public void delete(Supplier supplier) {
        db.delete(supplier);
    }

    public Optional<Supplier> findById(Long id) {
        return Optional.ofNullable(db.find(Supplier.class, id));
    }

    public Optional<Supplier> findByCode(String code) {
        return db.find(Supplier.class)
                .where()
                .eq("code", code)
                .findOneOrEmpty();
    }

    public List<Supplier> findAll() {
        return db.find(Supplier.class)
                .orderBy("name")
                .findList();
    }

    public List<Supplier> findActive() {
        return db.find(Supplier.class)
                .where()
                .eq("active", true)
                .orderBy("name")
                .findList();
    }

    public List<Supplier> search(String term) {
        return db.find(Supplier.class)
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
