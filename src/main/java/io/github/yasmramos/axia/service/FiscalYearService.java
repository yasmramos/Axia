package io.github.yasmramos.axia.service;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.config.DatabaseManager;
import io.github.yasmramos.axia.model.FiscalYear;
import io.github.yasmramos.axia.repository.FiscalYearRepository;
import io.ebean.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for FiscalYear business operations.
 *
 * <p>Manages fiscal year lifecycle including creation,
 * setting the current year, and period closing.
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class FiscalYearService {

    private static final Logger log = LoggerFactory.getLogger(FiscalYearService.class);

    private final FiscalYearRepository fiscalYearRepository;
    private final Database db;

    public FiscalYearService() {
        this.fiscalYearRepository = new FiscalYearRepository();
        this.db = DatabaseManager.getDatabase();
    }

    public FiscalYear create(Integer year, LocalDate startDate, LocalDate endDate) {
        if (fiscalYearRepository.findByYear(year).isPresent()) {
            throw new IllegalArgumentException("Ya existe un año fiscal: " + year);
        }

        FiscalYear fiscalYear = new FiscalYear();
        fiscalYear.setYear(year);
        fiscalYear.setStartDate(startDate);
        fiscalYear.setEndDate(endDate);
        fiscalYear.setClosed(false);
        fiscalYear.setCurrent(false);

        fiscalYearRepository.save(fiscalYear);
        return fiscalYear;
    }

    public FiscalYear setCurrent(Long id) {
        FiscalYear fiscalYear = fiscalYearRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Año fiscal no encontrado"));

        // Desmarcar el año fiscal actual
        fiscalYearRepository.findCurrent().ifPresent(current -> {
            current.setCurrent(false);
            fiscalYearRepository.update(current);
        });

        fiscalYear.setCurrent(true);
        fiscalYearRepository.update(fiscalYear);

        return fiscalYear;
    }

    public FiscalYear close(Long id) {
        FiscalYear fiscalYear = fiscalYearRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Año fiscal no encontrado"));

        if (fiscalYear.isClosed()) {
            throw new IllegalStateException("El año fiscal ya está cerrado");
        }

        fiscalYear.setClosed(true);
        fiscalYear.setCurrent(false);
        fiscalYearRepository.update(fiscalYear);

        return fiscalYear;
    }

    public Optional<FiscalYear> findById(Long id) {
        return fiscalYearRepository.findById(id);
    }

    public Optional<FiscalYear> findByYear(Integer year) {
        return fiscalYearRepository.findByYear(year);
    }

    public Optional<FiscalYear> findCurrent() {
        return fiscalYearRepository.findCurrent();
    }

    public List<FiscalYear> findAll() {
        return fiscalYearRepository.findAll();
    }

    public List<FiscalYear> findOpen() {
        return fiscalYearRepository.findOpen();
    }

    public void initializeCurrentYear() {
        int currentYear = LocalDate.now().getYear();
        if (fiscalYearRepository.findByYear(currentYear).isEmpty()) {
            FiscalYear fy = create(currentYear,
                    LocalDate.of(currentYear, 1, 1),
                    LocalDate.of(currentYear, 12, 31));
            setCurrent(fy.getId());
        }
    }
}
