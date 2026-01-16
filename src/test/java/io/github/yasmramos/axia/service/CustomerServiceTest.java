package io.github.yasmramos.axia.service;


import io.github.yasmramos.axia.model.Customer;
import io.github.yasmramos.axia.repository.CustomerRepository;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomerService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerServiceTest {

    private static CustomerService customerService;

    @BeforeAll
    static void setUp() {
        customerService = new CustomerService(new CustomerRepository());
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(Customer.class);
    }

    @Test
    @Order(1)
    @DisplayName("Should create customer")
    void testCreateCustomer() {
        Customer customer = customerService.create(
                "CUST-001",
                "Acme Corp",
                "123456789",
                "123 Main St",
                "New York",
                "555-1234",
                "acme@example.com"
        );

        assertNotNull(customer);
        assertNotNull(customer.getId());
        assertEquals("CUST-001", customer.getCode());
        assertEquals("Acme Corp", customer.getName());
        assertTrue(customer.isActive());
    }

    @Test
    @Order(2)
    @DisplayName("Should throw exception for duplicate code")
    void testDuplicateCodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            customerService.create("CUST-001", "Duplicate", null, null, null, null, null)
        );
    }

    @Test
    @Order(3)
    @DisplayName("Should find customer by code")
    void testFindByCode() {
        var customer = customerService.findByCode("CUST-001");
        
        assertTrue(customer.isPresent());
        assertEquals("Acme Corp", customer.get().getName());
    }

    @Test
    @Order(4)
    @DisplayName("Should find all customers")
    void testFindAll() {
        customerService.create("CUST-002", "Beta Inc", null, null, null, null, null);
        
        List<Customer> customers = customerService.findAll();
        
        assertTrue(customers.size() >= 2);
    }

    @Test
    @Order(5)
    @DisplayName("Should search customers")
    void testSearch() {
        List<Customer> results = customerService.search("Acme");
        
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(c -> c.getName().contains("Acme")));
    }

    @Test
    @Order(6)
    @DisplayName("Should update customer")
    void testUpdateCustomer() {
        Customer customer = customerService.findByCode("CUST-001").orElseThrow();
        customer.setPhone("555-9999");
        
        customerService.update(customer);
        
        Customer updated = customerService.findById(customer.getId()).orElseThrow();
        assertEquals("555-9999", updated.getPhone());
    }

    @Test
    @Order(7)
    @DisplayName("Should deactivate customer")
    void testDeactivateCustomer() {
        Customer customer = customerService.findByCode("CUST-002").orElseThrow();
        
        customerService.deactivate(customer.getId());
        
        Customer deactivated = customerService.findById(customer.getId()).orElseThrow();
        assertFalse(deactivated.isActive());
    }

    @Test
    @Order(8)
    @DisplayName("Should find only active customers")
    void testFindActive() {
        List<Customer> active = customerService.findActive();
        
        assertTrue(active.stream().allMatch(Customer::isActive));
    }
}
