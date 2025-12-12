package io.github.yasmramos.axia.service;

import io.github.yasmramos.axia.model.*;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InvoiceService.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceServiceTest {

    private static InvoiceService invoiceService;
    private static CustomerService customerService;
    private static SupplierService supplierService;
    private static AccountService accountService;
    private static Customer testCustomer;
    private static Supplier testSupplier;

    @BeforeAll
    static void setUp() {
        invoiceService = new InvoiceService();
        customerService = new CustomerService();
        supplierService = new SupplierService();
        accountService = new AccountService();
        
        // Initialize default accounts
        accountService.initializeDefaultAccounts();
        
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
        assertNotNull(invoice.getInvoiceNumber());
        assertTrue(invoice.getInvoiceNumber().startsWith("FV-"));
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
        assertTrue(invoice.getInvoiceNumber().startsWith("FC-"));
        assertEquals(InvoiceType.PURCHASE, invoice.getType());
        assertEquals(testSupplier.getId(), invoice.getSupplier().getId());
    }

    @Test
    @Order(3)
    @DisplayName("Should add lines to invoice")
    void testAddLine() {
        Invoice invoice = invoiceService.createSaleInvoice(testCustomer, LocalDate.now(), null);
        
        invoiceService.addLine(invoice, "Product A", new BigDecimal("2"), 
                new BigDecimal("100.00"), new BigDecimal("16"), null);
        invoiceService.addLine(invoice, "Product B", new BigDecimal("1"), 
                new BigDecimal("50.00"), new BigDecimal("16"), null);

        Invoice updated = invoiceService.findById(invoice.getId()).orElseThrow();
        
        assertEquals(2, updated.getLines().size());
        assertEquals(new BigDecimal("250.0000"), updated.getSubtotal());
        assertEquals(new BigDecimal("40.0000"), updated.getTaxAmount());
        assertEquals(new BigDecimal("290.0000"), updated.getTotal());
    }

    @Test
    @Order(4)
    @DisplayName("Should post sales invoice and create journal entry")
    void testPostSalesInvoice() {
        Invoice invoice = invoiceService.createSaleInvoice(testCustomer, LocalDate.now(), null);
        invoiceService.addLine(invoice, "Service", new BigDecimal("1"), 
                new BigDecimal("1000.00"), new BigDecimal("16"), null);

        Invoice posted = invoiceService.post(invoice.getId());

        assertEquals(InvoiceStatus.POSTED, posted.getStatus());
        assertNotNull(posted.getJournalEntry());
        assertTrue(posted.getJournalEntry().isPosted());
    }

    @Test
    @Order(5)
    @DisplayName("Should post purchase invoice and create journal entry")
    void testPostPurchaseInvoice() {
        Invoice invoice = invoiceService.createPurchaseInvoice(testSupplier, LocalDate.now(), null);
        invoiceService.addLine(invoice, "Office Supplies", new BigDecimal("10"), 
                new BigDecimal("25.00"), new BigDecimal("16"), null);

        Invoice posted = invoiceService.post(invoice.getId());

        assertEquals(InvoiceStatus.POSTED, posted.getStatus());
        assertNotNull(posted.getJournalEntry());
    }

    @Test
    @Order(6)
    @DisplayName("Should not post non-draft invoice")
    void testPostNonDraftInvoiceThrowsException() {
        Invoice invoice = invoiceService.createSaleInvoice(testCustomer, LocalDate.now(), null);
        invoiceService.addLine(invoice, "Item", BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO, null);
        invoiceService.post(invoice.getId());

        assertThrows(IllegalStateException.class, () -> 
            invoiceService.post(invoice.getId())
        );
    }

    @Test
    @Order(7)
    @DisplayName("Should not post empty invoice")
    void testPostEmptyInvoiceThrowsException() {
        Invoice invoice = invoiceService.createSaleInvoice(testCustomer, LocalDate.now(), null);

        assertThrows(IllegalStateException.class, () -> 
            invoiceService.post(invoice.getId())
        );
    }

    @Test
    @Order(8)
    @DisplayName("Should mark invoice as paid")
    void testMarkAsPaid() {
        Invoice invoice = invoiceService.createSaleInvoice(testCustomer, LocalDate.now(), null);
        invoiceService.addLine(invoice, "Item", BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO, null);
        invoiceService.post(invoice.getId());

        Invoice paid = invoiceService.markAsPaid(invoice.getId());

        assertEquals(InvoiceStatus.PAID, paid.getStatus());
    }

    @Test
    @Order(9)
    @DisplayName("Should cancel invoice")
    void testCancelInvoice() {
        Invoice invoice = invoiceService.createSaleInvoice(testCustomer, LocalDate.now(), null);
        invoiceService.addLine(invoice, "Item", BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO, null);
        invoiceService.post(invoice.getId());

        Invoice cancelled = invoiceService.cancel(invoice.getId());

        assertEquals(InvoiceStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @Order(10)
    @DisplayName("Should find invoices by type")
    void testFindByType() {
        List<Invoice> salesInvoices = invoiceService.findByType(InvoiceType.SALE);
        List<Invoice> purchaseInvoices = invoiceService.findByType(InvoiceType.PURCHASE);

        assertTrue(salesInvoices.stream().allMatch(i -> i.getType() == InvoiceType.SALE));
        assertTrue(purchaseInvoices.stream().allMatch(i -> i.getType() == InvoiceType.PURCHASE));
    }

    @Test
    @Order(11)
    @DisplayName("Should find invoices by status")
    void testFindByStatus() {
        List<Invoice> postedInvoices = invoiceService.findByStatus(InvoiceStatus.POSTED);

        assertTrue(postedInvoices.stream().allMatch(i -> i.getStatus() == InvoiceStatus.POSTED));
    }

    @Test
    @Order(12)
    @DisplayName("Should find invoices by date range")
    void testFindByDateRange() {
        LocalDate today = LocalDate.now();
        List<Invoice> invoices = invoiceService.findByDateRange(today.minusDays(1), today.plusDays(1));

        assertFalse(invoices.isEmpty());
    }
}
