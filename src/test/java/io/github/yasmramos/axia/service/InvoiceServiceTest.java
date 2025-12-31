package io.github.yasmramos.axia.service;

import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.repository.*;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InvoiceService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceServiceTest {

    private static InvoiceService invoiceService;
    private static CustomerService customerService;
    private static SupplierService supplierService;
    private static AccountService accountService;
    private static Customer testCustomer;
    private static Supplier testSupplier;
    private static Account salesAccount;

    @BeforeAll
    static void setUp() {
        AccountRepository accountRepo = new AccountRepository();
        JournalEntryRepository jeRepo = new JournalEntryRepository();
        JournalEntryService jes = new JournalEntryService(jeRepo, accountRepo);
        invoiceService = new InvoiceService(new InvoiceRepository(), accountRepo, jes);
        customerService = new CustomerService(new CustomerRepository());
        supplierService = new SupplierService(new SupplierRepository());
        accountService = new AccountService(accountRepo);
        
        // Initialize default accounts
        accountService.initializeDefaultAccounts();
        
        // Get sales account for lines
        salesAccount = accountService.findByCode("4.1.01").orElse(null);
        
        // Create test customer and supplier
        testCustomer = customerService.create("INV-CUST", "Invoice Test Customer", 
                null, null, null, null, null);
        testSupplier = supplierService.create("INV-SUPP", "Invoice Test Supplier", 
                null, null, null, null, null);
    }

    @AfterAll
    static void tearDown() {
        Database db = DB.getDefault();
        db.truncate(InvoiceLine.class);
        db.truncate(Invoice.class);
        db.truncate(JournalEntryLine.class);
        db.truncate(JournalEntry.class);
        db.truncate(Customer.class);
        db.truncate(Supplier.class);
        db.truncate(Account.class);
    }

    @Test
    @Order(1)
    @DisplayName("Should create sales invoice")
    void testCreateSalesInvoice() {
        Invoice invoice = invoiceService.createSaleInvoice(
                testCustomer,
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );

        assertNotNull(invoice);
        assertNotNull(invoice.getId());
        assertEquals(InvoiceType.SALE, invoice.getType());
        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
        assertEquals(testCustomer.getId(), invoice.getCustomer().getId());
    }

    @Test
    @Order(2)
    @DisplayName("Should create purchase invoice")
    void testCreatePurchaseInvoice() {
        Invoice invoice = invoiceService.createPurchaseInvoice(
                testSupplier,
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );

        assertNotNull(invoice);
        assertEquals(InvoiceType.PURCHASE, invoice.getType());
        assertEquals(testSupplier.getId(), invoice.getSupplier().getId());
    }

    @Test
    @Order(3)
    @DisplayName("Should add line to invoice")
    void testAddInvoiceLine() {
        List<Invoice> invoices = invoiceService.findByType(InvoiceType.SALE);
        Invoice invoice = invoices.get(0);
        
        Invoice updated = invoiceService.addLine(
                invoice,
                "Product A",
                new BigDecimal("2"),
                new BigDecimal("100.00"),
                new BigDecimal("21.00"),
                salesAccount
        );

        assertNotNull(updated);
        assertFalse(updated.getLines().isEmpty());
        InvoiceLine line = updated.getLines().get(0);
        assertEquals("Product A", line.getDescription());
    }

    @Test
    @Order(4)
    @DisplayName("Should calculate invoice totals")
    void testCalculateInvoiceTotals() {
        List<Invoice> invoices = invoiceService.findByType(InvoiceType.SALE);
        Invoice invoice = invoices.get(0);
        
        // Add another line
        invoiceService.addLine(invoice, "Product B", new BigDecimal("1"), 
                new BigDecimal("50.00"), new BigDecimal("21.00"), salesAccount);
        
        Invoice refreshed = invoiceService.findById(invoice.getId()).orElseThrow();
        
        // Should have calculated totals
        assertNotNull(refreshed.getSubtotal());
        assertNotNull(refreshed.getTotal());
        assertTrue(refreshed.getTotal().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @Order(5)
    @DisplayName("Should post sales invoice and create journal entry")
    void testPostSalesInvoice() {
        List<Invoice> invoices = invoiceService.findByType(InvoiceType.SALE);
        Invoice invoice = invoices.get(0);
        
        Invoice posted = invoiceService.post(invoice.getId());
        
        assertEquals(InvoiceStatus.POSTED, posted.getStatus());
        assertNotNull(posted.getJournalEntry());
        assertTrue(posted.getJournalEntry().isPosted());
        
        // Verify journal entry has correct structure
        JournalEntry je = posted.getJournalEntry();
        assertFalse(je.getLines().isEmpty());
    }

    @Test
    @Order(6)
    @DisplayName("Should cancel invoice")
    void testCancelInvoice() {
        // Create new invoice for cancellation
        Invoice invoice = invoiceService.createSaleInvoice(
                testCustomer,
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );
        invoiceService.addLine(invoice, "To Cancel", new BigDecimal("1"), 
                new BigDecimal("100.00"), new BigDecimal("21.00"), salesAccount);
        
        invoiceService.cancel(invoice.getId());
        
        Invoice cancelled = invoiceService.findById(invoice.getId()).orElseThrow();
        assertEquals(InvoiceStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @Order(7)
    @DisplayName("Should find invoices by status")
    void testFindByStatus() {
        List<Invoice> posted = invoiceService.findByStatus(InvoiceStatus.POSTED);
        List<Invoice> cancelled = invoiceService.findByStatus(InvoiceStatus.CANCELLED);
        
        assertFalse(posted.isEmpty());
        assertFalse(cancelled.isEmpty());
        assertTrue(posted.stream().allMatch(i -> i.getStatus() == InvoiceStatus.POSTED));
        assertTrue(cancelled.stream().allMatch(i -> i.getStatus() == InvoiceStatus.CANCELLED));
    }

    @Test
    @Order(8)
    @DisplayName("Should find invoices by type")
    void testFindByType() {
        List<Invoice> sales = invoiceService.findByType(InvoiceType.SALE);
        List<Invoice> purchases = invoiceService.findByType(InvoiceType.PURCHASE);
        
        assertFalse(sales.isEmpty());
        assertFalse(purchases.isEmpty());
        assertTrue(sales.stream().allMatch(i -> i.getType() == InvoiceType.SALE));
        assertTrue(purchases.stream().allMatch(i -> i.getType() == InvoiceType.PURCHASE));
    }

    @Test
    @Order(9)
    @DisplayName("Should find all invoices")
    void testFindAll() {
        List<Invoice> all = invoiceService.findAll();
        assertFalse(all.isEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("Should mark invoice as paid")
    void testMarkAsPaid() {
        // Create and post a new invoice
        Invoice invoice = invoiceService.createSaleInvoice(
                testCustomer,
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );
        invoiceService.addLine(invoice, "Paid Item", new BigDecimal("1"), 
                new BigDecimal("100.00"), new BigDecimal("21.00"), salesAccount);
        invoiceService.post(invoice.getId());
        
        Invoice paid = invoiceService.markAsPaid(invoice.getId());
        
        assertEquals(InvoiceStatus.PAID, paid.getStatus());
    }
}
