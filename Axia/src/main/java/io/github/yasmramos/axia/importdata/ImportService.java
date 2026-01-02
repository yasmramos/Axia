package io.github.yasmramos.axia.importdata;

import io.github.yasmramos.veld.annotation.Component;
import io.github.yasmramos.veld.annotation.Inject;

import io.github.yasmramos.axia.model.*;
import io.github.yasmramos.axia.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for importing accounting data from CSV files.
 *
 * @author Yasmany Ramos Garcia
 */
@Component
public class ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);

    private final AccountService accountService;
    private final CustomerService customerService;
    private final SupplierService supplierService;

    @Inject
    public ImportService(AccountService accountService, CustomerService customerService, SupplierService supplierService) {
        this.accountService = accountService;
        this.customerService = customerService;
        this.supplierService = supplierService;
    }

    /**
     * Imports accounts from a CSV file.
     * Expected format: Code,Name,Type,ParentCode,Active
     *
     * @param filePath path to the CSV file
     * @return import result with counts and errors
     * @throws IOException if file cannot be read
     */
    public ImportResult importAccountsFromCsv(String filePath) throws IOException {
        logger.info("Importing accounts from CSV: {}", filePath);
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 3) {
                        String code = parts[0].trim();
                        String name = parts[1].trim();
                        AccountType type = AccountType.valueOf(parts[2].trim().toUpperCase());
                        
                        accountService.create(code, name, type, null);
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    logger.warn("Error importing account at line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        logger.info("Accounts import completed: {} success, {} errors", successCount, errors.size());
        return new ImportResult(successCount, errors);
    }

    /**
     * Imports customers from a CSV file.
     * Expected format: Code,Name,TaxId,Address,Email,Phone
     *
     * @param filePath path to the CSV file
     * @return import result with counts and errors
     * @throws IOException if file cannot be read
     */
    public ImportResult importCustomersFromCsv(String filePath) throws IOException {
        logger.info("Importing customers from CSV: {}", filePath);
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 2) {
                        String code = parts[0].trim();
                        String name = parts[1].trim();
                        String taxId = parts.length > 2 ? parts[2].trim() : null;
                        String address = parts.length > 3 ? parts[3].trim() : null;
                        String email = parts.length > 4 ? parts[4].trim() : null;
                        String phone = parts.length > 5 ? parts[5].trim() : null;

                        customerService.create(code, name, taxId, address, "", phone, email);
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    logger.warn("Error importing customer at line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        logger.info("Customers import completed: {} success, {} errors", successCount, errors.size());
        return new ImportResult(successCount, errors);
    }

    /**
     * Imports suppliers from a CSV file.
     * Expected format: Code,Name,TaxId,Address,Email,Phone
     *
     * @param filePath path to the CSV file
     * @return import result with counts and errors
     * @throws IOException if file cannot be read
     */
    public ImportResult importSuppliersFromCsv(String filePath) throws IOException {
        logger.info("Importing suppliers from CSV: {}", filePath);
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length >= 2) {
                        String code = parts[0].trim();
                        String name = parts[1].trim();
                        String taxId = parts.length > 2 ? parts[2].trim() : null;
                        String address = parts.length > 3 ? parts[3].trim() : null;
                        String email = parts.length > 4 ? parts[4].trim() : null;
                        String phone = parts.length > 5 ? parts[5].trim() : null;

                        supplierService.create(code, name, taxId, address, "", phone, email);
                        successCount++;
                    }
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                    logger.warn("Error importing supplier at line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        logger.info("Suppliers import completed: {} success, {} errors", successCount, errors.size());
        return new ImportResult(successCount, errors);
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }
}
