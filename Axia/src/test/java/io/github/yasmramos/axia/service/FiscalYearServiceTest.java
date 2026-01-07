package io.github.yasmramos.axia.service;

import io.github.yasmramos.axia.model.FiscalYear;
import io.github.yasmramos.axia.repository.FiscalYearRepository;
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

    @Test
    @Order(10)
    @DisplayName("Should find fiscal year by ID")
    void testFindById() {
        FiscalYear fiscalYear = fiscalYearService.findByYear(2024).orElseThrow();
        var found = fiscalYearService.findById(fiscalYear.getId());
        
        assertTrue(found.isPresent());
        assertEquals(2024, found.get().getYear());
    }

    @Test
    @Order(11)
    @DisplayName("Should return empty when ID not found")
    void testFindByIdNotFound() {
        var result = fiscalYearService.findById(999999L);
        
        assertFalse(result.isPresent());
    }

    @Test
    @Order(12)
    @DisplayName("Should find by year not found")
    void testFindByYearNotFound() {
        var result = fiscalYearService.findByYear(2099);
        
        assertFalse(result.isPresent());
    }

    @Test
    @Order(13)
    @DisplayName("Should re-open closed fiscal year")
    void testReopenFiscalYear() {
        FiscalYear fiscalYear = fiscalYearService.findByYear(2024).orElseThrow();
        assertTrue(fiscalYear.isClosed());
        
        fiscalYearService.reopen(fiscalYear.getId());
        
        FiscalYear reopened = fiscalYearService.findById(fiscalYear.getId()).orElseThrow();
        assertFalse(reopened.isClosed());
    }

    @Test
    @Order(14)
    @DisplayName("Should not reopen open year")
    void testReopenOpenYearThrowsException() {
        FiscalYear fiscalYear = fiscalYearService.findByYear(2025).orElseThrow();
        assertFalse(fiscalYear.isClosed());
        
        assertThrows(IllegalStateException.class, () ->
            fiscalYearService.reopen(fiscalYear.getId())
        );
    }

    @Test
    @Order(15)
    @DisplayName("Should get date range for fiscal year")
    void testGetDateRange() {
        FiscalYear fiscalYear = fiscalYearService.findByYear(2024).orElseThrow();
        
        LocalDate startDate = fiscalYearService.getStartDate(fiscalYear.getId());
        LocalDate endDate = fiscalYearService.getEndDate(fiscalYear.getId());
        
        assertNotNull(startDate);
        assertNotNull(endDate);
        assertTrue(startDate.isBefore(endDate));
    }

    @Test
    @Order(16)
    @DisplayName("Should check if date is within fiscal year")
    void testIsDateWithin() {
        FiscalYear fiscalYear = fiscalYearService.findByYear(2024).orElseThrow();
        
        LocalDate dateWithin = LocalDate.of(2024, 6, 15);
        LocalDate dateOutside = LocalDate.of(2023, 6, 15);
        
        assertTrue(fiscalYearService.isDateWithin(fiscalYear.getId(), dateWithin));
        assertFalse(fiscalYearService.isDateWithin(fiscalYear.getId(), dateOutside));
    }

    @Test
    @Order(17)
    @DisplayName("Should find fiscal years by status")
    void testFindByStatus() {
        // Find open years
        List<FiscalYear> openYears = fiscalYearService.findByStatus(false);
        assertTrue(openYears.stream().noneMatch(FiscalYear::isClosed));
        
        // Find closed years
        List<FiscalYear> closedYears = fiscalYearService.findByStatus(true);
        assertTrue(closedYears.stream().allMatch(FiscalYear::isClosed));
    }

    @Test
    @Order(18)
    @DisplayName("Should count fiscal years")
    void testCount() {
        long count = fiscalYearService.count();
        
        assertTrue(count >= 2);
    }

    @Test
    @Order(19)
    @DisplayName("Should delete fiscal year with no transactions")
    void testDelete() {
        FiscalYear fiscalYear = fiscalYearService.create(
                2099,
                LocalDate.of(2099, 1, 1),
                LocalDate.of(2099, 12, 31)
        );
        Long id = fiscalYear.getId();
        
        fiscalYearService.delete(id);
        
        assertFalse(fiscalYearService.findById(id).isPresent());
    }

    @Test
    @Order(20)
    @DisplayName("Should throw exception when deleting non-existent year")
    void testDeleteNonExistent() {
        assertThrows(IllegalArgumentException.class, () ->
            fiscalYearService.delete(999999L)
        );
    }

    @Test
    @Order(21)
    @DisplayName("Should update fiscal year")
    void testUpdate() {
        FiscalYear fiscalYear = fiscalYearService.findByYear(2025).orElseThrow();
        fiscalYear.setDescription("Updated Description");
        
        fiscalYearService.update(fiscalYear);
        
        FiscalYear updated = fiscalYearService.findById(fiscalYear.getId()).orElseThrow();
        assertEquals("Updated Description", updated.getDescription());
    }

    @Test
    @Order(22)
    @DisplayName("Should find current or most recent fiscal year")
    void testFindCurrentOrMostRecent() {
        var result = fiscalYearService.findCurrentOrMostRecent();
        
        assertTrue(result.isPresent());
    }
}
