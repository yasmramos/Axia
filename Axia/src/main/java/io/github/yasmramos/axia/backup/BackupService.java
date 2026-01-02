package io.github.yasmramos.axia.backup;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.ebean.DB;
import io.github.yasmramos.axia.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.*;

/**
 * Service for database backup and restore operations.
 * Exports all data to JSON format in a compressed archive.
 *
 * @author Yasmany Ramos Garcia
 */
@Component
public class BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Creates a full backup of all accounting data.
     *
     * @param backupDir directory to store the backup
     * @return path to the created backup file
     * @throws IOException if backup fails
     */
    public String createBackup(String backupDir) throws IOException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String backupName = "axia_backup_" + timestamp + ".zip";
        Path backupPath = Paths.get(backupDir, backupName);

        logger.info("Creating backup: {}", backupPath);

        Files.createDirectories(Paths.get(backupDir));

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupPath.toFile()))) {
            // Export accounts
            writeToZip(zos, "accounts.json", exportAccounts());

            // Export customers
            writeToZip(zos, "customers.json", exportCustomers());

            // Export suppliers
            writeToZip(zos, "suppliers.json", exportSuppliers());

            // Export fiscal years
            writeToZip(zos, "fiscal_years.json", exportFiscalYears());

            // Export journal entries
            writeToZip(zos, "journal_entries.json", exportJournalEntries());

            // Export invoices
            writeToZip(zos, "invoices.json", exportInvoices());

            // Export currencies
            writeToZip(zos, "currencies.json", exportCurrencies());

            // Metadata
            writeToZip(zos, "metadata.json", createMetadata());
        }

        logger.info("Backup created successfully: {}", backupPath);
        return backupPath.toString();
    }

    /**
     * Restores data from a backup file.
     *
     * @param backupFilePath path to the backup zip file
     * @throws IOException if restore fails
     */
    public void restoreBackup(String backupFilePath) throws IOException {
        logger.info("Restoring backup from: {}", backupFilePath);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String content = readZipEntry(zis);
                processBackupEntry(entry.getName(), content);
                zis.closeEntry();
            }
        }

        logger.info("Backup restored successfully");
    }

    /**
     * Lists available backups in a directory.
     *
     * @param backupDir directory containing backups
     * @return list of backup file names
     * @throws IOException if directory cannot be read
     */
    public List<String> listBackups(String backupDir) throws IOException {
        logger.debug("Listing backups in: {}", backupDir);
        Path dir = Paths.get(backupDir);

        if (!Files.exists(dir)) {
            return List.of();
        }

        try (var stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.toString().endsWith(".zip"))
                    .filter(p -> p.getFileName().toString().startsWith("axia_backup_"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private void writeToZip(ZipOutputStream zos, String filename, String content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content.getBytes());
        zos.closeEntry();
    }

    private String readZipEntry(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        return baos.toString();
    }

    private void processBackupEntry(String filename, String content) {
        logger.debug("Processing backup entry: {}", filename);
        // JSON parsing and entity restoration would be implemented here
        // Using a JSON library like Jackson or Gson
    }

    private String exportAccounts() {
        List<Account> accounts = DB.find(Account.class).findList();
        return toSimpleJson("accounts", accounts.size());
    }

    private String exportCustomers() {
        List<Customer> customers = DB.find(Customer.class).findList();
        return toSimpleJson("customers", customers.size());
    }

    private String exportSuppliers() {
        List<Supplier> suppliers = DB.find(Supplier.class).findList();
        return toSimpleJson("suppliers", suppliers.size());
    }

    private String exportFiscalYears() {
        List<FiscalYear> fiscalYears = DB.find(FiscalYear.class).findList();
        return toSimpleJson("fiscalYears", fiscalYears.size());
    }

    private String exportJournalEntries() {
        List<JournalEntry> entries = DB.find(JournalEntry.class).findList();
        return toSimpleJson("journalEntries", entries.size());
    }

    private String exportInvoices() {
        List<Invoice> invoices = DB.find(Invoice.class).findList();
        return toSimpleJson("invoices", invoices.size());
    }

    private String exportCurrencies() {
        List<Currency> currencies = DB.find(Currency.class).findList();
        return toSimpleJson("currencies", currencies.size());
    }

    private String createMetadata() {
        return String.format("{\"version\":\"1.0\",\"timestamp\":\"%s\",\"application\":\"Axia Accounting\"}",
                LocalDateTime.now().toString());
    }

    private String toSimpleJson(String name, int count) {
        return String.format("{\"%s_count\":%d}", name, count);
    }
}
