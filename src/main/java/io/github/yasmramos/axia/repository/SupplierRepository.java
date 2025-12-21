package io.github.yasmramos.axia.repository;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.model.Supplier;
import io.ebean.Database;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Supplier entity persistence operations.
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class SupplierRepository {

    private final Database db;

    @Inject
    public SupplierRepository() {
        this.db = DatabaseManager.getDatabase();
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
