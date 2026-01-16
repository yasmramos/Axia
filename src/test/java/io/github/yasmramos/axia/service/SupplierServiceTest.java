package io.github.yasmramos.axia.service;


import io.github.yasmramos.axia.model.Supplier;
import io.github.yasmramos.axia.repository.SupplierRepository;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SupplierService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SupplierServiceTest {

    private static SupplierService supplierService;

    @BeforeAll
    static void setUp() {
        supplierService = new SupplierService(new SupplierRepository());
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(Supplier.class);
    }

    @Test
    @Order(1)
    @DisplayName("Should create supplier")
    void testCreateSupplier() {
        Supplier supplier = supplierService.create(
                "SUPP-001",
                "Office Supplies Inc",
                "987654321",
                "456 Commerce Ave",
                "Chicago",
                "555-5678",
                "supplies@example.com"
        );

        assertNotNull(supplier);
        assertNotNull(supplier.getId());
        assertEquals("SUPP-001", supplier.getCode());
        assertEquals("Office Supplies Inc", supplier.getName());
        assertTrue(supplier.isActive());
    }

    @Test
    @Order(2)
    @DisplayName("Should throw exception for duplicate code")
    void testDuplicateCodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            supplierService.create("SUPP-001", "Duplicate", null, null, null, null, null)
        );
    }

    @Test
    @Order(3)
    @DisplayName("Should find supplier by code")
    void testFindByCode() {
        var supplier = supplierService.findByCode("SUPP-001");
        
        assertTrue(supplier.isPresent());
        assertEquals("Office Supplies Inc", supplier.get().getName());
    }

    @Test
    @Order(4)
    @DisplayName("Should search suppliers")
    void testSearch() {
        supplierService.create("SUPP-002", "Tech Parts Ltd", null, null, null, null, null);
        
        List<Supplier> results = supplierService.search("Tech");
        
        assertFalse(results.isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("Should update supplier")
    void testUpdateSupplier() {
        Supplier supplier = supplierService.findByCode("SUPP-001").orElseThrow();
        supplier.setEmail("newemail@example.com");
        
        supplierService.update(supplier);
        
        Supplier updated = supplierService.findById(supplier.getId()).orElseThrow();
        assertEquals("newemail@example.com", updated.getEmail());
    }

    @Test
    @Order(6)
    @DisplayName("Should deactivate supplier")
    void testDeactivateSupplier() {
        Supplier supplier = supplierService.findByCode("SUPP-002").orElseThrow();
        
        supplierService.deactivate(supplier.getId());
        
        Supplier deactivated = supplierService.findById(supplier.getId()).orElseThrow();
        assertFalse(deactivated.isActive());
    }

    @Test
    @Order(7)
    @DisplayName("Should find only active suppliers")
    void testFindActive() {
        List<Supplier> active = supplierService.findActive();
        
        assertTrue(active.stream().allMatch(Supplier::isActive));
    }
}
