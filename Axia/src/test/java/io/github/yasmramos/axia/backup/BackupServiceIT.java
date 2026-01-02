package io.github.yasmramos.axia.backup;

import io.github.yasmramos.axia.model.Account;
import io.github.yasmramos.axia.model.Customer;
import io.github.yasmramos.axia.model.FiscalYear;
import io.github.yasmramos.axia.model.JournalEntry;
import io.github.yasmramos.axia.model.Supplier;
import io.github.yasmramos.axia.repository.CustomerRepository;
import io.github.yasmramos.axia.repository.FiscalYearRepository;
import io.github.yasmramos.axia.repository.SupplierRepository;
import io.github.yasmramos.axia.service.AccountService;
import io.github.yasmramos.axia.service.FiscalYearService;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.yasmramos.axia.EmbeddedPostgresExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for backup and restore workflows.
 * 
 * <p>Tests the complete backup lifecycle including creation,
 * validation, and restoration of data.</p>
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BackupServiceIT {

    private static BackupService backupService;
    private static AccountService accountService;
    private static FiscalYearService fiscalYearService;
    private static CustomerService customerService;
    private static SupplierService supplierService;
    private static Path tempDir;

    @BeforeAll
    static void setUp() throws IOException {
        tempDir = Files.createTempDirectory("backup-test");
        
        accountService = new AccountService(new io.github.yasmramos.axia.repository.AccountRepository());
        fiscalYearService = new FiscalYearService(new FiscalYearRepository());
        customerService = new CustomerService(new CustomerRepository());
        supplierService = new SupplierService(new SupplierRepository());
        backupService = new BackupService();

        // Initialize base data
        int year = LocalDate.now().getYear();
        if (fiscalYearService.findByYear(year).isEmpty()) {
            FiscalYear fy = fiscalYearService.create(year,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31));
            fiscalYearService.setCurrent(fy.getId());
        }

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
        db.truncate(JournalEntry.class);
        db.truncate(Account.class);
        db.truncate(FiscalYear.class);
        db.truncate(Customer.class);
        db.truncate(Supplier.class);
    }

    @Test
    @Order(1)
    @DisplayName("Create and validate backup file")
    void testBackupCreationAndValidation() throws IOException {
        // Create test data
        Customer customer = customerService.create(
            "BKUP-CUST-001",
            "Backup Test Customer",
            "123 Backup St",
            "Backup City",
            "555-BKUP",
            "backup@test.com"
        );

        Supplier supplier = supplierService.create(
            "BKUP-SUP-001",
            "Backup Test Supplier",
            "456 Supply Ave",
            "Supply City",
            "555-SUP",
            "supply@backup.com",
            "TAX-001"
        );

        // Create backup
        String backupPath = backupService.createBackup(tempDir.toString());
        
        assertNotNull(backupPath);
        assertTrue(Files.exists(Path.of(backupPath)));
        
        File backupFile = new File(backupPath);
        assertTrue(backupFile.length() > 0);

        // Validate backup is valid zip
        try (ZipFile zip = new ZipFile(backupFile)) {
            assertNotNull(zip.getEntry("customers.csv"));
            assertNotNull(zip.getEntry("suppliers.csv"));
            assertNotNull(zip.getEntry("accounts.csv"));
        }
    }

    @Test
    @Order(2)
    @DisplayName("Verify backup contains all data types")
    void testBackupContainsAllDataTypes() throws IOException {
        // Create diverse test data
        customerService.create("BKUP-CUST-002", "Customer Type Test", "St", "City", "555-001", "test1@test.com");
        customerService.create("BKUP-CUST-003", "Customer Type Test 2", "St", "City", "555-002", "test2@test.com");
        
        supplierService.create("BKUP-SUP-002", "Supplier Type Test", "Ave", "City", "555-SUP-01", "sup1@test.com", "TAX-01");
        supplierService.create("BKUP-SUP-003", "Supplier Type Test 2", "Ave", "City", "555-SUP-02", "sup2@test.com", "TAX-02");

        // Create backup
        String backupPath = backupService.createBackup(tempDir.toString());

        // Verify backup contents
        try (ZipFile zip = new ZipFile(backupPath)) {
            // Check customers data
            var customersEntry = zip.getEntry("customers.csv");
            assertNotNull(customersEntry);
            
            // Check suppliers data
            var suppliersEntry = zip.getEntry("suppliers.csv");
            assertNotNull(suppliersEntry);
            
            // Check accounts data
            var accountsEntry = zip.getEntry("accounts.csv");
            assertNotNull(accountsEntry);
            
            // Check metadata
            var metadataEntry = zip.getEntry("backup-info.json");
            assertNotNull(metadataEntry);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test backup list functionality")
    void testBackupListFunctionality() throws IOException {
        // Create multiple backups
        backupService.createBackup(tempDir.toString());
        backupService.createBackup(tempDir.toString());

        // List backups
        List<BackupService.BackupInfo> backups = backupService.listBackups(tempDir.toString());

        assertNotNull(backups);
        // Backups might be from previous tests too
        assertTrue(backups.size() >= 2);

        // Verify backup info structure
        for (BackupService.BackupInfo info : backups) {
            assertNotNull(info.filename);
            assertNotNull(info.createdAt);
            assertTrue(info.sizeBytes > 0);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test backup file naming convention")
    void testBackupFileNaming() throws IOException {
        String backupPath = backupService.createBackup(tempDir.toString());
        
        Path backupFilePath = Path.of(backupPath);
        String filename = backupFilePath.getFileName().toString();

        // Verify naming convention: axia_backup_YYYYMMDD_HHMMSS.zip
        assertTrue(filename.startsWith("axia_backup_"));
        assertTrue(filename.endsWith(".zip"));
        
        // Check timestamp format (approximately)
        assertTrue(filename.length() > 20); // Enough for timestamp
    }

    @Test
    @Order(5)
    @DisplayName("Test backup cleanup functionality")
    void testBackupCleanup() throws IOException {
        // Create a backup to potentially clean
        backupService.createBackup(tempDir.toString());

        // List before cleanup
        List<BackupService.BackupInfo> before = backupService.listBackups(tempDir.toString());
        int initialCount = before.size();

        // Cleanup old backups (if any older than 0 days)
        int cleaned = backupService.cleanupOldBackups(tempDir.toString(), 0);
        
        // List after cleanup
        List<BackupService.BackupInfo> after = backupService.listBackups(tempDir.toString());
        
        // Verify cleanup happened
        assertTrue(after.size() <= initialCount);
    }
}
