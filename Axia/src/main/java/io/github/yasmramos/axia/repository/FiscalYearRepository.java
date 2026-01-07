package io.github.yasmramos.axia.repository;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.model.FiscalYear;
import io.ebean.Database;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FiscalYear entity persistence operations.
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class FiscalYearRepository {

    private final Database db;

    @Inject
    public FiscalYearRepository() {
        this.db = DatabaseManager.getDatabase();
    }

    public void save(FiscalYear fiscalYear) {
        db.save(fiscalYear);
    }

    public void update(FiscalYear fiscalYear) {
        db.update(fiscalYear);
    }

    public Optional<FiscalYear> findById(Long id) {
        return Optional.ofNullable(db.find(FiscalYear.class, id));
    }

    public Optional<FiscalYear> findByYear(Integer year) {
        return db.find(FiscalYear.class)
                .where()
                .eq("year", year)
                .findOneOrEmpty();
    }

    public Optional<FiscalYear> findCurrent() {
        return db.find(FiscalYear.class)
                .where()
                .eq("current", true)
                .findOneOrEmpty();
    }

    public List<FiscalYear> findAll() {
        return db.find(FiscalYear.class)
                .orderBy("year desc")
                .findList();
    }

    public List<FiscalYear> findOpen() {
        return db.find(FiscalYear.class)
                .where()
                .eq("closed", false)
                .orderBy("year desc")
                .findList();
    }

    public long count() {
        return db.find(FiscalYear.class).findCount();
    }

    public void delete(FiscalYear fiscalYear) {
        db.delete(fiscalYear);
    }
}
