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
 * <p>Example usage:
 * <pre>
 * FiscalYearService fiscalYearService = Veld.get(FiscalYearService.class);
 * 
 * // Create a new fiscal year
 * FiscalYear fy = fiscalYearService.create(2024,
 *     LocalDate.of(2024, 1, 1),
 *     LocalDate.of(2024, 12, 31));
 * 
 * // Set as current year
 * fiscalYearService.setCurrent(fy.getId());
 * 
 * // Close a fiscal year
 * fiscalYearService.close(fy.getId());
 * </pre>
 *
 * @author Yasmany Ramos García
 * @version 1.0.0
 */
@Component
public class FiscalYearService {

    private static final Logger log = LoggerFactory.getLogger(FiscalYearService.class);

    private final FiscalYearRepository fiscalYearRepository;
    private final Database db;

    /**
     * Constructs a new FiscalYearService with injected dependencies.
     *
     * @param fiscalYearRepository the fiscal year repository
     */
    @Inject
    public FiscalYearService(FiscalYearRepository fiscalYearRepository) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.db = DatabaseManager.getDatabase();
        log.debug("FiscalYearService initialized");
    }

    /**
     * Creates a new fiscal year.
     *
     * @param year the fiscal year (e.g., 2024)
     * @param startDate the start date of the fiscal year
     * @param endDate the end date of the fiscal year
     * @return the created fiscal year
     * @throws IllegalArgumentException if a fiscal year with the same year already exists
     */
    public FiscalYear create(Integer year, LocalDate startDate, LocalDate endDate) {
        log.info("Creating fiscal year: {}", year);

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
        log.info("Fiscal year {} created successfully", year);
        return fiscalYear;
    }

    /**
     * Sets a fiscal year as the current year.
     *
     * @param id the fiscal year ID
     * @return the updated fiscal year
     * @throws IllegalArgumentException if the fiscal year is not found
     */
    public FiscalYear setCurrent(Long id) {
        log.info("Setting fiscal year {} as current", id);

        FiscalYear fiscalYear = fiscalYearRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Año fiscal no encontrado"));

        // Desmarcar el año fiscal actual
        fiscalYearRepository.findCurrent().ifPresent(current -> {
            current.setCurrent(false);
            fiscalYearRepository.update(current);
        });

        fiscalYear.setCurrent(true);
        fiscalYearRepository.update(fiscalYear);

        log.info("Fiscal year {} is now current", fiscalYear.getYear());
        return fiscalYear;
    }

    /**
     * Closes a fiscal year.
     *
     * <p>Once closed, a fiscal year cannot be modified and no new
     * transactions can be posted to it.
     *
     * @param id the fiscal year ID
     * @return the closed fiscal year
     * @throws IllegalArgumentException if the fiscal year is not found
     * @throws IllegalStateException if the fiscal year is already closed
     */
    public FiscalYear close(Long id) {
        log.info("Closing fiscal year: {}", id);

        FiscalYear fiscalYear = fiscalYearRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Año fiscal no encontrado"));

        if (fiscalYear.isClosed()) {
            throw new IllegalStateException("El año fiscal ya está cerrado");
        }

        fiscalYear.setClosed(true);
        fiscalYear.setCurrent(false);
        fiscalYearRepository.update(fiscalYear);

        log.info("Fiscal year {} closed successfully", fiscalYear.getYear());
        return fiscalYear;
    }

    /**
     * Finds a fiscal year by ID.
     *
     * @param id the fiscal year ID
     * @return the fiscal year if found
     */
    public Optional<FiscalYear> findById(Long id) {
        return fiscalYearRepository.findById(id);
    }

    /**
     * Finds a fiscal year by year number.
     *
     * @param year the fiscal year
     * @return the fiscal year if found
     */
    public Optional<FiscalYear> findByYear(Integer year) {
        return fiscalYearRepository.findByYear(year);
    }

    /**
     * Finds the current fiscal year.
     *
     * @return the current fiscal year if set
     */
    public Optional<FiscalYear> findCurrent() {
        return fiscalYearRepository.findCurrent();
    }

    /**
     * Finds all fiscal years.
     *
     * @return list of all fiscal years
     */
    public List<FiscalYear> findAll() {
        return fiscalYearRepository.findAll();
    }

    /**
     * Finds all open (non-closed) fiscal years.
     *
     * @return list of open fiscal years
     */
    public List<FiscalYear> findOpen() {
        return fiscalYearRepository.findOpen();
    }

    /**
     * Initializes the current year if it doesn't exist.
     *
     * <p>This method is typically called during application startup
     * to ensure there's always a current fiscal year available.
     */
    public void initializeCurrentYear() {
        int currentYear = LocalDate.now().getYear();
        log.info("Initializing current fiscal year: {}", currentYear);

        if (fiscalYearRepository.findByYear(currentYear).isEmpty()) {
            FiscalYear fy = create(currentYear,
                    LocalDate.of(currentYear, 1, 1),
                    LocalDate.of(currentYear, 12, 31));
            setCurrent(fy.getId());
            log.info("Fiscal year {} created and set as current", currentYear);
        } else {
            log.debug("Fiscal year {} already exists", currentYear);
        }
    }
}
