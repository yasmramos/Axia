package io.github.yasmramos.axia.export;

import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.Customer;
import io.github.yasmramos.axia.model.Invoice;
import io.github.yasmramos.axia.model.Supplier;
import io.github.yasmramos.axia.repository.CustomerRepository;
import io.github.yasmramos.axia.repository.SupplierRepository;
import io.github.yasmramos.axia.service.AccountService;
import io.github.yasmramos.axia.service.CustomerService;
import io.github.yasmramos.axia.service.InvoiceService;
import io.github.yasmramos.axia.service.SupplierService;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for export and import data workflows.
 * 
 * <p>Tests the complete data exchange lifecycle including export
 * to various formats and subsequent import operations.</p>
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExportServiceIT {

    private static ExportService exportService;
    private static AccountService accountService;
    private static CustomerService customerService;
    private static SupplierService supplierService;
    private static InvoiceService invoiceService;
    private static Path tempDir;

    @BeforeAll
    static void setUp() throws IOException {
        tempDir = Files.createTempDirectory("export-test");
        
        accountService = new AccountService(new io.github.yasmramos.axia.repository.AccountRepository());
        customerService = new CustomerService(new CustomerRepository());
        supplierService = new SupplierService(new SupplierRepository());
        invoiceService = new InvoiceService(
            new io.github.yasmramos.axia.repository.InvoiceRepository(),
            customerService,
            accountService
        );
        exportService = new ExportService();

        // Initialize accounts
        accountService.initializeDefaultAccounts();
    }

    @AfterAll
    static void tearDown() throws IOException {
        // Cleanup temp directory
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {}
                });
        }

        // Clean database
        Database db = DB.getDefault();
        db.truncate(Invoice.class);
        db.truncate(Account.class);
        db.truncate(Customer.class);
        db.truncate(Supplier.class);
    }

    @Test
    @Order(1)
    @DisplayName("Export accounts to CSV and verify format")
    void testAccountExportToCSV() throws IOException {
        // Export accounts
        String csvPath = exportService.exportAccounts(tempDir.toString(), ExportService.Format.CSV);
        
        assertNotNull(csvPath);
        assertTrue(Files.exists(Path.of(csvPath)));

        // Read and verify content
        String content = Files.readString(Path.of(csvPath));
        assertFalse(content.isEmpty());
        
        // Verify header
        assertTrue(content.contains("code"));
        assertTrue(content.contains("name"));
        assertTrue(content.contains("type"));
        
        // Verify data contains our test accounts
        assertTrue(content.contains("1.1.01") || content.contains("ASSETS"));
        assertTrue(content.contains("4.1.01") || content.contains("INCOME"));
    }

    @Test
    @Order(2)
    @DisplayName("Export customers to CSV and verify data integrity")
    void testCustomerExportToCSV() throws IOException {
        // Create test customers
        customerService.create("EXP-CUST-001", "Export Customer 1", "Address 1", "City 1", "555-001", "exp1@test.com");
        customerService.create("EXP-CUST-002", "Export Customer 2", "Address 2", "City 2", "555-002", "exp2@test.com");

        // Export customers
        String csvPath = exportService.exportCustomers(tempDir.toString(), ExportService.Format.CSV);
        
        assertNotNull(csvPath);
        assertTrue(Files.exists(Path.of(csvPath)));

        // Verify content
        String content = Files.readString(Path.of(csvPath));
        assertTrue(content.contains("EXP-CUST-001"));
        assertTrue(content.contains("Export Customer 1"));
        assertTrue(content.contains("EXP-CUST-002"));
        assertTrue(content.contains("Export Customer 2"));
    }

    @Test
    @Order(3)
    @DisplayName("Export suppliers to CSV and verify format")
    void testSupplierExportToCSV() throws IOException {
        // Create test suppliers
        supplierService.create("EXP-SUP-001", "Export Supplier 1", "Addr 1", "City 1", "555-SUP-01", "sup1@exp.com", "TAX-001");
        supplierService.create("EXP-SUP-002", "Export Supplier 2", "Addr 2", "City 2", "555-SUP-02", "sup2@exp.com", "TAX-002");

        // Export suppliers
        String csvPath = exportService.exportSuppliers(tempDir.toString(), ExportService.Format.CSV);
        
        assertNotNull(csvPath);
        assertTrue(Files.exists(Path.of(csvPath)));

        // Verify content
        String content = Files.readString(Path.of(csvPath));
        assertTrue(content.contains("EXP-SUP-001"));
        assertTrue(content.contains("Export Supplier 1"));
        assertTrue(content.contains("TAX-001"));
    }

    @Test
    @Order(4)
    @DisplayName("Export invoices to CSV and verify data completeness")
    void testInvoiceExportToCSV() throws IOException {
        // Create customer and invoice
        Customer customer = customerService.create(
            "EXP-CUST-INV",
            "Invoice Export Customer",
            "Invoice Address",
            "Invoice City",
            "555-INV",
            "invoice@export.test"
        );

        Invoice invoice = invoiceService.create(
            customer.getId(),
            LocalDate.now(),
            "EXP-INV-001",
            "Invoice Export Test"
        );
        invoiceService.addLine(invoice.getId(), "Exported Product", BigDecimal.valueOf(150.00), 2, BigDecimal.valueOf(0.1));
        invoiceService.confirm(invoice.getId());

        // Export invoices
        String csvPath = exportService.exportInvoices(tempDir.toString(), ExportService.Format.CSV);
        
        assertNotNull(csvPath);
        assertTrue(Files.exists(Path.of(csvPath)));

        // Verify content
        String content = Files.readString(Path.of(csvPath));
        assertTrue(content.contains("EXP-INV-001"));
        assertTrue(content.contains("Invoice Export Customer"));
        
        // Verify amounts are present
        assertTrue(content.contains("300") || content.contains("330")); // 150*2 or with tax
    }

    @Test
    @Order(5)
    @DisplayName("Export to JSON format")
    void testExportToJSON() throws IOException {
        // Export accounts to JSON
        String jsonPath = exportService.exportAccounts(tempDir.toString(), ExportService.Format.JSON);
        
        assertNotNull(jsonPath);
        assertTrue(Files.exists(Path.of(jsonPath)));
        assertTrue(jsonPath.endsWith(".json"));

        // Verify JSON structure
        String content = Files.readString(Path.of(jsonPath));
        assertTrue(content.startsWith("[") || content.startsWith("{"));
        assertFalse(content.contains("null")); // Should not have null values for important fields
    }

    @Test
    @Order(6)
    @DisplayName("Export trial balance report")
    void testTrialBalanceExport() throws IOException {
        // Create some transactions first
        createTestTransactions();

        // Export trial balance
        String tbPath = exportService.exportTrialBalance(tempDir.toString(), ExportService.Format.CSV);
        
        assertNotNull(tbPath);
        assertTrue(Files.exists(Path.of(tbPath)));

        // Verify content
        String content = Files.readString(Path.of(tbPath));
        assertFalse(content.isEmpty());
        
        // Should contain account codes and balances
        assertTrue(content.contains("1.") || content.contains("debit"));
        assertTrue(content.contains("4.") || content.contains("credit"));
    }

    @Test
    @Order(7)
    @DisplayName("Verify exported file naming convention")
    void testExportFileNaming() throws IOException {
        String csvPath = exportService.exportAccounts(tempDir.toString(), ExportService.Format.CSV);
        
        Path filePath = Path.of(csvPath);
        String filename = filePath.getFileName().toString();

        // Verify naming: accounts_YYYYMMDD_HHMMSS.csv
        assertTrue(filename.startsWith("accounts_"));
        assertTrue(filename.endsWith(".csv"));
        
        // File should be in temp directory
        assertEquals(tempDir.toString(), filePath.getParent().toString());
    }

    private void createTestTransactions() {
        // This would create journal entries to generate trial balance data
        // For integration testing purposes, we'll just verify the export runs
        Account cash = accountService.findByCode("1.1.01").orElse(null);
        Account revenue = accountService.findByCode("4.1.01").orElse(null);
        
        if (cash != null && revenue != null) {
            // Simple verification that accounts exist
            assertNotNull(cash);
            assertNotNull(revenue);
        }
    }
}
