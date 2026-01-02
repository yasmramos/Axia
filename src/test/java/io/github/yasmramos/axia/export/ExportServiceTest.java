package io.github.yasmramos.axia.export;

import io.github.yasmramos.axia.EmbeddedPostgresExtension;
import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.repository.*;
import io.github.yasmramos.axia.service.*;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExportService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExportServiceTest {

    private static ExportService exportService;
    private static AccountService accountService;
    private static CustomerService customerService;
    private static SupplierService supplierService;
    private static JournalEntryService journalEntryService;
    private static InvoiceService invoiceService;
    private static Path tempDir;

    @BeforeAll
    static void setUp() throws IOException {
        exportService = new ExportService();
        
        AccountRepository accountRepo = new AccountRepository();
        JournalEntryRepository jeRepo = new JournalEntryRepository();
        
        accountService = new AccountService(accountRepo);
        customerService = new CustomerService(new CustomerRepository());
        supplierService = new SupplierService(new SupplierRepository());
        journalEntryService = new JournalEntryService(jeRepo, accountRepo);
        invoiceService = new InvoiceService(new InvoiceRepository(), accountRepo, journalEntryService);
        
        accountService.initializeDefaultAccounts();
        
        // Create test data
        customerService.create("EXP-CUST", "Export Customer", "123456789", "Address 1", "City", "555-1234", "test@test.com");
        supplierService.create("EXP-SUPP", "Export Supplier", "987654321", "Address 2", "City", "555-5678", "supp@test.com");
        
        Account cash = accountService.findByCode("1.1.01").orElseThrow();
        Account revenue = accountService.findByCode("4.1.01").orElseThrow();
        
        JournalEntry entry = journalEntryService.create(LocalDate.now(), "Export Test Entry", "REF-001");
        journalEntryService.addLine(entry, cash, new BigDecimal("500"), null, "Debit");
        journalEntryService.addLine(entry, revenue, null, new BigDecimal("500"), "Credit");
        journalEntryService.post(entry.getId());
        
        Customer customer = customerService.findByCode("EXP-CUST").orElseThrow();
        Invoice invoice = invoiceService.createSaleInvoice(customer, LocalDate.now(), LocalDate.now().plusDays(30));
        invoiceService.addLine(invoice, "Export Product", new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("21"), revenue);
        
        tempDir = Files.createTempDirectory("axia_export_test");
    }

    @AfterAll
    static void tearDown() throws IOException {
        Database db = DB.getDefault();
        db.truncate(InvoiceLine.class);
        db.truncate(Invoice.class);
        db.truncate(JournalEntryLine.class);
        db.truncate(JournalEntry.class);
        db.truncate(Customer.class);
        db.truncate(Supplier.class);
        db.truncate(Account.class);
        
        // Clean up temp files
        if (tempDir != null) {
            Files.walk(tempDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should export accounts to CSV")
    void testExportAccountsToCsv() throws IOException {
        List<Account> accounts = accountService.findAll();
        String outputPath = tempDir.resolve("accounts.csv").toString();
        
        exportService.exportAccountsToCsv(accounts, outputPath);
        
        assertTrue(Files.exists(Path.of(outputPath)));
        String content = Files.readString(Path.of(outputPath));
        assertTrue(content.contains("Code,Name,Type"));
        assertTrue(content.contains("1.1.01"));
    }

    @Test
    @Order(2)
    @DisplayName("Should export customers to CSV")
    void testExportCustomersToCsv() throws IOException {
        List<Customer> customers = customerService.findAll();
        String outputPath = tempDir.resolve("customers.csv").toString();
        
        exportService.exportCustomersToCsv(customers, outputPath);
        
        assertTrue(Files.exists(Path.of(outputPath)));
        String content = Files.readString(Path.of(outputPath));
        assertTrue(content.contains("Name,Tax ID,Email"));
        assertTrue(content.contains("Export Customer"));
    }

    @Test
    @Order(3)
    @DisplayName("Should export suppliers to CSV")
    void testExportSuppliersToCsv() throws IOException {
        List<Supplier> suppliers = supplierService.findAll();
        String outputPath = tempDir.resolve("suppliers.csv").toString();
        
        exportService.exportSuppliersToCsv(suppliers, outputPath);
        
        assertTrue(Files.exists(Path.of(outputPath)));
        String content = Files.readString(Path.of(outputPath));
        assertTrue(content.contains("Name,Tax ID,Email"));
        assertTrue(content.contains("Export Supplier"));
    }

    @Test
    @Order(4)
    @DisplayName("Should export journal entries to CSV")
    void testExportJournalEntriesToCsv() throws IOException {
        List<JournalEntry> entries = journalEntryService.findAll();
        String outputPath = tempDir.resolve("journal_entries.csv").toString();
        
        exportService.exportJournalEntriesToCsv(entries, outputPath);
        
        assertTrue(Files.exists(Path.of(outputPath)));
        String content = Files.readString(Path.of(outputPath));
        assertTrue(content.contains("Entry Number,Date,Description"));
        assertTrue(content.contains("REF-001"));
    }

    @Test
    @Order(5)
    @DisplayName("Should export invoices to CSV")
    void testExportInvoicesToCsv() throws IOException {
        List<Invoice> invoices = invoiceService.findAll();
        String outputPath = tempDir.resolve("invoices.csv").toString();
        
        exportService.exportInvoicesToCsv(invoices, outputPath);
        
        assertTrue(Files.exists(Path.of(outputPath)));
        String content = Files.readString(Path.of(outputPath));
        assertTrue(content.contains("Invoice Number,Date,Due Date"));
        assertTrue(content.contains("Export Customer"));
    }

    @Test
    @Order(6)
    @DisplayName("Should handle empty list export")
    void testExportEmptyList() throws IOException {
        String outputPath = tempDir.resolve("empty.csv").toString();
        
        exportService.exportAccountsToCsv(List.of(), outputPath);
        
        assertTrue(Files.exists(Path.of(outputPath)));
        String content = Files.readString(Path.of(outputPath));
        assertTrue(content.contains("Code,Name,Type")); // Header only
    }
}
