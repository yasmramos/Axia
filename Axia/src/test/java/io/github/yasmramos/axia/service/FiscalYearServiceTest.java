package io.github.yasmramos.axia.service;

import io.github.yasmramos.axia.model.FiscalYear;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FiscalYearService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FiscalYearServiceTest {

    private static FiscalYearService fiscalYearService;

    @BeforeAll
    static void setUp() {
        fiscalYearService = new FiscalYearService(new FiscalYearRepository());
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(FiscalYear.class);
    }

    @Test
    @Order(1)
    @DisplayName("Should create fiscal year")
    void testCreateFiscalYear() {
        FiscalYear fiscalYear = fiscalYearService.create(
                2024,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)
        );

        assertNotNull(fiscalYear);
        assertNotNull(fiscalYear.getId());
        assertEquals(2024, fiscalYear.getYear());
        assertFalse(fiscalYear.isClosed());
        assertFalse(fiscalYear.isCurrent());
    }

    @Test
    @Order(2)
    @DisplayName("Should throw exception for duplicate year")
    void testDuplicateYearThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            fiscalYearService.create(2024, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
        );
    }

    @Test
    @Order(3)
    @DisplayName("Should set current fiscal year")
    void testSetCurrent() {
        FiscalYear fiscalYear = fiscalYearService.findByYear(2024).orElseThrow();
        
        fiscalYearService.setCurrent(fiscalYear.getId());
        
        FiscalYear current = fiscalYearService.findCurrent().orElseThrow();
        assertEquals(2024, current.getYear());
        assertTrue(current.isCurrent());
    }

    @Test
    @Order(4)
    @DisplayName("Should change current year when setting new current")
    void testChangeCurrentYear() {
        fiscalYearService.create(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        FiscalYear year2025 = fiscalYearService.findByYear(2025).orElseThrow();
        
        fiscalYearService.setCurrent(year2025.getId());
        
        FiscalYear current = fiscalYearService.findCurrent().orElseThrow();
        assertEquals(2025, current.getYear());
        
        // Previous year should no longer be current
        FiscalYear year2024 = fiscalYearService.findByYear(2024).orElseThrow();
        assertFalse(year2024.isCurrent());
    }

    @Test
    @Order(5)
    @DisplayName("Should close fiscal year")
    void testCloseFiscalYear() {
        FiscalYear fiscalYear = fiscalYearService.findByYear(2024).orElseThrow();
        
        fiscalYearService.close(fiscalYear.getId());
        
        FiscalYear closed = fiscalYearService.findById(fiscalYear.getId()).orElseThrow();
        assertTrue(closed.isClosed());
        assertFalse(closed.isCurrent());
    }

    @Test
    @Order(6)
    @DisplayName("Should not close already closed year")
    void testCloseAlreadyClosedThrowsException() {
        FiscalYear fiscalYear = fiscalYearService.findByYear(2024).orElseThrow();
        
        assertThrows(IllegalStateException.class, () -> 
            fiscalYearService.close(fiscalYear.getId())
        );
    }

    @Test
    @Order(7)
    @DisplayName("Should find all fiscal years")
    void testFindAll() {
        List<FiscalYear> years = fiscalYearService.findAll();
        
        assertTrue(years.size() >= 2);
    }

    @Test
    @Order(8)
    @DisplayName("Should find open fiscal years")
    void testFindOpen() {
        List<FiscalYear> openYears = fiscalYearService.findOpen();
        
        assertTrue(openYears.stream().noneMatch(FiscalYear::isClosed));
    }

    @Test
    @Order(9)
    @DisplayName("Should initialize current year if not exists")
    void testInitializeCurrentYear() {
        int currentYear = LocalDate.now().getYear();
        
        // Remove if exists
        fiscalYearService.findByYear(currentYear).ifPresent(fy -> {
            // Skip - already exists from previous tests
        });
        
        fiscalYearService.initializeCurrentYear();
        
        assertTrue(fiscalYearService.findByYear(currentYear).isPresent());
    }
}
