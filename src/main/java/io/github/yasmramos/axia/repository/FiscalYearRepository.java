package io.github.yasmramos.axia.repository;

import io.github.yasmramos.axia.config.DatabaseConfig;
import io.github.yasmramos.axia.model.FiscalYear;
import io.ebean.Database;

import java.util.List;
import java.util.Optional;

public class FiscalYearRepository {

    private final Database db;

    public FiscalYearRepository() {
        this.db = DatabaseConfig.getDatabase();
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
}
