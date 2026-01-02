package io.github.yasmramos.axia.service;

import io.github.yasmramos.axia.model.Customer;
import io.github.yasmramos.axia.model.Invoice;
import io.github.yasmramos.axia.model.InvoiceLine;
import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.repository.CustomerRepository;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete invoice and customer workflows.
 * 
 * <p>Tests the full lifecycle of invoice processing including customer
 * creation, invoice generation, payment collection, and account updates.</p>
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceServiceIT {

    private static InvoiceService invoiceService;
    private static CustomerService customerService;
    private static AccountService accountService;
    private static JournalEntryService journalEntryService;

    @BeforeAll
    static void setUp() {
        customerService = new CustomerService(new CustomerRepository());
        accountService = new AccountService(new io.github.yasmramos.axia.repository.AccountRepository());
        invoiceService = new InvoiceService(
            new io.github.yasmramos.axia.repository.InvoiceRepository(),
            customerService,
            accountService
        );
        journalEntryService = new JournalEntryService(
            new io.github.yasmramos.axia.repository.JournalEntryRepository(),
            accountService
        );

        // Initialize accounts
        accountService.initializeDefaultAccounts();
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(InvoiceLine.class);
        db.truncate(Invoice.class);
        db.truncate(Customer.class);
    }

    @Test
    @Order(1)
    @DisplayName("Complete customer to invoice workflow")
    void testCustomerToInvoiceWorkflow() {
        // Step 1: Create customer
        Customer customer = customerService.create(
            "IT-CUST-001",
            "Integration Test Company",
            "123 Test Street",
            "Test City",
            "555-TEST",
            "test@integration.test"
        );

        assertNotNull(customer);
        assertNotNull(customer.getId());
        assertEquals("IT-CUST-001", customer.getCode());
        assertTrue(customer.isActive());

        // Step 2: Create invoice for customer
        Invoice invoice = invoiceService.create(
            customer.getId(),
            LocalDate.now(),
            "IT-INV-001",
            "Integration Test Invoice"
        );

        assertNotNull(invoice);
        assertNotNull(invoice.getId());
        assertEquals(Invoice.Status.DRAFT, invoice.getStatus());
        assertEquals(customer.getId(), invoice.getCustomerId());

        // Step 3: Add invoice lines
        InvoiceLine line1 = invoiceService.addLine(
            invoice.getId(),
            "Test Product 1",
            BigDecimal.valueOf(100.00),
            2,
            BigDecimal.valueOf(0.10) // 10% tax
        );

        assertNotNull(line1);
        assertEquals(BigDecimal.valueOf(200.00), line1.getSubtotal());
        assertEquals(BigDecimal.valueOf(20.00), line1.getTax());
        assertEquals(BigDecimal.valueOf(220.00), line1.getTotal());

        InvoiceLine line2 = invoiceService.addLine(
            invoice.getId(),
            "Test Product 2",
            BigDecimal.valueOf(50.00),
            1,
            BigDecimal.valueOf(0.10)
        );

        assertNotNull(line2);

        // Step 4: Verify invoice totals
        Invoice updatedInvoice = invoiceService.findById(invoice.getId()).orElseThrow();
        assertEquals(2, updatedInvoice.getLines().size());
        assertEquals(BigDecimal.valueOf(250.00), updatedInvoice.getSubtotal());
        assertEquals(BigDecimal.valueOf(25.00), updatedInvoice.getTax());
        assertEquals(BigDecimal.valueOf(275.00), updatedInvoice.getTotal());

        // Step 5: Confirm invoice
        Invoice confirmedInvoice = invoiceService.confirm(invoice.getId());
        assertEquals(Invoice.Status.CONFIRMED, confirmedInvoice.getStatus());
        assertNotNull(confirmedInvoice.getConfirmedAt());
    }

    @Test
    @Order(2)
    @DisplayName("Complete invoice payment workflow")
    void testInvoicePaymentWorkflow() {
        // Create customer and invoice
        Customer customer = customerService.create(
            "IT-CUST-002",
            "Payment Test Company",
            "456 Payment Ave",
            "Pay City",
            "555-PAY",
            "payment@test.com"
        );

        Invoice invoice = invoiceService.create(
            customer.getId(),
            LocalDate.now(),
            "IT-INV-002",
            "Payment Test Invoice"
        );

        invoiceService.addLine(invoice.getId(), "Test Service", 
            BigDecimal.valueOf(500.00), 1, BigDecimal.valueOf(0.0));
        invoiceService.confirm(invoice.getId());

        // Verify initial status
        assertEquals(Invoice.Status.CONFIRMED, invoiceService.findById(invoice.getId()).orElseThrow().getStatus());

        // Record payment
        Invoice paidInvoice = invoiceService.recordPayment(
            invoice.getId(),
            LocalDate.now(),
            "CASH",
            "IT-PAY-001"
        );

        assertEquals(Invoice.Status.PAID, paidInvoice.getStatus());
        assertNotNull(paidInvoice.getPaidAt());
        assertEquals(BigDecimal.valueOf(500.00), paidInvoice.getPaidAmount());
        assertEquals("CASH", paidInvoice.getPaymentMethod());
    }

    @Test
    @Order(3)
    @DisplayName("Verify overdue invoice detection")
    void testOverdueInvoiceDetection() {
        // Create customer and invoice with past due date
        Customer customer = customerService.create(
            "IT-CUST-003",
            "Overdue Test Company",
            "789 Overdue St",
            "Late City",
            "555-LATE",
            "overdue@test.com"
        );

        LocalDate pastDate = LocalDate.now().minusDays(30);
        Invoice invoice = invoiceService.create(
            customer.getId(),
            pastDate,
            "IT-INV-003",
            "Overdue Test Invoice"
        );

        invoiceService.addLine(invoice.getId(), "Overdue Service",
            BigDecimal.valueOf(1000.00), 1, BigDecimal.valueOf(0.0));
        invoiceService.confirm(invoice.getId());

        // Find overdue invoices
        List<Invoice> overdueInvoices = invoiceService.findOverdue();

        assertFalse(overdueInvoices.isEmpty());
        assertTrue(overdueInvoices.stream()
            .anyMatch(inv -> inv.getInvoiceNumber().equals("IT-INV-003")));
    }

    @Test
    @Order(4)
    @DisplayName("Verify invoice cannot be modified after confirmation")
    void testConfirmedInvoiceCannotBeModified() {
        Customer customer = customerService.create(
            "IT-CUST-004",
            "Modify Test Company",
            "111 Modify St",
            "Mod City",
            "555-MOD",
            "modify@test.com"
        );

        Invoice invoice = invoiceService.create(
            customer.getId(),
            LocalDate.now(),
            "IT-INV-004",
            "Modify Test Invoice"
        );

        invoiceService.addLine(invoice.getId(), "Modifiable Item",
            BigDecimal.valueOf(200.00), 1, BigDecimal.valueOf(0.0));
        invoiceService.confirm(invoice.getId());

        // Attempting to add line should fail
        assertThrows(IllegalStateException.class, () ->
            invoiceService.addLine(invoice.getId(), "Should Fail",
                BigDecimal.valueOf(100.00), 1, BigDecimal.valueOf(0.0))
        );

        // Attempting to delete should fail
        Invoice confirmed = invoiceService.findById(invoice.getId()).orElseThrow();
        assertThrows(IllegalStateException.class, () ->
            invoiceService.deleteLine(invoice.getId(), confirmed.getLines().get(0).getId())
        );
    }

    @Test
    @Order(5)
    @DisplayName("Test customer search functionality")
    void testCustomerSearchFunctionality() {
        // Create multiple customers
        customerService.create("IT-CUST-005", "Alpha Corp", "Alpha St", "Alpha City", "555-001", "alpha@test.com");
        customerService.create("IT-CUST-006", "Beta Industries", "Beta St", "Beta City", "555-002", "beta@test.com");
        customerService.create("IT-CUST-007", "Gamma LLC", "Gamma St", "Gamma City", "555-003", "gamma@test.com");

        // Search by term
        List<Customer> alphaResults = customerService.search("Alpha");
        assertEquals(1, alphaResults.size());
        assertEquals("Alpha Corp", alphaResults.get(0).getName());

        // Search by code
        List<Customer> codeResults = customerService.findByCodeStartingWith("IT-CUST-00");
        assertEquals(5, codeResults.size()); // All customers created in this class

        // Find active customers
        List<Customer> activeCustomers = customerService.findActive();
        assertTrue(activeCustomers.size() >= 7);
        assertTrue(activeCustomers.stream().allMatch(Customer::isActive));
    }

    @Test
    @Order(6)
    @DisplayName("Test invoice statistics calculation")
    void testInvoiceStatisticsCalculation() {
        LocalDate today = LocalDate.now();

        // Create invoices with different statuses
        Customer customer = customerService.create(
            "IT-CUST-008",
            "Stats Test Company",
            "Stats St",
            "Stats City",
            "555-STATS",
            "stats@test.com"
        );

        // Draft invoice
        Invoice draft = invoiceService.create(customer.getId(), today, "IT-STAT-001", "Draft Invoice");
        invoiceService.addLine(draft.getId(), "Draft Item", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(0));

        // Confirmed invoice
        Invoice confirmed = invoiceService.create(customer.getId(), today, "IT-STAT-002", "Confirmed Invoice");
        invoiceService.addLine(confirmed.getId(), "Confirmed Item", BigDecimal.valueOf(200), 1, BigDecimal.valueOf(0));
        invoiceService.confirm(confirmed.getId());

        // Paid invoice
        Invoice paid = invoiceService.create(customer.getId(), today, "IT-STAT-003", "Paid Invoice");
        invoiceService.addLine(paid.getId(), "Paid Item", BigDecimal.valueOf(300), 1, BigDecimal.valueOf(0));
        invoiceService.confirm(paid.getId());
        invoiceService.recordPayment(paid.getId(), today, "CASH", "IT-PAY-STATS");

        // Get statistics
        InvoiceService.InvoiceStats stats = invoiceService.getStatistics();

        assertNotNull(stats);
        assertEquals(3, stats.totalInvoices);
        assertEquals(1, stats.draftCount);
        assertEquals(1, stats.confirmedCount);
        assertEquals(1, stats.paidCount);
    }
}
