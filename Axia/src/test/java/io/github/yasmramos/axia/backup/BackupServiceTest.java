package io.github.yasmramos.axia.backup;

import io.github.yasmramos.axia.EmbeddedPostgresExtension;
import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.repository.*;
import io.github.yasmramos.axia.service.*;
import io.ebean.DB;
import io.ebean.Database;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BackupService.
 */
@ExtendWith(EmbeddedPostgresExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BackupServiceTest {

    private static BackupService backupService;
    private static AccountService accountService;
    private static CustomerService customerService;
    private static Path tempDir;
    private static String backupPath;

    @BeforeAll
    static void setUp() throws IOException {
        backupService = new BackupService();
        
        AccountRepository accountRepo = new AccountRepository();
        accountService = new AccountService(accountRepo);
        customerService = new CustomerService(new CustomerRepository());
        
        accountService.initializeDefaultAccounts();
        customerService.create("BACK-CUST", "Backup Customer", null, null, null, null, null);
        
        tempDir = Files.createTempDirectory("axia_backup_test");
    }

    @AfterAll
    static void tearDown() throws IOException {
        Database db = DB.getDefault();
        db.truncate(Customer.class);
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
    @DisplayName("Should create backup file")
    void testCreateBackup() throws IOException {
        backupPath = backupService.createBackup(tempDir.toString());
        
        assertNotNull(backupPath);
        assertTrue(Files.exists(Path.of(backupPath)));
        assertTrue(backupPath.endsWith(".zip"));
        assertTrue(backupPath.contains("axia_backup_"));
    }

    @Test
    @Order(2)
    @DisplayName("Should create valid zip file")
    void testBackupIsValidZip() throws IOException {
        assertNotNull(backupPath);
        
        try (ZipFile zipFile = new ZipFile(backupPath)) {
            assertTrue(zipFile.size() > 0);
            assertNotNull(zipFile.getEntry("accounts.json"));
            assertNotNull(zipFile.getEntry("customers.json"));
            assertNotNull(zipFile.getEntry("suppliers.json"));
            assertNotNull(zipFile.getEntry("metadata.json"));
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should list backups")
    void testListBackups() throws IOException {
        List<String> backups = backupService.listBackups(tempDir.toString());
        
        assertFalse(backups.isEmpty());
        assertTrue(backups.stream().allMatch(b -> b.startsWith("axia_backup_")));
        assertTrue(backups.stream().allMatch(b -> b.endsWith(".zip")));
    }

    @Test
    @Order(4)
    @DisplayName("Should return empty list for non-existent directory")
    void testListBackupsNonExistentDir() throws IOException {
        List<String> backups = backupService.listBackups("/nonexistent/path");
        
        assertTrue(backups.isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("Should restore backup without errors")
    void testRestoreBackup() throws IOException {
        assertNotNull(backupPath);
        
        // Should not throw exception
        assertDoesNotThrow(() -> backupService.restoreBackup(backupPath));
    }

    @Test
    @Order(6)
    @DisplayName("Should create multiple backups with unique names")
    void testMultipleBackups() throws IOException, InterruptedException {
        String backup1 = backupService.createBackup(tempDir.toString());
        Thread.sleep(1000); // Ensure different timestamp
        String backup2 = backupService.createBackup(tempDir.toString());
        
        assertNotEquals(backup1, backup2);
        assertTrue(Files.exists(Path.of(backup1)));
        assertTrue(Files.exists(Path.of(backup2)));
    }
}
